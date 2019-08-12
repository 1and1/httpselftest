package net.oneandone.httpselftest.test.run;

import java.time.Instant;
import java.util.List;

import net.oneandone.httpselftest.http.WrappedRequest;
import net.oneandone.httpselftest.http.WrappedResponse;
import net.oneandone.httpselftest.log.LogDetails;

public final class TestRunData {

    public final String testName;
    public final Instant startInstant;
    public final String runId;

    long durationMillis;
    TestRunResult result;
    WrappedRequest wrappedRequest;
    WrappedResponse wrappedResponse;
    List<LogDetails> logs;
    int maxDuration;

    public TestRunData(String testName, Instant instant, String runId) {
        this.testName = testName;
        this.startInstant = instant;
        this.runId = runId;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public TestRunResult getResult() {
        return result;
    }

    public WrappedRequest getRequest() {
        return wrappedRequest;
    }

    public WrappedResponse getResponse() {
        return wrappedResponse;
    }

    public int getMaxDurationMillis() {
        return maxDuration;
    }

}
