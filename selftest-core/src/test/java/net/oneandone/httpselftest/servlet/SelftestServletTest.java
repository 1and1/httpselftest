package net.oneandone.httpselftest.servlet;

import static java.util.stream.Collectors.toList;
import static net.oneandone.httpselftest.log.logback.LogbackSupport.attachedSelftestAppenders;
import static net.oneandone.httpselftest.servlet.SelftestServlet.guaranteeLeadingAndTrailingSlash;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.util.ReflectionTestUtils.getField;

import java.util.List;
import java.util.Set;

import javax.servlet.ServletConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import net.oneandone.httpselftest.http.TestRequest;
import net.oneandone.httpselftest.http.TestResponse;
import net.oneandone.httpselftest.log.LogAccess;
import net.oneandone.httpselftest.log.LogSupport;
import net.oneandone.httpselftest.log.logback.LogbackSupport;
import net.oneandone.httpselftest.test.api.Context;
import net.oneandone.httpselftest.test.api.TestCase;
import net.oneandone.httpselftest.test.api.TestConfigs;
import net.oneandone.httpselftest.test.api.TestValues;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SelftestServletTest {

    @Mock
    private ServletConfig servletConfigMock;

    @Test
    public void testAppenderAttachment() throws Exception {
        SelftestServlet servlet = new SimpleSelftestServlet();

        doReturn("user:pw").when(servletConfigMock).getInitParameter(SelftestServlet.PROP_CREDENTIALS);
        doReturn("8080").when(servletConfigMock).getInitParameter(SelftestServlet.PROP_OVERRIDE_PORT);
        doReturn("basepath").when(servletConfigMock).getInitParameter(SelftestServlet.PROP_OVERRIDE_PATH);
        servlet.init(servletConfigMock);

        Object support = getField(servlet, "logSupport");
        assertThat(support).isExactlyInstanceOf(LogbackSupport.class);
        LogbackSupportStub stub = new LogbackSupportStub((LogbackSupport) support);
        ReflectionTestUtils.setField(servlet, "logSupport", stub);
        assertThat(stub.attachWasCalled).as("was attached").isFalse();

        // GET
        MockHttpServletResponse getResponse = new MockHttpServletResponse();
        servlet.doGet(newAuthorizedExecuteRequest(), getResponse);
        assertThat(getResponse).hasFieldOrPropertyWithValue("status", 200);
        assertThat(allAttachedSelftestAppenders()).as("attached appenders after GET").isEmpty();
        assertThat(stub.attachWasCalled).as("was attached").isFalse();

        // POST
        MockHttpServletResponse postResponse = new MockHttpServletResponse();
        servlet.doPost(newAuthorizedExecuteRequest(), postResponse);
        assertThat(postResponse).hasFieldOrPropertyWithValue("status", 200);
        assertThat(allAttachedSelftestAppenders()).as("attached appenders after POST").isEmpty();
        assertThat(stub.attachWasCalled).as("was attached").isTrue();
    }

    private MockHttpServletRequest newAuthorizedExecuteRequest() {
        MockHttpServletRequest requestMock = new MockHttpServletRequest();
        requestMock.addHeader("Authorization", "Basic dXNlcjpwdw=="); // user:pw
        requestMock.addParameter("execute", "yes please");
        requestMock.addParameter("p-param1", "value");
        return requestMock;
    }

    private Appender<?>[] allAttachedSelftestAppenders() {
        List<Appender<?>> appenders =
                allLogbackLoggers().stream().flatMap(logger -> attachedSelftestAppenders(logger).stream()).collect(toList());
        return appenders.toArray(new Appender<?>[0]);
    }

    private List<Logger> allLogbackLoggers() {
        return ((LoggerContext) LoggerFactory.getILoggerFactory()).getLoggerList();
    }

    @Test
    public void contextPathHandling() throws Exception {
        assertThat(guaranteeLeadingAndTrailingSlash("")).isEqualTo("/");
        assertThat(guaranteeLeadingAndTrailingSlash("/")).isEqualTo("/");
        assertThat(guaranteeLeadingAndTrailingSlash("/path")).isEqualTo("/path/");
        assertThat(guaranteeLeadingAndTrailingSlash("path/")).isEqualTo("/path/");
        assertThat(guaranteeLeadingAndTrailingSlash("path")).isEqualTo("/path/");
        assertThat(guaranteeLeadingAndTrailingSlash("/path/")).isEqualTo("/path/");
    }

    public static class SimpleSelftestServlet extends SelftestServlet {
        @Override
        protected TestConfigs getConfigs() {
            return new TestConfigs("param1");
        }

        public static class TestA implements TestCase {
            @Override
            public TestRequest prepareRequest(TestValues config, Context ctx) throws Exception {
                return new TestRequest("path", "GET");
            }

            @Override
            public void verify(TestValues config, TestResponse response, Context ctx) throws Exception {
                // no assertion
            }
        }
    }

    public class LogbackSupportStub implements LogSupport {
        public boolean attachWasCalled = false;
        private final LogbackSupport delegate;

        public LogbackSupportStub(LogbackSupport support) {
            delegate = support;
        }

        @Override
        public void runWithAttachedAppenders(Set<String> runIds, Runnable c) {
            delegate.runWithAttachedAppenders(runIds, () -> {
                attachWasCalled = true;
                c.run();
            });
        }

        @Override
        public List<LogAccess> getLogs(String runIds) {
            return delegate.getLogs(runIds);
        }
    }
}
