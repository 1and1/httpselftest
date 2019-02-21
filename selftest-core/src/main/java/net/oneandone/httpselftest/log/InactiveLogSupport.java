package net.oneandone.httpselftest.log;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Empty LogSupport implementation. Use if logging framework is not supported or log output is not desired.
 */
public class InactiveLogSupport implements LogSupport {

    @Override
    public void runWithAttachedAppenders(Set<String> runIds, Runnable c) {
        c.run();
    }

    @Override
    public List<LogAccess> getLogs(String runIds) {
        return Collections.emptyList();
    }

}
