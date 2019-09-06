package net.oneandone.httpselftest.http.presenter;

import static net.oneandone.httpselftest.http.SocketHttpClient.concat;

import java.util.Optional;

import net.oneandone.httpselftest.http.Headers;
import net.oneandone.httpselftest.http.HttpDetails;
import net.oneandone.httpselftest.http.WireBasedHttpDetails;

public class RawHttpPresenter implements HttpPresenter {

    @Override
    public String id() {
        return "raw";
    }

    @Override
    public Optional<String> parse(Headers headers, HttpDetails details) {
        if (!(details instanceof WireBasedHttpDetails)) {
            return Optional.empty();
        }

        WireBasedHttpDetails det = (WireBasedHttpDetails) details;
        byte[] allBytes = det.bodyBytes == null ? det.headerBytes : concat(det.headerBytes, det.bodyBytes);
        return Optional.of(Hexdump.hexdump(allBytes));
    }

}
