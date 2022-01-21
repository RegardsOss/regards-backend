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
package fr.cnes.regards.modules.model.domain.attributes.restriction.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;

import fr.cnes.regards.modules.model.domain.attributes.restriction.JsonSchemaRestriction;

/**
 * Check if given json schema is valid
 *
 * @author Sébastien Binda
 *
 */
public class CheckJsonSchemaValidator implements ConstraintValidator<CheckJsonSchema, JsonSchemaRestriction> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckJsonSchemaValidator.class);

    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    @Override
    public boolean isValid(JsonSchemaRestriction value, ConstraintValidatorContext context) {
        try {
            factory.getSchema(value.getJsonSchema());
            return true;
        } catch (JsonSchemaException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

}
