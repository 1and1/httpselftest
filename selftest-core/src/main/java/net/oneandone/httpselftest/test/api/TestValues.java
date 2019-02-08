package net.oneandone.httpselftest.test.api;

/**
 * Models the effective test parameters for a particular run.
 */
public interface TestValues {

    /**
     * Get test parameter by name.
     *
     * @param name the name of the test value
     * @return the value
     */
    String get(String name);

}
