package com.github.lc.oss.commons.web.advice;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.github.lc.oss.commons.web.annotations.HttpCachable;
import com.github.lc.oss.commons.web.services.ETagService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ETagAdvice extends AbstractControllerAdvice {
    @Autowired
    protected ETagService eTagService;

    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public void isGetRequest() {
    }

    @Pointcut("@annotation(com.github.lc.oss.commons.web.annotations.HttpCachable)")
    public void isHttpCachable() {
    }

    @Pointcut("inAnyController() && isGetRequest() && isHttpCachable()")
    public void isCachable() {
    }

    @Around("isCachable() && returnsResponseEntity()")
    public Object aroundRestCall(final ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            String etag = this.getETag(joinPoint);

            String header = this.getRequest().getHeader(HttpHeaders.IF_NONE_MATCH);
            ResponseEntity<?> response = null;
            if (this.isBlank(header) || !header.equals(etag)) {
                response = (ResponseEntity<?>) joinPoint.proceed();
            } else {
                response = new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.addAll(response.getHeaders());
            headers.setCacheControl(CacheControl.noCache().cachePrivate());
            headers.setETag(etag);
            return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Around("isCachable() && returnsModelAndView()")
    public Object aroundModelViewCall(final ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            String etag = this.getETag(joinPoint);

            String header = this.getRequest().getHeader(HttpHeaders.IF_NONE_MATCH);
            ModelAndView mv = null;
            if (this.isBlank(header) || !header.equals(etag)) {
                mv = (ModelAndView) joinPoint.proceed();
            } else {
                this.getResponse().setStatus(HttpStatus.NOT_MODIFIED.value());
            }

            HttpServletResponse response = this.getResponse();
            response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue());
            response.setHeader(HttpHeaders.ETAG, etag);
            return mv;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getETag(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        HttpCachable cachAnnotation = AnnotationUtils.findAnnotation(method, HttpCachable.class);
        GetMapping mappingAnnotation = AnnotationUtils.findAnnotation(method, GetMapping.class);
        String key = cachAnnotation.value();
        if (this.isBlank(key)) {
            key = Arrays.stream(mappingAnnotation.path()).collect(Collectors.joining());
        }
        return this.getETagService().getETag(key);
    }

    private HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }

    private HttpServletResponse getResponse() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().equals("");
    }

    private ETagService getETagService() {
        return this.eTagService;
    }
}
