package net.oneandone.httpselftest.http.socket;

import java.nio.charset.StandardCharsets;

import net.oneandone.httpselftest.http.WireRepresentation;

public class SocketRequestWireRepresentation implements WireRepresentation {

    private final byte[] requestBytes;

    public SocketRequestWireRepresentation(byte[] requestBytes) {
        this.requestBytes = requestBytes;
    }

    @Override
    public String stringValue() {
        return new String(requestBytes, StandardCharsets.UTF_8);
    }

}
