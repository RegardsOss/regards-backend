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

package fr.cnes.regards.modules.ingest.service;

import fr.cnes.regards.modules.model.domain.ComputationMode;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.service.validation.AbstractValidationService;
import fr.cnes.regards.modules.model.service.validation.IModelFinder;
import fr.cnes.regards.modules.model.service.validation.ValidationMode;
import fr.cnes.regards.modules.model.service.validation.validator.object.ComputationModeObjectValidator;
import fr.cnes.regards.modules.model.service.validation.validator.object.ObjectTypeValidator;
import fr.cnes.regards.modules.model.service.validation.validator.object.restriction.RestrictionValidatorObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.*;

/**
 * Service for validating Json like objects (Map<String, Object>) against a model
 *
 * @author Thibaud Michaudel
 **/
@Service
public class IngestValidationService extends AbstractValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestValidationService.class);

    public IngestValidationService(IModelFinder modelFinder) {
        super(modelFinder);
    }

    public Errors validate(String model, Map<String, Object> descriptiveInformation, String objectName) {

        Errors errors = new MapBindingResult(new HashMap<>(), objectName);

        // Retrieve attributes from model
        List<ModelAttrAssoc> modAtts = modelFinder.findByModel(model);

        // if the model doesn't exists
        if (modAtts == null) {
            errors.reject("error.unknown.model.message", String.format("Unknown model %s", model));
            return errors;
        }
        // Get a copy of entity attributes values to optimize the search of unexpected properties
        Set<String> toCheckProperties = new HashSet<>(descriptiveInformation.keySet());

        // Loop over model attributes ... to validate each properties
        for (ModelAttrAssoc modelAttrAssoc : modAtts) {
            errors.addAllErrors(checkModelAttribute(modelAttrAssoc,
                                                    objectName,
                                                    ValidationMode.CREATION,
                                                    descriptiveInformation,
                                                    toCheckProperties));
        }

        // If properties isn't empty it means some properties are unexpected by the model
        toCheckProperties.forEach(propPath -> errors.reject("error.unexpected.property.message",
                                                            String.format("%s isn't expected by the model %s",
                                                                          propPath,
                                                                          model)));

        return errors;
    }

    protected Errors checkModelAttribute(ModelAttrAssoc modelAttrAssoc,
                                         String objectName,
                                         ValidationMode mode,
                                         Map<String, Object> pptyMap,
                                         Set<String> toCheckProperties) {

        Errors errors = new MapBindingResult(new HashMap<>(), objectName);

        AttributeModel attModel = modelAttrAssoc.getAttribute();
        String attPath = attModel.getJsonPropertyPath();

        // Only validate attribute that have a ComputationMode of GIVEN. Otherwise the attribute value will most likely
        // be missing and is added during the crawling process
        if (modelAttrAssoc.getMode() == ComputationMode.GIVEN) {
            LOGGER.debug("Computed key : \"{}\"", attPath);

            // Retrieve property
            Object att;
            // Special case for fragments
            if (attPath.contains(".")) {
                String[] split = attPath.split("\\.");
                String frag = split[0];
                String attributeInFrag = split[1];
                // Check that there is no sub fragment
                if (attributeInFrag.contains(".")) {
                    throw new UnsupportedOperationException("Fragments cannot contains other fragments");
                }
                Map<String, Object> fragMap = (Map) pptyMap.get(frag);
                att = fragMap.get(attributeInFrag);
                // If this is the first time this fragment is validated,
                // remove it as it is not a real attribute and replace it by its children attributes
                if (toCheckProperties.contains(frag)) {
                    toCheckProperties.remove(frag);
                    fragMap.forEach((key, value) -> toCheckProperties.add(frag + "." + key));
                }
            } else {
                att = pptyMap.get(attPath);
            }

            // Null property check
            if (att == null) {
                checkNullProperty(attModel, errors, mode);
            } else {
                // Check if value is expected or not according to the validation context
                checkAuthorizedPropertyValue(attModel, errors, mode);

                if (!errors.hasErrors()) {

                    // Do validation
                    for (Validator validator : getValidators(modelAttrAssoc, attPath)) {
                        if (validator.supports(att.getClass())) {
                            validator.validate(att, errors);
                        } else {
                            errors.reject("error.unsupported.validator.message",
                                          String.format("Unsupported validator \"%s\" for property \"%s\"",
                                                        validator.getClass().getName(),
                                                        attPath));
                        }
                    }
                }
            }
        }
        // Ok, attribute has been checked or is a computed one
        toCheckProperties.remove(attPath);
        return errors;
    }

    protected List<Validator> getValidators(ModelAttrAssoc modelAttrAssoc, String attributeKey) {
        AttributeModel attModel = modelAttrAssoc.getAttribute();

        List<Validator> validators = new ArrayList<>();
        // Check computation mode
        validators.add(new ComputationModeObjectValidator(modelAttrAssoc.getMode(), attributeKey));
        // Check attribute type
        validators.add(new ObjectTypeValidator(attModel.getType(), attributeKey));
        // Check restriction
        if (attModel.hasRestriction()) {
            validators.add(RestrictionValidatorObjectFactory.getValidator(attModel.getRestriction(), attributeKey));
        }
        return validators;
    }
}
