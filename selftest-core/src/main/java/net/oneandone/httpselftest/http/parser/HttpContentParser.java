package net.oneandone.httpselftest.http.parser;

import java.util.Optional;

import net.oneandone.httpselftest.http.FullTestResponse;

// FIXME use for requests as well
public interface HttpContentParser {

    String id();

    Optional<String> parse(FullTestResponse response);

}
