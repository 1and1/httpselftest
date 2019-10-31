package net.oneandone.httpselftest.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class LogInfoTest {

    @Test
    public void snapshotWorksOnMultipleAppenders() throws Exception {
        SynchronousLogBuffer theOnlyBuffer = new SynchronousLogBuffer();
        theOnlyBuffer.add(SelftestEvent.of("log msg"));
        LogAccess log1 = new LogAccess(Collections.singletonList("appender1"), theOnlyBuffer, new EventRendererStub());
        LogAccess log2 = new LogAccess(Collections.singletonList("appender2"), theOnlyBuffer, new EventRendererStub());

        List<LogDetails> snapshot = LogAccess.snapshot(Arrays.asList(new LogAccess[] { log1, log2 }));

        assertThat(snapshot.get(0).logs.events).hasSize(1);
        assertThat(snapshot.get(0).logs.events.get(0).event).isEqualTo("log msg");

        assertThat(snapshot.get(1).logs.events).hasSize(1);
        assertThat(snapshot.get(1).logs.events.get(0).event).isEqualTo("log msg");
    }

}
