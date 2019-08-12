package net.oneandone.httpselftest.log.logback;

import static java.util.stream.Collectors.toList;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import net.oneandone.httpselftest.log.EventRenderer;
import net.oneandone.httpselftest.log.LogAccess;
import net.oneandone.httpselftest.log.LogSupport;
import net.oneandone.httpselftest.log.SynchronousLogBuffer;

public class LogbackSupport implements LogSupport {

    static final String APPENDER_PREFIX = "selftestAppender";

    private final Map<Logger, BoundedInMemoryAppender<ILoggingEvent>> appenderMap;

    private String mdcKey;

    private static final Object LOCK = new Object(); // static because logback is static
    private static List<Exception> exceptions = new LinkedList<>(); // synchronized by "LOCK"

    public LogbackSupport(String mdcKey) {
        this.mdcKey = mdcKey;
        appenderMap = new IdentityHashMap<>();
    }

    @Override
    public void runWithAttachedAppenders(Set<String> runIds, Runnable c) {
        synchronized (LOCK) {
            try {
                attach(runIds);
                c.run();
            } finally {
                detach();
            }
        }
    }

    // visible for testing
    protected void attach(Set<String> runIds) {
        abortIfBroken(exceptions);
        try {
            allLoggers().stream().filter(LogbackSupport::isAppendingDirectly)
                    .forEach(logger -> logger.addAppender(getAppender(logger, runIds)));
        } catch (Exception e) {
            exceptions.add(e);
            throw e;
        }
    }

    private void detach() {
        try {
            appenderMap.forEach((logger, appender) -> logger.detachAppender(appender));
            appenderMap.clear();
        } catch (Exception e) {
            exceptions.add(e);
            throw e;
        }
    }

    // this is hopefully never needed. it is there for the case that something unexpected breaks in logback.
    public static void abortIfBroken(List<Exception> exceptions) {
        if (exceptions != null && !exceptions.isEmpty()) {
            StringBuilder msg = new StringBuilder("Refusing operation. There were logback exceptions. Please investigate.\n");
            msg.append("Exceptions: \n");
            for (Exception e : exceptions) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                msg.append(sw.toString()).append("\n\n");
            }
            throw new IllegalStateException(msg.toString());
        }
    }

    @Override
    public List<LogAccess> getLogs(String runId) {
        return appenderMap.entrySet().stream() //
                .flatMap(entry -> {
                    Logger logger = entry.getKey();
                    SynchronousLogBuffer buffer = entry.getValue().getBuffer(runId);
                    List<String> names = new LinkedList<>();
                    Optional.ofNullable(logger.getName()).ifPresent(names::add);
                    List<Appender<?>> attachedAppenders = attachedAppenders(logger).stream()
                            .filter(a -> !(a instanceof BoundedInMemoryAppender)).collect(toList());

                    return attachedAppenders.stream().map(appender -> {
                        LinkedList<String> copy = new LinkedList<>(names);
                        Optional.ofNullable(appender.getName()).ifPresent(copy::add);
                        Optional<Layout<ILoggingEvent>> layout = extractLayout(appender);
                        EventRenderer renderer = new LogbackEventRenderer(layout);

                        return new LogAccess(copy, buffer, renderer);
                    });
                }).collect(toList());
    }

    @SuppressWarnings({ "unchecked" })
    private Optional<Layout<ILoggingEvent>> extractLayout(Appender<?> appender) {
        if (appender instanceof OutputStreamAppender) {
            Encoder<ILoggingEvent> encoder = ((OutputStreamAppender<ILoggingEvent>) appender).getEncoder();
            if (encoder instanceof LayoutWrappingEncoder) {
                return Optional.of(((LayoutWrappingEncoder<ILoggingEvent>) encoder).getLayout());
            }
        } else if (appender instanceof SyslogAppender) {
            return Optional.of(((SyslogAppender) appender).getLayout());
        }
        return Optional.empty();
    }

    private BoundedInMemoryAppender<ILoggingEvent> getAppender(Logger logger, Set<String> runIds) {
        return appenderMap.computeIfAbsent(logger, logr -> {
            BoundedInMemoryAppender<ILoggingEvent> appender = new BoundedInMemoryAppender<>(runIds, mdcKey);
            appender.setName(APPENDER_PREFIX + "_" + logr.getName());
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            appender.setContext(context);
            return appender;
        });
    }

    private static boolean isAppendingDirectly(Logger logger) {
        if (!attachedSelftestAppenders(logger).isEmpty()) {
            throw new IllegalStateException("Appender already attached. This should never happen! Please investigate.");
        }
        return !logger.isAdditive() || logger.getName().equals(org.slf4j.Logger.ROOT_LOGGER_NAME);
    }

    private static List<Logger> allLoggers() {
        return ((LoggerContext) LoggerFactory.getILoggerFactory()).getLoggerList();
    }

    public static List<Appender<?>> attachedSelftestAppenders(Logger logger) {
        return attachedAppenders(logger).stream().filter(a -> a instanceof BoundedInMemoryAppender).collect(toList());
    }

    public static List<Appender<?>> attachedAppenders(Logger logger) {
        List<Appender<?>> appenders = new LinkedList<>();
        Iterator<Appender<ILoggingEvent>> it = logger.iteratorForAppenders();
        while (it.hasNext()) {
            Appender<?> appender = it.next();
            appenders.add(appender);
        }
        return appenders;
    }

}
