package net.oneandone.httpselftest.writer;

import net.oneandone.httpselftest.log.LogDetails;
import net.oneandone.httpselftest.test.api.TestCase;
import net.oneandone.httpselftest.test.api.TestConfigs;
import net.oneandone.httpselftest.test.run.ResultType;
import net.oneandone.httpselftest.test.run.SimpleContext;
import net.oneandone.httpselftest.test.run.TestRunData;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SelfTestJsonWriter extends SelfTestWriter {

    public final List<TestRunData> testRunData;

    public SelfTestJsonWriter(PrintWriter w) {
        super(w);
        testRunData = new ArrayList<>();
    }

    @Override
    public void flush() {
    }

    @Override
    public void writePageStart(TestConfigs configs, Set<String> relevantConfigIds, TestConfigs.Values paramsToUse,
            String servletName, String testsBaseUrl, Instant lastTestRun, String callerIp, String lastTestrunIp) {
    }

    @Override
    public void writePageEnd() {
        boolean success = testRunData.stream().allMatch(data -> data.getResult().type == ResultType.SUCCESS);
        writer.append("{ \"success\": \"" + success + "\"}").flush();
    }

    @Override
    public void writeTestOutcome(TestRunData testRun, List<LogDetails> logs, SimpleContext ctx) {
        testRunData.add(testRun);
    }

    @Override
    public void writeText(String paragraph) {
    }

    @Override
    public void writeUncaughtException(Exception e) {
    }

    @Override
    public void writeUnrunTests(List<TestCase> tests) {
    }

}
