package net.oneandone.httpselftest.writer;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static net.oneandone.httpselftest.http.TestHttpHelper.stream;
import static net.oneandone.httpselftest.log.LogAccess.snapshot;
import static net.oneandone.httpselftest.log.SelftestEvent.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import j2html.tags.DomContent;
import net.oneandone.httpselftest.http.DataBasedHttpDetails;
import net.oneandone.httpselftest.http.Headers;
import net.oneandone.httpselftest.http.HttpDetails;
import net.oneandone.httpselftest.http.HttpException;
import net.oneandone.httpselftest.http.TestHttpHelper;
import net.oneandone.httpselftest.http.TestRequest;
import net.oneandone.httpselftest.http.TestResponse;
import net.oneandone.httpselftest.http.WireBasedHttpDetails;
import net.oneandone.httpselftest.http.WrappedRequest;
import net.oneandone.httpselftest.http.WrappedResponse;
import net.oneandone.httpselftest.log.EventRendererStub;
import net.oneandone.httpselftest.log.LogAccess;
import net.oneandone.httpselftest.log.SelftestEvent;
import net.oneandone.httpselftest.log.SynchronousLogBuffer;
import net.oneandone.httpselftest.log.logback.LogbackEventRenderer;
import net.oneandone.httpselftest.log.logback.LogbackSupport;
import net.oneandone.httpselftest.test.api.TestCase;
import net.oneandone.httpselftest.test.api.TestConfigs;
import net.oneandone.httpselftest.test.api.TestConfigs.Values;
import net.oneandone.httpselftest.test.run.SimpleContext;
import net.oneandone.httpselftest.test.run.TestRunData;
import net.oneandone.httpselftest.test.run.TestRunDataHelper;
import net.oneandone.httpselftest.test.run.TestRunResult;

public class SelftestHtmlWriterTest {

    @Rule
    public TestName name = new TestName();

    private static CapturingPrintWriter out;

    private static SelftestHtmlWriter writer;

    @BeforeClass
    public static void prepareFile() throws Exception {
        out = new CapturingPrintWriter("./target/writertest.html", "UTF-8");
        writer = new SelftestHtmlWriter(out);

        TestConfigs configs = someTestConfigs();
        Set<String> relevantConfigIds = new HashSet<>();
        relevantConfigIds.add("live relevant");
        relevantConfigIds.add("qs relevant");
        Values paramsToUse = configs.create("qs relevant", Collections.emptyMap());
        String servletName = "servletName";
        String testsBaseUrl = "some base URL";
        Instant lastTestRun = Instant.now();
        String callerIp = "123.5.4.3";
        String lastTestrunIp = "12.23.34.45";

        writer.writePageStart(configs, relevantConfigIds, paramsToUse, servletName, testsBaseUrl, lastTestRun, callerIp,
                lastTestrunIp);

        ReflectionTestUtils.setField(configs, "fixedParameterNames", Collections.emptyList());
        writer.writePageStart(configs, relevantConfigIds, paramsToUse, servletName, testsBaseUrl, lastTestRun, callerIp,
                lastTestrunIp);
    }

    static TestConfigs someTestConfigs() {
        TestConfigs testConfigs = new TestConfigs("param1", "param2", "param3");
        testConfigs.fixed("param3");
        testConfigs.put("live relevant", "value1", "value2", "value3");
        testConfigs.put("qs relevant", "value1", "value2", "value3");
        testConfigs.put("live other", "value1", "value2", "value3");
        testConfigs.put("qs other", "value1", "value2", "value3");
        return testConfigs;
    }

    @Before
    public void prependTestWithName() {
        testOutput(name.getMethodName());
        out.reset();
    }

    @AfterClass
    public static void finishFile() {
        try {
            writer.writePageEnd();
            out.write(
                    "<script>document.querySelectorAll('input[type=submit]').forEach(submit => {submit.onclick = (any => {return false})})</script>");
            testOutput("All tests done. Buttons have been disabled.");
        } finally {
            out.close();
        }
    }

