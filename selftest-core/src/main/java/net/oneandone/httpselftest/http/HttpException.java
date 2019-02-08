package net.oneandone.httpselftest.http;

public class HttpException extends RuntimeException {

    private final byte[] readBytes;

    public HttpException(Exception e, byte[] readBytes) {
        super(e);
        this.readBytes = readBytes;
    }

    // only needed for HttpUrlConnection client
    public HttpException(Exception e) {
        super(e);
        readBytes = null;
    }

    // only needed for HttpUrlConnection client
    public HttpException(String message) {
        super(message);
        readBytes = null;
    }

    public byte[] getBytes() {
        return readBytes;
    }

}
