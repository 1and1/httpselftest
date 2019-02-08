# HTTP Selftest
[![Build Status](https://travis-ci.org/1and1/httpselftest.svg?branch=master)](https://travis-ci.org/1and1/httpselftest)

This repo implements a base servlet and surrounding infrastructure to enable semi-automatic tests against a running application in any development stage.

- Test parameters can be supplied by the developer for different environments, but can also easily be overridden/provided by the user.
- Tests are run via HTTP from within the application against itself.
- HTTP request, response and optionally the corresponding application log messages of every request are output in detail. Currently Logback is supported as logging backend.
- Test cases are implemented by subclassing `net.oneandone.httpselftest.servlet.SelftestServlet`.
- Test cases are defined within the subclass as static inner implementations of `net.oneandone.httpselftest.test.api.TestCase`.

## Usage example
### Test run summary
![Single test output](doc/clip_run_collapsed.png)

### Single test output including request, response and log messages
![Single test output](doc/clip_single_test.png)

### Test case implementation example
```java
public class MinimalSelftestServlet extends SelftestServlet {
    @Override
    protected TestConfigs getConfigs() {
        return new TestConfigs("param1");
    }
    public static class TestEcho implements TestCase {
        @Override
        public TestRequest prepareRequest(TestValues config, Context ctx) throws Exception {
            return new TestRequest("echo", "GET");
        }
        @Override
        public void verify(TestValues config, TestResponse response, Context ctx) throws Exception {
            Assertions.assertEqual("status code", 200, response.getStatus());
        }
    }
}
```

### Interactive example
You can easily play around with a simple test suite. Just launch the example app in the `spring-boot-example` module.

```bash
git clone https://github.com/1and1/httpselftest.git
cd httpselftest/selftest-springboot-example/
mvn spring-boot:run
# point your browser to the actuator endpoint: http://localhost:8081/actuator/selftest
```

## Integration
### GAV
```xml
<dependency>
   <groupId>net.oneandone.httpselftest</groupId>
   <artifactId>selftest-core</artifactId>
   <version>$VERSION</version>
</dependency>
```

### Spring Boot 2
If your application is a Spring Boot 2 app, the servlet can be registered as a `@ServletEndpoint`. In this case the application port and base path may need to be provided manually. The endpoint MUST be exposed as a `@Bean`. The servlet will be running on the management port.

```java
@ServletEndpoint(id = "selftest")
public class SelftestEndpoint implements Supplier<EndpointServlet> {

    @Override
    public EndpointServlet get() {
        return new EndpointServlet(LoginAttemptsSelftestServlet.class)
            .withInitParameter(SelftestServlet.PROP_OVERRIDE_PORT, "8080")
            .withInitParameter(SelftestServlet.PROP_OVERRIDE_PATH, "/rest");
    }

}
```
### Spring Boot 1
If your application is a Spring Boot 1 app, the servlet can be registered by the way of `ServletRegistrationBean`. This way it will be running on the application port.

```java
@Bean
public ServletRegistrationBean selftestServlet() {
    ServletRegistrationBean servlet = new ServletRegistrationBean();
    servlet.setServlet(new PMDSelftestServlet());
    servlet.setName("selftestServlet");
    servlet.addUrlMappings("/-system/selftest");
    return servlet;
}
```

### Otherwise
If your application has a `web.xml`, the servlet containing your test cases can be registered there.
```xml
<servlet>
   <servlet-name>SelfTestServlet</servlet-name>
   <servlet-class>${SUBCLASS_OF_SelftestServlet}</servlet-class>
</servlet>
<servlet-mapping>
   <servlet-name>SelfTestServlet</servlet-name>
   <url-pattern>/-system/selftest</url-pattern>
</servlet-mapping>
```

### Collecting request tracking IDs
If you want to collect log messages and your application is not already collecting request tracking IDs in the MDC, you can do so by registering a `SelftestMDCFilter`.

The filter can be registered in Spring Boot apps by the way of `FilterRegistrationBean`.

```java
@Bean
public FilterRegistrationBean selftestFilter() {
    FilterRegistrationBean filter = new FilterRegistrationBean();
    filter.setFilter(new SelftestMDCFilter());
    filter.addUrlPatterns("/*");
    filter.setName("selftestFilter");
    filter.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return filter;
}
```

If you have a `web.xml` you can register it there.
```xml
<filter>
   <filter-name>SelftestMDCFilter</filter-name>
   <filter-class>net.oneandone.httpselftest.servlet.SelftestMDCFilter</filter-class>
</filter>
<filter-mapping>
   <filter-name>SelftestMDCFilter</filter-name>
   <url-pattern>/*</url-pattern>
</filter-mapping>
```

## Configuration
- `selftest.user.colon.password` (OPTIONAL, servlet init parameter) - Used for HTTP Basic authentication
- `selftest.override.port` (OPTIONAL, servlet init parameter) - Override application port. This is only needed if the servlet runs on another port.
- `selftest.override.contextpath` (OPTIONAL, servlet init parameter) - Override application context path. This is only needed if the servlet runs on another port.
- `selftest.override.mdckey` (OPTIONAL, servlet init parameter) - Override the MDC key storing the request tracking id. Defaults to `X-REQUEST-ID`.