    @Test
    public void metainfoBlock_silent() {
        String myIp = writer.metainfoBlock("base url", Instant.now(), "1.1.1.1", "1.1.1.1").render();
        assertThat(myIp).contains("from your IP");

        String otherIp = writer.metainfoBlock("base url", Instant.now(), "1.1.1.1", "2.2.2.2").render();
        assertThat(otherIp).contains("from another IP");

        String now = writer.metainfoBlock("base url", Instant.now(), "1.1.1.1", "1.1.1.1").render();
        assertThat(now).contains("just now");

        String day = writer.metainfoBlock("base url", Instant.now().minus(Period.ofDays(1)), "1.1.1.1", "1.1.1.1").render();
        assertThat(day).contains("1d ago");

        String hour = writer.metainfoBlock("base url", Instant.now().minus(Duration.ofHours(1)), "1.1.1.1", "1.1.1.1").render();
        assertThat(hour).contains("1h ago");

        String min = writer.metainfoBlock("base url", Instant.now().minus(Duration.ofMinutes(1)), "1.1.1.1", "1.1.1.1").render();
        assertThat(min).contains("1m ago");
    }

    @Test
    public void testParametersForm_silent() {
        TestConfigs configs = new TestConfigs("param1", "param2");
        configs.put("cid", "val1", "val2");

        String noParams = writer.testParametersForm(configs, configs.createEmpty(), "servletName").render();
        assertThat(noParams).contains("method=\"POST\"", "action=\"servletName\"", "p-param1", "p-param2",
                "name=\"config-id\" value=\"\"");

        String fromConfig = writer.testParametersForm(configs, configs.getValues("cid"), "servletName").render();
        assertThat(fromConfig).contains("name=\"config-id\" value=\"cid\"").doesNotContain("readonly");

        HashMap<String, String> params = new HashMap<>();
        params.put("param1", "value1");
        params.put("param2", "<>\"");
        String fromParams = writer.testParametersForm(configs, configs.create(params), "servletName").render();
        assertThat(fromParams).contains("value=\"value1\"", "value=\"&lt;&gt;&quot;\"");
    }

    @Test
    public void testParametersForm_fixed_silent() {
        TestConfigs fixed = new TestConfigs("param1");
        fixed.fixed("param1");

        String withFixed = writer.testParametersForm(fixed, fixed.createEmpty(), "servletName").render();
        assertThat(withFixed).contains("readonly=\"true\"", "class=\"fixed\"");
    }

    @Test
    public void providedConfigsForm_silent() {
        TestConfigs configs = new TestConfigs("param1", "param2", "param3");
        configs.fixed("param2");
        configs.put("config_1", "value1", "value2", "value3");
        configs.put("config_2", "value1", "value2", "value3");

        Optional<DomContent> noConfigs = writer.providedConfigsForm(new TestConfigs(), setOf(), Optional.empty());
        assertThat(noConfigs).isNotPresent();

        String noIdsRelevant = writer.providedConfigsForm(configs, setOf(), Optional.empty()).get().render();
        assertThat(noIdsRelevant).contains("relevantMarkets", "config-id", "config_1").doesNotContain("otherMarkets",
                "activeConfigId");

        String someIdsIrrelevant = writer.providedConfigsForm(configs, setOf("config_1"), Optional.of("config_1")).get().render();
        assertThat(someIdsIrrelevant).contains("relevantMarkets", "otherMarkets", "activeConfigId");

        String noIdsIrrelevant =
                writer.providedConfigsForm(configs, setOf("config_1", "config_2"), Optional.empty()).get().render();
        assertThat(noIdsIrrelevant).contains("relevantMarkets").doesNotContain("otherMarkets");
    }

    @Test
    public void writeText() {
        writer.writeText("Hallihallo, dies ist ein Test");
        assertThat(out.written()).contains("Hallihallo, dies ist ein Test");
    }

    @Test
    public void writeText_specialChars() {
        writer.writeText("<>&ü'\"");

        String html = out.written().replace("<br>", "").replace("<span>", "").replace("</span>", "") //
                .replace("<div>", "").replace("</div>", "");
        assertThat(html).doesNotContain("<");
        assertThat(html).doesNotContain(">");
        assertThat(html).contains("ü");
        assertThat(html).doesNotContain("&ü");
        assertThat(html).doesNotContain("'");
        assertThat(html).doesNotContain("\"");
    }

