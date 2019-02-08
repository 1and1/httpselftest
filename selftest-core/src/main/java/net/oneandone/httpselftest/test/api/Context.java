package net.oneandone.httpselftest.test.api;

/**
 * Models a key &rarr; value store to transport data from one test to the next. This can be used to chain process steps or for
 * cleanup. This is obviously dependent on test execution order and on previous test outcomes, thus can be shaky. Use only if
 * necessary.
 *
 * If there is conditional behavior in test preparation or verification then you REALLY SHOULD provide clues explaining the
 * branches that were taken.
 */
public interface Context {

    /**
     * Store a value in the context for subsequent steps.
     *
     * @param key   the key, not null
     * @param value the value
     */
    void store(String key, String value);

    /**
     * Retrieve a value from the context. Null if absent.
     *
     * @param key the key
     * @return stored value or null
     */
    String retrieve(String key);

    /**
     * Used to provide feedback to the user in case there are conditional steps during request preparation or response
     * verification. The goal is exclusively to enhance understandability of the test outcome. It is good practice to give
     * feedback on each decision that was made, and for every branch following that decision.<br>
     * <br>
     * Example:<br>
     * <pre>
     * if (ctx.retrieve(CTX_KEY) != null) {
     *     ctx.addClue("using created token");
     *     return ctx.retrieve(CTX_KEY);
     * } else {
     *     ctx.addClue("no token created");
     *     return "";
     * }
     * </pre>
     *
     * @param msg the feedback, should be very short
     */
    void addClue(String msg);

}
