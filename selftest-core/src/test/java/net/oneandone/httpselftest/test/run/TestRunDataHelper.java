package net.oneandone.httpselftest.test.run;

import net.oneandone.httpselftest.http.TestRequest;
import net.oneandone.httpselftest.http.TestResponse;
import net.oneandone.httpselftest.test.run.TestRunData;
import net.oneandone.httpselftest.test.run.TestRunResult;

public class TestRunDataHelper {

    public static void setDurationMillis(TestRunData data, long duration) {
        data.durationMillis = duration;
    }

    public static void setResult(TestRunData data, TestRunResult result) {
        data.result = result;
    }

    public static void setRequest(TestRunData data, TestRequest request) {
        data.request = request;
    }

    public static void setResponse(TestRunData data, TestResponse response) {
        data.response = response;
    }

    public static void setMaxDurationMillis(TestRunData data, int maxDuration) {
        data.maxDuration = maxDuration;
    }

}
