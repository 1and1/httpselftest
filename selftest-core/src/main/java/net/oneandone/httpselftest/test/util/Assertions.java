package net.oneandone.httpselftest.test.util;

import net.oneandone.httpselftest.test.api.AssertionException;

public interface Assertions {

    static void assertTrue(String description, boolean condition) {
        if (!condition) {
            throw new AssertionException(description);
        }
    }

    static void assertFalse(String description, boolean condition) {
        if (condition) {
            throw new AssertionException(description);
        }
    }

    static void assertEqual(String description, Object expected, Object actual) {
        if (expected == null && actual == null) {
            return;
        }

        if (expected == null || !expected.equals(actual)) {
            throw new AssertionException(description + " - expected: " + expected + ", was: " + actual);
        }
    }

}
