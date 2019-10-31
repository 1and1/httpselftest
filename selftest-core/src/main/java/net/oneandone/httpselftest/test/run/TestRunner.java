package net.oneandone.httpselftest.test.run;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.oneandone.httpselftest.http.Headers;
import net.oneandone.httpselftest.http.HttpClient;
import net.oneandone.httpselftest.http.SocketHttpClient;
import net.oneandone.httpselftest.http.TestRequest;
import net.oneandone.httpselftest.http.UrlConnectionHttpClient;
import net.oneandone.httpselftest.http.WrappedRequest;
import net.oneandone.httpselftest.log.LogAccess;
import net.oneandone.httpselftest.log.LogSupport;
import net.oneandone.httpselftest.test.api.AssertionException;
import net.oneandone.httpselftest.test.api.Context;
import net.oneandone.httpselftest.test.api.TestCase;
import net.oneandone.httpselftest.test.api.TestConfigs.Values;
import net.oneandone.httpselftest.writer.SelfTestWriter;

public class TestRunner {

    // TODO add some tests

    public static final String X_REQUEST_ID = "X-REQUEST-ID";

    private static final AtomicInteger EXECUTION_COUNTER = new AtomicInteger(0);

    private SelfTestWriter writer;

    private Values config;

    private String appUrl;

    private LogSupport logSupport;

    private List<TestCase> tests;

    public TestRunner(SelfTestWriter writer, Values testParams, String appUrl, List<TestCase> tests, LogSupport logSupport) {
        this.writer = writer;
        this.appUrl = appUrl;
        this.config = testParams;
        this.tests = tests;
        this.logSupport = logSupport;
    }

    public void runAll() {
        SimpleContext ctx = new SimpleContext();
        writer.writeText("Running tests:");

        IdentityHashMap<TestCase, String> runIds = new IdentityHashMap<>();
        tests.forEach(testcase -> runIds.put(testcase, runId(testcase.getName())));

        logSupport.runWithAttachedAppenders(new HashSet<>(runIds.values()), () -> {
            for (TestCase test : tests) {
                ctx.resetClues();
                run(test, runIds.get(test), ctx);
            }
        });

        writer.writeText("Done.");
    }

    private void run(TestCase testCase, String runId, SimpleContext ctx) {
        List<LogAccess> buffersForRunId = logSupport.getLogs(runId);
        final TestRunData testdata = execute(testCase, runId, config, appUrl, ctx, buffersForRunId);
        testdata.maxDuration = testCase.maxAcceptableDurationMillis();

        writer.writeTestOutcome(testdata, testdata.logs, ctx);
    }

    static int clamped(int waitForLogs) {
        return min(max(waitForLogs, 0), 5_000);
    }

    private static final TestRunData execute(TestCase test, String runId, Values config, String appUrl, Context ctx,
            List<LogAccess> buffersForRunId) {
        final TestRunData testRun = new TestRunData(test.getName(), Instant.now(), runId);
        testRun.logs = Collections.emptyList();

        try {
            testRun.wrappedRequest = new WrappedRequest(test.prepareRequest(config, ctx));
            addRunId(testRun.wrappedRequest.request.headers, runId);

            invokeKeepingTime(appUrl, testRun.wrappedRequest, testRun);

            sleep(clamped(test.waitForLogsMillis()));
            testRun.logs = LogAccess.snapshot(buffersForRunId);

            try {
                test.verify(config, testRun.wrappedResponse.response, ctx);
                testRun.result = TestRunResult.success();
            } catch (AssertionException e) {
                testRun.result = TestRunResult.failure(e.getMessage());
            }
        } catch (Exception e) {
            testRun.result = TestRunResult.error(e);
        }

        return testRun;
    }

    private static void addRunId(Headers headers, String runId) {
        if (headers.get(X_REQUEST_ID) != null) {
            throw new IllegalStateException("Header " + X_REQUEST_ID + " must no be set by test case.");
        }
        headers.add(X_REQUEST_ID, runId);
    }

    private static void invokeKeepingTime(String appUrl, WrappedRequest reqWrapper, final TestRunData testRun) {
        long timeBefore = System.nanoTime();
        try {
            testRun.wrappedResponse = getClient(reqWrapper.request).call(appUrl, reqWrapper, 3000);
        } finally {
            testRun.durationMillis = (System.nanoTime() - timeBefore) / 1_000_000;
        }
    }

    private static HttpClient getClient(TestRequest request) {
        switch (request.clientType) {
            case SOCKET:
                return new SocketHttpClient();
            case URLCON:
                return new UrlConnectionHttpClient();
            default:
                throw new IllegalStateException("Unknown clientType: " + request.clientType);
        }
    }

    static String runId(String testName) {
        String runId = testName + "-" + EXECUTION_COUNTER.incrementAndGet();
        runId = runId.replaceAll("[^a-zA-Z0-9-]", ""); // replace all but alnum-
        if (runId.length() < 20) {
            runId += new String(new char[20 - runId.length()]).replace("\0", "-");
        }
        if (runId.length() > 200) {
            runId = runId.substring(0, 200);
        }
        return runId;
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
