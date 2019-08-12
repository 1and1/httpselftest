package net.oneandone.httpselftest.http;

import java.util.Objects;

public class WrappedRequest {

    public final TestRequest request;
    HttpDetails details;

    public WrappedRequest(TestRequest request) {
        Objects.requireNonNull(request, "request may not be null");
        this.request = request;
    }

    public HttpDetails getDetails() {
        return details;
    }
}
