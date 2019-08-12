package net.oneandone.httpselftest.test.api;

import net.oneandone.httpselftest.http.TestResponse;
import net.oneandone.httpselftest.http.TestRequest;
import net.oneandone.httpselftest.test.util.Assertions;
import net.oneandone.httpselftest.test.util.RequestHelper;

/**
 * Central interface to implement test cases.
 */
public interface TestCase {

    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Defines an HTTP request made of
     * <ul>
     * <li>path (relative to the application base URL)</li>
     * <li>HTTP method</li>
     * <li>a map of HTTP headers with values</li>
     * <li>request body (will be UTF-8 encoded on the wire)</li>
     * </ul>
     * <p>
     * If a body is provided, a Content-Type header MUST be provided as well.<br>
     * Body MUST NOT be present on GET requests.<br>
     * {@link RequestHelper} contains some methods facilitating request creation.
     * </p>
     *
     * @param config config object containing the parameters provided by the frontend for this test run
     * @param ctx    test execution context spanning previously executed steps
     * @return request definition
     * @throws Exception if anything fails during preparation
     */
    TestRequest prepareRequest(TestValues config, Context ctx) throws Exception;

    /**
     * If a response was received, this method will be called to evaluate assertions on the response object. Assertions are
     * communicated by throwing {@link AssertionException}. Some assertion helpers can be found in {@linkplain Assertions}.
     *
     * @param config   config object containing the parameters provided by the frontend for this test run
     * @param ctx      test execution context spanning previously executed steps
     * @param response response received for the previously prepared request
     * @throws Exception if anything fails during verification
     */
    void verify(TestValues config, TestResponse response, Context ctx) throws Exception;

    /**
     * Duration to wait after test execution before rendering output. This is useful in case logging or execution happens
     * asynchronously. Otherwise log lines could be missed.
     *
     * @return duration in milliseconds
     */
    default int waitForLogsMillis() {
        return 20;
    }

    /**
     * If the server is unable to generate a response within this time, a warning is generated.
     *
     * @return max acceptableDuration in milliseconds
     */
    default int maxAcceptableDurationMillis() {
        return 100;
    }

}