    @Test
    public void writeUncaughtException() {
        writer.writeUncaughtException(new RuntimeException("secret exception message"));
        assertThat(out.written()).contains("secret exception message", "java.lang.RuntimeException",
                "SelftestHtmlWriterTest.java:");
    }

    @Test
    public void writeUncaughtException_nested() {
        writer.writeUncaughtException(new RuntimeException("outer message", new IllegalStateException("inner message")));

        String html = out.written();
        assertThat(html).contains("outer message", "java.lang.RuntimeException");
        assertThat(html).contains("inner message", "java.lang.IllegalStateException");
        assertThat(html).contains("SelftestHtmlWriterTest.java:");
    }

    @Test
    public void writeUnrunTests() {
        TestCase mock1 = mock(TestCase.class);
        when(mock1.getName()).thenReturn("mock1.name");
        TestCase mock2 = mock(TestCase.class);
        when(mock2.getName()).thenReturn("mock2.name");

        writer.writeUnrunTests(Arrays.asList(mock1, mock2));

        assertThat(out.written()).contains("Available test cases", "mock1.name", "mock2.name");
    }

    @Test
    public void writeTestOutcome_urlConResponse() throws Exception {
        TestRunData testRun = testRun("test1", "mn1", 234, TestRunResult.success());
        TestRunDataHelper.setResponse(testRun,
                dataBasedResponseWithBody("response body", "Header1: Value1_1", "Header1: Value1_2", "Header2: Value2"));

        writer.writeTestOutcome(testRun, snapshot(logInfos()), emptyContext());

        String html = out.written();
        assertThat(html).doesNotContain("presentationToggle"); // no toggle if only one presenter worked
        assertThat(html).doesNotContain("raw", "00000010"); // raw parser not active on urlConResponse
    }

    @Test
    public void writeTestOutcome_urlConResponse_json() throws Exception {
        TestRunData testRun = testRun("test1", "mn1", 234, TestRunResult.success());
        TestRunDataHelper.setResponse(testRun, dataBasedResponseWithBody("{\"a\":1}", "Content-Type: application/json"));

        writer.writeTestOutcome(testRun, snapshot(logInfos()), emptyContext());
    }

    @Test
    public void writeTestOutcome_rawPresentation() throws Exception {
        TestRunData testRun = testRun("test1", "mn1", 234, TestRunResult.success());
        TestRunDataHelper.setRequest(testRun, wireBasedRequestWithBody("request body"));
        TestRunDataHelper.setResponse(testRun, wireBasedResponseWithBody("response body"));

        writer.writeTestOutcome(testRun, snapshot(logInfos()), emptyContext());

        String html = out.written();
        assertThat(html).contains("raw", "request␣body", "nse␣bod");
    }

    @Test
    public void writeTestOutcome_formPresentation() throws Exception {
        TestRunData testRun = testRun("test1", "mn1", 234, TestRunResult.success());
        String formHeader = "Content-Type: application/x-www-form-urlencoded";
        String form = "key=value" + "&" + "encodedk%65y=encodedv%61l%75e" + "&" + "masked=aa%26bb%25cc%3ddd" + "&"
                + "valueAbsent=" + "&" + "keyOnly";

        TestRunDataHelper.setRequest(testRun, requestWithBody(form + "&request", formHeader));
        TestRunDataHelper.setResponse(testRun, responseWithBody(form + "&response", formHeader));

        writer.writeTestOutcome(testRun, snapshot(logInfos()), emptyContext());

        String html = out.written();
        assertThat(html).contains("key = value", "encodedkey = encodedvalue", "masked = aa&amp;bb%cc=dd", ">valueAbsent = <",
                "<span>keyOnly</span>");
    }

    @Test
    public void writeTestOutcome_jsonPresentation() throws Exception {
        TestRunData testRun = testRun("test1", "mn1", 234, TestRunResult.success());
        String json = "{\"a\":[1,2,3,],\"b\": {\"c\":1}, \"slash/key\" : \"slash/value\"}";
        TestRunDataHelper.setRequest(testRun, requestWithBody(json, "Content-Type: application/json"));

        writer.writeTestOutcome(testRun, snapshot(logInfos()), emptyContext());

        String html = out.written();
        assertThat(html).contains("<span>    1,</span><br><span>    2,</span><br>", "slash/key&quot;:&quot;slash/value");
    }

