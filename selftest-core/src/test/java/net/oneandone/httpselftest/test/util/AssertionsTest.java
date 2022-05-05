package net.oneandone.httpselftest.test.util;

import static net.oneandone.httpselftest.test.util.Assertions.assertEqual;
import static net.oneandone.httpselftest.test.util.Assertions.assertFalse;
import static net.oneandone.httpselftest.test.util.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.oneandone.httpselftest.test.api.AssertionException;
import org.junit.jupiter.api.Test;

public class AssertionsTest {

    @Test
    public void test_assertTrue() throws Exception {
        assertTrue("msg1", true);
        assertThatThrownBy(() -> assertTrue("msg2", false)).isInstanceOf(AssertionException.class).hasMessage("msg2");
    }

    @Test
    public void test_assertFalse() throws Exception {
        assertFalse("msg1", false);
        assertThatThrownBy(() -> assertFalse("msg2", true)).isInstanceOf(AssertionException.class).hasMessage("msg2");
    }

    @Test
    public void test_assertEqual() throws Exception {
        assertEqual("msg1", null, null);
        assertEqual("msg2", "a", "a");

        assertThatThrownBy(() -> assertEqual("msg3", "a", null)).isInstanceOf(AssertionException.class)
                .hasMessage("msg3 - expected: a, was: null");
        assertThatThrownBy(() -> assertEqual("msg4", null, "a")).isInstanceOf(AssertionException.class)
                .hasMessage("msg4 - expected: null, was: a");
        assertThatThrownBy(() -> assertEqual("msg5", "a", "b")).isInstanceOf(AssertionException.class)
                .hasMessage("msg5 - expected: a, was: b");
    }

}
