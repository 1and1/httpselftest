package net.oneandone.httpselftest.http;

import net.oneandone.httpselftest.test.api.TestResponse;

public interface FullTestResponse {

    TestResponse getTestResponse();

    String headerBlock();

    String bodyBlock();

}
