package net.oneandone.httpselftest.http;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

import net.oneandone.httpselftest.http.HttpClient.Type;

public class TestRequest {

    // provided by the test case
    public final String path;
    public final String method;
    public final Map<String, String> headers;
    public final String body;

    // collected by client during call
    private WireRepresentation wireRepresentation;

    public HttpClient.Type clientType = Type.SOCKET; // default

    /**
     * Create a request without significant headers and without body. See {@link #TestRequest(String, String, Map, String)}.
     *
     * @param path   the HTTP path suffix
     * @param method the HTTP verb
     */
    public TestRequest(String path, String method) {
        this(path, method, null, null);
    }

    /**
     * Create a request without body. See {@link #TestRequest(String, String, Map, String)}.
     *
     * @param path    the HTTP path suffix
     * @param method  the HTTP verb
     * @param headers a map of HTTP headers to values
     */
    public TestRequest(String path, String method, Map<String, String> headers) {
        this(path, method, headers, null);
    }

    /**
     * Create an arbitrary HTTP request. The path suffix is appended to the configured endpoint.<br>
     * <br>
     * The {@code SOCKET} client is used by default. It produces a reliable wire protocol but does not support all HTTP features.
     * The {@code URLCON} client is more feature complete but the wire protocol will be missing some details. The client can be
     * changed after construction. If using {@code URLCON} client, GET requests may not contain a body.
     *
     * @param path    the HTTP path suffix
     * @param method  the HTTP verb
     * @param headers a map of HTTP headers to values
     * @param body    the body
     */
    public TestRequest(String path, String method, Map<String, String> headers, String body) {
        requireNonNull(path, "Request path may not be null");
        requireNonNull(method, "Request method may not be null");

        this.path = path;
        this.method = method;
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
        this.body = body;
    }

    public void addWireRepresentation(WireRepresentation wireRepresentation) {
        this.wireRepresentation = wireRepresentation;
    }

    public String wireRepresentation() {
        return wireRepresentation == null ? "nothing sent" : wireRepresentation.stringValue();
    }

}
