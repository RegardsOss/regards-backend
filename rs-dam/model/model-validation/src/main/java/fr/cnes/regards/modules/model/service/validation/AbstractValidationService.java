/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.modules.model.service.validation;

import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;

/**
 * Service for validation against a model
 *
 * @author Thibaud Michaudel
 **/
public abstract class AbstractValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractValidationService.class);

    protected final IModelFinder modelFinder;

    public AbstractValidationService(IModelFinder modelFinder) {
        this.modelFinder = modelFinder;
    }

    protected void checkNullProperty(AttributeModel attModel, Errors errors, ValidationMode mode) {
        if (!ValidationMode.PATCH.equals(mode)) { // In PATCH mode, all properties can be null
            if (!attModel.isOptional()) {
                errors.reject("error.missing.required.property.message",
                              String.format("Missing required property \"%s\".", attModel.getJsonPropertyPath()));
                return;
            }
        }
        LOGGER.debug("Property \"{}\" is optional in {} context.", attModel.getJsonPropertyPath(), mode);
    }

    protected void checkNullPropertyValue(AttributeModel attModel, Errors errors, ValidationMode mode) {
        if (ValidationMode.PATCH == mode || ValidationMode.UPDATE == mode) {
            // In PATCH mode, null value is used to unset a property
            if (!attModel.isAlterable()) {
                errors.reject("error.unset.non.alterable.property.message",
                              String.format("Unalterable property \"%s\" cannot be unset.",
                                            attModel.getJsonPropertyPath()));
            } else {
                LOGGER.debug("Property \"{}\" will be unset in {} context.", attModel.getJsonPropertyPath(), mode);
            }
        }
    }

    protected void checkAuthorizedPropertyValue(AttributeModel attModel, Errors errors, ValidationMode mode) {
        if (ValidationMode.PATCH.equals(mode)) {
            if (!attModel.isAlterable()) {
                errors.reject("error.patch.non.alterable.property.message",
                              String.format("Unalterable property \"%s\" must not be set in patch request.",
                                            attModel.getJsonPropertyPath()));
            } else {
                LOGGER.debug("Property \"{}\" will be updated in {} context.", attModel.getJsonPropertyPath(), mode);
            }
        }
    }
}
