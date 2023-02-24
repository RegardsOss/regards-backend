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
package fr.cnes.regards.modules.configuration.dao;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalIT;
import fr.cnes.regards.modules.configuration.domain.Module;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

/**
 * Class LayoutRepositoryTest
 * <p>
 * DAO Test
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@TestPropertySource("classpath:test.properties")
public class ModuleRepositoryIT extends AbstractDaoTransactionalIT {

    @Autowired
    private IModuleRepository repository;

    /**
     * Common method to save a new module
     *
     * @since 1.0-SNAPSHOT
     */
    private Module addModule(final String pApplicationId) {
        // Create a new layout configuration
        final Module module = new Module();
        module.setApplicationId(pApplicationId);
        module.setActive(true);
        module.setConf("{}");
        module.setContainer("TestContainer");
        module.setDescription("Test module");
        module.setType("module");
        return repository.save(module);
    }

    /**
     * Test saving a new module configuration
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void saveModuleTest() {
        // Create a new layout configuration
        final Module module = addModule("TEST");
        final Module newModule = repository.save(module);
        final Module module2 = repository.findById(newModule.getId()).orElse(null);
        Assert.assertEquals(newModule.getApplicationId(), module2.getApplicationId());
    }

    /**
     * Test updating an existing module.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void updateModuleTest() {
        final Module module = addModule("TEST");
        module.setDescription("New description");
        final Module module2 = repository.save(module);
        Assert.assertEquals("New description", module2.getDescription());
    }

    /**
     * Test finding modules.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void findModulesTest() {
        addModule("TEST");
        addModule("TEST");
        addModule("OTHER_TEST");
        Page<Module> modulesPage = repository.findByApplicationId("TEST", PageRequest.of(0, 10));
        Assert.assertEquals(2, modulesPage.getTotalElements());

        modulesPage = repository.findByApplicationId("OTHER_TEST", PageRequest.of(0, 10));
        Assert.assertEquals(1, modulesPage.getTotalElements());
    }

    /**
     * Test deleting a module.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void deleteLayoutTest() {
        // Create a new layout configuration
        final Module module = addModule("TEST");
        repository.delete(module);
    }

}
