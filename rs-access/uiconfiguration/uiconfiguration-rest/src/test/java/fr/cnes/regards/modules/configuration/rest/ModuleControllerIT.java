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
package fr.cnes.regards.modules.configuration.rest;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.configuration.dao.IModuleRepository;
import fr.cnes.regards.modules.configuration.domain.Module;
import fr.cnes.regards.modules.configuration.domain.UIPage;

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

    private Module createModule(boolean active, UIPage page) {
        final Module module = new Module();
        module.setActive(active);
        module.setApplicationId(APPLICATION_TEST);
        module.setConf("{\"test\":\"test\"}");
        module.setContainer("TestContainer");
        module.setPage(page);
        module.setDescription("Description");
        module.setType("Module");
        return module;
    }

    @Before
    public void init() {

        final Module module = createModule(true, new UIPage(false, null, null, null));

        final Module module2 = createModule(false, new UIPage(true, null, null, null));

        moduleTest = repository.save(module);
        repository.save(module2);

    }

    @Test
    public void getUserApplicationModules() {
        performDefaultGet("/applications/{applicationId}/modules",
                          customizer().expectStatusOk().expectToHaveSize(JSON_PATH_CONTENT, 2),
                          "The module list of TEST application should contains two modules", APPLICATION_TEST);

        performDefaultGet("/applications/{applicationId}/modules",
                          customizer().expectStatusOk().expectToHaveSize(JSON_PATH_CONTENT, 0),
                          "The module list of TEST2 application should be empty", "TEST2");
    }

    @Test
    public void getUserApplicationActiveModules() {
        performDefaultGet("/applications/{applicationId}/modules",
                          customizer().expectStatusOk().expectToHaveSize(JSON_PATH_CONTENT, 1).addParameter("active",
                                                                                                            "true"),
                          "The active module list should contains only one module", APPLICATION_TEST);
    }

    @Test
    public void saveNewModule() {
        final Module module = createModule(true, new UIPage(true, null, null, null));
        performDefaultPost("/applications/{applicationId}/modules", module, customizer().expectStatusOk(),
                           "The POST to save a new module should be a success", APPLICATION_TEST);

        performDefaultGet("/applications/{applicationId}/modules",
                          customizer().expectStatusOk().expectToHaveSize(JSON_PATH_CONTENT, 3),
                          "The previously created module should be retrieved", APPLICATION_TEST);
    }

    @Test
    public void deleteModule() {
        performDefaultDelete(ModuleController.ROOT_MAPPING + ModuleController.MODULE_ID_MAPPING,
                             customizer().expectStatusOk(),
                             "The deletion of a given existing module should be a success", APPLICATION_TEST,
                             moduleTest.getId());

        performDefaultGet(ModuleController.ROOT_MAPPING + ModuleController.MODULE_ID_MAPPING,
                          customizer().expectStatusNotFound(), "The previously deleted module should not exist anymore",
                          APPLICATION_TEST, moduleTest.getId().toString());
    }

}
