package net.oneandone.httpselftest.example.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Path("/")
public class NoLoggerResource {

    private static final Logger LOG = LoggerFactory.getLogger(NoLoggerResource.class);

    public static final String FIRSTNAME = "firstname";
    public static final String LASTNAME = "lastname";

    @GET
    @Path("log")
    public String log() {
        LOG.error("logging synthetic exception", new IllegalStateException("should not be visible"));
        return "Thanks for testing!";
    }

}
