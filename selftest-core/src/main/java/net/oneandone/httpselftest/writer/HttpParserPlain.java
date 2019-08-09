package net.oneandone.httpselftest.writer;

import java.util.Optional;

import net.oneandone.httpselftest.http.TestResponse;

public class HttpParserPlain implements HttpContentParser {

    @Override
    public String id() {
        return "plain";
    }

    @Override
    public Optional<String> parse(TestResponse response) {
        return Optional.of(response.wireRepresentation());
    }

}
