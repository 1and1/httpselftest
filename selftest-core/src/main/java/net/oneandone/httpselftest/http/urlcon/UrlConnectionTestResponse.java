package net.oneandone.httpselftest.http.urlcon;

import java.util.List;

import net.oneandone.httpselftest.http.Headers;
import net.oneandone.httpselftest.http.TestResponse;
import net.oneandone.httpselftest.test.api.AssertionException;

public class UrlConnectionTestResponse implements TestResponse {

    final int statusCode;
    final String statusLine;
    final List<HttpHeader> headersInOrder;
    final String body;

    // these are derived fields
    final Headers headers;

    public UrlConnectionTestResponse(int statusCode, String statusLine, List<HttpHeader> headersInOrder, String body) {
        this.statusCode = statusCode;
        this.statusLine = statusLine;
        this.headersInOrder = headersInOrder;
        this.body = body;

        this.headers = new Headers();
        this.headersInOrder.forEach(header -> this.headers.add(header.name, header.value));
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
        StringBuilder sb = new StringBuilder(statusLine).append('\n');
        headersInOrder.forEach(header -> sb.append(header.name + ": " + header.value + "\n"));
        if (body != null) {
            sb.append("\n").append(body);
        }
        return sb.toString();
    }

}
