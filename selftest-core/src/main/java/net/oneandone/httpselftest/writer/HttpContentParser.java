package net.oneandone.httpselftest.writer;

import java.util.Optional;

import net.oneandone.httpselftest.http.TestResponse;

public interface HttpContentParser {

    String id();

    Optional<String> parse(TestResponse response);

}
