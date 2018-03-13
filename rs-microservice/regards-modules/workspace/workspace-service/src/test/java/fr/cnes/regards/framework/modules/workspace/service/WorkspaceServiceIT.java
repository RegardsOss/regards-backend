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
package fr.cnes.regards.framework.modules.workspace.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = { "classpath:workspace.properties" })
public class WorkspaceServiceIT extends AbstractRegardsServiceIT {

    @Autowired
    private IWorkspaceService workspaceService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IWorkspaceNotifier notifier;

    @Test
    public void testSetIntoWorkspace() throws IOException {
        Path src = Paths.get("src", "test", "resources", "test.txt");
        InputStream is = Files.newInputStream(src);
        workspaceService.setIntoWorkspace(is, "test.txt");
        Path pathInWS = Paths.get(workspaceService.getMicroserviceWorkspace().toString(), "test.txt");
        Assert.assertTrue(Files.exists(pathInWS));
    }

    @Test
    public void testRetrieveFromWS() throws IOException, NoSuchAlgorithmException {
        Path src = Paths.get("src", "test", "resources", "test.txt");
        InputStream is = Files.newInputStream(src);
        workspaceService.setIntoWorkspace(is, "test.txt");
        InputStream result = workspaceService.retrieveFromWorkspace("test.txt");
        // lets check the checksum so we are sure it is the same than original
        String srcChecksum = ChecksumUtils.computeHexChecksum(Files.newInputStream(src), "MD5");
        String wsChecksum = ChecksumUtils.computeHexChecksum(result, "MD5");
        Assert.assertEquals(srcChecksum, wsChecksum);
    }

    @Test(expected = UnsupportedOperationException.class) // because of default workspace notifier implementation
    @Requirement("REGARDS_DSL_ING_CMP_010")
    @Requirement("REGARDS_DSL_ING_CMP_020")
    @Requirement("REGARDS_DSL_ING_CMP_030")
    public void testMonitor() {
        IWorkspaceNotifier spiedNotifier = Mockito.spy(notifier);
        workspaceService.monitor(DEFAULT_TENANT);
        Mockito.verify(spiedNotifier, Mockito.times(1))
                .sendErrorNotification(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Assert.assertTrue(MaintenanceManager.getMaintenance(DEFAULT_TENANT));
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
