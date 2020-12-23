package fr.cnes.regards.framework.utils.parser;

import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fr.cnes.regards.framework.utils.parser.rule.AndRule;
import fr.cnes.regards.framework.utils.parser.rule.IRule;
import fr.cnes.regards.framework.utils.parser.rule.NotRule;
import fr.cnes.regards.framework.utils.parser.rule.PropertyRule;
import fr.cnes.regards.framework.utils.parser.rule.RegexpPropertyRule;

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
    public Boolean visit(AndRule rule) {
        LOGGER.debug("Visiting {}", rule.getClass().getName());
        Boolean result = Boolean.TRUE;
        for (IRule child : rule.getRules()) {
            result = result && child.accept(this);
        }
        return result;
    }

    @Override
    public Boolean visit(NotRule rule) {
        LOGGER.debug("Visiting {}", rule.getClass().getName());
        return !rule.getRule().accept(this);
    }

    @Override
    public Boolean visit(PropertyRule rule) {
        LOGGER.debug("Visiting {}", rule.getClass().getName());
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
    public Boolean visit(RegexpPropertyRule rule) {
        LOGGER.debug("Visiting {}", rule.getClass().getName());
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
}
