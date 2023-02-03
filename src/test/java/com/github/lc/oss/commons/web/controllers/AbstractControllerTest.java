package com.github.lc.oss.commons.web.controllers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.github.lc.oss.commons.l10n.L10N;
import com.github.lc.oss.commons.l10n.UserLocale;
import com.github.lc.oss.commons.l10n.Variable;
import com.github.lc.oss.commons.serialization.JsonMessage;
import com.github.lc.oss.commons.serialization.Jsonable;
import com.github.lc.oss.commons.serialization.JsonableCollection;
import com.github.lc.oss.commons.serialization.JsonableHashMap;
import com.github.lc.oss.commons.serialization.JsonableHashSet;
import com.github.lc.oss.commons.serialization.Message;
import com.github.lc.oss.commons.serialization.Response;
import com.github.lc.oss.commons.testing.AbstractMockTest;
import com.github.lc.oss.commons.web.services.ETagService;

public class AbstractControllerTest extends AbstractMockTest {
    private static class TestController extends AbstractController {
    }

    private enum Category implements Message.Category {
        C
    }

    @Test
    public void test_codeCoverage() {
        AbstractController controller = new TestController();

        Assertions.assertNotNull(controller.getLogger());
    }

    @Test
    public void test_notFound() {
        AbstractController controller = new TestController();

        ResponseEntity<?> result = controller.notFound();
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_noContent() {
        AbstractController controller = new TestController();

        ResponseEntity<?> result = controller.noContent();
        Assertions.assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_respond_nullMessage() {
        AbstractController controller = new TestController();

        ResponseEntity<Response<? extends Jsonable>> result = controller.respond((Message) null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertNull(result.getBody().getBody());
        Assertions.assertNotNull(result.getBody().getMessages());
        Assertions.assertTrue(result.getBody().getMessages().isEmpty());
    }

    @Test
    public void test_respond_hasWarnings() {
        AbstractController controller = new TestController();

        JsonMessage message1 = new JsonMessage(Category.C, Message.Severities.Info, 1);
        JsonMessage message2 = new JsonMessage(Category.C, Message.Severities.Warning, 1);
        ResponseEntity<Response<? extends Jsonable>> result = controller.respond(message1, message2);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.ACCEPTED, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertNull(result.getBody().getBody());
        Assertions.assertNotNull(result.getBody().getMessages());
        Assertions.assertEquals(2, result.getBody().getMessages().size());
        Assertions.assertTrue(result.getBody().getMessages().contains(message1));
        Assertions.assertTrue(result.getBody().getMessages().contains(message2));
    }

    @Test
    public void test_respond_hasError() {
        AbstractController controller = new TestController();

        JsonMessage message1 = new JsonMessage(Category.C, null, 1);
        JsonMessage message2 = new JsonMessage(Category.C, Message.Severities.Warning, 1);
        JsonMessage message3 = new JsonMessage(Category.C, Message.Severities.Error, 1);
        ResponseEntity<Response<? extends Jsonable>> result = controller.respond(message1, message2, message3);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertNull(result.getBody().getBody());
        Assertions.assertNotNull(result.getBody().getMessages());
        Assertions.assertEquals(1, result.getBody().getMessages().size());
        Assertions.assertFalse(result.getBody().getMessages().contains(message1));
        Assertions.assertFalse(result.getBody().getMessages().contains(message2));
        Assertions.assertTrue(result.getBody().getMessages().contains(message3));
    }

    @Test
    public void test_respond_noMessages_noBody() {
        AbstractController controller = new TestController();

        ResponseEntity<Response<Jsonable>> result = controller.respond((Jsonable) null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_respond_noMessages() {
        AbstractController controller = new TestController();

        Jsonable body = new Jsonable() {
        };

        ResponseEntity<Response<Jsonable>> result = controller.respond(body);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertSame(body, result.getBody().getBody());
        Assertions.assertNull(result.getBody().getMessages());
    }

    @Test
    public void test_respond_collection() {
        AbstractController controller = new TestController();

        Jsonable body = new Jsonable() {
        };

        ResponseEntity<Response<JsonableHashSet<Jsonable>>> result = controller.respond(new JsonableHashSet<>(Arrays.asList(body)));
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertNotNull(result.getBody().getBody());
        Assertions.assertEquals(1, result.getBody().getBody().size());
        Assertions.assertSame(body, result.getBody().getBody().iterator().next());
        Assertions.assertNull(result.getBody().getMessages());
    }

    @Test
    public void test_respond_emptyCollection() {
        AbstractController controller = new TestController();

        ResponseEntity<Response<JsonableHashSet<Jsonable>>> result = controller.respond(new JsonableHashSet<Jsonable>());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_respond_map() {
        AbstractController controller = new TestController();

        Jsonable body = new Jsonable() {
        };

        JsonableHashMap<Jsonable> map = new JsonableHashMap<>();
        map.put("key", body);
        ResponseEntity<Response<JsonableHashMap<Jsonable>>> result = controller.respond(map);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertNotNull(result.getBody().getBody());
        Assertions.assertEquals(1, result.getBody().getBody().size());
        Assertions.assertSame("key", result.getBody().getBody().entrySet().iterator().next().getKey());
        Assertions.assertSame(body, result.getBody().getBody().entrySet().iterator().next().getValue());
        Assertions.assertNull(result.getBody().getMessages());
    }

    @Test
    public void test_respond_emptyMap() {
        AbstractController controller = new TestController();

        ResponseEntity<Response<JsonableHashMap<Jsonable>>> result = controller.respond(new JsonableHashMap<>());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        Assertions.assertNull(result.getBody());
    }

    @Test
    public void test_respond() {
        AbstractController controller = new TestController();

        Response<Jsonable> response = new Response<>();
        response.addMessages(new JsonMessage(Category.C, Message.Severities.Error, 1, "oops"));
        ResponseEntity<Response<Jsonable>> result = controller.respond(response);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertNull(result.getBody().getBody());
        Assertions.assertNotNull(result.getBody().getMessages());
        Assertions.assertTrue(result.getBody().hasSeverity(Message.Severities.Error));
    }

    @Test
    public void test_respond_noContent() {
        AbstractController controller = new TestController();

        Response<Jsonable> response = new Response<>();
        ResponseEntity<Response<Jsonable>> result = controller.respond(response);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertNull(result.getBody().getBody());
        Assertions.assertNull(result.getBody().getMessages());
    }

    @Test
    public void test_respond_witHeaders() {
        AbstractController controller = new TestController();

        Response<Jsonable> response = new Response<>();
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache().cachePrivate());
        headers.setETag("W/\"tag\"");
        ResponseEntity<Response<Jsonable>> result = controller.respond(response, headers);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertNull(result.getBody().getBody());
        Assertions.assertNull(result.getBody().getMessages());
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"tag\"", result.getHeaders().getETag());
    }

    @Test
    public void test_respond_etag_nullService() {
        AbstractController controller = new TestController();

        Response<Jsonable> response = new Response<>();
        ResponseEntity<Response<Jsonable>> result = controller.respond(response, "etag-id");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertNull(result.getBody().getBody());
        Assertions.assertNull(result.getBody().getMessages());
        Assertions.assertNull(result.getHeaders().getCacheControl());
        Assertions.assertNull(result.getHeaders().getETag());
    }

    @Test
    public void test_respond_etag() {
        final ETagService etagService = Mockito.mock(ETagService.class);

        Mockito.when(etagService.getETag("etag-id")).thenReturn("W/\"tag\"");

        AbstractController controller = new TestController();
        this.setField("etagService", etagService, controller);

        Response<Jsonable> response = new Response<>();
        ResponseEntity<Response<Jsonable>> result = controller.respond(response, "etag-id");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        Assertions.assertNull(result.getBody().getBody());
        Assertions.assertNull(result.getBody().getMessages());
        Assertions.assertEquals(CacheControl.noCache().cachePrivate().getHeaderValue(), result.getHeaders().getCacheControl());
        Assertions.assertEquals("W/\"tag\"", result.getHeaders().getETag());
    }

    @Test
    public void test_respond_message() {
        AbstractController controller = new TestController();

        ResponseEntity<Response<JsonMessage>> response = controller.respond(Category.C, Message.Severities.Error, 1);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        Response<JsonMessage> body = response.getBody();
        Assertions.assertNotNull(body);
        JsonableCollection<JsonMessage> messages = body.getMessages();
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        Message message = messages.iterator().next();
        Assertions.assertNotNull(message);
        Assertions.assertEquals(Category.C, message.getCategory());
        Assertions.assertEquals(Message.Severities.Error, message.getSeverity());
        Assertions.assertEquals(1, message.getNumber());
        Assertions.assertNull(message.getText());
    }

    @Test
    public void test_respondMessage() {
        AbstractController controller = new TestController();

        ResponseEntity<Response<JsonMessage>> response = controller.respondMessage(new JsonMessage(Category.C, Message.Severities.Error, 1));
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        Response<JsonMessage> body = response.getBody();
        Assertions.assertNotNull(body);
        JsonableCollection<JsonMessage> messages = body.getMessages();
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        Message message = messages.iterator().next();
        Assertions.assertNotNull(message);
        Assertions.assertEquals(Category.C, message.getCategory());
        Assertions.assertEquals(Message.Severities.Error, message.getSeverity());
        Assertions.assertEquals(1, message.getNumber());
        Assertions.assertNull(message.getText());
    }

    @Test
    public void test_respond_messageCollection() {
        AbstractController controller = new TestController();

        Message m1 = new Message() {
            @Override
            public Category getCategory() {
                return AbstractControllerTest.Category.C;
            }

            @Override
            public Severity getSeverity() {
                return Message.Severities.Error;
            }

            @Override
            public int getNumber() {
                return 1;
            }
        };

        Message m2 = new Message() {
            @Override
            public Category getCategory() {
                return AbstractControllerTest.Category.C;
            }

            @Override
            public Severity getSeverity() {
                return Message.Severities.Info;
            }

            @Override
            public int getNumber() {
                return 2;
            }
        };

        JsonMessage m3 = new JsonMessage(Category.C, Message.Severities.Warning, 3, "junit");

        ResponseEntity<Response<JsonMessage>> response = controller.respond(m1, m2, null, m3);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        Response<JsonMessage> body = response.getBody();
        Assertions.assertNotNull(body);
        JsonableCollection<JsonMessage> messages = body.getMessages();
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        Message message = messages.iterator().next();
        Assertions.assertNotNull(message);
        Assertions.assertEquals(Category.C, message.getCategory());
        Assertions.assertEquals(Message.Severities.Error, message.getSeverity());
        Assertions.assertEquals(1, message.getNumber());
        Assertions.assertNull(message.getText());
    }

    @Test
    public void test_respond_jsonMessageCollection() {
        AbstractController controller = new TestController();

        JsonMessage m1 = new JsonMessage(Category.C, Message.Severities.Error, 1);
        JsonMessage m2 = new JsonMessage(Category.C, Message.Severities.Info, 2);
        JsonMessage m3 = new JsonMessage(Category.C, Message.Severities.Warning, 3, "junit");

        ResponseEntity<Response<JsonMessage>> response = controller.respond(m1, null, m2, m3);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        Response<JsonMessage> body = response.getBody();
        Assertions.assertNotNull(body);
        JsonableCollection<JsonMessage> messages = body.getMessages();
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        Message message = messages.iterator().next();
        Assertions.assertNotNull(message);
        Assertions.assertEquals(Category.C, message.getCategory());
        Assertions.assertEquals(Message.Severities.Error, message.getSeverity());
        Assertions.assertEquals(1, message.getNumber());
        Assertions.assertNull(message.getText());
    }

    @Test
    public void test_respond_stream() {
        AbstractController controller = new TestController();

        InputStreamResource content = Mockito.mock(InputStreamResource.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Test", "T");

        ResponseEntity<InputStreamResource> result = controller.respond(content, headers);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertSame(content, result.getBody());
        List<String> resultHeaders = result.getHeaders().get("X-Test");
        Assertions.assertNotNull(resultHeaders);
        Assertions.assertEquals(1, resultHeaders.size());
        Assertions.assertEquals("T", resultHeaders.iterator().next());
    }

    @Test
    public void test_toMessage_noL10N() {
        AbstractController controller = new TestController();

        Message message = controller.toMessage(Category.C, Message.Severities.Info, 1, new Variable("key", "value"));
        Assertions.assertNotNull(message);
        Assertions.assertEquals(Category.C, message.getCategory());
        Assertions.assertEquals(Message.Severities.Info, message.getSeverity());
        Assertions.assertEquals(1, message.getNumber());
        Assertions.assertNull(message.getText());
    }

    @Test
    public void test_toMessage_noUserLocale() {
        L10N l10n = Mockito.mock(L10N.class);

        Variable var = new Variable("key", "value");

        AbstractController controller = new TestController();
        this.setField("l10n", l10n, controller);

        Mockito.when(l10n.getDefaultLocale()).thenReturn(Locale.ENGLISH);
        Mockito.when(l10n.getText(Locale.ENGLISH, "messages.C.Info.1", var)).thenReturn("text value");

        Message message = controller.toMessage(Category.C, Message.Severities.Info, 1, var);
        Assertions.assertNotNull(message);
        Assertions.assertEquals(Category.C, message.getCategory());
        Assertions.assertEquals(Message.Severities.Info, message.getSeverity());
        Assertions.assertEquals(1, message.getNumber());
        Assertions.assertEquals("text value", message.getText());
    }

    @Test
    public void test_toMessage() {
        L10N l10n = Mockito.mock(L10N.class);
        UserLocale userLocale = new UserLocale(Locale.GERMAN);

        Variable var = new Variable("key", "value");

        AbstractController controller = new TestController();
        this.setField("l10n", l10n, controller);
        this.setField("userLocale", userLocale, controller);

        Mockito.when(l10n.getText(Locale.GERMAN, "messages.C.Info.1", var)).thenReturn("text value");

        Message src = new Message() {
            @Override
            public Category getCategory() {
                return AbstractControllerTest.Category.C;
            }

            @Override
            public Severity getSeverity() {
                return Message.Severities.Info;
            }

            @Override
            public int getNumber() {
                return 1;
            }

        };

        Message message = controller.toMessage(src, var);
        Assertions.assertNotNull(message);
        Assertions.assertEquals(Category.C, message.getCategory());
        Assertions.assertEquals(Message.Severities.Info, message.getSeverity());
        Assertions.assertEquals(1, message.getNumber());
        Assertions.assertEquals("text value", message.getText());
    }

    @Test
    public void test_toMessage_alreadyJsonMessage() {
        final JsonMessage message = new JsonMessage(Category.C, Message.Severities.Info, 1, "test");

        AbstractController controller = new TestController();

        JsonMessage result = controller.toMessage(message);
        Assertions.assertSame(message, result);
        Assertions.assertEquals("test", result.getText());
    }
}
