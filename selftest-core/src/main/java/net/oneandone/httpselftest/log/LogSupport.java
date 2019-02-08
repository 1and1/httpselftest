package net.oneandone.httpselftest.log;

import java.util.List;
import java.util.Set;

public interface LogSupport {

    void runWithAttachedAppenders(Set<String> runIds, Runnable c);

    List<LogAccess> getLogs(String runIds);

}
