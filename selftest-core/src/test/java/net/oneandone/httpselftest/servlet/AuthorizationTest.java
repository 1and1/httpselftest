package net.oneandone.httpselftest.servlet;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import net.trajano.commons.testing.UtilityClassTestUtil;

public class AuthorizationTest {

    @Test
    public void isUtilityClass() throws Exception {
        UtilityClassTestUtil.assertUtilityClassWellDefined(Authorization.class);
    }

    @Test
    public void noAuth() throws Exception {
        assertOk(null, empty());
        assertOk("", empty());
        assertOk("anything", empty());
    }

    @Test
    public void plainAuth() throws Exception {
        assertOk("Basic dXNlcjpwYXNz", creds("user:pass"));
        assertOk("Basic   dXNlcjpwYXNz  ", creds("user:pass")); // additional whitespace
        assertOk("Basic dXNlcjpwYWFzcw", creds("user:paass")); // no padding
        assertOk("Basic dXNlcjpwYWFzcw==", creds("user:paass")); // padding
        assertOk("Basic dXNlcjpwYTpzcw", creds("user:pa:ss")); // with ':'

        assertOk("Basic dXNlcjpwYXNz", creds("plain|user:pass"));
        assertNotOk("Basic dXNlcjpwYXNz", creds("PLAIN|user:pass"));
        assertNotOk("Basic dXNlcjpwYXNz", creds("other|user:pass"));

        assertNotOk("Basic", creds("user:pass"));
        assertNotOk("Basic ", creds("user:pass"));

        assertOk("Basic dXNlcjpwYWFzcw", creds("user:paass"));
        assertNotOk("basic dXNlcjpwYWFzcw", creds("user:paass")); // lower case
        assertNotOk("BASIC dXNlcjpwYWFzcw", creds("user:paass")); // upper case

        assertOk("Basic dXNlcjpwYXNz", creds("user:pass"));
        assertNotOk("dXNlcjpwYXNz", creds("user:pass")); // missing prefix
        assertNotOk("BasicdXNlcjpwYXNz", creds("user:pass")); // missing space
        assertNotOk("Basic dXNlcjpwYXNzA", creds("user:pass")); // one added char
        assertNotOk("Basic dXNlcjpwYXN", creds("user:pass")); // missing one char
    }

    @Test
    public void sha256Auth() throws Exception {
        assertOk("Basic dXNlcjpwYXNz", creds("sha256|ef4c914c591698b268db3c64163eafda7209a630f236ebf0eebf045460df723a"));
        assertOk("Basic dXNlcjpwYXNz", creds("sha256|EF4C914C591698B268DB3C64163EAFDA7209A630F236EBF0EEBF045460DF723A"));

        assertNotOk("Basic dXNlcjpwYXNz", creds("sha256|user:pass"));
        assertNotOk("Basic dXNlcjpwYXNz", creds("sha256|ef4c914c591698b268db3c64163eafda7209a630f236ebf0eebf045460df723"));
        assertNotOk("Basic dXNlcjpwYXNz", creds("sha256|ef4c914c591698b268db3c64163eafda7209a630f236ebf0eebf045460df723aa"));
    }

    private void assertOk(String authHeader, Optional<String> configured) {
        assertAuth(authHeader, configured, true);
    }

    private void assertNotOk(String authHeader, Optional<String> configured) {
        assertAuth(authHeader, configured, false);
    }

    private void assertAuth(String authHeader, Optional<String> configured, boolean expectation) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (authHeader != null) {
            req.addHeader("Authorization", authHeader);
        }

        assertThat(Authorization.isOk(req, configured)).as("isOk trying '" + authHeader + "' against " + configured)
                .isEqualTo(expectation);
    }

    private static Optional<String> creds(String credString) {
        return Optional.of(credString);
    }

}
