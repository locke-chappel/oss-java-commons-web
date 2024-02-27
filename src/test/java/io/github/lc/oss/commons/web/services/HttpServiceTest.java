package io.github.lc.oss.commons.web.services;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import io.github.lc.oss.commons.testing.AbstractMockTest;

public class HttpServiceTest extends AbstractMockTest {

    private HttpService service;

    @BeforeEach
    public void init() {
        this.service = new HttpService();
    }

    @Test
    public void test_call_badUrl() {
        try {
            this.service.call(null, "smb://", null, null, null);
            Assertions.fail("Expected excpetion");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Invalid URL", ex.getMessage());
        }
    }

    @Test
    public void test_call() {
        RestTemplate template = Mockito.mock(RestTemplate.class);
        HttpService test = new HttpService() {
            @Override
            public RestTemplate createRestTemplate() {
                return template;
            }
        };

        Mockito.when(template.exchange(ArgumentMatchers.notNull(), ArgumentMatchers.eq(HttpMethod.GET), ArgumentMatchers.notNull(),
                ArgumentMatchers.eq(Object.class))).thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        ResponseEntity<Object> result = test.call(HttpMethod.GET, "http://localhost", null, Object.class, null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    public void test_call_withHeaders() {
        RestTemplate template = Mockito.mock(RestTemplate.class);
        HttpService test = new HttpService() {
            @Override
            public RestTemplate createRestTemplate() {
                return template;
            }
        };

        Mockito.when(template.exchange(ArgumentMatchers.notNull(), ArgumentMatchers.eq(HttpMethod.GET), ArgumentMatchers.notNull(),
                ArgumentMatchers.eq(Object.class))).thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Test", "test");

        ResponseEntity<Object> result = test.call(HttpMethod.GET, "http://localhost", headers, Object.class, null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    public void test_createRestTemplate() {
        HttpService test = new HttpService();

        Assertions.assertNotNull(test.createRequestFactory());

        RestTemplate result = test.createRestTemplate();
        Assertions.assertNotNull(result);
    }

    @Test
    public void test_createRestTemplate_nulls() {
        HttpService test = new HttpService() {
            @Override
            public ClientHttpRequestFactory createRequestFactory() {
                return null;
            }
        };

        Assertions.assertNull(test.createRequestFactory());

        RestTemplate result = test.createRestTemplate();
        Assertions.assertNotNull(result);
    }

    @Test
    public void test_getTimeout() {
        Assertions.assertEquals(HttpService.DEFAULT_TIMEOUT, this.service.getTimeout());
    }

    @Test
    public void test_customErrorHandler() {
        final ResponseErrorHandler errorHandler = Mockito.mock(ResponseErrorHandler.class);

        HttpService test = new HttpService() {
            @Override
            protected ResponseErrorHandler getCustomResponseErrorHandler() {
                return errorHandler;
            }
        };

        RestTemplate template = test.createRestTemplate();
        Assertions.assertNotNull(template);
        Assertions.assertSame(errorHandler, template.getErrorHandler());
    }
}
