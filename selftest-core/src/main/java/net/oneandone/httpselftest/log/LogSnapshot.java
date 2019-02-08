package net.oneandone.httpselftest.log;

import static java.util.Collections.unmodifiableList;

import java.util.List;

public class LogSnapshot {

    public final List<SelftestEvent> events;
    public final boolean hasOverflown;

    public LogSnapshot(List<SelftestEvent> events, boolean hasOverflown) {
        this.events = unmodifiableList(events);
        this.hasOverflown = hasOverflown;
    }

}
