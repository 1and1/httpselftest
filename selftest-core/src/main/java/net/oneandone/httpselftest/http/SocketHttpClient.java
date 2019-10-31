package net.oneandone.httpselftest.http;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.oneandone.httpselftest.http.UrlConnectionHttpClient.concatAvoidingDuplicateSlash;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import net.oneandone.httpselftest.common.Pair;

public class SocketHttpClient implements HttpClient {

    @Override
    public WrappedResponse call(String baseUrl, WrappedRequest requestw, int timeoutMillis) {
        try (Socket socket = new Socket()) {
            TestRequest request = requestw.request;
            checkMethodCharset(request.method);
            checkPathCharset(request.path);
            checkHeadersCharset(request.headers);

            URL endpoint = new URL(baseUrl);

            int port = endpoint.getPort();
            if (port < 0) {
                throw new IllegalArgumentException("No port provided: " + baseUrl);
            }
            String hostname = endpoint.getHost();
            String path = concatAvoidingDuplicateSlash(endpoint.getPath(), request.path);

            socket.connect(new InetSocketAddress(hostname, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);

            sendRequest(socket.getOutputStream(), hostname, port, path, request, requestw);
            return parseResponse(capturedInputStream(socket.getInputStream()), request.method);
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            throw new HttpException(e);
        }
    }

    private static void sendRequest(OutputStream out, String host, int port, String path, TestRequest request,
            WrappedRequest wrapper) throws IOException {
        Pair<byte[], byte[]> requestBytes = prepareRequest(host, port, path, request);
        wrapper.details = new WireBasedHttpDetails(requestBytes.left, requestBytes.right);

        out.write(requestBytes.left);
        if (requestBytes.right != null) {
            out.write(requestBytes.right);
        }
        out.flush();
    }

    private static Pair<byte[], byte[]> prepareRequest(String hostname, int port, String path, TestRequest request) {
        Bytes headerBytes = new Bytes();

        headerBytes.appendLine(request.method + " " + path + " " + "HTTP/1.1");

        headerBytes.appendLine("Host: " + hostname + ":" + port);

        request.headers.stream().forEach(pair -> headerBytes.appendLine(pair.left + ": " + pair.right));

        if (request.body == null) {
            headerBytes.appendCrLf();
            return new Pair<>(headerBytes.toArray(), null);
        } else {
            byte[] bodyBytes = request.body.getBytes(UTF_8);
            headerBytes.appendLine("Content-Length: " + bodyBytes.length);
            headerBytes.appendCrLf();

            return new Pair<>(headerBytes.toArray(), bodyBytes);
        }
    }

    private static WrappedResponse parseResponse(CapturingInputStream in, String requestMethod) throws HttpException {
        try {

            List<String> headerList = consumeHeaders(in);
            byte[] headerBytes = in.readBytes();

            int statusCode = parseStatusCode(headerList.get(0));

            Headers headers = new Headers();
            for (int i = 1; i < headerList.size(); i++) { // skip status line
                String headerLine = headerList.get(i);
                String[] keyValue = headerLine.split(":", 2);
                if (keyValue.length != 2) {
                    throw new IllegalStateException("Could not parse header line: " + headerLine);
                }
                headers.add(keyValue[0], keyValue[1].trim());
            }

            byte[] bodyBytes;
            if (bodyIsNotAllowedAccordingToRfc(statusCode, requestMethod)) {
                bodyBytes = new byte[0];
            } else {
                bodyBytes = consumeBody(headers, in);
            }
            String body = new String(bodyBytes, UTF_8);

            TestResponse response = new TestResponse(statusCode, headers, body);
            HttpDetails responseDetails = new WireBasedHttpDetails(headerBytes, bodyBytes);
            return new WrappedResponse(response, responseDetails);
        } catch (Exception e) {
            throw new HttpException(e, in.readBytes());
        }
    }

    // no body on HEAD, 1xx, 204, 304; see https://tools.ietf.org/html/rfc7230#section-3.3.3
    private static boolean bodyIsNotAllowedAccordingToRfc(int status, String method) {
        return method.equals("HEAD") || status == 204 || status == 304 || status / 100 == 1;
    }

    private static CapturingInputStream capturedInputStream(InputStream in) {
        return new CapturingInputStream(in);
    }

    private static Optional<String> getLastValue(Headers headers, String headerName) {
        List<String> values = headers.get(headerName);
        return values == null ? Optional.empty() : Optional.of(values.get(values.size() - 1));
    }

    private static byte[] consumeBody(Headers headers, InputStream in) throws IOException {
        String txEncoding = getLastValue(headers, "Transfer-Encoding").orElse("identity").toLowerCase();

        switch (txEncoding) {
            case "chunked":
                return consumeBodyChunked(in);
            case "identity":
                return consumeBodyIdentity(headers, in);
            default:
                throw new IllegalStateException("This HTTP client does not implement Transfer-Encoding '" + txEncoding + "'.");
        }
    }

    private static byte[] consumeBodyChunked(InputStream in) throws IOException {
        byte[] result = new byte[0];
        int chunkSize = 0;
        do {
            chunkSize = readChunkSize(in);
            byte[] chunk = readChunk(in, chunkSize);
            result = concat(result, chunk);
        } while (chunkSize > 0);
        return result;
    }

    private static byte[] consumeBodyIdentity(Headers headers, InputStream in) throws IOException {
        Optional<String> contentLength = getLastValue(headers, "Content-Length");
        if (!contentLength.isPresent()) {
            return new byte[0];
        }
        return readNumberOfBytes(in, Integer.parseInt(contentLength.get(), 10));
    }

    private static int readChunkSize(InputStream in) throws IOException {
        byte[] bytes = dropLastTwo(readUntilCrLf(in));
        return Integer.parseInt(new String(bytes, UTF_8), 16);
    }

    private static byte[] dropLastTwo(byte[] bytes) {
        return Arrays.copyOf(bytes, bytes.length - 2);
    }

    private static byte[] readUntilCrLf(InputStream in) throws IOException {
        List<Byte> byteList = new ArrayList<>();
        do {
            int read = in.read();
            if (read == -1) {
                throw new IllegalStateException("unexpected end of stream; encountered -1 before CRLF");
            }
            byteList.add((byte) read);
        } while (!endingWithCrLf(byteList));
        return listToArray(byteList);
    }

    private static byte[] readChunk(InputStream in, int chunkSize) throws IOException {
        byte[] bytes = readNumberOfBytes(in, chunkSize);
        byte[] chunkEnding = dropLastTwo(readUntilCrLf(in));
        if (chunkEnding.length > 0) {
            throw new IllegalStateException("expected chunk delimiter, but found: " + Arrays.toString(chunkEnding));
        }
        return bytes;
    }

    private static byte[] readNumberOfBytes(InputStream in, int bytesToRead) throws IOException {
        byte[] result = new byte[0];
        byte[] buffer = new byte[bytesToRead];
        int length = 0;
        int lengthTotal = 0;
        while (lengthTotal < bytesToRead && (length = in.read(buffer)) != -1) {
            lengthTotal += length;
            result = concat(result, Arrays.copyOf(buffer, length));
        }
        if (lengthTotal != bytesToRead) {
            throw new IllegalStateException("unexpected end of file. expected: " + bytesToRead + "bytes, got: " + lengthTotal);
        }
        return result;
    }

    private static int parseStatusCode(String statusLine) {
        String[] split = statusLine.split(" ");
        if (split.length < 2) {
            throw new IllegalStateException("unrecognized status line: " + statusLine);
        }
        return Integer.valueOf(split[1]);
    }

    private static List<String> consumeHeaders(InputStream in) throws IOException {
        List<String> headerLines = new ArrayList<>();
        byte[] line;
        do {
            line = dropLastTwo(readUntilCrLf(in));
            if (line.length > 0) {
                headerLines.add(new String(line, UTF_8));
            }
        } while (line.length > 0);

        return headerLines;
    }

    private static byte[] listToArray(List<Byte> byteList) {
        int numBytes = byteList.size();
        byte[] byteArray = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            byteArray[i] = byteList.get(i);
        }
        return byteArray;
    }

