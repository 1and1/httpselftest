package net.oneandone.httpselftest.example.selftest;

import static net.oneandone.httpselftest.example.resource.NoLoggerResource.FIRSTNAME;
import static net.oneandone.httpselftest.example.resource.NoLoggerResource.LASTNAME;
import static net.oneandone.httpselftest.test.util.Assertions.assertEqual;

import net.oneandone.httpselftest.http.TestRequest;
import net.oneandone.httpselftest.http.TestResponse;
import net.oneandone.httpselftest.servlet.SelftestServlet;
import net.oneandone.httpselftest.test.api.Context;
import net.oneandone.httpselftest.test.api.TestCase;
import net.oneandone.httpselftest.test.api.TestConfigs;
import net.oneandone.httpselftest.test.api.TestValues;

public class NoLoggerSelftestServlet extends SelftestServlet {

    @Override
    protected TestConfigs getConfigs() {
        return new TestConfigs(FIRSTNAME, LASTNAME);
    }

    public static class LogTest implements TestCase {
        @Override
        public TestRequest prepareRequest(TestValues config, Context ctx) throws Exception {
            return new TestRequest("log", "GET");
        }

        @Override
        public void verify(TestValues config, TestResponse response, Context ctx) throws Exception {
            assertEqual("status code", 200, response.getStatus());
        }
    }

}
