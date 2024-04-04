package fr.cnes.regards.framework.utils.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
    public Boolean visitAnd(AndRule rule) {
        logVisit(rule.getClass());
        Boolean result = Boolean.TRUE;
        for (IRule child : rule.getRules()) {
            result = result && child.accept(this);
        }
        return result;
    }

    @Override
    public Boolean visitOr(OrRule rule) {
        logVisit(rule.getClass());
        Boolean result = Boolean.FALSE;
        for (IRule child : rule.getRules()) {
            result = result || child.accept(this);
        }
        return result;
    }

    @Override
    public Boolean visitNot(NotRule rule) {
        logVisit(rule.getClass());
        return !rule.getRule().accept(this);
    }

    @Override
    public Boolean visitProperty(PropertyRule rule) {
        logVisit(rule.getClass());
        // Find property to test
        JsonElement el = findPropertyByPath(rule.getProperty(), this.object);

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
        if (el.isJsonPrimitive()) {
            return rule.getValue().equals(el.getAsString());
        } else {
            return Boolean.FALSE;
        }
    }

    @Override
    public Boolean visitRegex(RegexpPropertyRule rule) {
        logVisit(rule.getClass());
        // Find property to test
        JsonElement el = findPropertyByPath(rule.getProperty(), this.object);

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

    private JsonElement findPropertyByPath(String absolutePath, JsonElement element) {
        // Retrieve property from absolute JSON path
        String[] paths = absolutePath.split("\\.");

        // Retrieve leaf
        JsonElement el = this.object;
        for (String path : paths) {
            if (el == null) {
                LOGGER.debug("Skipping search");
                continue;
            }
            if (el.isJsonObject()) {
                el = ((JsonObject) el).get(path);
            } else {
                LOGGER.debug("Property not found at {}!", absolutePath);
                return null;
            }
        }
        return el;
    }

    private static void logVisit(Class<? extends IRule> ruleClass) {
        LOGGER.debug("Visiting {}", ruleClass.getName());
    }
}
