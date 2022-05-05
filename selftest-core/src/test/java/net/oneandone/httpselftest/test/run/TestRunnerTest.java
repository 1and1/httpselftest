package net.oneandone.httpselftest.test.run;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class TestRunnerTest {

    @Test
    public void clamped() throws Exception {
        assertThat(TestRunner.clamped(30)).isEqualTo(30);
        assertThat(TestRunner.clamped(0)).isEqualTo(0);
        assertThat(TestRunner.clamped(-1)).isEqualTo(0);
        assertThat(TestRunner.clamped(5000)).isEqualTo(5000);
        assertThat(TestRunner.clamped(5001)).isEqualTo(5000);
    }

    @Test
    public void runIdFormat() throws Exception {
        assertThat(TestRunner.runId("abc")).startsWith("abc-").doesNotContain("_");
        assertThat(TestRunner.runId("a-b")).startsWith("a-b-");
        assertThat(TestRunner.runId("a_,.äöü§$%&z")).startsWith("az-").doesNotContain("_", "ä");
        assertThat(TestRunner.runId("1234567890123456789012345").length()).isGreaterThan(26);
        assertThat(TestRunner.runId("a").length()).isEqualTo(20);

        char[] soManyChars = new char[220];
        Arrays.fill(soManyChars, '5');
        String tooLong = new String(soManyChars);
        assertThat(tooLong.length()).isGreaterThan(200);
        assertThat(TestRunner.runId(tooLong).length()).isEqualTo(200);
    }

}
