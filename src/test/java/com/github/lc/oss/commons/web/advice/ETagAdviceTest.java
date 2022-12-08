package com.github.lc.oss.commons.web.advice;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.github.lc.oss.commons.testing.AbstractMockTest;
import com.github.lc.oss.commons.web.annotations.HttpCachable;
import com.github.lc.oss.commons.web.services.ETagService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ETagAdviceTest extends AbstractMockTest {
    private static class HelperClass {
        @GetMapping
        @HttpCachable("cache-key")
        public ModelAndView keyed() {
            return null;
        }

        @GetMapping(path = "/api/v1/resource/property")
        @HttpCachable
        public ModelAndView auto() {
            return null;
        }
    }

    private static class TestAnswer implements Answer<Object> {
        final private List<Object> expectedArgs;
        final private Object returnValue;

        public TestAnswer(Object returnValue) {
            this.expectedArgs = new ArrayList<>();
            this.returnValue = returnValue;
        }

        public TestAnswer(List<Object> expectedArgs) {
            this.expectedArgs = expectedArgs == null ? new ArrayList<>() : expectedArgs;
            this.returnValue = null;
        }

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            Assertions.assertEquals(this.expectedArgs.size(), invocation.getArguments().length);
            for (int i = 0; i < this.expectedArgs.size(); i++) {
                Assertions.assertEquals(this.expectedArgs.get(i), invocation.getArgument(i));
            }
            return this.returnValue;
        }
    }

    private ETagService eTagService;
    private ETagAdvice advice;

    @BeforeEach
    public void init() {
        this.advice = new ETagAdvice();
        this.eTagService = Mockito.mock(ETagService.class);
        this.setField("eTagService", this.eTagService, this.advice);
    }

    /*
     * PointCuts are called via reflection so we need to manually call them here to
     * inform code coverage tools that they really are being used...
     */
    @Test
    public void test_coverageFiller() {
        this.advice.isCachable();
        this.advice.isGetRequest();
        this.advice.isHttpCachable();
    }

    @Test
    public void test_aroundModelViewCall_nullHeader() {
        final String etag = "W/\"etag\"";
        final ModelAndView mv = new ModelAndView();

        Method method = ReflectionUtils.findMethod(HelperClass.class, "keyed");

        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        HttpServletResponse httpResonse = Mockito.mock(HttpServletResponse.class);
        HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);

        MethodSignature signature = Mockito.mock(MethodSignature.class);

        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(signature.getMethod()).thenReturn(method);
        Mockito.when(this.eTagService.getETag("cache-key")).thenReturn("W/\"etag\"");
        Mockito.when(httpRequest.getHeader(HttpHeaders.IF_NONE_MATCH)).thenReturn(null);
        try {
            Mockito.doAnswer(new TestAnswer(mv)).when(joinPoint).proceed();
        } catch (Throwable e) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.doAnswer(new TestAnswer(Arrays.asList( //
                HttpHeaders.CACHE_CONTROL, //
                CacheControl.noCache().cachePrivate().getHeaderValue()))). //
                when(httpResonse). //
                setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue());
        Mockito.doAnswer(new TestAnswer(Arrays.asList( //
                HttpHeaders.ETAG, //
                etag))). //
                when(httpResonse). //
                setHeader(HttpHeaders.ETAG, etag);

        ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest, httpResonse);
        RequestContextHolder.setRequestAttributes(attributes);

        try {
            ModelAndView result = (ModelAndView) this.advice.aroundModelViewCall(joinPoint);
            Assertions.assertNotNull(result);
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }
    }

    @Test
    public void test_aroundModelViewCall_emptyHeader() {
        final String etag = "W/\"etag\"";
        final ModelAndView mv = new ModelAndView();

        Method method = ReflectionUtils.findMethod(HelperClass.class, "keyed");

        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        HttpServletResponse httpResonse = Mockito.mock(HttpServletResponse.class);
        HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);

        MethodSignature signature = Mockito.mock(MethodSignature.class);

        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(signature.getMethod()).thenReturn(method);
        Mockito.when(this.eTagService.getETag("cache-key")).thenReturn("W/\"etag\"");
        Mockito.when(httpRequest.getHeader(HttpHeaders.IF_NONE_MATCH)).thenReturn("");
        try {
            Mockito.doAnswer(new TestAnswer(mv)).when(joinPoint).proceed();
        } catch (Throwable e) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.doAnswer(new TestAnswer(Arrays.asList( //
                HttpHeaders.CACHE_CONTROL, //
                CacheControl.noCache().cachePrivate().getHeaderValue()))). //
                when(httpResonse). //
                setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue());
        Mockito.doAnswer(new TestAnswer(Arrays.asList( //
                HttpHeaders.ETAG, //
                etag))). //
                when(httpResonse). //
                setHeader(HttpHeaders.ETAG, etag);

        ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest, httpResonse);
        RequestContextHolder.setRequestAttributes(attributes);

        try {
            ModelAndView result = (ModelAndView) this.advice.aroundModelViewCall(joinPoint);
            Assertions.assertNotNull(result);
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }
    }

    @Test
    public void test_aroundModelViewCall_blankHeader() {
        final String etag = "W/\"etag\"";
        final ModelAndView mv = new ModelAndView();

        Method method = ReflectionUtils.findMethod(HelperClass.class, "auto");

        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        HttpServletResponse httpResonse = Mockito.mock(HttpServletResponse.class);
        HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);

        MethodSignature signature = Mockito.mock(MethodSignature.class);

        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(signature.getMethod()).thenReturn(method);
        Mockito.when(this.eTagService.getETag("/api/v1/resource/property")).thenReturn("W/\"etag\"");
        Mockito.when(httpRequest.getHeader(HttpHeaders.IF_NONE_MATCH)).thenReturn(" \t \r \n \t ");
        try {
            Mockito.doAnswer(new TestAnswer(mv)).when(joinPoint).proceed();
        } catch (Throwable e) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.doAnswer(new TestAnswer(Arrays.asList( //
                HttpHeaders.CACHE_CONTROL, //
                CacheControl.noCache().cachePrivate().getHeaderValue()))). //
                when(httpResonse). //
                setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue());
        Mockito.doAnswer(new TestAnswer(Arrays.asList( //
                HttpHeaders.ETAG, //
                etag))). //
                when(httpResonse). //
                setHeader(HttpHeaders.ETAG, etag);

        ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest, httpResonse);
        RequestContextHolder.setRequestAttributes(attributes);

        try {
            ModelAndView result = (ModelAndView) this.advice.aroundModelViewCall(joinPoint);
            Assertions.assertNotNull(result);
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }
    }

    @Test
    public void test_aroundModelViewCall_headerMismatch() {
        final String etag = "W/\"etag\"";
        final ModelAndView mv = new ModelAndView();

        Method method = ReflectionUtils.findMethod(HelperClass.class, "keyed");

        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        HttpServletResponse httpResonse = Mockito.mock(HttpServletResponse.class);
        HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);

        MethodSignature signature = Mockito.mock(MethodSignature.class);

        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(signature.getMethod()).thenReturn(method);
        Mockito.when(this.eTagService.getETag("cache-key")).thenReturn("W/\"etag\"");
        Mockito.when(httpRequest.getHeader(HttpHeaders.IF_NONE_MATCH)).thenReturn("old-value");
        try {
            Mockito.doAnswer(new TestAnswer(mv)).when(joinPoint).proceed();
        } catch (Throwable e) {
            Assertions.fail("Unexpected exception");
        }
        Mockito.doAnswer(new TestAnswer(Arrays.asList( //
                HttpHeaders.CACHE_CONTROL, //
                CacheControl.noCache().cachePrivate().getHeaderValue()))). //
                when(httpResonse). //
                setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue());
        Mockito.doAnswer(new TestAnswer(Arrays.asList( //
                HttpHeaders.ETAG, //
                etag))). //
                when(httpResonse). //
                setHeader(HttpHeaders.ETAG, etag);

        ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest, httpResonse);
        RequestContextHolder.setRequestAttributes(attributes);

        try {
            ModelAndView result = (ModelAndView) this.advice.aroundModelViewCall(joinPoint);
            Assertions.assertNotNull(result);
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }
    }

    @Test
    public void test_aroundModelViewCall_headerMatch() {
        final String etag = "W/\"etag\"";

        Method method = ReflectionUtils.findMethod(HelperClass.class, "auto");

        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        HttpServletResponse httpResonse = Mockito.mock(HttpServletResponse.class);
        HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);

        MethodSignature signature = Mockito.mock(MethodSignature.class);

        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(signature.getMethod()).thenReturn(method);
        Mockito.when(this.eTagService.getETag("/api/v1/resource/property")).thenReturn("W/\"etag\"");
        Mockito.when(httpRequest.getHeader(HttpHeaders.IF_NONE_MATCH)).thenReturn(etag);

        Mockito.doAnswer(new TestAnswer(Arrays.asList( //
                HttpHeaders.CACHE_CONTROL, //
                CacheControl.noCache().cachePrivate().getHeaderValue()))). //
                when(httpResonse). //
                setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue());
        Mockito.doAnswer(new TestAnswer(Arrays.asList( //
                HttpHeaders.ETAG, //
                etag))). //
                when(httpResonse). //
                setHeader(HttpHeaders.ETAG, etag);
        Mockito.doAnswer(new TestAnswer(Arrays.asList( //
                HttpStatus.NOT_MODIFIED.value()))). //
                when(httpResonse). //
                setStatus(HttpStatus.NOT_MODIFIED.value());

        ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest, httpResonse);
        RequestContextHolder.setRequestAttributes(attributes);

        try {
            ModelAndView result = (ModelAndView) this.advice.aroundModelViewCall(joinPoint);
            Assertions.assertNull(result);
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }
    }

    @Test
    public void test_aroundModelViewCall_error() {
        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        Mockito.when(joinPoint.getSignature()).thenThrow(new IllegalArgumentException("boom!"));

        try {
            this.advice.aroundModelViewCall(joinPoint);
            Assertions.fail("Expected exception");
        } catch (Throwable ex) {
            Assertions.assertEquals("java.lang.IllegalArgumentException: boom!", ex.getMessage());
        }
    }

    @Test
    public void test_aroundRestCall_nullHeader() {
        final String etag = "W/\"etag\"";
        final ResponseEntity<?> response = new ResponseEntity<>(HttpStatus.OK);

        Method method = ReflectionUtils.findMethod(HelperClass.class, "keyed");

        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        HttpServletResponse httpResonse = Mockito.mock(HttpServletResponse.class);
        HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);

        MethodSignature signature = Mockito.mock(MethodSignature.class);

        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(signature.getMethod()).thenReturn(method);
        Mockito.when(this.eTagService.getETag("cache-key")).thenReturn("W/\"etag\"");
        Mockito.when(httpRequest.getHeader(HttpHeaders.IF_NONE_MATCH)).thenReturn(null);
        try {
            Mockito.doAnswer(new TestAnswer(response)).when(joinPoint).proceed();
        } catch (Throwable e) {
            Assertions.fail("Unexpected exception");
        }

        ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest, httpResonse);
        RequestContextHolder.setRequestAttributes(attributes);

        ResponseEntity<?> result = null;
        try {
            result = (ResponseEntity<?>) this.advice.aroundRestCall(joinPoint);
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }

        Assertions.assertNotNull(result);
        HttpHeaders headers = result.getHeaders();
        Assertions.assertNotNull(headers);
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), headers.getCacheControl());
        Assertions.assertEquals(etag, headers.getETag());
    }

    @Test
    public void test_aroundRestCall_emptyHeader() {
        final String etag = "W/\"etag\"";
        final ResponseEntity<?> response = new ResponseEntity<>(HttpStatus.OK);

        Method method = ReflectionUtils.findMethod(HelperClass.class, "keyed");

        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        HttpServletResponse httpResonse = Mockito.mock(HttpServletResponse.class);
        HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);

        MethodSignature signature = Mockito.mock(MethodSignature.class);

        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(signature.getMethod()).thenReturn(method);
        Mockito.when(this.eTagService.getETag("cache-key")).thenReturn("W/\"etag\"");
        Mockito.when(httpRequest.getHeader(HttpHeaders.IF_NONE_MATCH)).thenReturn("");
        try {
            Mockito.doAnswer(new TestAnswer(response)).when(joinPoint).proceed();
        } catch (Throwable e) {
            Assertions.fail("Unexpected exception");
        }

        ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest, httpResonse);
        RequestContextHolder.setRequestAttributes(attributes);

        ResponseEntity<?> result = null;
        try {
            result = (ResponseEntity<?>) this.advice.aroundRestCall(joinPoint);
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }

        Assertions.assertNotNull(result);
        HttpHeaders headers = result.getHeaders();
        Assertions.assertNotNull(headers);
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), headers.getCacheControl());
        Assertions.assertEquals(etag, headers.getETag());
    }

    @Test
    public void test_aroundRestCall_blankHeader() {
        final String etag = "W/\"etag\"";
        final ResponseEntity<?> response = new ResponseEntity<>(HttpStatus.OK);

        Method method = ReflectionUtils.findMethod(HelperClass.class, "auto");

        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        HttpServletResponse httpResonse = Mockito.mock(HttpServletResponse.class);
        HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);

        MethodSignature signature = Mockito.mock(MethodSignature.class);

        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(signature.getMethod()).thenReturn(method);
        Mockito.when(this.eTagService.getETag("/api/v1/resource/property")).thenReturn("W/\"etag\"");
        Mockito.when(httpRequest.getHeader(HttpHeaders.IF_NONE_MATCH)).thenReturn(" \t \r \n \t ");
        try {
            Mockito.doAnswer(new TestAnswer(response)).when(joinPoint).proceed();
        } catch (Throwable e) {
            Assertions.fail("Unexpected exception");
        }

        ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest, httpResonse);
        RequestContextHolder.setRequestAttributes(attributes);

        ResponseEntity<?> result = null;
        try {
            result = (ResponseEntity<?>) this.advice.aroundRestCall(joinPoint);
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }

        Assertions.assertNotNull(result);
        HttpHeaders headers = result.getHeaders();
        Assertions.assertNotNull(headers);
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), headers.getCacheControl());
        Assertions.assertEquals(etag, headers.getETag());
    }

    @Test
    public void test_aroundRestCall_headerMismatch() {
        final String etag = "W/\"etag\"";
        final ResponseEntity<?> response = new ResponseEntity<>(HttpStatus.OK);

        Method method = ReflectionUtils.findMethod(HelperClass.class, "auto");

        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        HttpServletResponse httpResonse = Mockito.mock(HttpServletResponse.class);
        HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);

        MethodSignature signature = Mockito.mock(MethodSignature.class);

        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(signature.getMethod()).thenReturn(method);
        Mockito.when(this.eTagService.getETag("/api/v1/resource/property")).thenReturn("W/\"etag\"");
        Mockito.when(httpRequest.getHeader(HttpHeaders.IF_NONE_MATCH)).thenReturn("junk");
        try {
            Mockito.doAnswer(new TestAnswer(response)).when(joinPoint).proceed();
        } catch (Throwable e) {
            Assertions.fail("Unexpected exception");
        }

        ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest, httpResonse);
        RequestContextHolder.setRequestAttributes(attributes);

        ResponseEntity<?> result = null;
        try {
            result = (ResponseEntity<?>) this.advice.aroundRestCall(joinPoint);
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }

        Assertions.assertNotNull(result);
        HttpHeaders headers = result.getHeaders();
        Assertions.assertNotNull(headers);
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), headers.getCacheControl());
        Assertions.assertEquals(etag, headers.getETag());
    }

    @Test
    public void test_aroundRestCall_headerMatch() {
        final String etag = "W/\"etag\"";

        Method method = ReflectionUtils.findMethod(HelperClass.class, "keyed");

        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        HttpServletResponse httpResonse = Mockito.mock(HttpServletResponse.class);
        HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);

        MethodSignature signature = Mockito.mock(MethodSignature.class);

        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Mockito.when(signature.getMethod()).thenReturn(method);
        Mockito.when(this.eTagService.getETag("cache-key")).thenReturn("W/\"etag\"");
        Mockito.when(httpRequest.getHeader(HttpHeaders.IF_NONE_MATCH)).thenReturn(etag);

        ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest, httpResonse);
        RequestContextHolder.setRequestAttributes(attributes);

        ResponseEntity<?> result = null;
        try {
            result = (ResponseEntity<?>) this.advice.aroundRestCall(joinPoint);
        } catch (Throwable ex) {
            Assertions.fail("Unexpected exception");
        }

        Assertions.assertNotNull(result);
        HttpHeaders headers = result.getHeaders();
        Assertions.assertNotNull(headers);
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), headers.getCacheControl());
        Assertions.assertEquals(etag, headers.getETag());
    }

    @Test
    public void test_aroundRestCall_error() {
        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        Mockito.when(joinPoint.getSignature()).thenThrow(new IllegalArgumentException("boom!"));

        try {
            this.advice.aroundRestCall(joinPoint);
            Assertions.fail("Expected exception");
        } catch (Throwable ex) {
            Assertions.assertEquals("java.lang.IllegalArgumentException: boom!", ex.getMessage());
        }
    }
}
