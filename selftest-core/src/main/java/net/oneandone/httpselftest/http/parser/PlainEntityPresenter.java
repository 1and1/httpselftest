package net.oneandone.httpselftest.http.parser;

import java.util.Optional;

import net.oneandone.httpselftest.http.Headers;
import net.oneandone.httpselftest.http.HttpDetails;

public class PlainEntityPresenter implements HttpPresenter {

    @Override
    public String id() {
        return "plain";
    }

    @Override
    public Optional<String> parse(Headers headers, HttpDetails resp) {
        String bodyBlock = resp.bodyBlock();

        if (bodyBlock == null) {
            return Optional.of(resp.headerBlock());
        } else {
            return Optional.of(resp.headerBlock() + bodyBlock);
        }
    }

}