    @Test
    public void writeTestOutcome_singleNonPlainPresenter() throws Exception {
        HttpDetails breakingPlainPresenter = new WireBasedHttpDetails("(header)".getBytes(US_ASCII), "(body)".getBytes(UTF_8)) {
            @Override
            public String headerBlock() {
                throw new IllegalArgumentException("synthetic");
            }
        };
        TestRunData testRun = testRun("test1", "mn1", 234, TestRunResult.success());
        WrappedResponse response = wireBasedResponseWithBody("response body");
        ReflectionTestUtils.setField(response, "responseDetails", breakingPlainPresenter);
        TestRunDataHelper.setResponse(testRun, response);

        // test
        writer.writeTestOutcome(testRun, snapshot(logInfos()), emptyContext());

        String html = out.written();
        assertThat(html).contains("presentationToggle", "raw").doesNotContain("plain", "json");
        assertThat(StringUtils.countOccurrencesOf(html, "presentationToggle")).as("#presentationToggle").isEqualTo(1);
    }

    @Test
    public void writeTestOutcome_full() {
        HttpException exception = new HttpException(new RuntimeException("inner"),
                "static string: holla die waldfee!".getBytes(StandardCharsets.UTF_8));

        TestRunData testRun = testRun("test1", "mn1", 234, TestRunResult.error(exception));
        TestRunDataHelper.setRequest(testRun, requestWithBody("request body", "Header: Value"));
        TestRunDataHelper.setResponse(testRun, wireBasedResponseWithBody("response body"));

        List<LogAccess> logs = logInfos("LOG1", "LOG2", "ROOT");
        logs.get(0).buffer.add(SelftestEvent.of("runId1", "simple line logger 1"));
        logs.get(1).buffer.add(SelftestEvent.of("runId1", "simple line logger 2"));
        logs.get(2).buffer.add(SelftestEvent.of("runId1", "simple line root logger"));

        writer.writeTestOutcome(testRun, snapshot(logs), context("hallo", "bernd"));

        String html = out.written();
        assertThat(html).contains("test1", "(234ms)", "title=\"mn1 ", "[hallo, bernd]");
        assertThat(html).contains("REQUEST", "request body");
        assertThat(html).contains("RESPONSE", "response body");
        assertThat(html).contains("EXCEPTION DURING EXECUTION", "java.lang.RuntimeException");
        assertThat(html).contains("LOG1", "simple line logger 1");
        assertThat(html).contains("LOG2", "simple line logger 2");
        assertThat(html).contains("ROOT").contains("simple line root logger");
        assertThat(html.indexOf("root logger")).as("root logger occurrence").isLessThan(html.indexOf("logger 1"));
    }

