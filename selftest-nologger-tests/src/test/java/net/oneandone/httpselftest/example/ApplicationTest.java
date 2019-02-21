package net.oneandone.httpselftest.example;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import net.oneandone.httpselftest.http.TestRequest;
import net.oneandone.httpselftest.http.socket.SocketHttpClient;
import net.oneandone.httpselftest.http.socket.SocketTestResponse;

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
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        TestRequest request = new TestRequest("", "POST", headers, "execute=true&p-firstname=pish&p-lastname=posh");

        SocketTestResponse response =
                new SocketHttpClient().call("http://localhost:" + mPort + "/actuator/selftest", request, "someId", 2000);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("Done.").doesNotContain("EXCEPTION DURING EXECUTION", "LOG");
    }

}
