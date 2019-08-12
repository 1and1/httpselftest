package net.oneandone.httpselftest.http;

import java.util.List;

import net.oneandone.httpselftest.test.api.AssertionException;

public class TestResponse {

    private final int statuscode;

    private final Headers headers;

    private final String body;

    public TestResponse(int statusCode, Headers headers, String body) {
        this.statuscode = statusCode;
        this.headers = headers;
        this.body = body;
    }

    public int getStatus() {
        return statuscode;
    }

    public Headers getHeaders() {
        return headers;
    }

    /**
     * Get the first value associated with this header.
     *
     * @param name the HTTP header name
     * @return the first value for this HTTP header
     */
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
