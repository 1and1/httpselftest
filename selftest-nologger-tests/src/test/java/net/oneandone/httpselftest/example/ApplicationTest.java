package net.oneandone.httpselftest.example;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import net.oneandone.httpselftest.http.Headers;
import net.oneandone.httpselftest.http.SocketHttpClient;
import net.oneandone.httpselftest.http.WrappedRequest;
import net.oneandone.httpselftest.http.TestResponse;
import net.oneandone.httpselftest.http.TestRequest;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.DEFINED_PORT)
public class ApplicationTest {

    @LocalServerPort
    int sPort;

    @LocalManagementPort
    int mPort;

    @Test
    public void contextLoads() throws Exception {
        // noop
    }

    @Test
    public void executeTests() throws Exception {
        Headers headers = new Headers();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        TestRequest request = new TestRequest("", "POST", headers, "execute=true&p-firstname=pish&p-lastname=posh");

        WrappedRequest wrapper = new WrappedRequest(request);
        TestResponse response =
                new SocketHttpClient().call("http://localhost:" + mPort + "/actuator/selftest", wrapper, 2000).response;

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("Done.").doesNotContain("EXCEPTION DURING EXECUTION", "LOG");
    }

}
