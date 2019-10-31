package net.oneandone.httpselftest.log.logback;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static net.oneandone.httpselftest.log.logback.LogbackSupport.abortIfBroken;
import static net.oneandone.httpselftest.log.logback.LogbackSupport.attachedSelftestAppenders;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.util.ReflectionTestUtils.getField;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import net.oneandone.httpselftest.log.LogAccess;
import net.oneandone.httpselftest.log.LogDetails;

public class LogbackSupportTest {

    private static final org.slf4j.Logger ROOT_LOGGER = LoggerFactory.getLogger("ROOT");
    private static final org.slf4j.Logger SEPARATE_LOGGER = LoggerFactory.getLogger("ATTACHING");

    private static final String MDC_KEY = "MDC_KEY";
    private static final int NUM_LOGGERS = 2;
    private static Set<String> REQUEST_IDS = Collections.emptySet();

    Logger additivityLogger;
    LogbackSupport logbackSupport;
    private AtomicInteger countHolder;

    @Before
    public void setup() {
        MDC.remove(MDC_KEY);
        logbackSupport = new LogbackSupport(MDC_KEY);
        additivityLogger = (Logger) LoggerFactory.getLogger("forAdditivityTests");
        additivityLogger.setAdditive(true);
        countHolder = new AtomicInteger();
    }

    @Test
    public void test_appenderMapType() throws Exception {
        assertThat(getField(logbackSupport, "appenderMap")).isExactlyInstanceOf(IdentityHashMap.class);
    }

    @Test
    public void test_singleAttachment() throws Exception {
        assertNumSelftestAppenders(0);
        logbackSupport.runWithAttachedAppenders(REQUEST_IDS, this::storeAppenderCount);
        assertNumSelftestAppendersWas(NUM_LOGGERS);
        assertNumSelftestAppenders(0);
    }

    @Test
    public void test_multiAttachment() throws Exception {
        assertNumSelftestAppenders(0);
        logbackSupport.runWithAttachedAppenders(REQUEST_IDS, this::storeAppenderCount);
        assertNumSelftestAppendersWas(NUM_LOGGERS);
        assertNumSelftestAppenders(0);

        logbackSupport.runWithAttachedAppenders(REQUEST_IDS, this::storeAppenderCount);
        assertNumSelftestAppendersWas(NUM_LOGGERS);
        assertNumSelftestAppenders(0);
    }

    @Test
    public void test_attachmentWithAddionalLogger() throws Exception {
        assertNumSelftestAppenders(0);
        logbackSupport.runWithAttachedAppenders(REQUEST_IDS, this::storeAppenderCount);
        assertNumSelftestAppendersWas(NUM_LOGGERS);
        assertNumSelftestAppenders(0);

        additivityLogger.setAdditive(false);

        assertNumSelftestAppenders(0);
        logbackSupport.runWithAttachedAppenders(REQUEST_IDS, this::storeAppenderCount);
        assertNumSelftestAppendersWas(NUM_LOGGERS + 1);
        assertNumSelftestAppenders(0);
    }

    @Test
    public void test_abortIfBrokenWorks() throws Exception {
        abortIfBroken(null); // does not throw
        abortIfBroken(emptyList()); // does not throw
        assertThatThrownBy(() -> abortIfBroken(Collections.singletonList(new IllegalArgumentException("huibuh"))))
                .isInstanceOf(IllegalStateException.class) //
                .hasMessageContaining("Refusing operation.") //
                .hasMessageContaining("java.lang.IllegalArgumentException: huibuh") //
                .hasMessageContaining("at net.oneandone.httpselftest");
    }

    @Test
    public void test_getLogs() throws Exception {
        logbackSupport.runWithAttachedAppenders(new HashSet<>(Arrays.asList("id1")), () -> {
            List<LogAccess> accessors = logbackSupport.getLogs("id1");

            logMsg(ROOT_LOGGER, "id1", "msg-a");

            List<LogDetails> snapshot = LogAccess.snapshot(accessors);
            assertMessage(snapshot, asList("ROOT", "STDOUT"), "msg-a");
            assertNumMessages(snapshot, 1);

            logMsg(SEPARATE_LOGGER, "id1", "msg-b");

            snapshot = LogAccess.snapshot(accessors);
            assertMessage(snapshot, asList("ATTACHING", "STDOUT"), "msg-b");
            assertMessage(snapshot, asList("ATTACHING", "FILE"), "msg-b", "WARN WARN");
            assertNumMessages(snapshot, 2);
        });
    }

    @Test
    public void test_getLogs_unknownRunId() throws Exception {
        logbackSupport.runWithAttachedAppenders(new HashSet<>(Arrays.asList("id1")), () -> {
            assertThatThrownBy(() -> logbackSupport.getLogs("otherid")).hasMessageContainingAll("Unknown", "otherid");
        });
    }

    @Test
    public void test_getLogs_outsideScope() throws Exception {
        List<LogAccess> accessors = logbackSupport.getLogs("id1");
        logMsg(ROOT_LOGGER, "id1", "msg-a");

        List<LogDetails> snapshot = LogAccess.snapshot(accessors);
        assertNumMessages(snapshot, 0);
    }

    private void assertNumMessages(List<LogDetails> snapshot, int expected) {
        long eventsInSnapshot = snapshot.stream() //
                .map(details -> details.logs) //
                .flatMap(logs -> logs.events.stream()) //
                .count();
        assertThat(eventsInSnapshot).as("number of events").isEqualTo(expected);
    }

    private void assertMessage(List<LogDetails> snapshot, List<String> logNames, String... msgParts) {
        LogDetails appenderDetails = snapshot.stream().filter(details -> details.logNames.equals(logNames)).findFirst().get();
        List<String> logLines = appenderDetails.logs.events.stream() //
                .map(evt -> appenderDetails.renderer.doLayout(evt.event)) //
                .collect(toList());
        assertThat(logLines).anySatisfy(line -> {
            assertThat(line).contains(msgParts);
        });
    }

    private void logMsg(org.slf4j.Logger logger, String runId, String msg) {
        MDC.put(MDC_KEY, runId);
        logger.warn(msg);
    }

    private void storeAppenderCount() {
        countHolder.set(allAttachedSelftestAppenders().length);
    }

    private void assertNumSelftestAppenders(int expected) {
        assertThat(allAttachedSelftestAppenders()).as("attached appenders").hasSize(expected);
    }

    private void assertNumSelftestAppendersWas(int expected) {
        assertThat(countHolder.get()).as("attached appenders count during execution").isEqualTo(expected);
    }

    private Appender<?>[] allAttachedSelftestAppenders() {
        List<Appender<?>> appenders =
                allLogbackLoggers().stream().flatMap(logger -> attachedSelftestAppenders(logger).stream()).collect(toList());
        return appenders.toArray(new Appender<?>[0]);
    }

    private List<Logger> allLogbackLoggers() {
        return ((LoggerContext) LoggerFactory.getILoggerFactory()).getLoggerList();
    }

}
