package net.oneandone.httpselftest.log.logback;

import java.time.Instant;
import java.util.Optional;

import net.oneandone.httpselftest.log.EventRenderer;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Layout;

public class LogbackEventRenderer extends EventRenderer {

    private final Optional<Layout<ILoggingEvent>> layout;

    public LogbackEventRenderer(Optional<Layout<ILoggingEvent>> layout) {
        this.layout = layout;
    }

    @Override
    public String doLayout(Object o) {
        LoggingEvent evt = (LoggingEvent) o;

        if (layout.isPresent()) {
            return layout.get().doLayout(evt);
        } else {
            String timestamp = TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(evt.getTimeStamp()));
            String threadName = evt.getThreadName();
            String message = evt.toString();

            return String.format("%s [%s] %s", timestamp, threadName, message);
        }
    }

    @Override
    public String getLevel(Object o) {
        LoggingEvent evt = (LoggingEvent) o;
        return evt.getLevel().levelStr;
    }
}
