package net.oneandone.httpselftest.log.logback;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.MDC;

import net.oneandone.httpselftest.log.SelftestEvent;
import net.oneandone.httpselftest.log.SynchronousLogBuffer;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.FilterReply;

public class BoundedInMemoryAppender<E> extends ContextAwareBase implements Appender<E> {

    private String name;

    private boolean started;

    private final String requestIdHolder;

    private final Map<String, SynchronousLogBuffer> buffers;


    public BoundedInMemoryAppender(Set<String> runIds, String requestIdHolder) {
        buffers = runIds.stream().collect(toMap(id -> id, id -> new SynchronousLogBuffer()));
        this.requestIdHolder = requestIdHolder;
    }

    @Override
    public void doAppend(E event) throws LogbackException {
        String requestId = MDC.get(requestIdHolder);
        if (requestId != null) {
            SynchronousLogBuffer buffer = buffers.get(requestId);
            if (buffer != null) {
                buffer.add(SelftestEvent.of(requestId, event));
            }
        }
    }

    public SynchronousLogBuffer getBuffer(String runId) {
        return buffers.get(runId);
    }

    @Override
    public void start() {
        this.started = true;
    }

    @Override
    public void stop() {
        this.started = false;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void addFilter(Filter<E> newFilter) {
        // ignore filter chain
    }

    @Override
    public void clearAllFilters() {
        // noop
    }

    @Override
    public List<Filter<E>> getCopyOfAttachedFiltersList() {
        return new ArrayList<>();
    }

    @Override
    public FilterReply getFilterChainDecision(E event) {
        // ignore filter chain
        return FilterReply.NEUTRAL;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

}
