package net.oneandone.httpselftest.http;

public interface HttpClient {

    TestResponse call(String baseUrl, TestRequest request, String runId, int timeoutMillis);

    enum Type {
        URLCON, SOCKET;
    }

}
