package net.oneandone.httpselftest.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

public class UrlConnectionHttpClient implements HttpClient {

    @Override
    public WrappedResponse call(String baseUrl, WrappedRequest requestw, int timeout) {

        TestRequest request = requestw.request;
        if (request.method.equalsIgnoreCase("GET") && request.body != null) {
            throw new IllegalArgumentException("Request body must be null for method GET");
        }
        rejectMultiHeaders(request.headers);

        String finalUrl = concatAvoidingDuplicateSlash(baseUrl, request.path);

        URL endpoint = null;
        HttpURLConnection conn = null;
        try {
            endpoint = new URL(finalUrl);
            conn = (HttpURLConnection) endpoint.openConnection();
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);

            prepareAndSendRequest(request, conn, finalUrl, requestw);
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

    private static void prepareAndSendRequest(TestRequest request, HttpURLConnection conn, String finalUrl,
            WrappedRequest requestw) throws IOException {
        conn.setInstanceFollowRedirects(false);
        conn.setDoInput(true);

        // method
        conn.setRequestMethod(request.method);

        // headers
        if (request.headers != null) {
            request.headers.stream().forEach(pair -> conn.setRequestProperty(pair.left, pair.right));
        }

        requestw.details = new DataBasedHttpDetails(request.method + " " + finalUrl, request.headers, request.body);

        // body
        if (request.body != null) {
            conn.setDoOutput(true);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(request.body.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        }
    }

    private static WrappedResponse parseResponse(HttpURLConnection conn) throws IOException {
        int statusCode = conn.getResponseCode();
        if (statusCode < 0) {
            throw new HttpException("could not parse status line: " + conn.getHeaderFieldKey(0) + "->" + conn.getHeaderField(0));
        }

        String statusLine = null;
        if (conn.getHeaderField(0).startsWith("HTTP/1.")) {
            statusLine = conn.getHeaderField(0);
        }

        // parse headers
        Headers headers = new Headers();
        String firstLineKey = conn.getHeaderFieldKey(0);
        if (firstLineKey != null) {
            headers.add(firstLineKey, conn.getHeaderField(0));
        }
        for (int i = 1;; i++) {
            String headerField = conn.getHeaderField(i);
            if (headerField == null) {
                break;
            }
            headers.add(conn.getHeaderFieldKey(i), headerField);
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

        DataBasedHttpDetails responseDetails = new DataBasedHttpDetails(statusLine, headers, body);
        TestResponse response = new TestResponse(statusCode, headers, body);
        return new WrappedResponse(response, responseDetails);
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

    private void rejectMultiHeaders(Headers headers) {
        Map<String, Long> headerCounts =
                headers.stream().collect(Collectors.groupingBy(pair -> pair.left.toLowerCase(), Collectors.counting()));
        Optional<Entry<String, Long>> duplicateHeader =
                headerCounts.entrySet().stream().filter(entry -> entry.getValue() > 1).findFirst();
        if (duplicateHeader.isPresent()) {
            throw new IllegalArgumentException(
                    UrlConnectionHttpClient.class.getSimpleName() + " does not support sending the same Header multiple times. "
                            + "If necessary, concatenate values with commas or use another client. Header in question: "
                            + duplicateHeader.get().getKey());
        }
    }

    public static String concatAvoidingDuplicateSlash(String baseUrl, String requestPath) {
        StringBuilder base = new StringBuilder(baseUrl);
        if (base.length() > 0 && base.charAt(base.length() - 1) == '/') {
            base.deleteCharAt(base.length() - 1);
        }
        StringBuilder path = new StringBuilder(requestPath);
        if (path.length() > 0 && path.charAt(0) == '/') {
            path.deleteCharAt(0);
        }

        if (path.length() > 0) {
            base.append('/').append(path);
        }

        return base.toString();
    }

}
