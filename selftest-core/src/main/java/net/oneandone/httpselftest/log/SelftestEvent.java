package net.oneandone.httpselftest.log;

public class SelftestEvent {

    public final Object event;

    private SelftestEvent(Object event) {
        this.event = event;
    }

    public static SelftestEvent of(Object event) {
        return new SelftestEvent(event);
    }

}
