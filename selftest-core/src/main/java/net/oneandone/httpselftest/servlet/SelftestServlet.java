package net.oneandone.httpselftest.servlet;

import static net.oneandone.httpselftest.writer.SelftestHtmlWriter.CONFIG_ID;
import static net.oneandone.httpselftest.writer.SelftestHtmlWriter.EXECUTE;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oneandone.httpselftest.log.LogSupport;
import net.oneandone.httpselftest.test.api.TestCase;
import net.oneandone.httpselftest.test.api.TestConfigs;
import net.oneandone.httpselftest.test.run.TestRunner;
import net.oneandone.httpselftest.writer.SelfTestJsonWriter;
import net.oneandone.httpselftest.writer.SelfTestWriter;
import net.oneandone.httpselftest.writer.SelftestHtmlWriter;

/**
 * If you implement a subclass of this servlet, keep in mind that the application calling itself locally may have more access
 * rights than the user who is calling the servlet.
 *
 * Test cases are executed in alphabetic order.
 */
public abstract class SelftestServlet extends HttpServlet {

    public static final String PROP_CREDENTIALS = "selftest.credentials";
    public static final String PROP_CONFIGGROUPS = "selftest.configgroups";
    public static final String PROP_LOGGER = "selftest.logger";
    public static final String PROP_OVERRIDE_PORT = "selftest.override.port";
    public static final String PROP_OVERRIDE_PATH = "selftest.override.contextpath";
    public static final String PROP_OVERRIDE_MDC_KEY = "selftest.override.mdckey";

    public static final String PARAMETER_PREFIX = "p-";

    private final ReentrantLock requestLock = new ReentrantLock();

    // fields synchronized by "requestLock"
    private Instant lastTestrunStart = null;
    private String lastTestrunIp = null;

    // fields synchronized by servlet life-cycle
    private LogSupport logSupport;
    private Optional<String> configuredCredentials;
    private Optional<Integer> configuredPort;
    private Optional<String> configuredContextPath;
    private Optional<List<String>> configuredConfigGroups;

