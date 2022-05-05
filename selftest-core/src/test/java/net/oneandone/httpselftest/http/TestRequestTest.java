package net.oneandone.httpselftest.http;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestRequestTest {

    @Test
    @SuppressWarnings("unused") // just testing for no exception
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
