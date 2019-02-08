package net.oneandone.httpselftest.http.urlcon;

import net.oneandone.httpselftest.http.WireRepresentation;
import net.oneandone.httpselftest.http.TestRequest;

public class UrlConnectionRequestWireRepresentation implements WireRepresentation {

    private final TestRequest request;
    private final String finalUrl;

    public UrlConnectionRequestWireRepresentation(TestRequest request, String finalUrl) {
        this.request = request;
        this.finalUrl = finalUrl;
    }

    @Override
    public String stringValue() {
        StringBuilder sb = new StringBuilder(request.method + " " + finalUrl + "\n");
        request.headers.forEach((key, value) -> sb.append(key + ": " + value + "\n"));
        if (request.body != null) {
            sb.append("\n").append(request.body);
        }
        return sb.toString();
    }

}
