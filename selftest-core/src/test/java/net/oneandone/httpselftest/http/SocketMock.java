package net.oneandone.httpselftest.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketMock {

    private static final Logger LOG = LoggerFactory.getLogger(SocketMock.class);

    private final ServerSocket serverSocket;
    private volatile String payloadToReturn;
    private volatile byte[] receivedBytes;

    public SocketMock() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(null); // pick free port
        Thread socketHandler = new Thread(() -> {
            try {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(250);

                // read
                InputStream in = socket.getInputStream();
                List<Byte> bytes = new LinkedList<>();
                try {
                    while (true) {
                        int read = in.read();
                        if (read >= 0) {
                            bytes.add((byte) read);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    // end of stream
                }
                receivedBytes = toArray(bytes);

                // write
                OutputStream out = socket.getOutputStream();
                if (payloadToReturn != null) {
                    out.write(payloadToReturn.getBytes(StandardCharsets.UTF_8));
                } else {
                    socket.close();
                }
            } catch (IOException e) {
                LOG.error("Unhandled exception in SocketMock.", e);
            }
        });
        socketHandler.start();
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public void replyWith(String payload) {
        this.payloadToReturn = payload;
    }

    public String requested() {
        return new String(receivedBytes, StandardCharsets.UTF_8);
    }

    private static byte[] toArray(List<Byte> list) {
        Byte[] boxed = list.toArray(new Byte[0]);
        int len = boxed.length;
        byte[] unboxed = new byte[len];
        for (int i = 0; i < len; i++) {
            unboxed[i] = boxed[i].byteValue();
        }
        return unboxed;
    }

}
