package net.oneandone.httpselftest.writer;

import net.oneandone.httpselftest.log.LogDetails;
import net.oneandone.httpselftest.test.api.TestCase;
import net.oneandone.httpselftest.test.api.TestConfigs;
import net.oneandone.httpselftest.test.run.SimpleContext;
import net.oneandone.httpselftest.test.run.TestRunData;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public abstract class SelfTestWriter {

    protected final PrintWriter writer;

    public SelfTestWriter(PrintWriter writer) {
        this.writer = writer;
    }

    public abstract void flush();

    public abstract void writePageEnd();

    public abstract void writePageStart(TestConfigs configs, Set<String> relevantConfigIds, TestConfigs.Values paramsToUse,
                                        String servletName,
                                        String testsBaseUrl, Instant lastTestRun, String callerIp,
                                        String lastTestrunIp);

    public abstract void writeTestOutcome(TestRunData testRun, List<LogDetails> logs, SimpleContext ctx);

    public abstract void writeText(String paragraph);

    public abstract void writeUncaughtException(Exception e);

    public abstract void writeUnrunTests(List<TestCase> tests);

}
