package net.oneandone.httpselftest.servlet;

import net.trajano.commons.testing.UtilityClassTestUtil;
import org.junit.jupiter.api.Test;

public class ConfiguratorTest {

    @Test
    public void isUtilityClass() throws Exception {
        UtilityClassTestUtil.assertUtilityClassWellDefined(Configurator.class);
    }

}