    @Test
    public void writeTestOutcome_log() {

        TestRunData testRun = testRun("nameIrrelevant", "mn2", 200, TestRunResult.success());

        SynchronousLogBuffer singleBuffer = new SynchronousLogBuffer();
        Layout<ILoggingEvent> layout = getStdOutPatternFromConfig();

        List<LogAccess> logs = new LinkedList<>();
        logs.add(new LogAccess(names(), singleBuffer, new EventRendererStub()));
        logs.add(new LogAccess(names("ROOT"), singleBuffer, new EventRendererStub()));
        logs.add(new LogAccess(names("LOGBACK", "NOLAYOUT"), singleBuffer, new LogbackEventRenderer(Optional.empty())));
        logs.add(new LogAccess(names("LOGBACK", "WITHLAYOUT"), singleBuffer, new LogbackEventRenderer(Optional.of(layout))));

        singleBuffer.add(of("mn2", event("th1", Level.TRACE, "long message long message long message long message "
                + "long message long message long message long message long message long message long message long message ")));
        singleBuffer.add(of("mn2", event("th1", Level.TRACE, "trace evt http://www.insecure.de suffix")));
        singleBuffer.add(of("mn2", event("th1", Level.DEBUG, "debug evt http://www.insecure.de suffix")));
        singleBuffer.add(of("mn2", event("th1", Level.INFO, "info evt http://www.insecure.de suffix")));
        singleBuffer.add(of("mn2", event("th1", Level.INFO, "two lines\n2nd line http://toast.de:123/")));
        singleBuffer.add(of("otherRunId1", event("th1", Level.WARN, "warn evt https://www.secure.de suffix")));
        singleBuffer.add(of("otherRunId2", event("th1", Level.ERROR, "error evt two lines http://toast.de:123/\n2nd line")));

        writer.writeTestOutcome(testRun, snapshot(logs), emptyContext());

        String html = out.written();
        assertThat(html).contains("[otherRunId1]", "[th1]", "[INFO]", "info evt", "201", "LOGBACK</span> &rarr;",
                "<span class=\"mono\">WITHLAYOUT", "NOLAYOUT");
        assertThat(html).contains("<span class=\"url\">http://www.insecure.de</span>");
        assertThat(html).contains("<span class=\"url\">https://www.secure.de</span>");
        assertThat(html).contains("level-warn", "level-unknown");
        assertThat(html).contains("indicator foreignlogs", "indicator errorlogs", "indicator warnlogs", "indicator slowresponse");
    }

    @Test
    public void writeTestOutcome_overflow() {
        TestRunData testRun = testRun("nameIrrelevant", "mn3", 50, TestRunResult.success());
        List<LogAccess> logInfos = logInfos("ROOT");
        IntStream.range(0, 250).forEach(i -> logInfos.get(0).buffer.add(of("mn3", "msg")));

        writer.writeTestOutcome(testRun, snapshot(logInfos), emptyContext());

        String html = out.written();
        assertThat(html).contains("indicator logoverflow");
    }

    @Test
    public void writeTestOutcome_minimal_success() {
        TestRunData testRun = testRun("nameIrrelevant", "mn3", 50, TestRunResult.success());

        writer.writeTestOutcome(testRun, snapshot(logInfos()), emptyContext());

        String html = out.written();
        assertThat(html).contains("test-success", "SUCCESS");
        assertThat(html).doesNotContain("indicator foreignlogs", "indicator errorlogs", "indicator warnlogs",
                "indicator slowresponse");
    }

    @Test
    public void writeTestOutcome_minimal_failure() {
        TestRunData testRun = testRun("nameIrrelevant", "mn4", 50, TestRunResult.failure("test failed spectacularly"));

        writer.writeTestOutcome(testRun, snapshot(logInfos()), emptyContext());

        assertThat(out.written()).contains("test-failure", "FAILURE", "FAILED ASSERTION", "test failed spectacularly");
    }

    @Test
    public void writeTestOutcome_minimal_error() {
        TestRunData testRun = testRun("nameIrrelevant", "mn5", 50,
                TestRunResult.error(new UnsupportedOperationException("totally unsupported")));

        writer.writeTestOutcome(testRun, snapshot(logInfos()), emptyContext());

        assertThat(out.written()).contains("test-error", "ERROR", "EXCEPTION DURING EXECUTION", "totally unsupported",
                "SelftestHtmlWriterTest.java:");
    }

    private LoggingEvent event(String threadName, Level level, String message) {
        LoggingEvent loggingEvent = new LoggingEvent();
        loggingEvent.setTimeStamp(Instant.now().toEpochMilli());
        loggingEvent.setLevel(level);
        loggingEvent.setThreadName(threadName);
        loggingEvent.setLoggerName("com.ui.sooper.dooper.SelftestHtmlWriterTest");
        loggingEvent.setMessage(message);
        return loggingEvent;
    }

    private static Set<String> setOf(String... elements) {
        HashSet<String> set = new HashSet<>();
        for (String element : elements) {
            set.add(element);
        }
        return set;
    }

    private static List<String> names(String... string) {
        return Arrays.asList(string);
    }

    static List<LogAccess> logInfos(String... logNames) {
        List<LogAccess> logs = new LinkedList<>();
        if (logNames != null) {
            for (String logName : logNames) {
                logs.add(new LogAccess(names(logName), new SynchronousLogBuffer(), new EventRendererStub()));
            }
        }
        return logs;
    }

