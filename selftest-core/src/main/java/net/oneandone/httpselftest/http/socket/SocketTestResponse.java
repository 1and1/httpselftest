package net.oneandone.httpselftest.http.socket;

import java.nio.charset.StandardCharsets;
import java.util.List;

import net.oneandone.httpselftest.http.Headers;
import net.oneandone.httpselftest.http.TestResponse;
import net.oneandone.httpselftest.test.api.AssertionException;

public class SocketTestResponse implements TestResponse {

    final int statusCode;
    final Headers headers;
    final String body;

    byte[] wireBytes;

    public SocketTestResponse(int statusCode, Headers headers, String body) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
    }

    @Override
    public int getStatus() {
        return statusCode;
    }

    @Override
    public String getHeader(String name) {
        List<String> values = headers.get(name);
        if (values == null) {
            return null;
        }
        if (values.size() != 1) {
            throw new AssertionException("expecting header [" + name + "] to be single-valued, but was: " + values);
        }
        return values.get(0);
    }

    @Override
    public List<String> getHeaderAllValues(String name) {
        return headers.get(name);
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public String wireRepresentation() {
        return new String(wireBytes, StandardCharsets.UTF_8);
    }

}
