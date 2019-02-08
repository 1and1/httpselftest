package net.oneandone.httpselftest.example.resource;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

@Component
@ApplicationPath("/app")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        register(ExampleResource.class);
    }
}
