package net.oneandone.httpselftest.http.socket;

import java.nio.charset.StandardCharsets;

import net.oneandone.httpselftest.http.Headers;
import net.oneandone.httpselftest.http.FullTestResponse;
import net.oneandone.httpselftest.test.api.TestResponse;

public class SocketTestResponse implements FullTestResponse {

    final int statusCode;
    final Headers headers;
    final String body;

    public byte[] headerBytes; // NOSONAR
    public byte[] bodyBytes; // NOSONAR

    public SocketTestResponse(int statusCode, Headers headers, String body) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
    }

    @Override
    public TestResponse getTestResponse() {
        return new TestResponse(statusCode, headers, body);
    }

    @Override
    public String headerBlock() {
        return new String(headerBytes, StandardCharsets.US_ASCII);
    }

    @Override
    public String bodyBlock() {
        if (bodyBytes == null || bodyBytes.length <= 0) {
            return null;
        }
        return new String(bodyBytes, StandardCharsets.UTF_8); // TODO honor response charset
    }

}
