package net.oneandone.httpselftest.http;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import net.oneandone.httpselftest.common.Pair;

/**
 * Case-insensitive header handling.
 */
public class Headers {

    private final Map<String, List<String>> map = new HashMap<>();
    private final List<Pair<String, String>> inOrder = new ArrayList<>();

    public void add(String name, String value) {
        requireNonNull(value, "header name may not be null");
        requireNonNull(value, "header value may not be null");

        List<String> bucket = map.computeIfAbsent(name.toLowerCase(), always -> new LinkedList<>());
        bucket.add(value);
        inOrder.add(new Pair<>(name, value));
    }

    public List<String> get(String name) {
        return map.get(name.toLowerCase());
    }

    Stream<Pair<String, String>> stream() {
        return inOrder.stream();
    }

}
