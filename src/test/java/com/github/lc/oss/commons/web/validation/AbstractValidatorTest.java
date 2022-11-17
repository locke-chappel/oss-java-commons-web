package com.github.lc.oss.commons.web.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.lc.oss.commons.serialization.JsonMessage;
import com.github.lc.oss.commons.serialization.Message;
import com.github.lc.oss.commons.serialization.Message.Category;

public class AbstractValidatorTest {
    public enum Categories implements Category {
        Application
    }

    private static class TestValidator extends AbstractValidator<String> {
        @Override
        public Set<Message> validate(String instance) {
            return new HashSet<>(Arrays.asList(new JsonMessage(Categories.Application, Message.Severities.Info, 1)));
        }
    }

    @Test
    public void test_matches() {
        AbstractValidator<?> validator = new TestValidator();

        Pattern p = Pattern.compile("^a$");
        Assertions.assertFalse(validator.matches(p, null));
        Assertions.assertFalse(validator.matches(p, ""));
        Assertions.assertFalse(validator.matches(p, " "));
        Assertions.assertFalse(validator.matches(p, "b"));
        Assertions.assertFalse(validator.matches(p, "A"));
        Assertions.assertTrue(validator.matches(p, "a"));
    }

    @Test
    public void test_matches_optional() {
        AbstractValidator<?> validator = new TestValidator();

        Pattern p = Pattern.compile("^a$");
        Assertions.assertTrue(validator.matches(p, null, false));
        Assertions.assertFalse(validator.matches(p, "", false));
        Assertions.assertFalse(validator.matches(p, " ", false));
        Assertions.assertFalse(validator.matches(p, "b", false));
        Assertions.assertFalse(validator.matches(p, "A", false));
        Assertions.assertTrue(validator.matches(p, "a", false));
    }

    @Test
    public void test_valid() {
        AbstractValidator<?> validator = new TestValidator();

        Set<Message> result = validator.valid();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_merge() {
        AbstractValidator<String> validator = new TestValidator();

        Set<Message> messages = new HashSet<>();
        Assertions.assertTrue(messages.isEmpty());

        validator.merge(messages, validator, "a");

        Assertions.assertEquals(1, messages.size());
        Message message = messages.iterator().next();
        Assertions.assertEquals(Categories.Application, message.getCategory());
        Assertions.assertEquals(Message.Severities.Info, message.getSeverity());
        Assertions.assertEquals(1, message.getNumber());
    }

    @Test
    public void test_missingValue_string() {
        AbstractValidator<String> validator = new AbstractValidator<String>() {
            @Override
            public Set<Message> validate(String instance) {
                return null;
            }
        };

        Assertions.assertTrue(validator.missingValue(null));
        Assertions.assertTrue(validator.missingValue(""));
        Assertions.assertTrue(validator.missingValue(" \t \r \b \t "));
        Assertions.assertFalse(validator.missingValue(" a "));
        Assertions.assertFalse(validator.missingValue("B"));
    }

    @Test
    public void test_missingValue_collection() {
        AbstractValidator<Collection<String>> validator = new AbstractValidator<Collection<String>>() {
            @Override
            public Set<Message> validate(Collection<String> instance) {
                return null;
            }
        };

        Assertions.assertTrue(validator.missingValue(null));
        Assertions.assertTrue(validator.missingValue(new ArrayList<>()));
        Assertions.assertFalse(validator.missingValue(Arrays.asList("")));
    }

    @Test
    public void test_missingValue_object() {
        AbstractValidator<Object> validator = new AbstractValidator<Object>() {
            @Override
            public Set<Message> validate(Object instance) {
                return null;
            }
        };

        Assertions.assertTrue(validator.missingValue(null));
        Assertions.assertFalse(validator.missingValue(new Object()));
    }
}
