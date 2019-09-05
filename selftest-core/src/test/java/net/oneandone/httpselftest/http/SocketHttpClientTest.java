package net.oneandone.httpselftest.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import net.oneandone.httpselftest.test.api.AssertionException;

public class SocketHttpClientTest {

    private static final List<String> ALL_HTTP_METHODS =
            Collections.unmodifiableList(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD", "TRACE"));

    @Rule
    public WireMockRule wire = new WireMockRule(WireMockConfiguration.options().dynamicPort());
    private String baseUrl;

    private SocketHttpClient client;

    private SocketMock socketMock;
    private String baseUrlSocket;

    @Before
    public void setup() throws Exception {
        baseUrl = "http://localhost:" + wire.port() + "/prefix/";
        stub(200);
        client = new SocketHttpClient();

        socketMock = new SocketMock();
        baseUrlSocket = "http://localhost:" + socketMock.port() + "/prefix/";
    }

    @After
    public void cleanup() {
        assertThat(wire.findAllUnmatchedRequests()).isEmpty();
    }

    @Test
    public void methodCharset() {
        for (char c = 'A'; c <= 'Z'; c++) {
            String character = new String(new char[] { c });
            invokeCharsetTest(new TestRequest("path", character));
        }
        invokeCharsetTest(new TestRequest("path", "AZ"));
        stream("äöüß§µ²abz-_/. ").forEach(c -> {
            assertThatThrownBy(() -> invokeCharsetTest(new TestRequest("path", c))).hasMessageContaining("character")
                    .hasMessageContaining(c);
        });
    }

    @Test
    public void pathCharset() {
        for (char c = ' '; c <= '~'; c++) {
            String character = new String(new char[] { c });
            invokeCharsetTest(new TestRequest(character, "POST"));
        }
        invokeCharsetTest(new TestRequest(" ~", "POST"));
        stream("äöüß§µ²").forEach(c -> {
            assertThatThrownBy(() -> invokeCharsetTest(new TestRequest(c, "POST"))).hasMessageContaining("character")
                    .hasMessageContaining(c);
        });
    }

    @Test
    public void headerCharset() {
        for (char c = ' '; c <= '~'; c++) {
            String character = new String(new char[] { c });
            invokeCharsetTest(new TestRequest("path", "POST", headers("key", character)));
            invokeCharsetTest(new TestRequest("path", "POST", headers(character, "value")));
        }
        invokeCharsetTest(new TestRequest("path", "POST", headers(" ~", " ~")));
        stream("äöüß§µ²").forEach(c -> {
            assertThatThrownBy(() -> invokeCharsetTest(new TestRequest("path", "POST", headers(c, "value"))))
                    .hasMessageContaining("character").hasMessageContaining(c);
            assertThatThrownBy(() -> invokeCharsetTest(new TestRequest("path", "POST", headers("key", c))))
                    .hasMessageContaining("character").hasMessageContaining(c);
        });
    }

    private void invokeCharsetTest(TestRequest request) {
        client.call(baseUrl, wrapped(request), 500);
    }

    private Stream<String> stream(String characters) {
        return Arrays.asList(characters.split("")).stream();
    }

    @Test
    public void minimalRequest_noBody_noHeaders() {
        stub(200);

        // execute
        TestResponse response = client.call(baseUrl, wrapped(new TestRequest("path", "GET")), 1000).response;

        // verify request
        verify(1, anyRequestedFor(anyUrl()));
        LoggedRequest request = findAll(getRequestedFor(anyUrl())).get(0);
        assertThat(request.getMethod()).isEqualTo(RequestMethod.GET);
        assertThat(request.getUrl()).isEqualTo("/prefix/path");
        assertThat(request.getHeader("Host")).isEqualTo("localhost:" + wire.port());
        assertThat(request.getBodyAsString()).isEqualTo("");

        // verify response
        assertThat(response).as("response").isNotNull();
        assertThat(response.getStatus()).as("status code").isEqualTo(200);
    }

    @Test
    public void requestLayout_noBody() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n\r\n");

        client.call(baseUrlSocket, wrapped(new TestRequest("path", "GET")), 1000);

        assertThat(socketMock.requested()).isEqualTo("GET /prefix/path HTTP/1.1\r\n" //
                + "Host: localhost:" + socketMock.port() + "\r\n" //
                + "\r\n");
    }

    @Test
    public void requestLayout_pathIsLeftUntouched() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n\r\n");

        client.call(baseUrlSocket, wrapped(new TestRequest("path?key=value&toast&encoded=%5B%5D#blob", "GET")), 1000);

