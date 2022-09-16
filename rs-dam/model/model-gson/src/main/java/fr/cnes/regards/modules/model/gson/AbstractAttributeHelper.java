/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.Fragment;
import fr.cnes.regards.modules.model.domain.attributes.restriction.JsonSchemaRestriction;
import fr.cnes.regards.modules.model.domain.attributes.restriction.RestrictionType;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Abstract class to wrap retrieve of attributes to generate virtual attributes.
 * Virtual attributes are attributes computed from {@link PropertyType#JSON} attributes.
 *
 * @author Sébastien Binda
 */
public abstract class AbstractAttributeHelper implements IAttributeHelper {

    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractAttributeHelper.class);

    public static final String S_S = "%s.%s";

    private static ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<AttributeModel> getAllAttributes() {
        List<AttributeModel> attributes = doGetAllAttributes();
        return computeAttributes(attributes);
    }

    /**
     * Retrieve all {@link AttributeModel}s
     *
     * @return {@link AttributeModel}s
     */
    protected abstract List<AttributeModel> doGetAllAttributes();

    /**
     * Compute attributes generated from {@link PropertyType#JSON} attributes.
     *
     * @param attributes : {@link AttributeModel}s to compute
     * @return {@link AttributeModel}s computed attributes
     */
    public static List<AttributeModel> computeAttributes(List<AttributeModel> attributes) {
        List<AttributeModel> jsonSchemaAttributes = Lists.newArrayList();
        attributes.stream()
                  .filter(a -> (a.getType() == PropertyType.JSON) && a.hasRestriction() && (a.getRestriction().getType()
                                                                                            == RestrictionType.JSON_SCHEMA))
                  .forEach(a -> jsonSchemaAttributes.addAll(fromJsonSchema(a.getJsonPropertyPath(),
                                                                           ((JsonSchemaRestriction) a.getRestriction()).getJsonSchema(),
                                                                           a.isIndexed(),
                                                                           new ArrayList<>(((JsonSchemaRestriction) a.getRestriction()).getIndexableFields()))));
        attributes.addAll(jsonSchemaAttributes);
        attributes.forEach(a -> LOGGER.debug("Attribute found : {} - {} / {}",
                                             a.getName(),
                                             a.getFullJsonPath(),
                                             Optional.ofNullable(a.getFragment()).map(f -> f.getName()).orElse("-")));
        return attributes;
    }

    /**
     * Compute {@link AttributeModel}s fro the given json schema
     *
     * @param attributePath root path of the {@link PropertyType#JSON} {@link AttributeModel}
     * @param schema        json schema to read
     * @return computed {@link AttributeModel}s
     */
    public static List<AttributeModel> fromJsonSchema(String attributePath,
                                                      String schema,
                                                      boolean isIndexed,
                                                      List<String> indexableFields) {
        List<AttributeModel> jsonSchemaAttributes = Lists.newArrayList();
        try {
            JsonNode root = mapper.readTree(schema);
            int idx = attributePath.lastIndexOf(".");
            if (idx > 0) {
                createAttributes(attributePath.substring(idx + 1),
                                 attributePath.substring(0, idx),
                                 root,
                                 isIndexed,
                                 indexableFields,
                                 jsonSchemaAttributes);
            } else {
                createAttributes(attributePath, null, root, isIndexed, indexableFields, jsonSchemaAttributes);
            }

        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return jsonSchemaAttributes;
    }

    /**
     * Creates {@link AttributeModel}s from the {@link JsonNode} of the Json schema
     *
     * @param name                 name of the attribute represented by the given {@link JsonNode}
     * @param path                 path of the attribute represented by the given {@link JsonNode}
     * @param node                 {@link JsonNode} to compute
     * @param jsonSchemaAttributes {@link AttributeModel} list to add computed ones
     */
    private static void createAttributes(String name,
                                         String path,
                                         JsonNode node,
                                         boolean isIndexed,
                                         List<String> indexableFields,
                                         List<AttributeModel> jsonSchemaAttributes) {
        String attributeType = node.get(JsonSchemaConstants.TYPE).textValue();
        switch (attributeType) {
            case JsonSchemaConstants.OBJECT_TYPE:
                createObjectAttributes(name, path, node, isIndexed, indexableFields, jsonSchemaAttributes);
                break;
            case JsonSchemaConstants.STRING_TYPE:
                createStringAttribute(name,
                                      path,
                                      node,
                                      shouldJsonAttributeBeIndexed(isIndexed, path, name, indexableFields),
                                      jsonSchemaAttributes);
                break;
            case JsonSchemaConstants.INTEGER_TYPE:
                createIntegerAttribute(name,
                                       path,
                                       node,
                                       shouldJsonAttributeBeIndexed(isIndexed, path, name, indexableFields),
                                       jsonSchemaAttributes);
                break;
            case JsonSchemaConstants.NUMBER_TYPE:
                createDoubleAttribute(name,
                                      path,
                                      node,
                                      shouldJsonAttributeBeIndexed(isIndexed, path, name, indexableFields),
                                      jsonSchemaAttributes);
                break;
            case JsonSchemaConstants.BOOLEAN_TYPE:
                createBooleanAttribute(name,
                                       path,
                                       node,
                                       shouldJsonAttributeBeIndexed(isIndexed, path, name, indexableFields),
                                       jsonSchemaAttributes);
                break;
            case JsonSchemaConstants.ARRAY_TYPE:
                createArrayAttributes(name, path, node, isIndexed, indexableFields, jsonSchemaAttributes);
                break;
            default:
                throw new IllegalArgumentException(String.format("Invalid type %s for attribute %s",
                                                                 attributeType,
                                                                 name));
        }
    }

    /**
     * Creates attributes from the object type given {@link JsonNode}
     *
     * @param name                 name of the attribute represented by the given {@link JsonNode}
     * @param path                 path of the attribute represented by the given {@link JsonNode}
     * @param node                 {@link JsonNode} to compute
     * @param parentIsIndexed      the indexation status of the non json parent attribute
     * @param indexableAttributes  the list of attributes path that should be indexed
     * @param jsonSchemaAttributes {@link AttributeModel} list to add computed ones
     */
    private static void createObjectAttributes(String name,
                                               String path,
                                               JsonNode node,
                                               boolean parentIsIndexed,
                                               List<String> indexableAttributes,
                                               List<AttributeModel> jsonSchemaAttributes) {
        Optional.ofNullable(node.get(JsonSchemaConstants.PROPERTIES)).ifPresent(properties -> {
            Iterator<Entry<String, JsonNode>> it = properties.fields();
            do {
                Entry<String, JsonNode> field = it.next();
                String fieldPath = Optional.ofNullable(path).map(p -> String.format(S_S, p, name)).orElse(name);
                createAttributes(field.getKey(),
                                 fieldPath,
                                 field.getValue(),
                                 parentIsIndexed,
                                 indexableAttributes,
                                 jsonSchemaAttributes);
            } while (it.hasNext());
        });
    }

    /**
     * Creates attributes from the array type given {@link JsonNode}
     *
     * @param name                 name of the attribute represented by the given {@link JsonNode}
     * @param path                 path of the attribute represented by the given {@link JsonNode}
     * @param node                 {@link JsonNode} to compute
     * @param parentIsIndexed      the indexation status of the non json parent attribute
     * @param indexableAttributes  the list of attributes path that should be indexed
     * @param jsonSchemaAttributes {@link AttributeModel} list to add computed ones
     */
    private static void createArrayAttributes(String name,
                                              String path,
                                              JsonNode node,
                                              boolean parentIsIndexed,
                                              List<String> indexableAttributes,
                                              List<AttributeModel> jsonSchemaAttributes) {
        Optional.ofNullable(node.get(JsonSchemaConstants.ITEMS)).ifPresent(items -> {
            createAttributes(name, path, items, parentIsIndexed, indexableAttributes, jsonSchemaAttributes);
        });
    }

    /**
     * Creates attributes from the String type given {@link JsonNode}
     *
     * @param name                 name of the attribute represented by the given {@link JsonNode}
     * @param path                 path of the attribute represented by the given {@link JsonNode}
     * @param node                 {@link JsonNode} to compute
     * @param isIndexed            whether the attribute should be indexed
     * @param jsonSchemaAttributes {@link AttributeModel} list to add computed ones
     */
    private static void createStringAttribute(String name,
                                              String path,
                                              JsonNode node,
                                              boolean isIndexed,
                                              List<AttributeModel> jsonSchemaAttributes) {
        String description = Optional.ofNullable(node.get(JsonSchemaConstants.DESCRIPTION))
                                     .map(JsonNode::asText)
                                     .orElse(null);
        String unit = Optional.ofNullable(node.get(JsonSchemaConstants.UNIT)).map(JsonNode::asText).orElse(null);
        String label = Optional.ofNullable(node.get(JsonSchemaConstants.TITLE)).map(JsonNode::asText).orElse(name);
        String format = Optional.ofNullable(node.get(JsonSchemaConstants.FORMAT))
                                .map(JsonNode::asText)
                                .orElse(JsonSchemaConstants.DEFAULT_FORMAT);
        AttributeModel attribute = new AttributeModel();
        attribute.setName(name);
        attribute.setJsonPath(Optional.ofNullable(path).map(p -> String.format(S_S, p, name)).orElse(name));
        Fragment fragment = new Fragment();
        fragment.setVirtual(true);
        fragment.setName(path);
        attribute.setFragment(fragment);
        attribute.setDescription(description);
        attribute.setVirtual(true);
        attribute.setIndexed(isIndexed);
        switch (format) {
            case JsonSchemaConstants.URI_FORMAT:
                attribute.setType(PropertyType.URL);
                break;
            case JsonSchemaConstants.DATETIME_FORMAT:
                attribute.setType(PropertyType.DATE_ISO8601);
                break;
            case JsonSchemaConstants.DEFAULT_FORMAT:
            default:
                attribute.setType(PropertyType.STRING);
        }
        attribute.setUnit(unit);
        attribute.setLabel(label);
        jsonSchemaAttributes.add(attribute);
    }

    /**
     * Creates attributes from the Boolean type given {@link JsonNode}
     *
     * @param name                 name of the attribute represented by the given {@link JsonNode}
     * @param path                 path of the attribute represented by the given {@link JsonNode}
     * @param node                 {@link JsonNode} to compute
     * @param isIndexed            whether the attribute should be indexed
     * @param jsonSchemaAttributes {@link AttributeModel} list to add computed ones
     */
    private static void createBooleanAttribute(String name,
                                               String path,
                                               JsonNode node,
                                               boolean isIndexed,
                                               List<AttributeModel> jsonSchemaAttributes) {
        String description = Optional.ofNullable(node.get(JsonSchemaConstants.DESCRIPTION))
                                     .map(JsonNode::asText)
                                     .orElse(null);
        String unit = Optional.ofNullable(node.get(JsonSchemaConstants.UNIT)).map(JsonNode::asText).orElse(null);
        String label = Optional.ofNullable(node.get(JsonSchemaConstants.TITLE)).map(JsonNode::asText).orElse(name);
        AttributeModel attribute = new AttributeModel();
        attribute.setName(name);
        attribute.setJsonPath(String.format(S_S, path, name));
        Fragment fragment = new Fragment();
        fragment.setName(path);
        fragment.setVirtual(true);
        attribute.setFragment(fragment);
        attribute.setDescription(description);
        attribute.setType(PropertyType.BOOLEAN);
        attribute.setUnit(unit);
        attribute.setLabel(label);
        attribute.setVirtual(true);
        attribute.setIndexed(isIndexed);
        jsonSchemaAttributes.add(attribute);
    }

    /**
     * Creates attributes from the Integer type given {@link JsonNode}
     *
     * @param name                 name of the attribute represented by the given {@link JsonNode}
     * @param path                 path of the attribute represented by the given {@link JsonNode}
     * @param node                 {@link JsonNode} to compute
     * @param isIndexed            whether the attribute should be indexed
     * @param jsonSchemaAttributes {@link AttributeModel} list to add computed ones
     */
    private static void createIntegerAttribute(String name,
                                               String path,
                                               JsonNode node,
                                               boolean isIndexed,
                                               List<AttributeModel> jsonSchemaAttributes) {
        String description = Optional.ofNullable(node.get(JsonSchemaConstants.DESCRIPTION))
                                     .map(JsonNode::asText)
                                     .orElse(null);
        String unit = Optional.ofNullable(node.get(JsonSchemaConstants.UNIT)).map(JsonNode::asText).orElse(null);
        String label = Optional.ofNullable(node.get(JsonSchemaConstants.TITLE)).map(JsonNode::asText).orElse(name);
        AttributeModel attribute = new AttributeModel();
        attribute.setName(name);
        attribute.setJsonPath(String.format(S_S, path, name));
        Fragment fragment = new Fragment();
        fragment.setName(path);
        fragment.setVirtual(true);
        attribute.setFragment(fragment);
        attribute.setDescription(description);
        attribute.setType(PropertyType.INTEGER);
        attribute.setUnit(unit);
        attribute.setLabel(label);
        attribute.setVirtual(true);
        attribute.setIndexed(isIndexed);
        jsonSchemaAttributes.add(attribute);
    }

    /**
     * Creates attributes from the Numerical type given {@link JsonNode}
     *
     * @param name                 name of the attribute represented by the given {@link JsonNode}
     * @param path                 path of the attribute represented by the given {@link JsonNode}
     * @param node                 {@link JsonNode} to compute
     * @param isIndexed            whether the attribute should be indexed
     * @param jsonSchemaAttributes {@link AttributeModel} list to add computed ones
     */
    private static void createDoubleAttribute(String name,
                                              String path,
                                              JsonNode node,
                                              boolean isIndexed,
                                              List<AttributeModel> jsonSchemaAttributes) {
        String description = Optional.ofNullable(node.get(JsonSchemaConstants.DESCRIPTION))
                                     .map(JsonNode::asText)
                                     .orElse(null);
        String unit = Optional.ofNullable(node.get(JsonSchemaConstants.UNIT)).map(JsonNode::asText).orElse(null);
        String label = Optional.ofNullable(node.get(JsonSchemaConstants.TITLE)).map(JsonNode::asText).orElse(name);
        AttributeModel attribute = new AttributeModel();
        attribute.setName(name);
        attribute.setJsonPath(String.format(S_S, path, name));
        Fragment fragment = new Fragment();
        fragment.setName(path);
        fragment.setVirtual(true);
        attribute.setFragment(fragment);
        attribute.setDescription(description);
        attribute.setType(PropertyType.DOUBLE);
        attribute.setUnit(unit);
        attribute.setLabel(label);
        attribute.setVirtual(true);
        attribute.setIndexed(isIndexed);
        jsonSchemaAttributes.add(attribute);
    }

    /**
     * Compute whether the json attribute should be indexed
     *
     * @param parentIsIndexed     the indexation status of the non json parent attribute
     * @param path                the json path of the attribute
     * @param name                the attribute name
     * @param indexableAttributes the list of attributes path that should be indexed
     * @return true if the json attribute should be indexed
     */
    private static boolean shouldJsonAttributeBeIndexed(boolean parentIsIndexed,
                                                        String path,
                                                        String name,
                                                        List<String> indexableAttributes) {
        if (!parentIsIndexed) {
            return false;
        }
        if (indexableAttributes.isEmpty()) {
            return true;
        }
        return indexableAttributes.contains(path + "." + name);
    }

}
