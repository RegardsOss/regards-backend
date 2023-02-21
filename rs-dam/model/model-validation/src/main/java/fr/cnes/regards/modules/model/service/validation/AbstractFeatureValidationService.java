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
package fr.cnes.regards.modules.model.service.validation;

import fr.cnes.regards.framework.geojson.AbstractFeature;
import fr.cnes.regards.modules.model.domain.ComputationMode;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.service.validation.validator.iproperty.ComputationModePropertyValidator;
import fr.cnes.regards.modules.model.service.validation.validator.iproperty.PropertyTypeValidator;
import fr.cnes.regards.modules.model.service.validation.validator.iproperty.restriction.RestrictionValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.*;

/**
 * Override this class to validate feature properties
 *
 * @author oroussel
 * @author Marc SORDI
 */
public abstract class AbstractFeatureValidationService<F extends AbstractFeature<Set<IProperty<?>>, ?>>
    extends AbstractValidationService implements IValidationService<F> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureValidationService.class);

    public AbstractFeatureValidationService(IModelFinder modelFinder) {
        super(modelFinder);
    }

    @Override
    public Errors validate(String model, F feature, ValidationMode mode, String objectName) {

        Errors errors = new MapBindingResult(new HashMap<>(), objectName);

        // Retrieve attributes from model
        List<ModelAttrAssoc> modAtts = modelFinder.findByModel(model);

        // if the model doesn't exists
        if (modAtts == null) {
            errors.reject("error.unknown.model.message", String.format("Unknown model %s", model));
            return errors;
        }
        // Build fast property access map
        Map<String, IProperty<?>> pptyMap = IProperty.getPropertyMap(feature.getProperties());
        // Get a copy of entity attributes values to optimize the search of unexpected properties
        // FIXME check it's a real copy!
        Set<String> toCheckProperties = new HashSet<>(pptyMap.keySet());

        // Loop over model attributes ... to validate each properties
        for (ModelAttrAssoc modelAttrAssoc : modAtts) {
            errors.addAllErrors(checkModelAttribute(modelAttrAssoc,
                                                    objectName,
                                                    mode,
                                                    feature,
                                                    pptyMap,
                                                    toCheckProperties));
        }

        // If properties isn't empty it means some properties are unexpected by the model
        if (!toCheckProperties.isEmpty()) {
            toCheckProperties.forEach(propPath -> errors.reject("error.unexpected.property.message",
                                                                String.format("%s isn't expected by the model %s",
                                                                              propPath,
                                                                              model)));
        }

        return errors;
    }

    /**
     * Validate a property according to its corresponding model attribute
     *
     * @param modelAttrAssoc    model attribute
     * @param objectName        name of the object to validate
     * @param pptyMap           properties to check
     * @param toCheckProperties properties not already checked
     * @return validation errors
     */
    protected Errors checkModelAttribute(ModelAttrAssoc modelAttrAssoc,
                                         String objectName,
                                         ValidationMode mode,
                                         F feature,
                                         Map<String, IProperty<?>> pptyMap,
                                         Set<String> toCheckProperties) {

        Errors errors = new MapBindingResult(new HashMap<>(), objectName);

        AttributeModel attModel = modelAttrAssoc.getAttribute();
        String attPath = attModel.getJsonPropertyPath();

        // Only validate attribute that have a ComputationMode of GIVEN. Otherwise the attribute value will most likely
        // be missing and is added during the crawling process
        if (modelAttrAssoc.getMode() == ComputationMode.GIVEN) {
            LOGGER.debug("Computed key : \"{}\"", attPath);

            // Retrieve property
            IProperty<?> att = pptyMap.get(attPath);

            // Null property check
            if (att == null) {
                checkNullProperty(attModel, errors, mode);
            } else {

                // Null property value check
                if (att.getValue() == null) {
                    checkNullPropertyValue(attModel, errors, mode);
                } else {

                    // Check if value is expected or not according to the validation context
                    checkAuthorizedPropertyValue(attModel, errors, mode);

                    if (!errors.hasErrors()) {

                        doValidation(errors, modelAttrAssoc, mode, feature, attPath, att);
                    }
                }
            }
        }
        // Ok, attribute has been checked or is a computed one
        toCheckProperties.remove(attPath);
        return errors;
    }

    private void doValidation(Errors errors,
                              ModelAttrAssoc modelAttrAssoc,
                              ValidationMode mode,
                              F feature,
                              String attPath,
                              IProperty<?> att) {
        // Do validation
        for (Validator validator : getValidators(modelAttrAssoc, attPath, mode, feature)) {
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

    /**
     * Get validators
     */
    protected List<Validator> getValidators(ModelAttrAssoc modelAttrAssoc,
                                            String attributeKey,
                                            ValidationMode mode,
                                            F feature) {
        AttributeModel attModel = modelAttrAssoc.getAttribute();

        List<Validator> validators = new ArrayList<>();
        // Check computation mode
        validators.add(new ComputationModePropertyValidator(modelAttrAssoc.getMode(), attributeKey));
        // Check attribute type
        validators.add(new PropertyTypeValidator(attModel.getType(), attributeKey));
        // Check restriction
        if (attModel.hasRestriction()) {
            validators.add(RestrictionValidatorFactory.getValidator(attModel.getRestriction(), attributeKey));
        }
        return validators;
    }

}
