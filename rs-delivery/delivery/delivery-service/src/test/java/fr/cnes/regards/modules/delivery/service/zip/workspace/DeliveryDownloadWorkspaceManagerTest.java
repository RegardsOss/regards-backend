/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.zip.workspace;

import fr.cnes.regards.framework.modules.workspace.service.WorkspaceService;
import fr.cnes.regards.modules.delivery.domain.exception.DeliveryOrderException;
import fr.cnes.regards.modules.delivery.service.order.zip.workspace.DeliveryDownloadWorkspaceManager;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static fr.cnes.regards.modules.delivery.service.zip.env.utils.DeliveryStepUtils.WORKSPACE_PATH;

/**
 * Test for {@link DeliveryDownloadWorkspaceManager}
 * <p>The purpose of this test is to check if a delivery folder is properly managed in the delivery workspace.</p>
 * TEST PLAN :
 * <ul>
 *  <li>Nominal cases :
 *    <ul>
 *      <li>{@link #givenStart_whenInitWorkspace_thenWorkspaceCreated()}</li>
 *      <li>{@link #givenError_whenDeleteWorkspace_thenWorkspaceDeleted()}</li>
 *    </ul>
 *  </li>
 * </ul>
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class DeliveryDownloadWorkspaceManagerTest {

    @Mock
    private WorkspaceService workspaceService;

    @Before
    public void init() throws IOException {
        FileUtils.deleteDirectory(WORKSPACE_PATH.toFile());
        Files.createDirectories(WORKSPACE_PATH);
    }

    @Test
    public void givenStart_whenInitWorkspace_thenWorkspaceCreated() throws DeliveryOrderException {
        // GIVEN
        String corrId = "test-create";

        // WHEN
        DeliveryDownloadWorkspaceManager downloadWorkspaceManager = new DeliveryDownloadWorkspaceManager(corrId,
                                                                                                         WORKSPACE_PATH);
        downloadWorkspaceManager.createDeliveryFolder();

        // THEN
        // expect created delivery workspace
        Assertions.assertThat(downloadWorkspaceManager).isNotNull();
        Path expectedDeliveryWorkspace = WORKSPACE_PATH.resolve(corrId);
        Assertions.assertThat(Files.exists(expectedDeliveryWorkspace))
                  .as(String.format("Expected creation of delivery workspace at '%s'.", expectedDeliveryWorkspace))
                  .isTrue();
    }

    @Test
    public void givenError_whenDeleteWorkspace_thenWorkspaceDeleted() throws DeliveryOrderException {
        // GIVEN
        String corrId = "test-delete";
        DeliveryDownloadWorkspaceManager downloadWorkspaceManager = new DeliveryDownloadWorkspaceManager(corrId,
                                                                                                         WORKSPACE_PATH);
        downloadWorkspaceManager.createDeliveryFolder();
        Assertions.assertThat(Files.exists(downloadWorkspaceManager.getDeliveryTmpFolderPath())).isTrue();

        // WHEN
        downloadWorkspaceManager.deleteDeliveryFolder();

        // THEN
        Assertions.assertThat(Files.exists(downloadWorkspaceManager.getDeliveryTmpFolderPath()))
                  .as(String.format("Expected deletion of delivery workspace at '%s'.",
                                    downloadWorkspaceManager.getDeliveryTmpFolderPath()))
                  .isFalse();
    }

}
