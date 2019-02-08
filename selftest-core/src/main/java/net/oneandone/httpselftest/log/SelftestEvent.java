package net.oneandone.httpselftest.log;

public class SelftestEvent {

    public final String runId;
    public final Object event;

    private SelftestEvent(String runId, Object event) {
        this.runId = runId;
        this.event = event;
    }

    public static SelftestEvent of(String runId, Object event) {
        return new SelftestEvent(runId, event);
    }

}
