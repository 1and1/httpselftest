package net.oneandone.httpselftest.test.api;

/**
 * Used to transport failed assertions from inside {@link TestCase#verify(TestValues, TestResponse, Context)}.
 */
public class AssertionException extends RuntimeException {

    public AssertionException(String description) {
        super(description);
    }

}
