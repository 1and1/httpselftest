package net.oneandone.httpselftest.http;

import java.util.stream.Stream;

import net.oneandone.httpselftest.common.Pair;

public class TestHttpHelper {

    public static void setDetails(WrappedRequest wrapper, HttpDetails details) {
        wrapper.details = details;
    }

    public static Stream<Pair<String, String>> stream(Headers headers) {
        return headers.stream();
    }

}
