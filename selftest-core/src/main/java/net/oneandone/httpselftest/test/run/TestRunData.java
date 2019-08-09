package net.oneandone.httpselftest.test.run;

import java.time.Instant;
import java.util.List;

import net.oneandone.httpselftest.http.FullTestResponse;
import net.oneandone.httpselftest.http.TestRequest;
import net.oneandone.httpselftest.log.LogDetails;

public final class TestRunData {

    public final String testName;
    public final Instant startInstant;
    public final String runId;

    long durationMillis;
    TestRunResult result;
    TestRequest request;
    FullTestResponse response;
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

    public TestRequest getRequest() {
        return request;
    }

    public FullTestResponse getResponse() {
        return response;
    }

    public int getMaxDurationMillis() {
        return maxDuration;
    }

}
