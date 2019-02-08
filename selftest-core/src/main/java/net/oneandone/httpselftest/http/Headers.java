package net.oneandone.httpselftest.http;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Case-insensitive header handling.
 */
public class Headers {

    private final Map<String, List<String>> map = new HashMap<>();

    public void add(String name, String value) {
        List<String> bucket = map.computeIfAbsent(name.toLowerCase(), always -> new LinkedList<>());
        bucket.add(value);
    }

    public List<String> get(String name) {
        return map.get(name.toLowerCase());
    }

}
