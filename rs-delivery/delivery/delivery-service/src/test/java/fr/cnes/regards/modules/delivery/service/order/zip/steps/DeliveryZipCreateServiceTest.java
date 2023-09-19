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
package fr.cnes.regards.modules.delivery.service.order.zip.steps;

import fr.cnes.regards.framework.modules.workspace.service.WorkspaceService;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.delivery.domain.exception.DeliveryOrderException;
import fr.cnes.regards.modules.delivery.domain.order.zip.ZipDeliveryInfo;
import fr.cnes.regards.modules.delivery.service.order.zip.env.utils.DeliveryStepUtils;
import fr.cnes.regards.modules.delivery.service.order.zip.workspace.DeliveryDownloadWorkspaceManager;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Test for {@link DeliveryZipCreateService}.
 * <p>The purpose of this test is to check if a zip is properly created from files previously downloaded in the
 * delivery workspace.</p>
 * TEST PLAN :
 * <ul>
 *  <li>Nominal cases :
 *    <ul>
 *      <li>{@link #givenMultipleDeliveryFiles_whenZip_thenZipMultipleTypeCreated()}</li>
 *      <li>{@link #givenSingleDeliveryFile_whenZip_thenZipSingleTypeCreated()}</li>
 *    </ul></li>
 * </ul>
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class DeliveryZipCreateServiceTest {

    private DeliveryZipCreateService zipService; // class under test

    private DeliveryDownloadWorkspaceManager deliveryWorkspaceManager;

    @Mock
    private WorkspaceService workspaceService;

    @Before
    public void init() throws IOException, DeliveryOrderException {
        // clean workspace directory if it already exists
        FileUtils.deleteDirectory(DeliveryStepUtils.WORKSPACE_PATH.toFile());
        Files.createDirectories(DeliveryStepUtils.WORKSPACE_PATH);
        // init services
        deliveryWorkspaceManager = new DeliveryDownloadWorkspaceManager(DeliveryStepUtils.DELIVERY_CORRELATION_ID,
                                                                        DeliveryStepUtils.WORKSPACE_PATH);
        deliveryWorkspaceManager.createDeliveryFolder();
        zipService = new DeliveryZipCreateService();
    }

    @Test
    public void givenMultipleDeliveryFiles_whenZip_thenZipMultipleTypeCreated()
        throws IOException, DeliveryOrderException, NoSuchAlgorithmException {
        // GIVEN
        FileUtils.copyDirectory(DeliveryStepUtils.TEST_FILES_ORDER_RESOURCES.toFile(),
                                deliveryWorkspaceManager.getDownloadSubfolder().toFile());

        // WHEN
        ZipDeliveryInfo zipCreatedInfo = zipService.createDeliveryZip(deliveryWorkspaceManager);

        // THEN
        // compare expected zip to actual zip that was created by zip service
        Path srcMultipleZipLocation = Objects.requireNonNull(DeliveryStepUtils.TEST_MULTIPLE_ZIP_ORDER_RESOURCE.toFile()
                                                                                                               .listFiles())[0].toPath();
        Path expectedCreatedZipPath = deliveryWorkspaceManager.getDeliveryTmpFolderPath()
                                                              .resolve(String.format(DeliveryStepUtils.MULTIPLE_FILES_ZIP_NAME_PATTERN,
                                                                                     DeliveryStepUtils.DELIVERY_CORRELATION_ID));
        ZipDeliveryInfo expectedCreatedZipInfo = new ZipDeliveryInfo(DeliveryStepUtils.DELIVERY_CORRELATION_ID,
                                                                     srcMultipleZipLocation.getFileName().toString(),
                                                                     srcMultipleZipLocation.toFile().length(),
                                                                     ChecksumUtils.computeHexChecksum(
                                                                         expectedCreatedZipPath,
                                                                         "MD5"),
                                                                     expectedCreatedZipPath.toUri().toString());
        Assertions.assertThat(expectedCreatedZipPath.toFile()).exists();
        Assertions.assertThat(zipCreatedInfo).isEqualTo(expectedCreatedZipInfo);
    }

    @Test
    public void givenSingleDeliveryFile_whenZip_thenZipSingleTypeCreated()
        throws IOException, DeliveryOrderException, NoSuchAlgorithmException {
        // GIVEN
        File srcFileFolder = Objects.requireNonNull(DeliveryStepUtils.TEST_FILES_ORDER_RESOURCES.toFile()
                                                                                                .listFiles((dir, name) -> name.equals(
                                                                                                    "data-0")))[0];
        FileUtils.copyDirectory(srcFileFolder, deliveryWorkspaceManager.getDownloadSubfolder().toFile());

        // WHEN
        ZipDeliveryInfo zipCreatedInfo = zipService.createDeliveryZip(deliveryWorkspaceManager);

        // THEN
        // compare expected zip to actual zip that was created by zip service
        Path srcSingleZipLocation = Objects.requireNonNull(DeliveryStepUtils.TEST_SINGLE_ZIP_ORDER_RESOURCE.toFile()
                                                                                                           .listFiles())[0].toPath();
        String expectedZipName = "file-0.zip";
        Path expectedCreatedZipPath = deliveryWorkspaceManager.getDeliveryTmpFolderPath().resolve(expectedZipName);
        ZipDeliveryInfo expectedCreatedZipInfo = new ZipDeliveryInfo(DeliveryStepUtils.DELIVERY_CORRELATION_ID,
                                                                     expectedZipName,
                                                                     srcSingleZipLocation.toFile().length(),
                                                                     ChecksumUtils.computeHexChecksum(
                                                                         expectedCreatedZipPath,
                                                                         "MD5"),
                                                                     expectedCreatedZipPath.toUri().toString());
        Assertions.assertThat(expectedCreatedZipPath.toFile()).exists();
        Assertions.assertThat(zipCreatedInfo).isEqualTo(expectedCreatedZipInfo);
    }

}
