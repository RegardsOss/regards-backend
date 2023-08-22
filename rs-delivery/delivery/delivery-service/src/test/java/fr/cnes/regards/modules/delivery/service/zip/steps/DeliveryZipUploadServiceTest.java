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
package fr.cnes.regards.modules.delivery.service.zip.steps;

import fr.cnes.regards.framework.s3.domain.*;
import fr.cnes.regards.framework.s3.exception.ChecksumDoesntMatchException;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.delivery.domain.exception.DeliveryOrderException;
import fr.cnes.regards.modules.delivery.domain.zip.ZipDeliveryInfo;
import fr.cnes.regards.modules.delivery.service.order.s3.DeliveryS3ManagerService;
import fr.cnes.regards.modules.delivery.service.order.zip.steps.DeliveryZipUploadService;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

import static fr.cnes.regards.modules.delivery.service.zip.env.utils.DeliveryStepUtils.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Test for {@link DeliveryZipUploadServiceTest}.
 * <p>The purpose of this test is to check if a zip can be delivered to a S3 server.</p>
 * TEST PLAN :
 * <ul>
 *  <li>Nominal cases :
 *    <ul>
 *      <li>{@link #givenZipToUpload_whenUploaded_thenSuccessResult()}</li>
 *    </ul></li>
 *  <li>Error cases :
 *    <ul>
 *      <li>{@link #givenZipToUpload_whenUploadedFailure_thenFailureResult()}</li>
 *    </ul></li>
 * </ul>
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class DeliveryZipUploadServiceTest {

    @InjectMocks
    private DeliveryZipUploadService uploadService;

    @Mock
    private DeliveryS3ManagerService s3ManagerService;

    @Before
    public void init() throws IOException {
        // clean workspace directory if it already exists
        FileUtils.deleteDirectory(WORKSPACE_PATH.toFile());
        Files.createDirectories(WORKSPACE_PATH);
    }

    @Test
    public void givenZipToUpload_whenUploaded_thenSuccessResult()
        throws IOException, NoSuchAlgorithmException, DeliveryOrderException {
        //  GIVEN
        // mock zip upload success
        ZipDeliveryInfo createdZipInfo = mockS3UploadSuccess();

        // WHEN
        ZipDeliveryInfo actualZipUploadedInfo = uploadService.uploadZipToS3DeliveryServer(buildDeliveryRequest(),
                                                                                          createdZipInfo);

        // THEN
        ZipDeliveryInfo expectedCreatedZipInfo = new ZipDeliveryInfo(DELIVERY_CORRELATION_ID,
                                                                     actualZipUploadedInfo.name(),
                                                                     actualZipUploadedInfo.sizeInBytes(),
                                                                     actualZipUploadedInfo.md5Checksum(),
                                                                     "s3://"
                                                                     + "delivery-test-bucket"
                                                                     + "/"
                                                                     + DELIVERY_CORRELATION_ID
                                                                     + "/"
                                                                     + actualZipUploadedInfo.name());
        Assertions.assertThat(actualZipUploadedInfo).isEqualTo(expectedCreatedZipInfo);
    }

    @Test
    public void givenZipToUpload_whenUploadedFailure_thenFailureResult() throws IOException, NoSuchAlgorithmException {
        //  GIVEN
        // mock zip upload success
        ZipDeliveryInfo createdZipInfo = mockS3UploadFailure();

        // WHEN / THEN
        Assertions.assertThatThrownBy(() -> uploadService.uploadZipToS3DeliveryServer(buildDeliveryRequest(),
                                                                                      createdZipInfo))
                  .isInstanceOf(DeliveryOrderException.class)
                  .hasMessageContaining("not upload");
    }

    private ZipDeliveryInfo mockS3UploadSuccess() throws NoSuchAlgorithmException, IOException {
        // simulate zip delivery info created previously
        ZipDeliveryInfo createdZipInfo = mockZipCreated();
        // simulate S3 configuration
        mockStorageConfig();
        // simulate upload
        Mockito.when(s3ManagerService.uploadFileToDeliveryS3(any(), any(), any(), any())).thenAnswer(ans -> {
            StorageConfig storageConfig = ans.getArgument(1);
            StorageEntry storageEntry = ans.getArgument(2);
            String checksum = ans.getArgument(3);
            StorageCommand.Write cmd = new StorageCommand.Write.Impl(storageConfig,
                                                                     new StorageCommandID("upload-delivery"
                                                                                          + "-delivery-test-bucket",
                                                                                          UUID.randomUUID()),
                                                                     storageEntry.getFullPath(),
                                                                     storageEntry,
                                                                     checksum);

            return new StorageCommandResult.WriteSuccess(cmd, createdZipInfo.sizeInBytes(), checksum);
        });

        return createdZipInfo;
    }

    private ZipDeliveryInfo mockS3UploadFailure() throws NoSuchAlgorithmException, IOException {
        // simulate zip delivery info created previously
        ZipDeliveryInfo createdZipInfo = mockZipCreated();
        // simulate S3 configuration
        mockStorageConfig();
        // simulate upload
        Mockito.when(s3ManagerService.uploadFileToDeliveryS3(any(), any(), any(), any())).thenAnswer(ans -> {
            StorageConfig storageConfig = ans.getArgument(1);
            StorageEntry storageEntry = ans.getArgument(2);
            String checksum = ans.getArgument(3);
            StorageCommand.Write cmd = new StorageCommand.Write.Impl(storageConfig,
                                                                     new StorageCommandID("upload-delivery"
                                                                                          + "-delivery-test"
                                                                                          + "-bucket",
                                                                                          UUID.randomUUID()),
                                                                     storageEntry.getFullPath(),
                                                                     storageEntry,
                                                                     checksum);

            return new StorageCommandResult.WriteFailure(cmd, new ChecksumDoesntMatchException(checksum, "error"));
        });

        return createdZipInfo;
    }

    private ZipDeliveryInfo mockZipCreated() throws NoSuchAlgorithmException, IOException {
        // copy zip to workspace directory
        Path zipPath = WORKSPACE_PATH.resolve(DELIVERY_CORRELATION_ID)
                                     .resolve(String.format(MULTIPLE_FILES_ZIP_NAME_PATTERN, DELIVERY_CORRELATION_ID));
        File zipFile = zipPath.toFile();
        FileUtils.copyFile(Objects.requireNonNull(TEST_MULTIPLE_ZIP_ORDER_RESOURCE.toFile().listFiles())[0], zipFile);

        return new ZipDeliveryInfo(DELIVERY_CORRELATION_ID,
                                   zipPath.getFileName().toString(),
                                   zipFile.length(),
                                   ChecksumUtils.computeHexChecksum(zipPath, "MD5"),
                                   zipPath.toUri().toString());
    }

    private void mockStorageConfig() {
        try {
            Mockito.when(s3ManagerService.buildDeliveryStorageConfig(DELIVERY_CORRELATION_ID))
                   .thenReturn(StorageConfig.builder("http://localhost:5232", "fr-regards-1", "key", "super secret")
                                            .rootPath(DELIVERY_CORRELATION_ID)
                                            .bucket("delivery-test-bucket")
                                            .build());
        } catch (DeliveryOrderException e) {
            throw new RuntimeException(e);
        }
    }

}
