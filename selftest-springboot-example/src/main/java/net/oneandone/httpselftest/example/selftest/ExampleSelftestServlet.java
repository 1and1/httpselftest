package net.oneandone.httpselftest.example.selftest;

import static net.oneandone.httpselftest.example.resource.ExampleResource.FIRSTNAME;
import static net.oneandone.httpselftest.example.resource.ExampleResource.LASTNAME;
import static net.oneandone.httpselftest.test.util.Assertions.assertEqual;

import java.util.StringJoiner;

import org.springframework.http.MediaType;

import net.oneandone.httpselftest.http.Headers;
import net.oneandone.httpselftest.http.HttpClient.Type;
import net.oneandone.httpselftest.http.TestResponse;
import net.oneandone.httpselftest.http.TestRequest;
import net.oneandone.httpselftest.servlet.SelftestServlet;
import net.oneandone.httpselftest.test.api.Context;
import net.oneandone.httpselftest.test.api.TestCase;
import net.oneandone.httpselftest.test.api.TestConfigs;
import net.oneandone.httpselftest.test.api.TestValues;
import net.oneandone.httpselftest.test.util.Assertions;
import net.oneandone.httpselftest.test.util.RequestHelper;

public class ExampleSelftestServlet extends SelftestServlet {

    private static final String P_FIXED = "fixed";

    @Override
    protected TestConfigs.Builder getConfigs() {

        TestConfigs.Builder configs = new TestConfigs.Builder(FIRSTNAME, LASTNAME, P_FIXED);
        configs.fixed(P_FIXED);
        configs.put("someconfig local", "stephen", "colbert", "");
        configs.put("otherconfig local", "stephen", "hillenburg", "");
        configs.put("someconfig staging", "calvin", "hobbes", "");
        configs.put("otherconfig staging", "ellen", "ripley", "meow");
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
            RequestHelper.addFormParamsUtf8(join, config, FIRSTNAME, LASTNAME, P_FIXED);

            Headers headers = new Headers();
            headers.add("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);
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

    public static class UrlConnectionTest implements TestCase {
        @Override
        public TestRequest prepareRequest(TestValues config, Context ctx) throws Exception {
            TestRequest request = new TestRequest("echo", "GET");
            request.clientType = Type.URLCON;
            return request;
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
