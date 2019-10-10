package net.oneandone.httpselftest.writer;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

import net.oneandone.httpselftest.log.LogDetails;
import net.oneandone.httpselftest.test.api.TestCase;
import net.oneandone.httpselftest.test.api.TestConfigs;
import net.oneandone.httpselftest.test.run.ResultType;
import net.oneandone.httpselftest.test.run.SimpleContext;
import net.oneandone.httpselftest.test.run.TestRunData;

public class SelfTestJsonWriter extends SelfTestWriter {

    public final List<TestRunData> testRunData;
    public final List<Throwable> uncaughtExceptions;

    public SelfTestJsonWriter(PrintWriter w) {
        super(w);
        testRunData = new ArrayList<>();
        uncaughtExceptions = new ArrayList<>();
    }

    @Override
    public void writePageStart(TestConfigs configs, Set<String> relevantConfigIds, TestConfigs.Values paramsToUse,
            String servletName, String testsBaseUrl, Instant lastTestRun, String callerIp, String lastTestrunIp) {
    }

    @Override
    public void writePageEnd() {
        boolean success = uncaughtExceptions.isEmpty()
                && testRunData.stream().allMatch(data -> data.getResult().type == ResultType.SUCCESS);

        JsonArray failures = testRunData.stream().filter(data -> data.getResult().type == ResultType.FAILURE)
                .map(data -> jsonFailureOf(data.testName, data.getResult().assertionMessage))
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

        JsonArray errors = testRunData.stream().filter(data -> data.getResult().type == ResultType.ERROR)
                .map(data -> jsonErrorOf(data.testName, data.getResult().uncaught))
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

        JsonArray exceptions =
                uncaughtExceptions.stream().map(Throwable::toString).collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

        writer.append(jsonResponseOf(success, failures, errors, exceptions));
    }

    @Override
    public void writeTestOutcome(TestRunData testRun, List<LogDetails> logs, SimpleContext ctx) {
        testRunData.add(testRun);
    }

    @Override
    public void writeText(String paragraph) {
    }

    @Override
    public void writeUncaughtException(Throwable t) {
        uncaughtExceptions.add(t);
    }

    @Override
    public void writeUnrunTests(List<TestCase> tests) {
    }

    private static String jsonResponseOf(boolean success, JsonArray failures, JsonArray errors, JsonArray exceptions) {
        JsonObject s = new JsonObject();
        s.put("success", success);
        s.put("testFailures", failures);
        s.put("testErrors", errors);
        s.put("exceptions", exceptions);
        return s.toJson();
    }

    private static JsonObject jsonFailureOf(String name, String message) {
        JsonObject o = new JsonObject();
        o.put("name", name);
        o.put("message", message);
        return o;
    }

    private static JsonObject jsonErrorOf(String name, Throwable t) {
        return jsonFailureOf(name, t.toString());
    }

}
