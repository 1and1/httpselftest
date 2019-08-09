package net.oneandone.httpselftest.writer;

import java.util.Optional;

import net.oneandone.httpselftest.http.TestResponse;
import net.oneandone.httpselftest.http.socket.SocketTestResponse;

public class HttpParserRaw implements HttpContentParser {

    @Override
    public String id() {
        return "raw";
    }

    @Override
    public Optional<String> parse(TestResponse response) {
        if (!(response instanceof SocketTestResponse)) {
            return Optional.empty();
        }

        return Optional.of(Hexdump.hexdump(((SocketTestResponse) response).getBytes()));
    }

}
