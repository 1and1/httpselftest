package net.oneandone.httpselftest.http;

import java.nio.charset.StandardCharsets;

public class WireBasedHttpDetails implements HttpDetails {

    public final byte[] headerBytes;
    public final byte[] bodyBytes;

    public WireBasedHttpDetails(byte[] headerBytes, byte[] bodyBytes) {
        this.headerBytes = headerBytes;
        this.bodyBytes = bodyBytes;
    }

    @Override
    public String headerBlock() {
        return new String(headerBytes, StandardCharsets.US_ASCII);
    }

    @Override
    public String bodyBlock() {
        if (bodyBytes == null || bodyBytes.length <= 0) {
            return null;
        }
        return new String(bodyBytes, StandardCharsets.UTF_8); // TODO honor charset
    }

}
