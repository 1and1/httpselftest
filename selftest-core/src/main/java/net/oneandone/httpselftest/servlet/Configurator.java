package net.oneandone.httpselftest.servlet;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static net.oneandone.httpselftest.servlet.SelftestServlet.PROP_CONFIGGROUPS;
import static net.oneandone.httpselftest.servlet.SelftestServlet.PROP_CREDENTIALS;
import static net.oneandone.httpselftest.servlet.SelftestServlet.PROP_LOGGER;
import static net.oneandone.httpselftest.servlet.SelftestServlet.PROP_OVERRIDE_MDC_KEY;
import static net.oneandone.httpselftest.servlet.SelftestServlet.PROP_OVERRIDE_PATH;
import static net.oneandone.httpselftest.servlet.SelftestServlet.PROP_OVERRIDE_PORT;
import static net.oneandone.httpselftest.test.run.TestRunner.X_REQUEST_ID;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.servlet.ServletConfig;

import net.oneandone.httpselftest.log.InactiveLogSupport;
import net.oneandone.httpselftest.log.LogSupport;
import net.oneandone.httpselftest.log.logback.LogbackSupport;

public final class Configurator {

    private static final List<String> LOGGER_VALUES = Arrays.asList("none", "logback");

    private Configurator() {
    }

    static Optional<String> getCredentials(ServletConfig config) {
        String credentials = config.getInitParameter(PROP_CREDENTIALS);

        if (credentials == null || credentials.trim().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(credentials.trim());
    }

    static Optional<Integer> getPort(ServletConfig config) {
        Optional<Integer> port = Optional.ofNullable(config.getInitParameter(PROP_OVERRIDE_PORT)).map(Integer::parseInt);

        if (port.isPresent() && port.get() <= 0) {
            throw new IllegalStateException(String.format("invalid value for %s: %s", PROP_OVERRIDE_PORT, port.get()));
        }

        return port;
    }

    static Optional<String> getContextPath(ServletConfig config) {
        return Optional.ofNullable(config.getInitParameter(PROP_OVERRIDE_PATH));
    }

    static LogSupport getLogSupport(ServletConfig config) {
        String mdcKey = Optional.ofNullable(config.getInitParameter(PROP_OVERRIDE_MDC_KEY)).orElse(X_REQUEST_ID);
        String loggerType = Optional.ofNullable(config.getInitParameter(PROP_LOGGER)).orElse("logback");

        if (!LOGGER_VALUES.contains(loggerType)) {
            throw new IllegalStateException(
                    String.format("invalid value for %s: %s (possible values: %s)", PROP_LOGGER, loggerType, LOGGER_VALUES));
        }

        return loggerType.equals("logback") ? new LogbackSupport(mdcKey) : new InactiveLogSupport();
    }

    static Optional<List<String>> getConfigGroups(ServletConfig config) {
        String groups = config.getInitParameter(PROP_CONFIGGROUPS);

        if (groups == null || groups.trim().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(asList(groups.split(",")).stream().map(String::trim).collect(toList()));
    }

}
