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
package fr.cnes.regards.modules.ltamanager.service.settings.customizers;

import com.google.common.base.Joiner;
import com.google.gson.JsonSyntaxException;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.ltamanager.domain.settings.DatatypeParameter;
import fr.cnes.regards.modules.ltamanager.domain.settings.LtaSettings;
import fr.cnes.regards.modules.ltamanager.service.settings.LtaModelCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.HashMap;
import java.util.Map;

/**
 * Customizer for {@link LtaSettings#DATATYPES_KEY}
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class LtaDatatypesSettingCustomizer implements IDynamicTenantSettingCustomizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LtaDatatypesSettingCustomizer.class);

    private static final String ERROR_CODE_DATATYPE = "lta.manager.settings.invalid.datatype.key";

    private static final String ERROR_CODE_MODEL = "lta.manager.settings.model.not.found";

    private static final int LIMIT_CHARACTERS_LENGTH = 255;

    private final Validator validator;

    private final LtaModelCacheService modelCacheService;

    public LtaDatatypesSettingCustomizer(Validator validator, LtaModelCacheService modelCacheService) {
        this.validator = validator;
        this.modelCacheService = modelCacheService;
    }

    /**
     * Check if LtaSettings#DATATYPE is valid on 3 conditions :
     * <ul>
     *     <li>The datatype key contains only alphanumeric characters</li>
     *     <li>The datatype is associated to a valid {@link DatatypeParameter}</li>
     *     <li>The associated model exists</li>
     * </ul>
     *
     * @return if all conditions are verified
     */
    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        Map<String, Object> datatypes = dynamicTenantSetting.getValue();
        // Bind errors to datatypes
        Errors errors = new MapBindingResult(new HashMap<>(), DatatypeParameter.class.getName());
        // Check the validity of each datatype, if invalid add error to the binding
        for (Map.Entry<String, Object> datatype : datatypes.entrySet()) {
            try {
                // Convert value to datatype params, datatype is a Map<String, DatatypeParameter>
                DatatypeParameter datatypeParams = GsonUtil.fromJson(datatype.getValue(), DatatypeParameter.class);
                validateDatatype(datatype.getKey(), datatypeParams, errors);
            } catch (JsonSyntaxException e) {
                rejectMalformedDatatype(errors, datatype.getKey(), e);
            }
        }
        return validateAllDatatypesSettings(errors);

    }

    private void validateDatatype(String datatypeKey, DatatypeParameter datatypeParam, Errors errors) {
        // Check if datatype key contains only alphanumeric characters
        if (!datatypeKey.matches("^[\\w]*$") || datatypeKey.length() > LIMIT_CHARACTERS_LENGTH) {
            errors.rejectValue(datatypeKey,
                               ERROR_CODE_DATATYPE,
                               "The datatype key must contain only alphanumeric characters and have a max length of "
                               + LIMIT_CHARACTERS_LENGTH
                               + " characters.");
        }

        // Validate with hibernate datatype params
        validator.validate(datatypeParam, errors);

        // Check if the associated model exists only if validation is ok
        if (!errors.hasErrors()) {
            String modelName = datatypeParam.getModel();
            if (!modelCacheService.modelExists(modelName)) {
                errors.rejectValue(modelName,
                                   ERROR_CODE_MODEL,
                                   "The model was not found in the database. Make sure the model used already exists.");
            }
        }
    }

    private void rejectMalformedDatatype(Errors errors, String datatypeKey, JsonSyntaxException e) {
        LOGGER.error("Error on datatype key \"{}\"", datatypeKey, e);
        errors.rejectValue(datatypeKey, ERROR_CODE_DATATYPE, String.format("""
                                                                               The configuration of datatype "%s" is not valid.
                                                                               The string type key datatype must be associated to:
                                                                               - a REGARDS data model "model"
                                                                               - a storage path configuration "storePath"
                                                                               example :
                                                                               {
                                                                                 "datatypeSample1" : {
                                                                                   "model": "modelSample1",
                                                                                   "storePath": "${YEAR}/${MONTH}/${DAY}/${PROPERTY(path1.to.property)}"
                                                                                 }
                                                                                 "datatypeSample2" : {
                                                                                   "model": "modelSample2",
                                                                                   "storePath": "${YEAR}/${MONTH}/${DAY}/${PROPERTY(path2.to.property)}"
                                                                                 }
                                                                               }
                                                                               """, datatypeKey));
    }

    private boolean validateAllDatatypesSettings(Errors errors) {
        // Invalidate setting update if there are any errors
        boolean isValid = !errors.hasErrors();
        if (!isValid) {
            String errorMsg = Joiner.on("\n ---> ").join(errors.getAllErrors());
            LOGGER.error("Invalid {} setting parameters. {} errors detected. Cause:\n ---> {}",
                         LtaSettings.DATATYPES_KEY,
                         errors.getErrorCount(),
                         errorMsg);
        }
        return isValid;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return LtaSettings.DATATYPES_KEY.equals(dynamicTenantSetting.getName());
    }
}
