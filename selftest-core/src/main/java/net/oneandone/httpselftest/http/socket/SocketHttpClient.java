package net.oneandone.httpselftest.http.socket;

import static net.oneandone.httpselftest.common.Constants.X_REQUEST_ID;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import net.oneandone.httpselftest.http.Headers;
import net.oneandone.httpselftest.http.HttpClient;
import net.oneandone.httpselftest.http.HttpException;
import net.oneandone.httpselftest.http.TestRequest;

public class SocketHttpClient implements HttpClient {

    @Override
    public SocketTestResponse call(String baseUrl, TestRequest request, String runId, int timeoutMillis) {
        try (Socket socket = new Socket()) {
            checkMethodCharset(request.method);
            checkPathCharset(request.path);
            checkHeadersCharset(request.headers);

            URL endpoint = new URL(baseUrl);

            int port = endpoint.getPort();
            if (port < 0) {
                throw new IllegalArgumentException("no port provided: " + baseUrl);
            }
            String hostname = endpoint.getHost();
            String prefix = endpoint.getPath();

            socket.connect(new InetSocketAddress(hostname, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);

            sendRequest(socket.getOutputStream(), hostname, port, prefix, request, runId);
            return parseResponse(capturedInputStream(socket.getInputStream()), request.method);
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            throw new HttpException(e);
        }
    }

    private static void sendRequest(OutputStream out, String host, int port, String pathPrefix, TestRequest request, String runId)
            throws IOException {
        byte[] requestBytes = prepareRequest(host, port, pathPrefix, request, runId);
        request.addWireRepresentation(new SocketRequestWireRepresentation(requestBytes));
        out.write(requestBytes);
        out.flush();
    }

    private static byte[] prepareRequest(String hostname, int port, String pathPrefix, TestRequest request, String runId) {
        String path = pathPrefix + request.path;

        Bytes bytes = new Bytes();

        bytes.appendLine(request.method + " " + path + " " + "HTTP/1.1");

        bytes.appendLine("Host: " + hostname + ":" + port);
        request.headers.entrySet().forEach(header -> bytes.appendLine(header.getKey() + ": " + header.getValue()));
        bytes.appendLine(X_REQUEST_ID + ": " + runId);

        if (request.body == null) {
            bytes.appendCrLf();
        } else {
            byte[] bodyBytes = request.body.getBytes(UTF_8);
            bytes.appendLine("Content-Length: " + bodyBytes.length);
            bytes.appendCrLf();
            bytes.append(bodyBytes);
        }

        return bytes.toArray();
    }

    private static SocketTestResponse parseResponse(CapturingInputStream in, String requestMethod) throws HttpException {
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

            SocketTestResponse response = new SocketTestResponse(statusCode, headers, body);
            response.headerBytes = headerBytes;
            response.bodyBytes = bodyBytes;
            return response;
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

    public static byte[] concat(byte[] left, byte[] right) {
        byte[] newBuffer = new byte[left.length + right.length];

        for (int i = 0; i < left.length; i++) {
            newBuffer[i] = left[i];
        }

        int offset = left.length;
        for (int i = 0; i < right.length; i++) {
            newBuffer[i + offset] = right[i];
        }

        // TODO use System.arraycopy instead?

        return newBuffer;
    }

    private static void checkMethodCharset(String method) {
        checkCharset(method, c -> 'A' <= c && c <= 'Z');
    }

    private static void checkPathCharset(String path) {
        checkCharset(path, c -> ' ' <= c && c <= '~');
    }

    private void checkHeadersCharset(Map<String, String> headers) {
        if (headers != null) {
            headers.entrySet().stream().forEach(header -> {
                checkPathCharset(header.getKey());
                checkPathCharset(header.getValue());
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
