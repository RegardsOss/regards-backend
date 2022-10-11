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
package fr.cnes.regards.modules.model.service.validation.validator.common.restriction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import fr.cnes.regards.modules.model.domain.attributes.restriction.JsonSchemaRestriction;
import fr.cnes.regards.modules.model.service.validation.validator.common.AbstractValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;

import java.io.IOException;

/**
 * Validates à Json object against a JsonSchema using {@link JsonSchemaRestriction}
 *
 * @author Thibaud Michaudel
 **/
public abstract class AbstractJsonSchemaValidator extends AbstractValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJsonSchemaValidator.class);

    public static final String ERROR_VALUE_NOT_CONFORM_TO_JSON_SCHEMA = "error.value.not.conform.to.json.schema";

    private final ObjectMapper mapper = new ObjectMapper();

    private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    /**
     * Configured restriction
     */
    private final JsonSchemaRestriction restriction;

    public AbstractJsonSchemaValidator(JsonSchemaRestriction pRestriction, String pAttributeKey) {
        super(pAttributeKey);
        this.restriction = pRestriction;
    }

    protected abstract String getJsonValue(Object target);

    protected abstract boolean isJson(Class<?> clazz);

    @Override
    public void validate(Object target, Errors errors) {
        if (isJson(target.getClass())) {
            validate(getJsonValue(target), errors);
        } else {
            rejectUnsupported(errors);
        }
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return isJson(clazz);
    }

    public void validate(String jsonString, Errors errors) {
        try {
            getJsonSchema().validate(getJsonNode(jsonString)).forEach(e -> {
                errors.reject(ERROR_VALUE_NOT_CONFORM_TO_JSON_SCHEMA,
                              String.format("Attribute %s.%s not valid with jsonSchema. Cause : %s",
                                            attributeKey,
                                            e.getPath(),
                                            e.getMessage()));
            });
        } catch (JsonSchemaException e) {
            LOGGER.error(e.getMessage(), e);
            errors.reject(ERROR_VALUE_NOT_CONFORM_TO_JSON_SCHEMA,
                          String.format("Json schema is not valid. Cause : %s", e.getMessage()));
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            errors.rejectValue(ERROR_VALUE_NOT_CONFORM_TO_JSON_SCHEMA,
                               String.format("Attribute %s not valid with given json schema", attributeKey));
        }
    }

    protected JsonSchema getJsonSchema() throws JsonSchemaException {
        return factory.getSchema(this.restriction.getJsonSchema());
    }

    protected JsonNode getJsonNode(String content) throws IOException {
        return mapper.readTree(content);
    }

}
