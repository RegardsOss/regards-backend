/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workermanager.service.cache;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.modules.tenant.settings.dao.IDynamicTenantSettingRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.workermanager.dao.IWorkerConfigRepository;
import fr.cnes.regards.modules.workermanager.service.config.ConfigManager;
import fr.cnes.regards.modules.workermanager.service.config.WorkerConfigCacheService;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Léo Mieulet
 **/
@TestPropertySource(properties = { "regards.amqp.enabled=false" },
        locations = { "classpath:application-test.properties" })
public abstract class AbstractWorkerManagerServiceUtilsTest extends AbstractMultitenantServiceTest {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorkerManagerServiceUtilsTest.class);

    /**
     * Services
     */

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IDynamicTenantSettingRepository dynamicTenantSettingRepository;

    @Autowired
    private IWorkerConfigRepository workerConfigRepository;

    @Autowired
    private ConfigManager configManager;

    @Autowired
    private WorkerConfigCacheService workerConfigCacheService;

    // -------------
    // BEFORE METHODS
    // -------------

    @Before
    public void init() throws Exception {

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Remove any old value
        cleanRepositories();

        // Clean cache
        configManager.resetConfiguration();
        workerConfigCacheService.cleanCache();

        // simulate application started and ready
        simulateApplicationStartedEvent();
        simulateApplicationReadyEvent();

        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // override this method to custom action performed before
        doInit();
    }

    /**
     * Custom test initialization to override
     *
     * @throws Exception
     */
    protected void doInit() throws Exception {
        // Override to init something
    }

    // -------------
    // AFTER METHODS
    // -------------

    @After
    public void after() throws Exception {
        cleanRepositories();
        doAfter();
    }

    /**
     * Custom test cleaning to override
     *
     * @throws Exception
     */
    protected void doAfter() throws Exception {
        // Override to init something
    }

    // -------------
    //     REPO
    // -------------
    private void cleanRepositories() {
        LOGGER.info("Clean repository");
        dynamicTenantSettingRepository.deleteAll();
        workerConfigRepository.deleteAll();
    }

    // --------------
    //  WORKER MANAGER UTILS
    // --------------

}