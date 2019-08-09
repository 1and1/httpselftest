package net.oneandone.httpselftest.test.api;

import java.util.List;

import net.oneandone.httpselftest.http.Headers;

public class TestResponse {

    private final int statusCode;

    private final Headers headers;

    private final String body;

    public TestResponse(int statusCode, Headers headers, String body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }

    public int getStatus() {
        return statusCode;
    }

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

    public List<String> getHeaderAllValues(String headerName) {
        return headers.get(headerName);
    }

    public String getBody() {
        return body;
    }

}
