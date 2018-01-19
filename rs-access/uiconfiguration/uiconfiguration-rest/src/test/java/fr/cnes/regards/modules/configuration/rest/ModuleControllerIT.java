/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.configuration.rest;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestParamBuilder;
import fr.cnes.regards.modules.configuration.dao.IModuleRepository;
import fr.cnes.regards.modules.configuration.domain.Module;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 * Class InstanceLayoutControllerIT
 *
 * IT Tests for REST Controller
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class ModuleControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ModuleControllerIT.class);

    @Autowired
    private IModuleRepository repository;

    private Module moduleTest;

    private final static String APPLICATION_TEST = "TEST";

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    private Module createModule(final boolean pActive, final boolean pDefault) {
        final Module module = new Module();
        module.setActive(pActive);
        module.setApplicationId(APPLICATION_TEST);
        module.setConf("{\"test\":\"test\"}");
        module.setContainer("TestContainer");
        module.setDefaultDynamicModule(pDefault);
        module.setDescription("Description");
        module.setType("Module");
        return module;
    }

    @Before
    public void init() {

        final Module module = createModule(true, false);

        final Module module2 = createModule(false, true);

        moduleTest = repository.save(module);
        repository.save(module2);

    }

    @Test
    public void getUserApplicationModules() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(2)));
        performDefaultGet("/applications/{applicationId}/modules", expectations,
                          "The module list of TEST application should contains two modules", APPLICATION_TEST);

        expectations.clear();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(0)));
        performDefaultGet("/applications/{applicationId}/modules", expectations,
                          "The module list of TEST2 application should be empty", "TEST2");
    }

    @Test
    public void getUserApplicationActiveModules() {
        final RequestParamBuilder param = RequestParamBuilder.build().param("active", "true");
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(1)));
        performDefaultGet("/applications/{applicationId}/modules", expectations,
                          "The active module list should contains only one module", param, APPLICATION_TEST);
    }

    @Test
    public void saveNewModule() {
        final Module module = createModule(true, true);
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultPost("/applications/{applicationId}/modules", module, expectations,
                           "The POST to save a new module should be a success", APPLICATION_TEST);

        expectations.clear();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(3)));
        performDefaultGet("/applications/{applicationId}/modules", expectations,
                          "The previously created module should be retrieved", APPLICATION_TEST);
    }

    @Test
    public void deleteModule() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultDelete(ModuleController.ROOT_MAPPING + ModuleController.MODULE_ID_MAPPING, expectations,
                             "The deletion of a given existing module should be a success", APPLICATION_TEST,
                             moduleTest.getId());

        expectations.clear();
        expectations.add(status().isNotFound());
        performDefaultGet(ModuleController.ROOT_MAPPING + ModuleController.MODULE_ID_MAPPING, expectations,
                          "The previously deleted module should not exist anymore", APPLICATION_TEST,
                          moduleTest.getId().toString());
    }

}
