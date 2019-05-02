package net.oneandone.httpselftest.example.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ExampleResource {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleResource.class);

    public static final String FIRSTNAME = "firstname";
    public static final String LASTNAME = "lastname";

    @GetMapping("/log")
    public @ResponseBody String log() {
        LOG.info("hit /log. logging some lines.");
        LOG.debug("busy debugging...");
        LOG.warn("a warn line");
        LOG.error("logging synthetic exception", new IllegalStateException("this can't be!"));
        return "Thanks for testing!";
    }

    @PostMapping(path = "/echo")
    public @ResponseBody String echo(@RequestParam(FIRSTNAME) String p1, @RequestParam(LASTNAME) String p2) {
        LOG.info("hit /echo with {} and {}", p1, p2);
        return p1 + " " + p2;
    }

    @GetMapping("/slow")
    public @ResponseBody String slow() throws InterruptedException {
        Thread.sleep(500);
        return "Whew, finally!";
    }

}
