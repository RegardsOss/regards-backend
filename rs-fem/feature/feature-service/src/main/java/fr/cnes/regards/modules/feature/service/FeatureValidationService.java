/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.service;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.model.service.validation.AbstractValidationService;
import fr.cnes.regards.modules.model.service.validation.IModelFinder;
import fr.cnes.regards.modules.model.service.validation.IValidationService;
import fr.cnes.regards.modules.model.service.validation.ValidationMode;

/**
 * Validate incoming features
 *
 * @author Marc SORDI
 *
 */
@Service
public class FeatureValidationService extends AbstractValidationService<Feature>
        implements IFeatureValidationService, IValidationService<Feature> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureValidationService.class);

    private static final String URN_FIELD = "urn";

    private static final String ID_FIELD = "id";

    private static final Integer ID_LENGTH = 100;

    /**
     * Standard validator based on annotation
     */
    @Autowired
    private Validator validator;

    public FeatureValidationService(IModelFinder modelFinder) {
        super(modelFinder);
    }

    @Override
    public Errors validate(Feature feature, ValidationMode mode) {

        String objectName = Feature.class.getName();
        Errors errors = new MapBindingResult(new HashMap<>(), objectName);

        // Validate feature
        validator.validate(feature, errors);

        if (feature.getId() == null) {
            errors.rejectValue(ID_FIELD, "feature.id.null.error.message", "Feature id mustn't be null");
        } else {
            if (feature.getId().length() > ID_LENGTH) {
                errors.rejectValue(ID_FIELD, "feature.id.length.error.message",
                                   String.format("Feature id must not exceed %s characters", ID_LENGTH));
            }
        }

        // Programmatic validation according to the context
        switch (mode) {
            case CREATION:
                if (feature.getUrn() != null) {
                    errors.rejectValue(URN_FIELD, "feature.urn.unexpected.error.message",
                                       "Unexpected URN in feature creation");
                }
                break;
            case UPDATE:
            case PATCH:
                if (feature.getUrn() == null) {
                    errors.rejectValue(URN_FIELD, "feature.urn.required.error.message",
                                       "URN is required in feature update");
                }
                break;
            default:
                break;
        }

        // Try validating properties according to data model
        if ((feature.getModel() != null) && (feature.getProperties() != null)) { // If model is null, error already detected before!
            errors.addAllErrors(validate(feature.getModel(), feature, mode, objectName));
        }

        if (errors.hasErrors()) {
            LOGGER.error("Error validating feature \"{}\" : {}", feature.getId(),
                         ErrorTranslator.getErrorsAsString(errors));
        }

        return errors;
    }
}
