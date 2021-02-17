/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.model.gson;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.Fragment;
import fr.cnes.regards.modules.model.domain.attributes.restriction.JsonSchemaRestriction;
import fr.cnes.regards.modules.model.domain.attributes.restriction.RestrictionType;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;

/**
 *
 * @author Sébastien Binda
 *
 */
public abstract class AbstractAttributeHelper implements IAttributeHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAttributeHelper.class);

    private static final String TITLE = "title";

    private static final String TYPE = "type";

    private static final String DESCRIPTION = "description";

    private static final String UNIT = "unit";

    private static final String FORMAT = "format";

    private static final String PROPERTIES = "properties";

    private static final String ITEMS = "items";

    private static final String OBJECT_TYPE = "object";

    private static final String STRING_TYPE = "string";

    private static final String INTEGER_TYPE = "integer";

    private static final String NUMBER_TYPE = "number";

    private static final String BOOLEAN_TYPE = "boolean";

    private static final String ARRAY_TYPE = "array";

    private static final String DEFAULT_FORMAT = "default";

    private static final String DATETIME_FORMAT = "date-time";

    private static final String URI_FORMAT = "uri";

    ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<AttributeModel> getAllAttributes(String tenant) {
        List<AttributeModel> attributes = doGetAllAttributes(tenant);
        List<AttributeModel> jsonSchemaAttributes = Lists.newArrayList();
        attributes.stream()
                .filter(a -> (a.getType() == PropertyType.JSON) && a.hasRestriction()
                        && (a.getRestriction().getType() == RestrictionType.JSON_SHCEMA))
                .forEach(a -> jsonSchemaAttributes
                        .addAll(fromJsonSchema(a.getJsonPropertyPath(),
                                               ((JsonSchemaRestriction) a.getRestriction()).getJsonSchema())));
        attributes.addAll(jsonSchemaAttributes);
        return attributes;
    }

    private List<AttributeModel> fromJsonSchema(String attributePath, String schema) {
        List<AttributeModel> jsonSchemaAttributes = Lists.newArrayList();
        try {
            JsonNode root = mapper.readTree(schema);
            int idx = attributePath.lastIndexOf(".");
            if (idx > 0) {
                createObjectAttributes(attributePath.substring(idx + 1), attributePath.substring(0, idx), root,
                                       jsonSchemaAttributes);
            } else {
                createObjectAttributes(attributePath, null, root, jsonSchemaAttributes);
            }

        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return jsonSchemaAttributes;
    }

    private void createAttributes(String name, String path, JsonNode node, List<AttributeModel> jsonSchemaAttributes) {
        String attributeType = node.get(TYPE).textValue();
        switch (attributeType) {
            case OBJECT_TYPE:
                createObjectAttributes(name, path, node, jsonSchemaAttributes);
                break;
            case STRING_TYPE:
                createStringAttribute(name, path, node, jsonSchemaAttributes);
                break;
            case INTEGER_TYPE:
                createIntegerAttribute(name, path, node, jsonSchemaAttributes);
                break;
            case NUMBER_TYPE:
                createDoubleAttribute(name, path, node, jsonSchemaAttributes);
                break;
            case BOOLEAN_TYPE:
                createBooleanAttribute(name, path, node, jsonSchemaAttributes);
                break;
            case ARRAY_TYPE:
                createArrayAttributes(name, path, node, jsonSchemaAttributes);
                break;
        }
    }

    private void createObjectAttributes(String name, String path, JsonNode node,
            List<AttributeModel> jsonSchemaAttributes) {
        Optional.ofNullable(node.get(PROPERTIES)).ifPresent(properties -> {
            Iterator<Entry<String, JsonNode>> it = properties.fields();
            do {
                Entry<String, JsonNode> field = it.next();
                String fieldPath = Optional.ofNullable(path).map(p -> String.format("%s.%s", p, name)).orElse(name);
                createAttributes(field.getKey(), fieldPath, field.getValue(), jsonSchemaAttributes);
            } while (it.hasNext());
        });
    }

    private void createArrayAttributes(String name, String path, JsonNode node,
            List<AttributeModel> jsonSchemaAttributes) {
        Optional.ofNullable(node.get(ITEMS)).ifPresent(items -> {
            createAttributes(name, path, items, jsonSchemaAttributes);
        });
    }

    private void createStringAttribute(String name, String path, JsonNode node,
            List<AttributeModel> jsonSchemaAttributes) {
        String description = Optional.ofNullable(node.get(DESCRIPTION)).map(JsonNode::asText).orElse(null);
        String unit = Optional.ofNullable(node.get(UNIT)).map(JsonNode::asText).orElse(null);
        String label = Optional.ofNullable(node.get(TITLE)).map(JsonNode::asText).orElse(name);
        String format = Optional.ofNullable(node.get(FORMAT)).map(JsonNode::asText).orElse(DEFAULT_FORMAT);
        AttributeModel attribute = new AttributeModel();
        attribute.setName(name);
        attribute.setJsonPath(Optional.ofNullable(path).map(p -> String.format("%s.%s", p, name)).orElse(name));
        Fragment fragment = new Fragment();
        fragment.setName(path);
        attribute.setFragment(fragment);
        attribute.setDescription(description);
        switch (format) {
            case URI_FORMAT:
                attribute.setType(PropertyType.URL);
                break;
            case DATETIME_FORMAT:
                attribute.setType(PropertyType.DATE_ISO8601);
                break;
            case DEFAULT_FORMAT:
            default:
                attribute.setType(PropertyType.STRING);
        }
        attribute.setUnit(unit);
        attribute.setLabel(label);
        jsonSchemaAttributes.add(attribute);
    }

    private void createBooleanAttribute(String name, String path, JsonNode node,
            List<AttributeModel> jsonSchemaAttributes) {
        String description = Optional.ofNullable(node.get(DESCRIPTION)).map(JsonNode::asText).orElse(null);
        String unit = Optional.ofNullable(node.get(UNIT)).map(JsonNode::asText).orElse(null);
        String label = Optional.ofNullable(node.get(TITLE)).map(JsonNode::asText).orElse(name);
        AttributeModel attribute = new AttributeModel();
        attribute.setName(name);
        attribute.setJsonPath(String.format("%s.%s", path, name));
        Fragment fragment = new Fragment();
        fragment.setName(path);
        attribute.setFragment(fragment);
        attribute.setDescription(description);
        attribute.setType(PropertyType.BOOLEAN);
        attribute.setUnit(unit);
        attribute.setLabel(label);
        jsonSchemaAttributes.add(attribute);
    }

    private void createIntegerAttribute(String name, String path, JsonNode node,
            List<AttributeModel> jsonSchemaAttributes) {
        String description = Optional.ofNullable(node.get(DESCRIPTION)).map(JsonNode::asText).orElse(null);
        String unit = Optional.ofNullable(node.get(UNIT)).map(JsonNode::asText).orElse(null);
        String label = Optional.ofNullable(node.get(TITLE)).map(JsonNode::asText).orElse(name);
        AttributeModel attribute = new AttributeModel();
        attribute.setName(name);
        attribute.setJsonPath(String.format("%s.%s", path, name));
        Fragment fragment = new Fragment();
        fragment.setName(path);
        attribute.setFragment(fragment);
        attribute.setDescription(description);
        attribute.setType(PropertyType.INTEGER);
        attribute.setUnit(unit);
        attribute.setLabel(label);
        jsonSchemaAttributes.add(attribute);
    }

    private void createDoubleAttribute(String name, String path, JsonNode node,
            List<AttributeModel> jsonSchemaAttributes) {
        String description = Optional.ofNullable(node.get(DESCRIPTION)).map(JsonNode::asText).orElse(null);
        String unit = Optional.ofNullable(node.get(UNIT)).map(JsonNode::asText).orElse(null);
        String label = Optional.ofNullable(node.get(TITLE)).map(JsonNode::asText).orElse(name);
        AttributeModel attribute = new AttributeModel();
        attribute.setName(name);
        attribute.setJsonPath(String.format("%s.%s", path, name));
        Fragment fragment = new Fragment();
        fragment.setName(path);
        attribute.setFragment(fragment);
        attribute.setDescription(description);
        attribute.setType(PropertyType.DOUBLE);
        attribute.setUnit(unit);
        attribute.setLabel(label);
        jsonSchemaAttributes.add(attribute);
    }

    protected abstract List<AttributeModel> doGetAllAttributes(String tenant);

}
