package net.oneandone.httpselftest.writer;

import net.oneandone.httpselftest.http.HttpException;
import net.oneandone.httpselftest.log.LogAccess;
import static net.oneandone.httpselftest.log.LogAccess.snapshot;
import net.oneandone.httpselftest.test.run.TestRunData;
import net.oneandone.httpselftest.test.run.TestRunResult;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SelfTestJsonWriterTest {

    private static SelftestHtmlWriterTest.CapturingPrintWriter out;

    private static SelfTestJsonWriter writer;

    @Before
    public void prepareFile() throws Exception {
        out = new SelftestHtmlWriterTest.CapturingPrintWriter("./target/jsonwritertest.html", "UTF-8");
        writer = new SelfTestJsonWriter(out);
    }

    @After
    public void finishFile() {
        out.close();
    }

    @Test
    public void writeTestOutcome_error() {
        HttpException exception = new HttpException(new RuntimeException("inner"),
                "static string: holla die waldfee!".getBytes(StandardCharsets.UTF_8));

        TestRunData testRun = SelftestHtmlWriterTest.testRun("test1", "mn1", 234, TestRunResult.error(exception));

        List<LogAccess> logs = SelftestHtmlWriterTest.logInfos("ROOT");

        writer.writeTestOutcome(testRun, snapshot(logs), SelftestHtmlWriterTest.context("hallo", "bernd"));
        writer.writePageEnd();

        String json = out.written();
        assertThat(json).contains("success", "false");
    }

    @Test
    public void writeTestOutcome_failed() {
        TestRunData testRun = SelftestHtmlWriterTest.testRun("test1", "mn1", 234, TestRunResult.failure("failed"));
        List<LogAccess> logs = SelftestHtmlWriterTest.logInfos("ROOT");

        writer.writeTestOutcome(testRun, snapshot(logs), SelftestHtmlWriterTest.context("hallo", "bernd"));
        writer.writePageEnd();

        String json = out.written();
        assertThat(json).contains("success", "false");
    }

    @Test
    public void writeTestOutcome_success() {
        TestRunData testRun = SelftestHtmlWriterTest.testRun("test1", "mn1", 234, TestRunResult.success());
        List<LogAccess> logs = SelftestHtmlWriterTest.logInfos("ROOT");

        writer.writeTestOutcome(testRun, snapshot(logs), SelftestHtmlWriterTest.context("hallo", "bernd"));
        writer.writePageEnd();

        String json = out.written();
        assertThat(json).contains("success", "true");
    }

}
