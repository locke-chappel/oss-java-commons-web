package io.github.lc.oss.commons.web.services;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.commons.testing.AbstractMockTest;

public class AbstractETagServiceTest extends AbstractMockTest {
    private static class TestService extends AbstractETagService {
        @Override
        protected String getAppVersion() {
            return "0.0.0-Test";
        }
    }

    private static class TestExpiredService extends AbstractETagService {
        @Override
        protected Clock getClock() {
            return Clock.fixed(Instant.ofEpochMilli(System.currentTimeMillis() - 24 * 60 * 60 * 1000), Clock.systemDefaultZone().getZone());
        }

        @Override
        protected String getAppVersion() {
            return "0.0.0-Test";
        }
    }

    @Test
    public void test_getETag_nullId_enabled() {
        ETagService service = new TestService();
        service.setEnabled(true);
        Assertions.assertTrue(service.isEnabled());

        try {
            service.getETag(null);
            Assertions.fail("Expected exception");
        } catch (IllegalArgumentException ex) {
            Assertions.assertEquals("id cannot be null/blank", ex.getMessage());
        }
    }

    @Test
    public void test_getETag_blankId_enabled() {
        ETagService service = new TestService();
        this.setField("clock", Clock.systemDefaultZone(), service);
        service.setEnabled(true);
        Assertions.assertTrue(service.isEnabled());

        String result = service.getETag("");

        this.delay();

        String result2 = service.getETag("");
        Assertions.assertSame(result, result2);

        service.clearCache();

        String result3 = service.getETag("");
        Assertions.assertNotSame(result, result3);
        Assertions.assertEquals(result, result3);
    }

    @Test
    public void test_getETag_emptyId_enabled() {
        ETagService service = new TestService();
        this.setField("clock", Clock.systemDefaultZone(), service);
        service.setEnabled(true);
        Assertions.assertTrue(service.isEnabled());

        String result = service.getETag(" \t \r \n \t ");

        this.delay();

        String result2 = service.getETag(" \t \r \n \t ");
        Assertions.assertSame(result, result2);

        service.clearCache();

        String result3 = service.getETag(" \t \r \n \t ");
        Assertions.assertNotSame(result, result3);
        Assertions.assertEquals(result, result3);
    }

    @Test
    public void test_getETag_enabled() {
        ETagService service = new TestService();
        this.setField("clock", Clock.systemDefaultZone(), service);
        service.setEnabled(true);
        Assertions.assertTrue(service.isEnabled());

        String result = service.getETag("cache-id");

        this.delay();

        String result2 = service.getETag("cache-id");
        Assertions.assertSame(result, result2);

        service.clearCache();

        String result3 = service.getETag("cache-id");
        Assertions.assertNotSame(result, result3);
        Assertions.assertEquals(result, result3);
    }

    @Test
    public void test_getETag_expired_enabled() {
        ETagService service = new TestExpiredService();
        service.setEnabled(true);
        Assertions.assertTrue(service.isEnabled());

        String result = service.getETag("cache-id");

        this.delay();

        String result2 = service.getETag("cache-id");
        Assertions.assertNotSame(result, result2);
        Assertions.assertEquals(result, result2);
    }

    @Test
    public void test_getETag_nullId_disabled() {
        ETagService service = new TestService();
        service.setEnabled(false);
        Assertions.assertFalse(service.isEnabled());

        try {
            service.getETag(null);
            Assertions.fail("Expected exception");
        } catch (IllegalArgumentException ex) {
            Assertions.assertEquals("id cannot be null/blank", ex.getMessage());
        }
    }

    @Test
    public void test_getETag_blankId_disabled() {
        ETagService service = new TestService();
        service.setEnabled(false);
        Assertions.assertFalse(service.isEnabled());

        String result = service.getETag("");

        this.delay();

        String result2 = service.getETag("");
        Assertions.assertNotEquals(result, result2);
    }

    @Test
    public void test_getETag_emptyId_disabled() {
        ETagService service = new TestService();
        service.setEnabled(false);
        Assertions.assertFalse(service.isEnabled());

        String result = service.getETag(" \t \r \n \t ");

        this.delay();

        String result2 = service.getETag(" \t \r \n \t ");
        Assertions.assertNotEquals(result, result2);
    }

    @Test
    public void test_getETag_disabled() {
        ETagService service = new TestService();
        service.setEnabled(false);
        Assertions.assertFalse(service.isEnabled());

        String result = service.getETag("cache-id");

        this.delay();

        String result2 = service.getETag("cache-id");
        Assertions.assertNotEquals(result, result2);
    }

    @Test
    public void test_getETag_nullClock() {
        ETagService service = new TestService();
        this.setField("clock", null, service);
        service.setEnabled(true);
        Assertions.assertTrue(service.isEnabled());

        try {
            service.getETag("cache-id");
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Clock cannot be null", ex.getMessage());
        }
    }

    @Test
    public void test_getETag_nullVersion() {
        ETagService service = new AbstractETagService() {
            @Override
            protected Clock getClock() {
                return Clock.systemDefaultZone();
            }

            @Override
            protected String getAppVersion() {
                return null;
            }
        };
        service.setEnabled(true);
        Assertions.assertTrue(service.isEnabled());

        try {
            service.getETag("cache-id");
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Version cannot be blank", ex.getMessage());
        }
    }

    @Test
    public void test_getETag_emptyVersion() {
        ETagService service = new AbstractETagService() {
            @Override
            protected Clock getClock() {
                return Clock.systemDefaultZone();
            }

            @Override
            protected String getAppVersion() {
                return "";
            }
        };
        service.setEnabled(true);
        Assertions.assertTrue(service.isEnabled());

        try {
            service.getETag("cache-id");
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Version cannot be blank", ex.getMessage());
        }
    }

    @Test
    public void test_getETag_blankVersion() {
        ETagService service = new AbstractETagService() {
            @Override
            protected Clock getClock() {
                return Clock.systemDefaultZone();
            }

            @Override
            protected String getAppVersion() {
                return " \t \r \n \t ";
            }
        };
        service.setEnabled(true);
        Assertions.assertTrue(service.isEnabled());

        try {
            service.getETag("cache-id");
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Version cannot be blank", ex.getMessage());
        }
    }

    @Test
    public void test_evictETag() {
        ETagService service = new TestService();
        this.setField("clock", Clock.systemDefaultZone(), service);
        service.setEnabled(true);
        Assertions.assertTrue(service.isEnabled());

        String result = service.getETag("cache-id");

        // evict id
        service.evictETag("cache-id");

        // evict an id that isn't there
        service.evictETag("cache-id");

        String result2 = service.getETag("cache-id");
        Assertions.assertNotSame(result, result2);
        Assertions.assertEquals(result, result2);
    }

    private void delay() {
        final long now = System.currentTimeMillis();
        this.waitUntil(() -> System.currentTimeMillis() >= now + 100);
    }
}
