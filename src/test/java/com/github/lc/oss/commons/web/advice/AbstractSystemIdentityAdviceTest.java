package com.github.lc.oss.commons.web.advice;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.github.lc.oss.commons.testing.AbstractTest;

public class AbstractSystemIdentityAdviceTest extends AbstractTest {
    private static class TestAdvice extends AbstractSystemIdentityAdvice {
        public Authentication systemAuth = new UsernamePasswordAuthenticationToken("system", null);

        @Override
        protected Authentication getSystemAuthentication() {
            return this.systemAuth;
        }
    }

    private ProceedingJoinPoint method = Mockito.mock(ProceedingJoinPoint.class);
    private Authentication currentUser = new UsernamePasswordAuthenticationToken("user-id", "creds");

    @AfterEach
    public void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void test_coverageFiller() {
        AbstractSystemIdentityAdvice advice = new TestAdvice();

        advice.systemIdentity();
    }

    @Test
    public void test_authorizeSystem_nullSystemUser() {
        TestAdvice advice = new TestAdvice();
        advice.systemAuth = null;

        SecurityContextHolder.getContext().setAuthentication(this.currentUser);

        try {
            advice.authorizeSystem(this.method);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Unable to authorize system user because it is null", ex.getMessage());
        }

        Assertions.assertSame(this.currentUser, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void test_authorizeSystem_methodException() {
        TestAdvice advice = new TestAdvice();

        try {
            Mockito.when(this.method.proceed()).thenThrow(new Throwable("BOOM!"));
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }

        SecurityContextHolder.getContext().setAuthentication(this.currentUser);

        try {
            advice.authorizeSystem(this.method);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("java.lang.Throwable: BOOM!", ex.getMessage());
        }

        Assertions.assertSame(this.currentUser, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void test_authorizeSystem() {
        TestAdvice advice = new TestAdvice();

        try {
            Mockito.doAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    Assertions.assertSame(advice.systemAuth, SecurityContextHolder.getContext().getAuthentication());
                    Assertions.assertNotSame(AbstractSystemIdentityAdviceTest.this.currentUser, SecurityContextHolder.getContext().getAuthentication());
                    return Integer.valueOf(123);
                }
            }).when(this.method).proceed();
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }

        SecurityContextHolder.getContext().setAuthentication(this.currentUser);

        Object result = advice.authorizeSystem(this.method);
        Assertions.assertEquals(Integer.valueOf(123), result);

        Assertions.assertSame(this.currentUser, SecurityContextHolder.getContext().getAuthentication());
    }
}