    private static boolean endingWithCrLf(List<Byte> bytes) {
        int numBytes = bytes.size();
        return numBytes >= 2 && bytes.get(numBytes - 2) == '\r' && bytes.get(numBytes - 1) == '\n';
    }

    public static byte[] concat(byte[] first, byte[] second) {
        byte[] combined = new byte[first.length + second.length];
        System.arraycopy(first, 0, combined, 0, first.length);
        System.arraycopy(second, 0, combined, first.length, second.length);
        return combined;
    }

    private static void checkMethodCharset(String method) {
        checkCharset(method, c -> 'A' <= c && c <= 'Z');
    }

    private static void checkPathCharset(String path) {
        checkCharset(path, c -> ' ' <= c && c <= '~');
    }

    private void checkHeadersCharset(Headers headers) {
        if (headers != null) {
            headers.stream().forEach(pair -> {
                checkPathCharset(pair.left);
                checkPathCharset(pair.right);
            });
        }
    }

    private static void checkCharset(String string, Predicate<Integer> p) {
        string.codePoints().forEach(c -> {
            if (!p.test(c)) {
                throw new IllegalArgumentException("Illegal character: " + new String(Character.toChars(c)));
            }
        });
    }

    public static class Bytes {

        private byte[] buffer = new byte[0];

        public void append(String msg) {
            append(msg.getBytes(UTF_8));
        }

        public void append(byte[] bytes) {
            buffer = concat(buffer, bytes);
        }

        public void appendLine(String line) {
            append(line);
            appendCrLf();
        }

        public void appendCrLf() {
            buffer = concat(buffer, "\r\n".getBytes(UTF_8));
        }

        public byte[] toArray() {
            return concat(buffer, new byte[0]);
        }

    }

    private static class CapturingInputStream extends InputStream {

        private InputStream actual;
        private List<Byte> readBytes;

        public CapturingInputStream(InputStream in) {
            this.actual = in;
            readBytes = new ArrayList<>();
        }

        @Override
        public int read() throws IOException {
            int read = actual.read();
            if (read >= 0) {
                readBytes.add((byte) read);
            }
            return read;
        }

        byte[] readBytes() {
            byte[] bytes = new byte[readBytes.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = readBytes.get(i);
            }
            return bytes;
        }

    }

}
