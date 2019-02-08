package net.oneandone.httpselftest.http.socket;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.oneandone.httpselftest.http.HttpException;
import net.oneandone.httpselftest.http.SocketMock;
import net.oneandone.httpselftest.http.TestRequest;
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
            invokeCharsetTest(new TestRequest("path", "POST", singletonMap("key", character)));
            invokeCharsetTest(new TestRequest("path", "POST", singletonMap(character, "value")));
        }
        invokeCharsetTest(new TestRequest("path", "POST", singletonMap(" ~", " ~")));
        stream("äöüß§µ²").forEach(c -> {
            assertThatThrownBy(() -> invokeCharsetTest(new TestRequest("path", "POST", singletonMap(c, "value"))))
                    .hasMessageContaining("character").hasMessageContaining(c);
            assertThatThrownBy(() -> invokeCharsetTest(new TestRequest("path", "POST", singletonMap("key", c))))
                    .hasMessageContaining("character").hasMessageContaining(c);
        });
    }

    private void invokeCharsetTest(TestRequest request) {
        client.call(baseUrl, request, "anyRunId", 500);
    }

    private Stream<String> stream(String characters) {
        return Arrays.asList(characters.split("")).stream();
    }

    @Test
    public void minimalRequest_noBody_noHeaders() {
        stub(200);

        // execute
        SocketTestResponse response = client.call(baseUrl, new TestRequest("path", "GET"), "runIdX", 1000);

        // verify request
        verify(1, anyRequestedFor(anyUrl()));
        LoggedRequest request = findAll(getRequestedFor(anyUrl())).get(0);
        assertThat(request.getMethod()).isEqualTo(RequestMethod.GET);
        assertThat(request.getUrl()).isEqualTo("/prefix/path");
        assertThat(request.getHeader("Host")).isEqualTo("localhost:" + wire.port());
        assertThat(request.getHeader("X-REQUEST-ID")).isEqualTo("runIdX");
        assertThat(request.getBodyAsString()).isEqualTo("");

        // verify response
        assertThat(response).as("response").isNotNull();
        assertThat(response.getStatus()).as("status code").isEqualTo(200);
    }

    @Test
    public void responseParsing_noBody() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n\r\n");

        client.call(baseUrlSocket, new TestRequest("path", "GET"), "runIdX", 1000);

        assertThat(socketMock.requested()).isEqualTo("GET /prefix/path HTTP/1.1\r\n" //
                + "Host: localhost:" + socketMock.port() + "\r\n" //
                + "X-REQUEST-ID: runIdX\r\n" //
                + "\r\n");
    }

    @Test
    public void responseParsing_moreBytesThanAdvertised() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n" //
                + "Transfer-Encoding: identity\r\n" //
                + "Content-Length: 7\r\n" //
                + "\r\n" //
                + "autobahn"); // 8 instead of 7 bytes

        SocketTestResponse response = client.call(baseUrlSocket, simpleGet(), "runIdX", 1000);

        assertThat(response.getBody()).isEqualTo("autobah"); // missing last byte 'n'
    }

    @Test
    public void responseParsing_identityEncoding() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n" //
                + "Transfer-Encoding: identity\r\n" //
                + "Content-Length: 8\r\n" //
                + "\r\n" //
                + "autobahn");

        SocketTestResponse response = client.call(baseUrlSocket, simpleGet(), "runIdX", 1000);

        assertThat(response.getBody()).isEqualTo("autobahn");
    }

    @Test
    public void responseParsing_identityEncodingByDefault() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n" //
                + "Content-Length: 8\r\n" //
                + "\r\n" //
                + "autobahn");

        SocketTestResponse response = client.call(baseUrlSocket, simpleGet(), "runIdX", 1000);

        assertThat(response.getBody()).isEqualTo("autobahn");
    }

    @Test
    public void responseParsing_http10() {
        socketMock.replyWith("HTTP/1.0 200 OK\r\n" //
                + "Transfer-Encoding: identity\r\n" //
                + "Content-Length: 8\r\n" //
                + "\r\n" //
                + "autobahn");

        SocketTestResponse response = client.call(baseUrlSocket, simpleGet(), "runIdX", 1000);

        assertThat(response.getBody()).isEqualTo("autobahn");
    }

    @Test
    public void unboundPort() {
        int unboundPort = socketMock.port() + 1;
        assertThatThrownBy(() -> {
            client.call("http://localhost:" + unboundPort + "/prefix", simpleGet(), "runIdX", 1000);
        }).hasRootCauseExactlyInstanceOf(ConnectException.class);
    }

    @Test
    public void responseParsing_lessBytesThanAdvertised() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n" //
                + "Transfer-Encoding: identity\r\n" //
                + "Content-Length: 9\r\n" //
                + "\r\n" //
                + "12345678"); // missing one byte

        assertThatThrownBy(() -> client.call(baseUrlSocket, simpleGet(), "runIdX", 1000))
                .hasRootCauseExactlyInstanceOf(SocketTimeoutException.class);
    }

    @Test
    public void responseParsing_unknownTransferEncoding() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n" //
                + "Transfer-Encoding: deflate\r\n" //
                + "\r\n");

        assertThatThrownBy(() -> client.call(baseUrlSocket, simpleGet(), "runIdX", 1000))
                .hasMessageContaining("Transfer-Encoding 'deflate'");
    }

    @Test
    public void requestWithHeaders() {
        Map<String, String> headers = headers("A", "1", "B", "2");
        invoke(new TestRequest("path", "GET", headers));

        LoggedRequest request = findAll(getRequestedFor(anyUrl())).get(0);
        assertThat(request.header("A").values()).contains("1");
        assertThat(request.header("B").values()).contains("2");
    }

    @Test
    public void requestWithBody() {
        String expectedBody = "!\"§$%&/()=?ßüäö²³µ|^°'`~";
        int bodyLengthInUtf8 = expectedBody.getBytes(UTF_8).length;
        assertThat(bodyLengthInUtf8).isNotEqualTo(expectedBody.length());

        invoke(new TestRequest("path", "POST", null, expectedBody));

        verify(1, anyRequestedFor(anyUrl()));
        LoggedRequest request = sentRequest();
        assertThat(request.getBody()).isEqualTo(expectedBody.getBytes(UTF_8));
        assertThat(request.getHeader("Content-Length")).isEqualTo(String.valueOf(bodyLengthInUtf8));
    }

    @Test
    public void responseWithHeaders() {
        wire.stubFor(
                any(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("HeaderA", "1", "2").withHeader("HeaderB", "5")));

        SocketTestResponse response = invoke(simpleGet());

        assertThatThrownBy(() -> response.getHeader("HeaderA")).isInstanceOf(AssertionException.class);
        assertThat(response.getHeaderAllValues("HeaderA")).containsSequence("1", "2").hasSize(2);

        assertThat(response.getHeader("HeaderB")).isEqualTo("5");
        assertThat(response.getHeaderAllValues("HeaderB")).containsSequence("5").hasSize(1);

        assertThat(response.getHeader("HeaderC")).isNull();
        assertThat(response.getHeaderAllValues("HeaderC")).isNull();
    }

    @Test
    public void responseWithoutBody() {
        SocketTestResponse response = invoke(simpleGet());

        assertThat(response.getBody()).isEqualTo("");
    }

    @Test
    public void responseWithEmptyBody() {
        stub(200, "");

        SocketTestResponse response = invoke(simpleGet());
        assertThat(response.getBody()).isEqualTo("");
    }

    @Test
    public void responseWithBody() {
        String expectedBody = "!\"§$%&/()=?ßüäö²³µ|^°'`~";
        wire.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(expectedBody.getBytes(UTF_8))));

        SocketTestResponse response = invoke(simpleGet());

        assertThat(response.getBody()).isEqualTo(expectedBody);
    }

    @Test
    public void requestBodyByMethod() {
        for (String method : ALL_HTTP_METHODS) {
            wire.resetAll();
            stub(200);

            invoke(new TestRequest("path", method, null, "body"));
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

            SocketTestResponse response = invoke(new TestRequest("path", method));
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

            SocketTestResponse response = invoke(simpleGet());
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

    private LoggedRequest sentRequest() {
        return findAll(anyRequestedFor(anyUrl())).get(0);
    }

    private TestRequest simpleGet() {
        return new TestRequest("path", "GET");
    }

    private static HashMap<String, String> headers(String... keysAndValues) {
        assertThat(keysAndValues.length % 2).as("unbalanced header key-value vararg").isZero();
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    private void stub(int statusCode) {
        wire.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(statusCode)));
    }

    private void stub(int statusCode, String body) {
        wire.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(statusCode).withBody(body)));
    }

    private SocketTestResponse invoke(TestRequest request) {
        return client.call(baseUrl, request, "irrelevant", 2000);
    }

}
