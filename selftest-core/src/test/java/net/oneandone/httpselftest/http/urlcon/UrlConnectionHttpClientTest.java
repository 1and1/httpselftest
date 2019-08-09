package net.oneandone.httpselftest.http.urlcon;

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

import java.io.IOException;
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import net.oneandone.httpselftest.test.api.TestResponse;

public class UrlConnectionHttpClientTest {

    private static final List<String> ALL_HTTP_METHODS =
            Collections.unmodifiableList(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "TRACE"));

    @Rule
    public WireMockRule wire = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    private String baseUrl;

    private UrlConnectionHttpClient client;

    private SocketMock socketMock;
    private String baseUrlSocket;

    @Before
    public void setup() throws Exception {
        baseUrl = "http://localhost:" + wire.port() + "/prefix";
        stub(200);
        client = new UrlConnectionHttpClient();

        socketMock = new SocketMock();
        baseUrlSocket = "http://localhost:" + socketMock.port() + "/prefix/";
    }

    @After
    public void cleanup() {
        assertThat(wire.findAllUnmatchedRequests()).isEmpty();
    }

    // TODO add tests for gzip, deflate, ..

    @Test
    public void minimalRequest_noBody_noHeaders() throws Exception {
        stub(200);

        // execute
        TestResponse response = client.call(baseUrl, new TestRequest("path", "GET"), "runIdX", 1000).getTestResponse();

        // verify request
        verify(1, anyRequestedFor(anyUrl()));
        LoggedRequest request = findAll(getRequestedFor(anyUrl())).get(0);
        assertThat(request.getMethod()).isEqualTo(RequestMethod.GET);
        assertThat(request.getAbsoluteUrl()).isEqualTo(baseUrl + "/path");
        assertThat(request.getHeader("X-REQUEST-ID")).isEqualTo("runIdX");
        assertThat(request.getBodyAsString()).isEqualTo("");

        // verify response
        assertThat(response).as("response").isNotNull();
        assertThat(response.getStatus()).as("status code").isEqualTo(200);
        assertThat(response.getBody()).as("body").isEqualTo("");
    }

    @Test
    public void requestWithHeaders() throws Exception {
        Map<String, String> headers = headers("A", "1", "B", "2");
        invoke(new TestRequest("path", "GET", headers));

        LoggedRequest request = findAll(getRequestedFor(anyUrl())).get(0);
        assertThat(request.header("A").values()).contains("1");
        assertThat(request.header("B").values()).contains("2");
    }

    @Test
    public void requestWithBody() throws Exception {
        String expectedBody = "!\"§$%&/()=?ßüäö²³µ|^°'`~";
        int bodyLengthInUtf8 = expectedBody.getBytes(UTF_8).length;
        assertThat(bodyLengthInUtf8).isNotEqualTo(expectedBody.length());

        invoke(new TestRequest("path", "POST", null, expectedBody));

        LoggedRequest request = wire.findAll(anyRequestedFor(anyUrl())).get(0);
        assertThat(request.getBody()).isEqualTo(expectedBody.getBytes(UTF_8));
        assertThat(request.getHeader("Content-Length")).isEqualTo(String.valueOf(bodyLengthInUtf8));
    }

    @Test
    public void responseWithHeaders() throws Exception {
        wire.stubFor(
                any(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("HeaderA", "1", "2").withHeader("HeaderB", "5")));

        TestResponse response = invoke(simpleGet()).getTestResponse();

        assertThatThrownBy(() -> response.getHeader("HeaderA")).isInstanceOf(AssertionException.class);
        assertThat(response.getHeaderAllValues("HeaderA")).containsSequence("1", "2").hasSize(2);

        assertThat(response.getHeader("HeaderB")).isEqualTo("5");
        assertThat(response.getHeaderAllValues("HeaderB")).containsSequence("5").hasSize(1);

        assertThat(response.getHeader("HeaderC")).isNull();
        assertThat(response.getHeaderAllValues("HeaderC")).isNull();
    }

    @Test
    public void responseWithBody() throws Exception {
        String expectedBody = "!\"§$%&/()=?ßüäö²³µ|^°'`~";
        wire.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(expectedBody.getBytes(UTF_8))));

        TestResponse response = invoke(simpleGet()).getTestResponse();

        assertThat(response.getBody()).isEqualTo(expectedBody);
    }

    @Test
    public void methodsWithBody() throws Exception {
        ArrayList<String> allowedWithBody = new ArrayList<>(ALL_HTTP_METHODS);
        allowedWithBody.removeAll(Arrays.asList("GET", "TRACE"));

        for (String method : allowedWithBody) {
            wire.resetAll();
            stub(200);
            invoke(new TestRequest("path", method, null, "body"));
            assertThat(findAll(anyRequestedFor(anyUrl())).get(0).getMethod().getName()).as("method: " + method).isEqualTo(method);
        }

        wire.resetAll();
        stub(200);
        assertThatThrownBy(() -> invoke(new TestRequest("path", "GET", null, "body")))
                .isInstanceOf(IllegalArgumentException.class);

        wire.resetAll();
        stub(200);
        assertThatThrownBy(() -> invoke(new TestRequest("path", "TRACE", null, "body")))
                .hasCauseInstanceOf(ProtocolException.class);
    }

    @Test
    public void methodsWithoutBody() throws Exception {
        for (String method : ALL_HTTP_METHODS) {
            wire.resetAll();
            stub(200);
            invoke(new TestRequest("path", method));
            assertThat(findAll(anyRequestedFor(anyUrl())).get(0).getMethod().getName()).as("method: " + method).isEqualTo(method);
        }
    }

    @Test
    public void methodUnknownIsNotAllowed() throws Exception {
        assertThatThrownBy(() -> invoke(new TestRequest("path", "BOOP"))).hasCauseInstanceOf(ProtocolException.class);
    }

    @Test
    public void mostStatusCodesAreParseableWithBody() throws Exception {
        for (Integer statusCode : new int[] { 200, 201, 300, 301, 302, 400, 401, 403, 404, 410, 500, 501, 502, 502, 504 }) {
            wire.resetAll();
            stub(statusCode, "body");
            TestResponse response = invoke(simpleGet()).getTestResponse();
            assertThat(response.getStatus()).as("status code for: " + statusCode).isEqualTo(statusCode);
            assertThat(response.getBody()).as("body for :" + statusCode).isEqualTo("body");
        }
    }

    @Test
    public void statusCodesThatDontParseABody() throws Exception {
        for (Integer statusCode : new int[] { 204 }) {
            wire.resetAll();
            stub(statusCode, "body");
            TestResponse response = invoke(simpleGet()).getTestResponse();
            assertThat(response.getStatus()).as("status code for: " + statusCode).isEqualTo(statusCode);
            assertThat(response.getBody()).as("body for :" + statusCode).isEqualTo("");
        }
    }

    @Test
    public void timeout() throws Exception {
        wire.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withFixedDelay(5000)));
        assertThatThrownBy(() -> invoke(simpleGet())).isInstanceOf(HttpException.class)
                .hasCauseInstanceOf(SocketTimeoutException.class);
    }

    // the exception changes between JDK8 and JDK11
    @Test
    public void faultConnectionReset() throws Exception {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withFault(Fault.CONNECTION_RESET_BY_PEER)));
        assertThatThrownBy(() -> invoke(simpleGet())).isInstanceOf(HttpException.class).hasCauseInstanceOf(SocketException.class);
    }

    @Test
    public void faultEmptyResponse() throws Exception {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withFault(Fault.EMPTY_RESPONSE)));
        assertThatThrownBy(() -> invoke(simpleGet())).isInstanceOf(HttpException.class).hasCauseInstanceOf(SocketException.class);
    }

    @Test
    public void faultMalformedChunk() throws Exception {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
        assertThatThrownBy(() -> invoke(simpleGet())).isInstanceOf(HttpException.class).hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void faultRandomData() throws Exception {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
        assertThatThrownBy(() -> invoke(simpleGet())).isInstanceOf(HttpException.class);
    }

    // TODO handle absent body in client
    public void responseParsing_noBody() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n\r\n");

        client.call(baseUrlSocket, new TestRequest("path", "GET"), "runIdX", 1000);

        assertThat(socketMock.requested()).isEqualTo("GET /prefix/path HTTP/1.1\r\n" //
                + "Host: localhost:" + socketMock.port() + "\r\n" //
                + "X-REQUEST-ID: runIdX\r\n" //
                + "\r\n");
    }

    // TODO handle identity encoding correctly. can this be fixed?
    public void responseParsing_moreBytesThanAdvertised() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n" //
                + "Transfer-Encoding: identity\r\n" //
                + "Content-Length: 7\r\n" //
                + "\r\n" //
                + "autobahn"); // 8 instead of 7 bytes

        TestResponse response = client.call(baseUrlSocket, simpleGet(), "runIdX", 1000).getTestResponse();

        assertThat(response.getBody()).isEqualTo("autobah"); // missing last byte 'n'
    }

    @Test
    public void responseParsing_identityEncoding() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n" //
                + "Transfer-Encoding: identity\r\n" //
                + "Content-Length: 8\r\n" //
                + "\r\n" //
                + "autobahn");

        TestResponse response = client.call(baseUrlSocket, simpleGet(), "runIdX", 1000).getTestResponse();

        assertThat(response.getBody()).isEqualTo("autobahn");
    }

    @Test
    public void responseParsing_identityEncodingByDefault() {
        socketMock.replyWith("HTTP/1.1 200 OK\r\n" //
                + "Content-Length: 8\r\n" //
                + "\r\n" //
                + "autobahn");

        TestResponse response = client.call(baseUrlSocket, simpleGet(), "runIdX", 1000).getTestResponse();

        assertThat(response.getBody()).isEqualTo("autobahn");
    }

    @Test
    public void responseParsing_http10() {
        socketMock.replyWith("HTTP/1.0 200 OK\r\n" //
                + "Transfer-Encoding: identity\r\n" //
                + "Content-Length: 8\r\n" //
                + "\r\n" //
                + "autobahn");

        TestResponse response = client.call(baseUrlSocket, simpleGet(), "runIdX", 1000).getTestResponse();

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

    private void stub(Integer statusCode, String body) {
        wire.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(statusCode).withBody(body)));
    }

    private UrlConnectionTestResponse invoke(TestRequest request) {
        return client.call(baseUrl, request, "irrelevant", 1000);
    }

}
