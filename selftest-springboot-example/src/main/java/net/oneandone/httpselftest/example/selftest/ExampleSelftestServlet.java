package net.oneandone.httpselftest.example.selftest;

import static net.oneandone.httpselftest.example.resource.ExampleResource.FIRSTNAME;
import static net.oneandone.httpselftest.example.resource.ExampleResource.LASTNAME;
import static net.oneandone.httpselftest.test.util.Assertions.assertEqual;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import javax.ws.rs.core.MediaType;

import net.oneandone.httpselftest.http.TestRequest;
import net.oneandone.httpselftest.http.TestResponse;
import net.oneandone.httpselftest.servlet.SelftestServlet;
import net.oneandone.httpselftest.test.api.Context;
import net.oneandone.httpselftest.test.api.TestCase;
import net.oneandone.httpselftest.test.api.TestConfigs;
import net.oneandone.httpselftest.test.api.TestValues;
import net.oneandone.httpselftest.test.util.Assertions;
import net.oneandone.httpselftest.test.util.RequestHelper;

public class ExampleSelftestServlet extends SelftestServlet {

    @Override
    protected TestConfigs getConfigs() {

        TestConfigs configs = new TestConfigs(FIRSTNAME, LASTNAME);
        configs.put("someconfig local", "stephen", "colbert");
        configs.put("otherconfig local", "stephen", "hillenburg");
        configs.put("someconfig staging", "calvin", "hobbes");
        configs.put("otherconfig staging", "ellen", "ripley");
        return configs;
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

    public static class EchoTest implements TestCase {
        @Override
        public TestRequest prepareRequest(TestValues config, Context ctx) throws Exception {
            StringJoiner join = new StringJoiner("&");
            RequestHelper.addFormParamsUtf8(join, config, FIRSTNAME, LASTNAME);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
            return new TestRequest("echo", "POST", headers, join.toString());
        }

        @Override
        public void verify(TestValues config, TestResponse response, Context ctx) throws Exception {
            assertEqual("status code", 200, response.getStatus());
            Assertions.assertTrue("body does not contain 'stephen'", response.getBody().contains("stephen"));
        }
    }

    public static class FailingTest implements TestCase {
        @Override
        public TestRequest prepareRequest(TestValues config, Context ctx) throws Exception {
            return new TestRequest("echo", "PUT");
        }

        @Override
        public void verify(TestValues config, TestResponse response, Context ctx) throws Exception {
            assertEqual("status code", 200, response.getStatus());
        }
    }

    public static class SlowResponseTest implements TestCase {
        @Override
        public TestRequest prepareRequest(TestValues config, Context ctx) throws Exception {
            return new TestRequest("slow", "GET");
        }

        @Override
        public void verify(TestValues config, TestResponse response, Context ctx) throws Exception {
        }
    }

}
