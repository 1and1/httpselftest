package net.oneandone.httpselftest.servlet;

import org.junit.Test;

import net.trajano.commons.testing.UtilityClassTestUtil;

public class ConfiguratorTest {

    @Test
    public void isUtilityClass() throws Exception {
        UtilityClassTestUtil.assertUtilityClassWellDefined(Configurator.class);
    }

}