    /**
     * @return all predefined test properties
     */
    protected abstract TestConfigs.Builder getConfigs();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        configuredCredentials = Configurator.getCredentials(config);
        configuredPort = Configurator.getPort(config);
        configuredContextPath = Configurator.getContextPath(config);
        logSupport = Configurator.getLogSupport(config);
        configuredConfigGroups = Configurator.getConfigGroups(config);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp, writer -> get(req, writer));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp, writer -> post(req, writer));
    }

    private void handleRequest(HttpServletRequest req, HttpServletResponse resp, Consumer<SelfTestWriter> businessLogic)
            throws IOException {
        if (!Authorization.isOk(req, configuredCredentials)) {
            resp.setStatus(401);
            resp.setHeader("WWW-Authenticate", "Basic");
            return;
        }

        resp.setCharacterEncoding("UTF-8");
        final SelfTestWriter writer;

        if (req.getHeader("Accept") != null && req.getHeader("Accept").contains("application/json")) {
            writer = new SelfTestJsonWriter(resp.getWriter());
            resp.addHeader("Content-Type", "application/json");
        } else {
            writer = new SelftestHtmlWriter(resp.getWriter());
            resp.addHeader("Content-Type", "text/html; charset=UTF-8");
        }

        if (requestLock.tryLock()) {
            try {
                executeWritingUncaughtExceptions(resp, writer, businessLogic);
            } finally {
                requestLock.unlock();
            }
        } else {
            writer.writeText("The selftest servlet is currently in use. "
                    + "Please re-send your request in a couple seconds or consider switching to another node.");
        }
    }

    private void get(HttpServletRequest req, final SelfTestWriter writer) {
        final TestConfigs configs = new TestConfigs(getConfigs());
        final String callerIp = req.getRemoteAddr();
        writer.writePageStart(configs, relevantConfigIds(configs, req), determineValuesForGet(req, configs), servletName(req),
                determineAppBaseUrl(req), lastTestrunStart, callerIp, lastTestrunIp);

        final List<TestCase> tests = tests();
        writer.writeUnrunTests(tests);
    }

    private void post(HttpServletRequest req, final SelfTestWriter writer) {
        final TestConfigs configs = new TestConfigs(getConfigs());
        final String callerIp = req.getRemoteAddr();

        final TestConfigs.Values testParams = extractParamsFromRequest(req, configs);
        final String appUrl = determineAppBaseUrl(req);

        if (req.getParameter(EXECUTE) != null) {
            lastTestrunStart = Instant.now();
            lastTestrunIp = callerIp;
        }

        writer.writePageStart(configs, relevantConfigIds(configs, req), testParams, servletName(req), appUrl, lastTestrunStart,
                callerIp, lastTestrunIp);

        if (req.getParameter(EXECUTE) != null) {
            final List<TestCase> tests = tests();
            TestRunner runner = new TestRunner(writer, testParams, appUrl, tests, logSupport);
            runner.runAll();
        } else {
            writer.writeText("Unrecognized request!");
        }
    }

    // TODO can this be simplified?
    private Set<String> relevantConfigIds(TestConfigs configs, HttpServletRequest req) {
        if (!configuredConfigGroups.isPresent()) {
            return configs.getIds();
        }
        Optional<String> requestGroup = determineConfigGroupOfRequest(req);
        if (!requestGroup.isPresent()) {
            return configs.getIds();
        }
        String hostGroup = requestGroup.get();
        return configs.getIds().stream().filter(configId -> {
            Optional<String> configGroup = firstMatchingGroup(configId);
            return configGroup.map(hostGroup::equals).orElse(false);
        }).collect(toSet());
    }

    private Optional<String> determineConfigGroupOfRequest(HttpServletRequest req) {
        String hostname = req.getHeader("Host");
        if (hostname == null) {
            return Optional.empty();
        }
        int dotIndex = hostname.indexOf('.');
        String hostprefix = dotIndex >= 0 ? hostname.substring(0, dotIndex) : hostname;
        return firstMatchingGroup(hostprefix);
    }

    private Optional<String> firstMatchingGroup(String hostprefix) {
        return configuredConfigGroups.get().stream().filter(hostprefix::contains).findFirst();
    }

    private List<TestCase> tests() {
        List<TestCase> testcases = new LinkedList<>();

        for (Class<?> clazz : getClass().getDeclaredClasses()) {
            if (TestCase.class.isAssignableFrom(clazz)) {
                try {
                    final TestCase testcase = (TestCase) clazz.newInstance();
                    testcases.add(testcase);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Failed to instantiate test case '" + clazz.getSimpleName() + "'", e);
                }
            }
        }

        return testcases.stream().sorted(comparing(testcase -> testcase.getClass().getSimpleName())).collect(toList());
    }

    private static String servletName(HttpServletRequest req) {
        String requestURI = req.getRequestURI();
        return requestURI.substring(requestURI.lastIndexOf('/') + 1);
    }

    private static void executeWritingUncaughtExceptions(HttpServletResponse resp, SelfTestWriter writer,
            Consumer<SelfTestWriter> businessLogic) {
        try {
            businessLogic.accept(writer);
        } catch (Exception e) {
            resp.setStatus(500);
            writer.writeUncaughtException(e);
        } catch (Throwable t) {
            resp.setStatus(500);
            writer.writeUncaughtException(t);
            throw t;
        } finally {
            writer.writePageEnd();
        }
    }

    private String determineAppBaseUrl(HttpServletRequest req) {
        int port = configuredPort.orElse(req.getLocalPort());

        String contextPath = configuredContextPath.orElseGet(() -> getServletContext().getContextPath());
        contextPath = guaranteeLeadingAndTrailingSlash(contextPath);

        return "http://localhost:" + port + contextPath;
    }

    static String guaranteeLeadingAndTrailingSlash(String contextPath) {
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        if (!contextPath.endsWith("/")) {
            contextPath = contextPath + "/";
        }
        return contextPath;
    }

    private static TestConfigs.Values determineValuesForGet(HttpServletRequest req, final TestConfigs configs) {
        final String configId = req.getParameter(CONFIG_ID);
        if (configId == null || !configs.getIds().contains(configId)) {
            return configs.createEmpty();
        } else {
            return configs.create(configId);
        }
    }

    private static TestConfigs.Values extractParamsFromRequest(HttpServletRequest req, final TestConfigs configs) {
        String configId = req.getParameter(CONFIG_ID);

        Map<String, String> testParams = new HashMap<>();
        for (String key : req.getParameterMap().keySet()) {
            if (key.startsWith(PARAMETER_PREFIX)) {
                testParams.put(key.substring(PARAMETER_PREFIX.length()), req.getParameter(key));
            }
        }

        if (configId == null || !configs.getIds().contains(configId)) {
            return configs.create(testParams);
        } else {
            return configs.create(configId, testParams);
        }
    }

}
