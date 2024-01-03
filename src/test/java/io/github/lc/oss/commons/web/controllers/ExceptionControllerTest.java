package io.github.lc.oss.commons.web.controllers;

import java.io.IOException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.NoHandlerFoundException;

import io.github.lc.oss.commons.serialization.JsonMessage;
import io.github.lc.oss.commons.serialization.JsonableCollection;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.commons.serialization.Response;
import io.github.lc.oss.commons.testing.AbstractMockTest;
import io.github.lc.oss.commons.web.annotations.HttpCachable;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ExceptionControllerTest extends AbstractMockTest {
    private static class SelfRootedException extends Exception {
        private static final long serialVersionUID = 2536899574560993728L;

        @Override
        public synchronized Throwable getCause() {
            return this;
        }
    }

    private static class CallHelper {
        public boolean wasCalled = false;
    }

    @Controller
    private static class TestController {
        @HttpCachable
        public String notRestful() {
            return null;
        }

        @GetMapping
        public String getNothing() {
            return null;
        }

        @GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
        public String getXml() {
            return null;
        }

        @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        public String getJson() {
            return null;
        }

        @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        public String deleteJson() {
            return null;
        }

        @PatchMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        public String patchJson() {
            return null;
        }

        @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        public String postJson() {
            return null;
        }

        @PutMapping(produces = MediaType.APPLICATION_XML_VALUE)
        public String putXml() {
            return null;
        }

        @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        public String requestJson() {
            return null;
        }
    }

    private HttpServletRequest request;
    private HttpServletResponse response;

    private ExceptionController controller;

    @BeforeEach
    public void init() {
        this.request = Mockito.mock(HttpServletRequest.class);
        this.response = Mockito.mock(HttpServletResponse.class);
        this.controller = new ExceptionController();
    }

    @Test
    public void test_catchException_null() {
        Exception ex = null;

        ResponseEntity<?> result = this.controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_null_withJsonHeader() {
        Exception ex = null;

        Mockito.when(this.request.getHeader(HttpHeaders.ACCEPT)).thenReturn(MediaType.APPLICATION_JSON_VALUE.toUpperCase());

        ResponseEntity<?> result = this.controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_null_withImageHeader() {
        Exception ex = null;

        Mockito.when(this.request.getHeader(HttpHeaders.ACCEPT)).thenReturn(MediaType.IMAGE_PNG_VALUE);

        ResponseEntity<?> result = this.controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_catchException_unhandledType() {
        Exception ex = new SelfRootedException();

        ResponseEntity<?> result = this.controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_permissions() {
        ServletContext context = Mockito.mock(ServletContext.class);
        Mockito.when(this.request.getServletContext()).thenReturn(context);

        Exception ex = new RuntimeException("boom", new AccessDeniedException("nope"));

        ResponseEntity<?> result = this.controller.catchException(this.request, this.response, ex);
        Assertions.assertNull(result);
    }

    @Test
    public void test_catchException_permissions_withHandler() {
        AccessDeniedHandler accessDeniedHandler = Mockito.mock(AccessDeniedHandler.class);
        ExceptionController controller = new ExceptionController();
        this.setField("accessDeniedHandler", accessDeniedHandler, controller);
        AccessDeniedException ex = new AccessDeniedException("nope");

        final CallHelper helper = new CallHelper();
        try {
            Mockito.doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    Assertions.assertFalse(helper.wasCalled);
                    helper.wasCalled = true;
                    return null;
                }

            }).when(accessDeniedHandler).handle(this.request, this.response, ex);
        } catch (IOException | ServletException e) {
            Assertions.fail("Unexpected exception");
        }

        Assertions.assertFalse(helper.wasCalled);
        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNull(result);
        Assertions.assertTrue(helper.wasCalled);
    }

    @Test
    public void test_catchException_permissions_exception() {
        ServletContext context = Mockito.mock(ServletContext.class);
        Mockito.when(this.request.getServletContext()).thenReturn(context);
        try {
            Mockito.doAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    throw new IOException("boom!");
                }
            }).when(this.response).sendRedirect(ArgumentMatchers.anyString());
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        Exception ex = new AccessDeniedException("nope");

        ResponseEntity<?> result = this.controller.catchException(this.request, this.response, ex);
        Assertions.assertNull(result);
    }

    @Test
    public void test_catchException_badRequest() {
        Exception ex = new HttpMessageNotReadableException("boom", (HttpInputMessage) null);

        ResponseEntity<?> result = this.controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_methodNotSupported() {
        Exception ex = new HttpRequestMethodNotSupportedException("boom");

        ResponseEntity<?> result = this.controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.METHOD_NOT_ALLOWED, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_unsupportedMediaType() {
        Exception ex = new HttpMediaTypeNotSupportedException("boom");

        ResponseEntity<?> result = this.controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_notAcceptable() {
        Exception ex = new HttpMediaTypeNotAcceptableException("boom");

        ResponseEntity<?> result = this.controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_ACCEPTABLE, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_notFound() {
        Exception ex = new NoHandlerFoundException(null, null, null);

        ResponseEntity<?> result = this.controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_controllerMethod_classNotFound() {
        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("Junk", "notRestful", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        ResponseEntity<?> result = this.controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_controllerMethod_methodNotFound() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "junk", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_controllerMethod_exception() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }

            @Override
            Method[] getDeclaredMethods(Class<?> clazz) throws SecurityException {
                throw new SecurityException("BOOM!");
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "error", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_controllerMethodNotRest() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "notRestful", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_controllerMethod_noProduces() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "getNothing", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_controllerMethod_getXml() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "getXml", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_controllerMethod_getJson() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "getJson", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNotNull(response);
        Assertions.assertNull(response.getBody());
        JsonableCollection<JsonMessage> messages = response.getMessages();
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        Message message = messages.iterator().next();
        Assertions.assertEquals(Message.Categories.Application, message.getCategory());
        Assertions.assertEquals(Message.Severities.Error, message.getSeverity());
        Assertions.assertEquals(1, message.getNumber());
    }

    @Test
    public void test_catchException_controllerMethod_postJson() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "postJson", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNotNull(response);
        Assertions.assertNull(response.getBody());
        JsonableCollection<JsonMessage> messages = response.getMessages();
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        Message message = messages.iterator().next();
        Assertions.assertEquals(Message.Categories.Application, message.getCategory());
        Assertions.assertEquals(Message.Severities.Error, message.getSeverity());
        Assertions.assertEquals(1, message.getNumber());
    }

    @Test
    public void test_catchException_controllerMethod_putXml() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "putXml", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_controllerMethod_patchJson() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "patchJson", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNotNull(response);
        Assertions.assertNull(response.getBody());
        JsonableCollection<JsonMessage> messages = response.getMessages();
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        Message message = messages.iterator().next();
        Assertions.assertEquals(Message.Categories.Application, message.getCategory());
        Assertions.assertEquals(Message.Severities.Error, message.getSeverity());
        Assertions.assertEquals(1, message.getNumber());
    }

    @Test
    public void test_catchException_controllerMethod_deleteJson() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "deleteJson", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNotNull(response);
        Assertions.assertNull(response.getBody());
        JsonableCollection<JsonMessage> messages = response.getMessages();
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        Message message = messages.iterator().next();
        Assertions.assertEquals(Message.Categories.Application, message.getCategory());
        Assertions.assertEquals(Message.Severities.Error, message.getSeverity());
        Assertions.assertEquals(1, message.getNumber());
    }

    @Test
    public void test_catchException_controllerMethod_requestJon() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "requestJson", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNotNull(response);
        Assertions.assertNull(response.getBody());
        JsonableCollection<JsonMessage> messages = response.getMessages();
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        Message message = messages.iterator().next();
        Assertions.assertEquals(Message.Categories.Application, message.getCategory());
        Assertions.assertEquals(Message.Severities.Error, message.getSeverity());
        Assertions.assertEquals(1, message.getNumber());
    }

    @Test
    public void test_catchException_controllerMethod_withAllHeader_respondJson() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "getJson", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        Mockito.when(this.request.getHeader(HttpHeaders.ACCEPT)).thenReturn(MediaType.ALL_VALUE.toUpperCase());

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNotNull(response);
        Assertions.assertNull(response.getBody());
        JsonableCollection<JsonMessage> messages = response.getMessages();
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        Message message = messages.iterator().next();
        Assertions.assertEquals(Message.Categories.Application, message.getCategory());
        Assertions.assertEquals(Message.Severities.Error, message.getSeverity());
        Assertions.assertEquals(1, message.getNumber());
    }

    @Test
    public void test_catchException_controllerMethod_withAllHeader_respondXml() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "getXml", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        Mockito.when(this.request.getHeader(HttpHeaders.ACCEPT)).thenReturn(MediaType.ALL_VALUE.toUpperCase());

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_controllerMethod_withJsonHeader_respondJson() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "getJson", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        Mockito.when(this.request.getHeader(HttpHeaders.ACCEPT)).thenReturn(MediaType.APPLICATION_JSON_VALUE.toUpperCase());

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNotNull(response);
        Assertions.assertNull(response.getBody());
        JsonableCollection<JsonMessage> messages = response.getMessages();
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        Message message = messages.iterator().next();
        Assertions.assertEquals(Message.Categories.Application, message.getCategory());
        Assertions.assertEquals(Message.Severities.Error, message.getSeverity());
        Assertions.assertEquals(1, message.getNumber());
    }

    @Test
    public void test_catchException_controllerMethod_withJsonHeader_respondXml() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "getXml", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        Mockito.when(this.request.getHeader(HttpHeaders.ACCEPT)).thenReturn(MediaType.APPLICATION_JSON_VALUE.toLowerCase());

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_controllerMethod_withXmlHeader_respondXml() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "getXml", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        Mockito.when(this.request.getHeader(HttpHeaders.ACCEPT)).thenReturn(MediaType.APPLICATION_XML_VALUE.toLowerCase());

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }

    @Test
    public void test_catchException_controllerMethod_withXmlHeader_respondJson() {
        final Class<?> clazz = TestController.class;
        ExceptionController controller = new ExceptionController() {
            @Override
            Class<?> getClassForName(String name) throws ClassNotFoundException {
                return clazz;
            }
        };

        Exception ex = Mockito.mock(Exception.class);

        StackTraceElement[] st = new StackTraceElement[] { new StackTraceElement("TestController", "getJson", "ExceptionControllerTest.java", 52) };
        Mockito.when(ex.getStackTrace()).thenReturn(st);

        Mockito.when(this.request.getHeader(HttpHeaders.ACCEPT)).thenReturn(MediaType.APPLICATION_XML_VALUE.toLowerCase());

        ResponseEntity<?> result = controller.catchException(this.request, this.response, ex);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<?> response = (Response<?>) result.getBody();
        Assertions.assertNull(response);
    }
}
