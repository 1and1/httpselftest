package net.oneandone.httpselftest.log;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class EventRenderer {

    public static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss,SSS").withZone(ZoneId.systemDefault());

    public String doLayout(Object o) {
        return o.toString();
    }

    public String getLevel(Object o) {
        return "unknown";
    }

}
