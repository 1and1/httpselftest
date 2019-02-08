package net.oneandone.httpselftest.log;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import net.oneandone.httpselftest.log.logback.BoundedInMemoryAppender;

public class AppenderTest {

    private BoundedInMemoryAppender<String> appender;
    private static Set<String> RUN_IDS = new HashSet<>(Arrays.asList("toastarena", "runId"));

    @Before
    public void setup() {
        appender = new BoundedInMemoryAppender<>(RUN_IDS, "X-REQUEST-ID");
        MDC.remove("X-REQUEST-ID");
    }

    @Test
    public void startsAndStops() throws Exception {
        BoundedInMemoryAppender<?> appender = new BoundedInMemoryAppender<>(RUN_IDS, "X-REQUEST-ID");
        assertThat(appender.isStarted()).isFalse();
        appender.start();
        assertThat(appender.isStarted()).isTrue();
        appender.stop();
        assertThat(appender.isStarted()).isFalse();
    }

    @Test
    public void doesNotAppendUnmarkedEvent() throws Exception {
        appender.doAppend("regularEvent");

        assertThat(appender.getBuffer("runId").snapshot().events).isEmpty();
        assertThat(appender.getBuffer("toastarena").snapshot().events).isEmpty();
    }

    @Test
    public void appendsMarkedEvent() throws Exception {
        markThreadWithSelftestRunId("toastarena");
        appender.doAppend("selftestEvent");

        LogSnapshot snapshot = appender.getBuffer("toastarena").snapshot();
        assertThat(snapshot.events).hasSize(1);
        assertThat(snapshot.hasOverflown).as("overflown").isFalse();

        List<SelftestEvent> events = snapshot.events;
        assertThat(events.get(0).runId).isEqualTo("toastarena");
        assertThat(events.get(0).event).isEqualTo("selftestEvent");
    }

    @Test
    public void isBounded() throws Exception {
        markThreadWithSelftestRunId("runId");
        IntStream.range(0, 250).forEach(i -> appender.doAppend("event(" + i + ")"));
        LogSnapshot snapshot = appender.getBuffer("runId").snapshot();

        assertThat(snapshot.hasOverflown).as("overflown").isTrue();
        assertThat(snapshot.events.size()).isEqualTo(200);

        List<Object> messages = snapshot.events.stream().map(event -> event.event).collect(toList());
        assertThat(messages).contains("event(50)", "event(249)").doesNotContain("event(0)", "event(49)");
    }

    @Test
    public void snapshotClears() throws Exception {
        markThreadWithSelftestRunId("runId");
        appender.doAppend("irrelevant");

        assertThat(appender.getBuffer("runId").snapshot().events).isNotEmpty();
        assertThat(appender.getBuffer("runId").snapshot().events).isEmpty();
    }

    private void markThreadWithSelftestRunId(String runId) {
        MDC.put("X-REQUEST-ID", runId);
    }

}
