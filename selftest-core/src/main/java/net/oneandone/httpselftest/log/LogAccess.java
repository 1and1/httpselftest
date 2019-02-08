package net.oneandone.httpselftest.log;

import static java.util.stream.Collectors.toList;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class LogAccess {

    public final List<String> logNames;
    public final SynchronousLogBuffer buffer;
    public final EventRenderer renderer;

    public LogAccess(List<String> logNames, SynchronousLogBuffer buffer, EventRenderer renderer) {
        this.logNames = logNames;
        this.buffer = buffer;
        this.renderer = renderer;
    }

    public static List<LogDetails> snapshot(List<LogAccess> loggers) {

        Map<SynchronousLogBuffer, LogSnapshot> snapshots = new IdentityHashMap<>();

        loggers.forEach(info -> {
            if (!snapshots.containsKey(info.buffer)) {
                snapshots.put(info.buffer, info.buffer.snapshot());
            }
        });

        return loggers.stream().map(info -> new LogDetails(info.logNames, snapshots.get(info.buffer), info.renderer))
                .collect(toList());
    }

}
