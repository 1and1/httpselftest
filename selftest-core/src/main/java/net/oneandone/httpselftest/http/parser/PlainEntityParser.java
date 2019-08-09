package net.oneandone.httpselftest.http.parser;

import java.util.Optional;

import net.oneandone.httpselftest.http.FullTestResponse;

public class PlainEntityParser implements HttpContentParser {

    @Override
    public String id() {
        return "plain";
    }

    @Override
    public Optional<String> parse(FullTestResponse resp) {
        if (resp.bodyBlock() == null) {
            return Optional.of(resp.headerBlock());
        } else {
            return Optional.of(resp.headerBlock() + resp.bodyBlock());
        }
    }

}
