package net.oneandone.httpselftest.http.urlcon;

import static net.oneandone.httpselftest.common.Constants.X_REQUEST_ID;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import net.oneandone.httpselftest.http.HttpClient;
import net.oneandone.httpselftest.http.HttpException;
import net.oneandone.httpselftest.http.TestRequest;

public class UrlConnectionHttpClient implements HttpClient {

    @Override
    public UrlConnectionTestResponse call(String baseUrl, TestRequest request, String runId, int timeout) {

        if (request.method.equalsIgnoreCase("GET") && request.body != null) {
            throw new IllegalArgumentException("Request body must be null for method GET");
        }

        String finalUrl = computePath(baseUrl, request);
        request.addWireRepresentation(new UrlConnectionRequestWireRepresentation(request, finalUrl));
        request.headers.put(X_REQUEST_ID, runId);

        URL endpoint = null;
        HttpURLConnection conn = null;
        try {
            endpoint = new URL(finalUrl);
            conn = (HttpURLConnection) endpoint.openConnection();
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            prepareAndSendRequest(request, conn);
            return parseResponse(conn);
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            throw new HttpException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void prepareAndSendRequest(TestRequest request, HttpURLConnection conn) throws IOException {
        conn.setInstanceFollowRedirects(false);
        conn.setDoInput(true);

        // method
        conn.setRequestMethod(request.method);

        // headers
        if (request.headers != null) {
            request.headers.entrySet().forEach(pair -> conn.setRequestProperty(pair.getKey(), pair.getValue()));
        }

        // body
        if (request.body != null) {
            conn.setDoOutput(true);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(request.body.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        }
    }

    private static UrlConnectionTestResponse parseResponse(HttpURLConnection conn) throws IOException {
        int statusCode = conn.getResponseCode();
        if (statusCode < 0) {
            throw new HttpException("could not parse status line: " + conn.getHeaderFieldKey(0) + "->" + conn.getHeaderField(0));
        }

        String statusLine = null;
        if (conn.getHeaderField(0).startsWith("HTTP/1.")) {
            statusLine = conn.getHeaderField(0);
        }

        // parse headers
        List<HttpHeader> headers = new LinkedList<>();
        String firstLineKey = conn.getHeaderFieldKey(0);
        if (firstLineKey != null) {
            headers.add(new HttpHeader(firstLineKey, conn.getHeaderField(0)));
        }
        for (int i = 1;; i++) {
            String headerField = conn.getHeaderField(i);
            if (headerField == null) {
                break;
            }
            headers.add(new HttpHeader(conn.getHeaderFieldKey(i), headerField));
        }

        // parse body if present
        String body = null;
        try (InputStream in = conn.getInputStream()) {
            body = consume(in, "UTF-8");
        } catch (IOException e) { // getInputStream is not allow on "error" status codes, fall-back to getErrorStream
            if (statusCode < 400) { // inputStream should have been there
                throw e;
            }
            InputStream err = conn.getErrorStream();
            if (err != null) {
                body = consume(err, "UTF-8");
            }
        }

        return new UrlConnectionTestResponse(statusCode, statusLine, headers, body);
    }

    private static String consume(InputStream in, String charset) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        int length = 0;
        while ((length = in.read(buffer)) != -1) {
            out.write(buffer, 0, length);
        }
        return out.toString(charset);
    }

    private static String computePath(String baseUrl, TestRequest request) {
        StringBuilder base = new StringBuilder(baseUrl);
        if (base.charAt(base.length() - 1) == '/') {
            base.deleteCharAt(base.length() - 1);
        }
        StringBuilder path = new StringBuilder(request.path);
        if (path.charAt(0) == '/') {
            path.deleteCharAt(0);
        }

        return base.append('/').append(path).toString();
    }

}
