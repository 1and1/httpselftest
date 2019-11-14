package net.oneandone.httpselftest.test.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.StringJoiner;

import org.junit.Test;

import net.oneandone.httpselftest.test.api.TestConfigs;
import net.trajano.commons.testing.UtilityClassTestUtil;

public class RequestHelperTest {

    @Test
    public void isUtilityClass() throws Exception {
        UtilityClassTestUtil.assertUtilityClassWellDefined(RequestHelper.class);
    }

    @Test
    public void addFormParams() throws Exception {
        StringJoiner joiner = new StringJoiner("&");
        TestConfigs.Builder configBuilder = new TestConfigs.Builder("ka", "kö", "k&", "k=", "kunused");
        configBuilder.put("id", "va", "vö", "v&", "v=", "vunused");
        TestConfigs config = new TestConfigs(configBuilder);

        // exercise
        RequestHelper.addFormParamsUtf8(joiner, config.create("id"), "ka", "k&", "kö", "k=");

        assertThat(joiner.toString()).isEqualTo("ka=va&k%26=v%26&k%C3%B6=v%C3%B6&k%3D=v%3D");
    }

}
