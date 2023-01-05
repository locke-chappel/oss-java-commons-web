package com.github.lc.oss.commons.web.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.lc.oss.commons.testing.AbstractMockTest;

public class DefaultServiceConfigurationTest extends AbstractMockTest {
    @Test
    public void test_coverage_filler() {
        /*
         * Currently the DefaultServiceConfig does not add anything to the
         * AbstractConfiguration but code coverage wants to see this class instantiated
         * at least once
         */
        DefaultServiceConfiguration config = new DefaultServiceConfiguration();
        Assertions.assertNotNull(config);
    }
}
