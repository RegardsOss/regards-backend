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
package fr.cnes.regards.modules.model.service.validation.validator.iproperty.restriction;

import fr.cnes.regards.modules.model.domain.attributes.restriction.JsonSchemaRestriction;
import fr.cnes.regards.modules.model.dto.properties.JsonProperty;
import fr.cnes.regards.modules.model.service.validation.validator.common.restriction.AbstractJsonSchemaValidator;

/**
 * Validates a string property against a json schema
 *
 * @author Sébastien Binda
 */
public class JsonSchemaPropertyValidator extends AbstractJsonSchemaValidator {

    public JsonSchemaPropertyValidator(JsonSchemaRestriction restriction, String attributeKey) {
        super(restriction, attributeKey);
    }

    @Override
    protected String getJsonValue(Object target) {
        return ((JsonProperty) target).getValue().toString();
    }

    @Override
    protected boolean isJson(Class<?> clazz) {
        return clazz == JsonProperty.class;
    }

}
