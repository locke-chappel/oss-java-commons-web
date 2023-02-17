package io.github.lc.oss.commons.web.advice;

import org.junit.jupiter.api.Test;

import io.github.lc.oss.commons.testing.AbstractTest;

public class AbstractControllerAdviceTest extends AbstractTest {
    private static class TestAdvice extends AbstractControllerAdvice {

    }

    @Test
    public void test_coverage_filler() {
        /**
         * These methods are used by AOP and so are never programmatically called so
         * code coverage reports them as 0% :(
         */

        AbstractControllerAdvice advice = new TestAdvice();
        advice.inAnyController();
        advice.returnsResponseEntity();
        advice.returnsModelAndView();
        advice.withRequestMapping();
    }
}
