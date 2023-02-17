package io.github.lc.oss.commons.web.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.github.lc.oss.commons.serialization.Jsonable;
import io.github.lc.oss.commons.testing.AbstractMockTest;

public class JsonServiceTest extends AbstractMockTest {
    @JsonInclude(Include.NON_EMPTY)
    public static class TestObject implements Jsonable {
        public Integer field;
    }

    @InjectMocks
    private JsonService service;

    @Test
    public void test_from_error() {
        try {
            this.service.from("not-json", Jsonable.class);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error deseralizing from JSON", ex.getMessage());
        }
    }

    @Test
    public void test_from() {
        TestObject result = this.service.from(null, TestObject.class);
        Assertions.assertNull(result);

        result = this.service.from("", TestObject.class);
        Assertions.assertNull(result);

        result = this.service.from("null", TestObject.class);
        Assertions.assertNull(result);

        result = this.service.from(" \t \r \n \t ", TestObject.class);
        Assertions.assertNull(result);

        result = this.service.from("{}", TestObject.class);
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.field);

        result = this.service.from("{\"field\" : 100}", TestObject.class);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(100, result.field);
    }

    @Test
    public void test_to_error() {
        try {
            this.service.to(new Jsonable() {
                @SuppressWarnings("unused")
                String explode() {
                    throw new RuntimeException("Boom!");
                }
            });
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error seralizing to JSON", ex.getMessage());
        }
    }

    @Test
    public void test_to() {
        String result = this.service.to(null);
        Assertions.assertEquals("null", result);

        TestObject o1 = new TestObject();

        result = this.service.to(o1);
        Assertions.assertEquals("{}", result);

        TestObject o2 = new TestObject();
        o2.field = 100;
        result = this.service.to(o2);
        Assertions.assertEquals("{\"field\":100}", result);
    }
}
