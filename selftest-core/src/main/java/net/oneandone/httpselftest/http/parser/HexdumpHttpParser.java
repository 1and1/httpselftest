package net.oneandone.httpselftest.http.parser;

import static net.oneandone.httpselftest.http.socket.SocketHttpClient.concat;

import java.util.Optional;

import net.oneandone.httpselftest.http.FullTestResponse;
import net.oneandone.httpselftest.http.socket.SocketTestResponse;

public class HexdumpHttpParser implements HttpContentParser {

    @Override
    public String id() {
        return "raw";
    }

    @Override
    public Optional<String> parse(FullTestResponse response) {
        if (!(response instanceof SocketTestResponse)) {
            return Optional.empty();
        }

        SocketTestResponse resp = (SocketTestResponse) response;
        byte[] bytes = response.bodyBlock() == null ? resp.headerBytes : concat(resp.headerBytes, resp.bodyBytes);
        return Optional.of(Hexdump.hexdump(bytes));
    }

}
