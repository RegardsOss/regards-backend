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
package fr.cnes.regards.modules.ltamanager.service.settings;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ltamanager.domain.settings.DatatypeParameter;
import fr.cnes.regards.modules.ltamanager.service.settings.customizers.LtaDatatypesSettingCustomizer;
import fr.cnes.regards.modules.ltamanager.service.settings.customizers.LtaRequestExpirationCustomizer;
import fr.cnes.regards.modules.ltamanager.service.settings.customizers.LtaStorageSettingCustomizer;
import fr.cnes.regards.modules.model.client.IModelClient;
import fr.cnes.regards.modules.model.domain.Model;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static fr.cnes.regards.modules.ltamanager.domain.settings.LtaSettings.*;

/**
 * Test for Lta customizers
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=datatypes_settings_it" })
@ActiveProfiles({ "noscheduler" })
public class LtaSettingCustomizerIT extends AbstractMultitenantServiceIT {

    @Autowired
    private LtaDatatypesSettingCustomizer datatypeSettingService;

    @Autowired
    private LtaStorageSettingCustomizer storageSettingService;

    @Autowired
    private LtaRequestExpirationCustomizer expirationSettingService;

    @MockBean
    private IModelClient modelClient;

    @MockBean
    private IPublisher publisher;

    @Autowired
    private LtaModelCacheService modelCacheService;

    @Test
    @Purpose("Test if datatypes setting are valid if linked models exist in cache or repository")
    public void datatypes_check_success_model() {
        // GIVEN
        String model_1 = "modelSample1";
        String model_2 = "modelSample2";
        // the purpose is to test if both params are valid
        Map<String, DatatypeParameter> datatypesParams = Map.of("datatype_1",
                                                                new DatatypeParameter(model_1, "/path/example1"),
                                                                "datatype_2",
                                                                new DatatypeParameter(model_2, "/path/example2"));
        // one model is present in cache and the other in the repository
        modelCacheService.addCacheEntry(model_1, 1L);
        Model model = Model.build(model_2, "", EntityType.DATA);
        model.setId(1L);
        Mockito.when(modelClient.getModel(model_2)).thenReturn(ResponseEntity.ok(EntityModel.of(model)));

        // init tenant settings with datatype params
        DynamicTenantSetting datatypesSetting = new DynamicTenantSetting(DATATYPES_KEY,
                                                                         "The datatype of incoming requests.",
                                                                         datatypesParams);
        // WHEN
        // check datatypesSetting validity
        boolean hasErrors = datatypeSettingService.isValid(datatypesSetting).hasErrors();

        // THEN
        Assertions.assertFalse(hasErrors, "datatypesSetting should be valid");

    }

    @Test
    @Purpose("Test if datatype setting is invalid if linked model do not exist in cache or repository")
    public void datatypes_check_error_invalid_model() {
        // GIVEN
        String notExistingModel = "NotExistingModel";
        Map<String, DatatypeParameter> datatypesParams = Map.of("datatype_1",
                                                                new DatatypeParameter(notExistingModel,
                                                                                      "/path/example1"));
        Mockito.when(modelClient.getModel(notExistingModel))
               .thenAnswer(aws -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                                .body(new ServerErrorResponse("Model was not retrieved",
                                                                              new ModuleException("ModelNotRetrieved"))));

        // init tenant settings with datatype params
        DynamicTenantSetting datatypesSetting = new DynamicTenantSetting(DATATYPES_KEY,
                                                                         "The datatype of incoming requests.",
                                                                         datatypesParams);
        // WHEN
        // check datatype validity
        boolean hasErrors = datatypeSettingService.isValid(datatypesSetting).hasErrors();

        // THEN
        Assertions.assertTrue(hasErrors,
                              "datatypeSetting should be invalid because the associated model does not exists.");
    }

    @Test
    @Purpose("Test if datatypes setting are invalid if params are malformed")
    public void datatypes_check_error_invalid_params() {
        // GIVEN
        String model_1 = "modelSample1";
        // the purpose is to test if all params are invalid
        Map<String, DatatypeParameter> datatypesParams = Map.of("datatype_1",
                                                                new DatatypeParameter(model_1, ""),
                                                                "datatype_2",
                                                                new DatatypeParameter(null, null),
                                                                "S@!!!$$$",
                                                                new DatatypeParameter(model_1, null));

        // init tenant settings with datatype params
        DynamicTenantSetting datatypesSetting = new DynamicTenantSetting(DATATYPES_KEY,
                                                                         "The datatype of incoming requests.",
                                                                         datatypesParams);
        // WHEN
        // check datatypesSetting validity
        boolean hasErrors = datatypeSettingService.isValid(datatypesSetting).hasErrors();

        // THEN
        Assertions.assertTrue(hasErrors,
                              "datatypesSetting should be invalid because parameters do not respect constraints.");
    }

    @Test
    public void datatypes_check_error_malformed_datatype() {
        // GIVEN
        // the purpose is to test if malformed datatypes are rejected
        Map<String, Object> datatypesParams = Map.of("datatype_1",
                                                     "hello",
                                                     "datatype_2",
                                                     List.of("stillInvalid", "parameters"));

        // init tenant settings with datatype params
        DynamicTenantSetting datatypesSetting = new DynamicTenantSetting(DATATYPES_KEY,
                                                                         "The datatype of incoming requests.",
                                                                         datatypesParams);
        // WHEN
        // check datatypesSetting validity
        boolean hasErrors = datatypeSettingService.isValid(datatypesSetting).hasErrors();

        // THEN
        Assertions.assertTrue(hasErrors, "datatypesSetting should be invalid because it is malformed.");
    }

    @Test
    @Purpose("Test if storage setting is valid with correct configuration")
    public void storage_check_success() {
        // GIVEN
        // init tenant settings with storage param
        DynamicTenantSetting storageSetting = new DynamicTenantSetting(STORAGE_KEY,
                                                                       "storage valid",
                                                                       RandomStringUtils.randomAlphabetic(254));

        // WHEN
        // check tenant settings validity
        boolean hasErrors = storageSettingService.isValid(storageSetting).hasErrors();

        // THEN
        Assertions.assertFalse(hasErrors, "storageSetting should be valid");
    }

    @Test
    @Purpose("Test if storage setting is invalid with incorrect configuration")
    public void storage_check_error_invalid_params() {
        // GIVEN
        // init tenant settings with storage param
        DynamicTenantSetting storageSetting = new DynamicTenantSetting(STORAGE_KEY,
                                                                       "storage error more than 255 characters",
                                                                       RandomStringUtils.randomAlphabetic(256));

        // WHEN
        // check tenant settings validity
        boolean hasErrors = storageSettingService.isValid(storageSetting).hasErrors();

        // THEN
        Assertions.assertTrue(hasErrors, "storageSetting should be invalid because it exceeds the limits authorized");
    }

    @Test
    @Purpose("Test if expiration hours setting is valid with correct configuration")
    public void expiration_check_success() {
        // GIVEN
        // init tenant settings with expiration hours param
        DynamicTenantSetting storageSetting = new DynamicTenantSetting(SUCCESS_EXPIRATION_IN_HOURS_KEY,
                                                                       "expiration valid",
                                                                       20);

        // WHEN
        // check tenant settings validity
        boolean hasErrors = expirationSettingService.isValid(storageSetting).hasErrors();

        // THEN
        Assertions.assertFalse(hasErrors, "expirationSetting should be valid");
    }

    @Test
    @Purpose("Test if expiration hours setting is invalid with incorrect configuration")
    public void expiration_check_error_invalid_params_not_int() {
        // GIVEN
        // init tenant settings with expiration hours param
        DynamicTenantSetting storageSetting = new DynamicTenantSetting(SUCCESS_EXPIRATION_IN_HOURS_KEY,
                                                                       "expiration should be integer",
                                                                       RandomStringUtils.randomAlphabetic(256));

        // WHEN
        // check tenant settings validity
        boolean hasErrors = expirationSettingService.isValid(storageSetting).hasErrors();

        // THEN
        Assertions.assertTrue(hasErrors, "expirationSetting should be invalid because it is not an integer");
    }

    @Test
    @Purpose("Test if expiration hours setting is invalid with incorrect configuration")
    public void expiration_check_error_invalid_params_negative() {
        // GIVEN
        // init tenant settings with expiration hours param
        DynamicTenantSetting storageSetting = new DynamicTenantSetting(SUCCESS_EXPIRATION_IN_HOURS_KEY,
                                                                       "expiration should be > 0",
                                                                       -1);

        // WHEN
        // check tenant settings validity
        boolean hasErrors = expirationSettingService.isValid(storageSetting).hasErrors();

        // THEN
        Assertions.assertTrue(hasErrors, "expirationSetting should be invalid because it is inferior to 0");
    }
}
