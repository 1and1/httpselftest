package net.oneandone.httpselftest.http;

public interface HttpClient {

    WrappedResponse call(String baseUrl, WrappedRequest request, int timeoutMillis);

    enum Type {
        URLCON, SOCKET;
    }

}
