package net.oneandone.httpselftest.test.run;

import net.oneandone.httpselftest.http.WrappedRequest;
import net.oneandone.httpselftest.http.WrappedResponse;

public class TestRunDataHelper {

    public static void setDurationMillis(TestRunData data, long duration) {
        data.durationMillis = duration;
    }

    public static void setResult(TestRunData data, TestRunResult result) {
        data.result = result;
    }

    public static void setRequest(TestRunData data, WrappedRequest request) {
        data.wrappedRequest = request;
    }

    public static void setResponse(TestRunData data, WrappedResponse response) {
        data.wrappedResponse = response;
    }

    public static void setMaxDurationMillis(TestRunData data, int maxDuration) {
        data.maxDuration = maxDuration;
    }

}
