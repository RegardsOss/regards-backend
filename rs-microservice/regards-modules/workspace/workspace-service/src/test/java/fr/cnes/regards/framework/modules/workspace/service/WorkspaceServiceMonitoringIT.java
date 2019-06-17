/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.workspace.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(properties = { "regards.cipher.key-location=src/test/resources/testKey",
        "regards.cipher.iv=1234567812345678", "regards.workspace.occupation.threshold=0",
        "regards.workspace.critical.occupation.threshold=0", "spring.datasource.type=" })
public class WorkspaceServiceMonitoringIT extends AbstractRegardsServiceIT {

    @Autowired
    private IWorkspaceService workspaceService;

    @Autowired
    private IWorkspaceNotifier notifier;

    @Test(expected = UnsupportedOperationException.class) // because of default workspace notifier implementation
    @Requirement("REGARDS_DSL_ING_CMP_010")
    @Requirement("REGARDS_DSL_ING_CMP_020")
    @Requirement("REGARDS_DSL_ING_CMP_030")
    public void testMonitor() {
        IWorkspaceNotifier spiedNotifier = Mockito.spy(notifier);
        workspaceService.monitor(getDefaultTenant());
        Mockito.verify(spiedNotifier, Mockito.times(1)).sendErrorNotification(Mockito.any(), Mockito.any(),
                                                                              Mockito.any());
        Assert.assertTrue(MaintenanceManager.getMaintenance(getDefaultTenant()));
    }

    @After
    public void cleanUp() throws IOException {
        try {
            Files.walk(workspaceService.getMicroserviceWorkspace()).forEach(p -> p.toFile().delete());
        } catch (NoSuchFileException e) {
            // we don't care
        }
    }

}
