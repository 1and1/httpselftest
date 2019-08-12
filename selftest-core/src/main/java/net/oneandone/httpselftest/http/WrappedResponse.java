package net.oneandone.httpselftest.http;

import java.util.Objects;

public class WrappedResponse {

    public final TestResponse response;
    public final HttpDetails responseDetails;

    public WrappedResponse(TestResponse response, HttpDetails responseDetails) {
        Objects.requireNonNull(response, "response may not be null");
        Objects.requireNonNull(responseDetails, "responseDetails may not be null");
        this.response = response;
        this.responseDetails = responseDetails;
    }

}
