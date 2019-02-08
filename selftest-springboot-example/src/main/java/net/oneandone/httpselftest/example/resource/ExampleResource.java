package net.oneandone.httpselftest.example.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Path("/")
public class ExampleResource {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleResource.class);

    public static final String FIRSTNAME = "firstname";
    public static final String LASTNAME = "lastname";

    @GET
    @Path("log")
    public String log() {
        LOG.info("hit /log. logging some lines.");
        LOG.debug("busy debugging...");
        LOG.warn("a warn line");
        LOG.error("logging synthetic exception", new IllegalStateException("this can't be!"));
        return "Thanks for testing!";
    }

    @POST
    @Path("echo")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String echo(@FormParam(FIRSTNAME) String p1, @FormParam(LASTNAME) String p2) {
        LOG.info("hit /echo with {} and {}", p1, p2);
        return p1 + " " + p2;
    }

    @GET
    @Path("slow")
    public void slow() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
