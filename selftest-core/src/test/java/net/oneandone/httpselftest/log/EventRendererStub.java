package net.oneandone.httpselftest.log;

public class EventRendererStub implements EventRenderer {

    @Override
    public String doLayout(Object o) {
        return o.toString();
    }

    @Override
    public String getLevel(Object o) {
        return "unknown";
    }

}
