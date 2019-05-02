package net.oneandone.httpselftest.example.selftest;

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.EndpointServlet;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import net.oneandone.httpselftest.servlet.SelftestMDCFilter;
import net.oneandone.httpselftest.servlet.SelftestServlet;

@Configuration
public class SelftestConfiguration {

    @Autowired
    Environment env;

    @Bean
    public SelftestEndpoint selftestEndpoint() {
        return new SelftestEndpoint();
    }

    @Bean
    public FilterRegistrationBean<SelftestMDCFilter> filterRegistration() {
        FilterRegistrationBean<SelftestMDCFilter> reg = new FilterRegistrationBean<>();
        reg.addUrlPatterns("/*");
        reg.setFilter(new SelftestMDCFilter());
        return reg;
    }

    @ServletEndpoint(id = "selftest")
    public class SelftestEndpoint implements Supplier<EndpointServlet> {

        @Override
        public EndpointServlet get() {
            return new EndpointServlet(ExampleSelftestServlet.class) //
                    .withInitParameter(SelftestServlet.PROP_CONFIGGROUPS, env.getProperty(SelftestServlet.PROP_CONFIGGROUPS)) //
                    .withInitParameter(SelftestServlet.PROP_OVERRIDE_PORT, env.getProperty(SelftestServlet.PROP_OVERRIDE_PORT)) //
                    .withInitParameter(SelftestServlet.PROP_OVERRIDE_PATH, env.getProperty(SelftestServlet.PROP_OVERRIDE_PATH));
        }

    }

}
