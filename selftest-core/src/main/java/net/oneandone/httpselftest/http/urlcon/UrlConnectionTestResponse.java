package net.oneandone.httpselftest.http.urlcon;

import java.util.List;

import net.oneandone.httpselftest.http.Headers;
import net.oneandone.httpselftest.http.FullTestResponse;
import net.oneandone.httpselftest.test.api.TestResponse;

public class UrlConnectionTestResponse implements FullTestResponse {

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
    public TestResponse getTestResponse() {
        return new TestResponse(statusCode, headers, body);
    }

    @Override
    public String headerBlock() {
        StringBuilder sb = new StringBuilder(statusLine).append('\n');
        headersInOrder.forEach(header -> sb.append(header.name + ": " + header.value + "\n"));
        if (body != null) {
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String bodyBlock() {
        return body;
    }

}