        assertThat(socketMock.requested()).startsWith("GET /prefix/path?key=value&toast&encoded=%5B%5D#blob HTTP/1.1\r\n");
    }

    @Test
    public void responseParsing_moreBytesThanAdvertised() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n" //
                + "Transfer-Encoding: identity\r\n" //
                + "Content-Length: 7\r\n" //
                + "\r\n" //
                + "autobahn"); // 8 instead of 7 bytes

        TestResponse response = client.call(baseUrlSocket, wrapped(simpleGet()), 1000).response;

        assertThat(response.getBody()).isEqualTo("autobah"); // missing last byte 'n'
    }

    @Test
    public void responseParsing_identityEncoding() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n" //
                + "Transfer-Encoding: identity\r\n" //
                + "Content-Length: 8\r\n" //
                + "\r\n" //
                + "autobahn");

        TestResponse response = client.call(baseUrlSocket, wrapped(simpleGet()), 1000).response;

        assertThat(response.getBody()).isEqualTo("autobahn");
    }

    private WrappedRequest wrapped(TestRequest request) {
        return new WrappedRequest(request);
    }

    @Test
    public void responseParsing_identityEncodingByDefault() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n" //
                + "Content-Length: 8\r\n" //
                + "\r\n" //
                + "autobahn");

        TestResponse response = client.call(baseUrlSocket, wrapped(simpleGet()), 1000).response;

        assertThat(response.getBody()).isEqualTo("autobahn");
    }

    @Test
    public void responseParsing_http10() {
        socketMock.replyWith("HTTP/1.0 200 OK\r\n" //
                + "Transfer-Encoding: identity\r\n" //
                + "Content-Length: 8\r\n" //
                + "\r\n" //
                + "autobahn");

        TestResponse response = client.call(baseUrlSocket, wrapped(simpleGet()), 1000).response;

        assertThat(response.getBody()).isEqualTo("autobahn");
    }

    @Test
    public void unboundPort() {
        int unboundPort = socketMock.port() + 1;
        assertThatThrownBy(() -> {
            client.call("http://localhost:" + unboundPort + "/prefix", wrapped(simpleGet()), 1000);
        }).hasRootCauseExactlyInstanceOf(ConnectException.class);
    }

    @Test
    public void responseParsing_lessBytesThanAdvertised() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n" //
                + "Transfer-Encoding: identity\r\n" //
                + "Content-Length: 9\r\n" //
                + "\r\n" //
                + "12345678"); // missing one byte

        assertThatThrownBy(() -> client.call(baseUrlSocket, wrapped(simpleGet()), 1000))
                .hasRootCauseExactlyInstanceOf(SocketTimeoutException.class);
    }

    @Test
    public void responseParsing_unknownTransferEncoding() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n" //
                + "Transfer-Encoding: deflate\r\n" //
                + "\r\n");

        assertThatThrownBy(() -> client.call(baseUrlSocket, wrapped(simpleGet()), 1000))
                .hasMessageContaining("Transfer-Encoding 'deflate'");
    }

    @Test
    public void requestWithHeaders() {
        Headers headers = headers("A", "1", "A", "2", "B", "3");
        invoke(new TestRequest("path", "GET", headers));

        LoggedRequest request = findAll(getRequestedFor(anyUrl())).get(0);
        assertThat(request.header("A").values()).containsExactly("1", "2");
        assertThat(request.header("B").values()).containsExactly("3");
    }

    @Test
    public void requestWithBody() {
        String expectedBody = "!\"§$%&/()=?ßüäö²³µ|^°'`~";
        int bodyLengthInUtf8 = expectedBody.getBytes(UTF_8).length;
        assertThat(bodyLengthInUtf8).isNotEqualTo(expectedBody.length());

        invoke(new TestRequest("path", "POST", headers(), expectedBody));

        LoggedRequest requested = sentRequest();
        assertThat(requested.getBody()).isEqualTo(expectedBody.getBytes(UTF_8));
        assertThat(requested.getHeader("Content-Length")).isEqualTo(String.valueOf(bodyLengthInUtf8));
    }

    @Test
    public void responseWithHeaders() {
        wire.stubFor(
                any(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("HeaderA", "1", "2").withHeader("HeaderB", "5")));

        TestResponse response = invoke(simpleGet()).response;

        assertThatThrownBy(() -> response.getHeader("HeaderA")).isInstanceOf(AssertionException.class);
        assertThat(response.getHeaderAllValues("HeaderA")).containsSequence("1", "2").hasSize(2);

        assertThat(response.getHeader("HeaderB")).isEqualTo("5");
        assertThat(response.getHeaderAllValues("HeaderB")).containsSequence("5").hasSize(1);

        assertThat(response.getHeader("HeaderC")).isNull();
        assertThat(response.getHeaderAllValues("HeaderC")).isNull();
    }

    @Test
    public void responseWithoutBody() {
        TestResponse response = invoke(simpleGet()).response;

        assertThat(response.getBody()).isEqualTo("");
    }

    @Test
    public void responseWithEmptyBody() {
        stub(200, "");

        TestResponse response = invoke(simpleGet()).response;
        assertThat(response.getBody()).isEqualTo("");
    }

    @Test
    public void responseWithBody() {
        String expectedBody = "!\"§$%&/()=?ßüäö²³µ|^°'`~";
        wire.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(expectedBody.getBytes(UTF_8))));

        TestResponse response = invoke(simpleGet()).response;

        assertThat(response.getBody()).isEqualTo(expectedBody);
    }

    @Test
    public void requestBodyByMethod() {
        for (String method : ALL_HTTP_METHODS) {
            wire.resetAll();
            stub(200);

            invoke(new TestRequest("path", method, headers(), "body"));
            assertThat(sentRequest().getMethod().getName()).as("method: " + method).isEqualTo(method);
        }
    }

    @Test
    public void emptyRequestBodyByMethod() {
        for (String method : ALL_HTTP_METHODS) {
            wire.resetAll();
            stub(200);

            invoke(new TestRequest("path", method));
            assertThat(sentRequest().getMethod().getName()).as("method: " + method).isEqualTo(method);
        }
    }

    @Test
    public void responseBodyByMethod() {
        for (String method : ALL_HTTP_METHODS) {
            wire.resetAll();
            stub(200, "body");

            TestResponse response = invoke(new TestRequest("path", method)).response;
            if (method.equals("HEAD")) {
                assertThat(response.getBody()).as("method: " + method).isEqualTo("");
            } else {
                assertThat(response.getBody()).as("method: " + method).isEqualTo("body");
            }
        }
    }

    @Test
    public void responseBodyParsingByStatusCode() {
        for (int statusCode : new int[] { 100, 200, 201, 204, 300, 301, 302, 304, 400, 401, 403, 404, 410, 500, 501, 502, 502,
                504 }) {
            wire.resetAll();
            stub(statusCode, "body");

            TestResponse response = invoke(simpleGet()).response;
            assertThat(response.getStatus()).as("status code for: " + statusCode).isEqualTo(statusCode);
            if (statusCode == 100 || statusCode == 204 || statusCode == 304) {
                assertThat(response.getBody()).as("body for: " + statusCode).isEqualTo("");
            } else {
                assertThat(response.getBody()).as("body for: " + statusCode).isEqualTo("body");
            }
        }
    }

    @Test
    public void timeout() {
        wire.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withFixedDelay(5000)));
        assertThatThrownBy(() -> invoke(simpleGet())).isInstanceOf(HttpException.class)
                .hasCauseInstanceOf(SocketTimeoutException.class);
    }

    // the exception changes between JDK8 and JDK11
    @Test
    public void faultConnectionReset() {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withFault(Fault.CONNECTION_RESET_BY_PEER)));
        assertThatThrownBy(() -> invoke(simpleGet())).isInstanceOf(HttpException.class).hasCauseInstanceOf(SocketException.class);
    }

    @Test
    public void faultEmptyResponse() {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withFault(Fault.EMPTY_RESPONSE)));
        assertThatThrownBy(() -> invoke(simpleGet())).isInstanceOf(HttpException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    public void faultMalformedChunk() {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
        assertThatThrownBy(() -> invoke(simpleGet())).isInstanceOf(HttpException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    public void faultRandomData() {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
        assertThatThrownBy(() -> invoke(simpleGet())).isInstanceOf(HttpException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    public void httpDetails() {
        WrappedRequest req = new WrappedRequest(new TestRequest("path", "GET", new Headers(), "request corgi"));
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody("reeesponse body")));

        WrappedResponse resp = client.call(baseUrl, req, 1_000);

        assertThat(req.details).isNotNull();
        assertThat(req.details.bodyBlock()).isEqualTo("request corgi");
        assertThat(req.details.headerBlock()).contains("Content-Length: ");
        assertThat(resp.responseDetails).isNotNull();
        assertThat(resp.responseDetails.bodyBlock()).isEqualTo("reeesponse body");
        assertThat(resp.responseDetails.headerBlock()).contains("Transfer-Encoding: chunked");
    }

    @Test
    public void httpDetailsOnException() {
        WrappedRequest req = new WrappedRequest(new TestRequest("path", "GET", new Headers(), "request body"));
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withFixedDelay(1500)));

        assertThatThrownBy(() -> client.call(baseUrl, req, 1_000)).isInstanceOfSatisfying(HttpException.class, e -> {
            assertThat(e.getBytes()).isEmpty(); // got no response
        });
        assertThat(req.details).isNotNull();
        assertThat(req.details.bodyBlock()).isEqualTo("request body");
    }

    private LoggedRequest sentRequest() {
        List<LoggedRequest> sentRequests = findAll(anyRequestedFor(anyUrl()));
        assertThat(sentRequests).as("sanity check: number of requests sent").hasSize(1);
        return sentRequests.get(0);
    }

    private TestRequest simpleGet() {
        return new TestRequest("path", "GET");
    }

    private static Headers headers(String... keysAndValues) {
        assertThat(keysAndValues.length % 2).as("unbalanced header key-value vararg").isZero();
        Headers headers = new Headers();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            headers.add(keysAndValues[i], keysAndValues[i + 1]);
        }
        return headers;
    }

    private void stub(int statusCode) {
        wire.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(statusCode)));
    }

    private void stub(int statusCode, String body) {
        wire.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(statusCode).withBody(body)));
    }

    private WrappedResponse invoke(TestRequest request) {
        WrappedRequest wrapper = new WrappedRequest(request);
        return client.call(baseUrl, wrapper, 2000);
    }

}
