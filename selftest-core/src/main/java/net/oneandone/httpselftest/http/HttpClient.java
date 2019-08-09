package net.oneandone.httpselftest.http;

public interface HttpClient {

    FullTestResponse call(String baseUrl, TestRequest request, String runId, int timeoutMillis);

    enum Type {
        URLCON, SOCKET;
    }

}
