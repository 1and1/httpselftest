package net.oneandone.httpselftest.http;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public class TestRequestTest {

    @Test
    public void minimal() throws Exception {
        new TestRequest("path", "method");
    }

    @Test
    public void requiresPath() throws Exception {
        assertThatThrownBy(() -> new TestRequest(null, "method")).hasMessageContaining("path");
    }

    @Test
    public void requiresMethod() throws Exception {
        assertThatThrownBy(() -> new TestRequest("path", null)).hasMessageContaining("method");
    }

}