    private SimpleContext emptyContext() {
        return new SimpleContext();
    }

    static SimpleContext context(String... clues) {
        SimpleContext ctx = new SimpleContext();
        for (String clue : clues) {
            ctx.addClue(clue);
        }
        return ctx;
    }

    static TestRunData testRun(String testName, String runId, long duration, TestRunResult result) {
        TestRunData testRun = new TestRunData(testName, Instant.now(), runId);
        TestRunDataHelper.setDurationMillis(testRun, duration);
        TestRunDataHelper.setMaxDurationMillis(testRun, 100);
        TestRunDataHelper.setResult(testRun, result);
        return testRun;
    }

    private static WrappedResponse dataBasedResponseWithBody(String body, String... headerPairs) {
        Headers headers = toHeaders(headerPairs);
        TestResponse response = new TestResponse(200, headers, body);
        DataBasedHttpDetails details = new DataBasedHttpDetails("HTTP/1.1 200 OK (synthetic)", headers, body);
        return new WrappedResponse(response, details);
    }

    private static WrappedRequest requestWithBody(String body, String... headerPairs) {
        return wireBasedRequestWithBody(body, headerPairs);
    }

    private static WrappedResponse responseWithBody(String body, String... headerPairs) {
        return wireBasedResponseWithBody(body, headerPairs);
    }

    private static WrappedRequest wireBasedRequestWithBody(String body, String... headerPairs) {
        Headers headers = toHeaders(headerPairs);
        String headerBlock = stream(headers).map(pair -> pair.left + ": " + pair.right).collect(joining("\r\n")) + "\r\n\r\n";
        TestRequest request = new TestRequest("/path", "GET", headers, body);
        WireBasedHttpDetails details =
                new WireBasedHttpDetails(headerBlock.getBytes(StandardCharsets.US_ASCII), body.getBytes(StandardCharsets.UTF_8));
        WrappedRequest wrapped = new WrappedRequest(request);
        TestHttpHelper.setDetails(wrapped, details);
        return wrapped;
    }

    private static WrappedResponse wireBasedResponseWithBody(String body, String... headerPairs) {
        Headers headers = toHeaders(headerPairs);

        String headerBlock = stream(headers).map(pair -> pair.left + ": " + pair.right).collect(joining("\r\n")) + "\r\n\r\n";
        TestResponse response = new TestResponse(302, headers, body);
        WireBasedHttpDetails details =
                new WireBasedHttpDetails(headerBlock.getBytes(StandardCharsets.US_ASCII), body.getBytes(StandardCharsets.UTF_8));
        return new WrappedResponse(response, details);
    }

    private static Headers toHeaders(String... headerPairs) {
        Headers headers = new Headers();
        if (headerPairs != null) {
            for (String pair : headerPairs) {
                String[] split = pair.split(":");
                headers.add(split[0], split[1]);
            }
        }
        return headers;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Layout<ILoggingEvent> getStdOutPatternFromConfig() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("ROOT");
        List<Appender<?>> attachedAppenders = LogbackSupport.attachedAppenders(logger);
        Appender<?> appender = attachedAppenders.stream().filter(a -> "STDOUT".equals(a.getName())).findFirst().get();
        Encoder e = ((OutputStreamAppender) appender).getEncoder();
        return ((LayoutWrappingEncoder) e).getLayout();
    }

    private static void testOutput(String message) {
        out.write("<h4>&gt; " + message + " &lt;</h4>");
    }

    static class CapturingPrintWriter extends PrintWriter {
        boolean writeToFile;
        StringBuilder captured;

        CapturingPrintWriter(String filename, String charset) throws Exception {
            super(filename, charset);
            writeToFile = true;
            captured = new StringBuilder();
        }

        @Override
        public void write(String s) {
            if (writeToFile) {
                super.write(s);
            }
            captured.append(s);
        }

        public void reset() {
            captured = new StringBuilder();
            writeToFile = true;
        }

        public void captureOnly() {
            writeToFile = false;
        }

        public String written() {
            return captured.toString();
        }
    }

}
