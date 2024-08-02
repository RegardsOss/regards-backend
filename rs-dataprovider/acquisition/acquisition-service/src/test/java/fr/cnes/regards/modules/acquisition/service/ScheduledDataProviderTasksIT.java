/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.service;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Sébastien Binda
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acq_schedulers" })
public class ScheduledDataProviderTasksIT extends AbstractMultitenantServiceIT {

    @Autowired
    private ScheduledDataProviderTasks scheduledDataProviderTasks;

    @SpyBean
    private IAcquisitionProcessingChainRepository acquisitionProcessingChainRepository;

    @After
    public void after() {
        MaintenanceManager.unSetMaintenance(getDefaultTenant());
    }

    @Before
    public void init() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Mockito.clearInvocations(acquisitionProcessingChainRepository);
    }

    @Test
    public void check_maintenance_mode_disable_automatic_acquisition_chains() {

        // Given
        MaintenanceManager.setMaintenance(getDefaultTenant());

        // With
        scheduledDataProviderTasks.processAcquisitionChains();

        // Then
        Mockito.verify(acquisitionProcessingChainRepository, Mockito.never()).findAllBootableAutomaticChains();
    }

    @Test
    public void check_automatic_acquisition_chains_runner() {

        // Given
        MaintenanceManager.unSetMaintenance(getDefaultTenant());

        // With
        scheduledDataProviderTasks.processAcquisitionChains();

        // Then
        Mockito.verify(acquisitionProcessingChainRepository, Mockito.atLeastOnce()).findAllBootableAutomaticChains();
    }

}
