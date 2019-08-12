package net.oneandone.httpselftest.log;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public interface EventRenderer {

    static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss,SSS").withZone(ZoneId.systemDefault());

    String doLayout(Object o);

    String getLevel(Object o);

}
