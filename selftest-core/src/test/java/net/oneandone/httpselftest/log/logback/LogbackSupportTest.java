package net.oneandone.httpselftest.log.logback;

import static net.oneandone.httpselftest.log.logback.LogbackSupport.abortIfBroken;
import static net.oneandone.httpselftest.log.logback.LogbackSupport.attachedSelftestAppenders;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.util.ReflectionTestUtils.getField;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;

public class LogbackSupportTest {

    private static final int NUM_LOGGERS = 2;
    private static Set<String> REQUEST_IDS = Collections.emptySet();

    Logger additivityLogger;
    LogbackSupport sut;
    private AtomicInteger countHolder;

    @Before
    public void setup() {
        sut = new LogbackSupport("MDC_KEY");
        additivityLogger = (Logger) LoggerFactory.getLogger("forAdditivityTests");
        additivityLogger.setAdditive(true);
        countHolder = new AtomicInteger();
    }

    // TODO test getLogs

    @Test
    public void appenderMapType() throws Exception {
        assertThat(getField(sut, "appenderMap")).isExactlyInstanceOf(IdentityHashMap.class);
    }

    @Test
    public void singleAttachment() throws Exception {
        assertNumSelftestAppenders(0);
        sut.runWithAttachedAppenders(REQUEST_IDS, this::storeAppenderCount);
        assertNumSelftestAppendersWas(NUM_LOGGERS);
        assertNumSelftestAppenders(0);
    }

    @Test
    public void multiAttachment() throws Exception {
        assertNumSelftestAppenders(0);
        sut.runWithAttachedAppenders(REQUEST_IDS, this::storeAppenderCount);
        assertNumSelftestAppendersWas(NUM_LOGGERS);
        assertNumSelftestAppenders(0);

        sut.runWithAttachedAppenders(REQUEST_IDS, this::storeAppenderCount);
        assertNumSelftestAppendersWas(NUM_LOGGERS);
        assertNumSelftestAppenders(0);
    }

    @Test
    public void attachmentWithAddionalLogger() throws Exception {
        assertNumSelftestAppenders(0);
        sut.runWithAttachedAppenders(REQUEST_IDS, this::storeAppenderCount);
        assertNumSelftestAppendersWas(NUM_LOGGERS);
        assertNumSelftestAppenders(0);

        additivityLogger.setAdditive(false);

        assertNumSelftestAppenders(0);
        sut.runWithAttachedAppenders(REQUEST_IDS, this::storeAppenderCount);
        assertNumSelftestAppendersWas(NUM_LOGGERS + 1);
        assertNumSelftestAppenders(0);
    }

    @Test
    public void abortIfBrokenWorks() throws Exception {
        abortIfBroken(null); // does not throw
        abortIfBroken(emptyList()); // does not throw
        assertThatThrownBy(() -> abortIfBroken(Collections.singletonList(new IllegalArgumentException("huibuh"))))
                .isInstanceOf(IllegalStateException.class) //
                .hasMessageContaining("Refusing operation.") //
                .hasMessageContaining("java.lang.IllegalArgumentException: huibuh") //
                .hasMessageContaining("at net.oneandone.httpselftest");
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
