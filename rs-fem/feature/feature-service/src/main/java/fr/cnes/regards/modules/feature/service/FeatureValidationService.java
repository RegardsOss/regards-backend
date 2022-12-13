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
package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureFile;
import fr.cnes.regards.modules.feature.dto.FeatureFileLocation;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.model.service.validation.AbstractFeatureValidationService;
import fr.cnes.regards.modules.model.service.validation.IModelFinder;
import fr.cnes.regards.modules.model.service.validation.IValidationService;
import fr.cnes.regards.modules.model.service.validation.ValidationMode;
import fr.cnes.regards.modules.storage.domain.flow.ReferenceFlowItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Validate incoming features
 *
 * @author Marc SORDI
 * @author Sébastien Binda
 */
@Service
public class FeatureValidationService extends AbstractFeatureValidationService<Feature>
    implements IFeatureValidationService, IValidationService<Feature> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureValidationService.class);

    private static final String FEATURE = "self";

    private static final String URN_FIELD = "urn";

    private static final String ID_FIELD = "id";

    private static final String FILES_FIELD = "files";

    private static final Integer ID_LENGTH = 100;

    private static final String FILES_STORAGE_ERROR_CODE = "feature.files.storage.unsupported";

    /**
     * Standard validator based on annotation
     */
    private final Validator validator;

    public FeatureValidationService(IModelFinder modelFinder, Validator validator) {
        super(modelFinder);
        this.validator = validator;
    }

    @Override
    public Errors validate(Feature feature, ValidationMode mode) {

        String objectName = Feature.class.getName();
        Errors errors = new MapBindingResult(new HashMap<>(), objectName);

        if (feature == null) {
            // Error might be detected before and so might be reported twice
            errors.rejectValue(FEATURE, "feature.null.error.message", "Feature must not be null");
            return errors;
        }

        // Validate feature
        validator.validate(feature, errors);

        // Validate feature files to ensure unique checksums
        List<String> checksums = new ArrayList<>();
        if (feature.getFiles() != null) {
            feature.getFiles().forEach(file -> {
                if (checksums.contains(file.getAttributes().getChecksum())) {
                    errors.rejectValue("files",
                                       "request.feature.files.invalid.message",
                                       "Feature can not contains many files with the same checksum");
                } else {
                    checksums.add(file.getAttributes().getChecksum());
                }
            });
        }

        String featureId = feature.getId();
        FeatureUniformResourceName urn = feature.getUrn();

        if ((featureId == null) && (mode != ValidationMode.PATCH)) {
            errors.rejectValue(ID_FIELD, "feature.id.null.error.message", "Feature id must not be null");
        } else {
            if ((featureId != null) && (featureId.length() > ID_LENGTH)) {
                errors.rejectValue(ID_FIELD,
                                   "feature.id.length.error.message",
                                   String.format("Feature id must not exceed %s characters", ID_LENGTH));
            }
        }

        // Programmatic validation according to the context
        switch (mode) {
            case CREATION:
                if (urn != null) {
                    validator.validate(urn, errors);
                }
                break;
            case UPDATE:
            case PATCH:
                if (urn == null) {
                    errors.rejectValue(URN_FIELD,
                                       "feature.urn.required.error.message",
                                       "URN is required in feature update");
                }
                break;
            default:
                break;
        }

        // Try validating properties according to data model
        if ((feature.getModel() != null) && (feature.getProperties()
                                             != null)) { // If model is null, error already detected before!
            errors.addAllErrors(validate(feature.getModel(), feature, mode, objectName));
        }

        validateFiles(feature, errors);

        if (errors.hasErrors()) {
            LOGGER.error("Error validating feature \"{}\" : {}", featureId, ErrorTranslator.getErrorsAsString(errors));
        }

        return errors;
    }

    /**
     * Validate {@link Feature} files
     *
     * @param feature {@link Feature} to valid files
     * @param errors  {@link Errors} in which add new errors if any
     */
    private void validateFiles(Feature feature, Errors errors) {
        Long numberOfFilesToStore = 0L;
        Long numberOfFilesToReference = 0L;

        if (!CollectionUtils.isEmpty(feature.getFiles())) {
            for (FeatureFile file : feature.getFiles()) {
                numberOfFilesToStore += file.getLocations().stream().filter(loc -> loc.getStorage() == null).count();
                numberOfFilesToReference += file.getLocations()
                                                .stream()
                                                .filter(loc -> loc.getStorage() != null)
                                                .count();
                file.getLocations().forEach(loc -> validateFileLocation(loc, errors));
            }
            if (numberOfFilesToStore > 0 && numberOfFilesToReference > 0) {
                String message = String.format("Feature creation can not handle both store and reference files. "
                                               + "Feature contains %s files to store and %s files to reference",
                                               numberOfFilesToStore,
                                               numberOfFilesToReference);
                errors.rejectValue(FILES_FIELD, FILES_STORAGE_ERROR_CODE, message);
            }

            if (numberOfFilesToStore > ReferenceFlowItem.MAX_REQUEST_PER_GROUP) {
                String message = String.format("Too many files to store for feature {}. Limit is {}.",
                                               numberOfFilesToStore,
                                               ReferenceFlowItem.MAX_REQUEST_PER_GROUP);
                errors.rejectValue(FILES_FIELD, FILES_STORAGE_ERROR_CODE, message);
            }

            if (numberOfFilesToReference > ReferenceFlowItem.MAX_REQUEST_PER_GROUP) {
                String message = String.format("Too many files to reference for feature {}. Limit is {}.",
                                               numberOfFilesToReference,
                                               ReferenceFlowItem.MAX_REQUEST_PER_GROUP);
                errors.rejectValue(FILES_FIELD, FILES_STORAGE_ERROR_CODE, message);
            }
        }
    }

    /**
     * Validate {@link FeatureFileLocation} of feature files
     *
     * @param loc
     * @param errors
     */
    public void validateFileLocation(FeatureFileLocation loc, Errors errors) {
        try {
            URL url = new URL(loc.getUrl());
        } catch (MalformedURLException e) {
            String errorMessage = String.format("Invalid URL %s cause : %s", loc.getUrl(), e.getMessage());
            LOGGER.error(errorMessage, e);
            errors.rejectValue("files.location.url", FILES_STORAGE_ERROR_CODE, errorMessage);
        }
    }

}
