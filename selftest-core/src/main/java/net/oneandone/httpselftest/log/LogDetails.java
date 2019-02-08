package net.oneandone.httpselftest.log;

import java.util.List;

public class LogDetails {

    public final List<String> logNames;
    public final LogSnapshot logs;
    public final EventRenderer renderer;

    public LogDetails(List<String> logNames, LogSnapshot logs, EventRenderer renderer) {
        this.logNames = logNames;
        this.logs = logs;
        this.renderer = renderer;
    }

}
