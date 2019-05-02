package net.oneandone.httpselftest.example.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Component
public class NoLoggerResource {

    private static final Logger LOG = LoggerFactory.getLogger(NoLoggerResource.class);

    public static final String FIRSTNAME = "firstname";
    public static final String LASTNAME = "lastname";

    @GetMapping("/log")
    public @ResponseBody String log() {
        LOG.error("logging synthetic exception", new IllegalStateException("should not be visible"));
        return "Thanks for testing!";
    }

}
