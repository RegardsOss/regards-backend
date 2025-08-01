package fr.cnes.regards.framework.utils.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fr.cnes.regards.framework.utils.parser.rule.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;

public class JsonObjectMatchVisitor implements IRuleVisitor<Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonObjectMatchVisitor.class);

    private final JsonObject object;

    public JsonObjectMatchVisitor(JsonObject object) {
        this.object = object;
        if (this.object == null) {
            String message = "JSON object cannot be null";
            LOGGER.error(message);
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public Boolean visitAlways() {
        return Boolean.TRUE;
    }

    @Override
    public Boolean visitNever() {
        return Boolean.FALSE;
    }

    @Override
    public Boolean visitAnd(AndRule rule) {
        logVisit(rule);
        for (IRule subRule : rule.getRules()) {
            if (!subRule.accept(this)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Boolean visitOr(OrRule rule) {
        logVisit(rule);
        for (IRule subRule : rule.getRules()) {
            if (subRule.accept(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitNot(NotRule rule) {
        logVisit(rule);
        return !rule.getRule().accept(this);
    }

    @Override
    public Boolean visitProperty(PropertyRule rule) {
        logVisit(rule);
        // Find property to test
        JsonElement el = findPropertyByPath(rule.getPropertyPath());

        // Test if property is matching rule
        // - null element
        if (el == null) {
            return rule.getValue() == null;
        }
        // - null value
        if (rule.getValue() == null) {
            return el.getAsString() == null;
        }
        // - real value
        return matchesString(rule.getValue(), el);
    }

    private boolean matchesString(String value, JsonElement el) {
        if (el.isJsonPrimitive()) {
            return value.equals(el.getAsString());
        } else if (el.isJsonArray()) {
            for (JsonElement item : el.getAsJsonArray()) {
                if (matchesString(value, item)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    @Override
    public Boolean visitRegex(RegexpPropertyRule rule) {
        logVisit(rule);
        // Find property to test
        JsonElement el = findPropertyByPath(rule.getPropertyPath());

        // Test if property is matching rule
        // - null element
        if (el == null) {
            return Boolean.FALSE;
        }
        // - real value
        if (el.isJsonPrimitive()) {
            Matcher matcher = rule.getPattern().matcher(el.getAsString());
            return matcher.matches();
        } else {
            return Boolean.FALSE;
        }
    }

    @Override
    public Boolean visitNumberRange(NumberRangePropertyRule rule) {
        logVisit(rule);
        // Find property to test
        JsonElement el = findPropertyByPath(rule.getPropertyPath());

        if (el == null || !el.isJsonPrimitive()) {
            return Boolean.FALSE;
        }
        JsonPrimitive primitive = el.getAsJsonPrimitive();
        if (!primitive.isNumber()) {
            return Boolean.FALSE;
        }
        return rule.matchesValue(primitive.getAsBigDecimal());
    }

    private JsonElement findPropertyByPath(String[] absolutePath) {
        // Retrieve leaf
        JsonElement el = this.object;
        for (String path : absolutePath) {
            if (el == null) {
                LOGGER.debug("Skipping search");
                break;
            }
            if (el.isJsonObject()) {
                el = ((JsonObject) el).get(path);
            } else {
                LOGGER.debug("Property not found at {}", String.join(".", absolutePath));
                return null;
            }
        }
        return el;
    }

    private static void logVisit(IRule rule) {
        LOGGER.debug("Visiting {}", rule.getClass().getName());
    }
}
