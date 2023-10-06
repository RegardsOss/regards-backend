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

import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import fr.cnes.regards.framework.modules.workspace.service.WorkspaceService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.delivery.domain.exception.DeliveryOrderException;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.service.order.zip.env.utils.DeliveryStepUtils;
import fr.cnes.regards.modules.delivery.service.order.zip.workspace.DeliveryDownloadWorkspaceManager;
import fr.cnes.regards.modules.order.client.feign.IOrderDataFileAvailableClient;
import fr.cnes.regards.modules.order.client.feign.IOrderDataFileClient;
import fr.cnes.regards.modules.order.domain.dto.OrderDataFileDTO;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * Test for {@link DeliveryDownloadService}.
 * <p>The purpose of this test is to check if files are properly retrieved according to the
 * {@link DeliveryRequest} received.</p>
 * TEST PLAN :
 * <ul>
 *  <li>Nominal cases :
 *    <ul>
 *      <li>{@link #givenDelivery_whenDownload_thenFilesDownloadedInWorkspace()}</li>
 *    </ul></li>
 *  <li>Error cases :
 *    <ul>
 *      <li>{@link #givenDelivery_whenDownloadChecksumError_thenException()}</li>
 *      <li>{@link #givenDelivery_whenAvailableResponseError_thenException()}</li>
 *    </ul></li>
 * </ul>
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class DeliveryDownloadServiceTest {

    private DeliveryDownloadService deliveryDownloadService; // class under test

    @Mock
    private IOrderDataFileAvailableClient orderClient;

    @Mock
    private IOrderDataFileClient dataFileClient;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private IRuntimeTenantResolver runtimeTenantResolver;

    private DeliveryDownloadWorkspaceManager deliveryWorkspaceManager;

    @Before
    public void init() throws IOException, DeliveryOrderException {
        // clean workspace directory if it already exists
        FileUtils.deleteDirectory(DeliveryStepUtils.WORKSPACE_PATH.toFile());
        Files.createDirectories(DeliveryStepUtils.WORKSPACE_PATH);
        // init services
        deliveryDownloadService = new DeliveryDownloadService(orderClient, dataFileClient, runtimeTenantResolver, 2);
        deliveryWorkspaceManager = new DeliveryDownloadWorkspaceManager(DeliveryStepUtils.DELIVERY_CORRELATION_ID,
                                                                        DeliveryStepUtils.WORKSPACE_PATH);
        deliveryWorkspaceManager.createDeliveryFolder();
    }

    @Test
    public void givenDelivery_whenDownload_thenFilesDownloadedInWorkspace()
        throws DeliveryOrderException, NoSuchAlgorithmException, IOException {
        // --- GIVEN ---
        // simulate order requested
        List<OrderDataFileDTO> simulatedOrderDataFiles = DeliveryStepUtils.buildOrderDataFileDtos();
        Mockito.when(orderClient.getAvailableFilesInOrder(anyLong(), any())).thenAnswer(ans -> {
            Pageable page = ans.getArgument(1);
            return ResponseEntity.ok(DeliveryStepUtils.handleOrderDataFilesDtosByPage(page.getPageNumber(),
                                                                                      page.getPageSize(),
                                                                                      simulatedOrderDataFiles));
        });
        // simulate download of file
        Mockito.when(dataFileClient.downloadFile(anyLong())).thenAnswer(ans -> {
            long dataFileId = ans.getArgument(0);
            return Response.builder()
                           .status(HttpStatus.OK.value())
                           .request(Request.create(Request.HttpMethod.GET,
                                                   "url",
                                                   new HashMap<>(),
                                                   Request.Body.empty(),
                                                   new RequestTemplate()))
                           .body(new FileInputStream(Path.of(String.format(DeliveryStepUtils.TEST_FILES_ORDER_RESOURCES.resolve(
                               "data-%d").resolve("file-%d.txt").toString(), dataFileId, dataFileId)).toFile()), 100)
                           .build();
        });

        // --- WHEN ---
        deliveryDownloadService.getAndDownloadFiles(DeliveryStepUtils.buildDeliveryRequest(), deliveryWorkspaceManager);

        // --- THEN ---
        // check files were successfully downloaded in tmp delivery workspace
        for (OrderDataFileDTO dataFile : simulatedOrderDataFiles) {
            Path expectedDownloadedFilePath = deliveryWorkspaceManager.getDownloadSubfolder()
                                                                      .resolve(String.format(DeliveryStepUtils.PRODUCT_FOLDER_PATTERN,
                                                                                             dataFile.getProductId(),
                                                                                             dataFile.getVersion()))
                                                                      .resolve(dataFile.getFilename());
            Assertions.assertThat(expectedDownloadedFilePath.toFile()).exists();
            Assertions.assertThat(dataFile.getChecksum())
                      .isEqualTo(ChecksumUtils.computeHexChecksum(expectedDownloadedFilePath, "MD5"));
        }
    }

    @Test
    public void givenDelivery_whenDownloadChecksumError_thenException() {
        // --- GIVEN ---
        // simulate order requested
        List<OrderDataFileDTO> simulatedOrderDataFiles = DeliveryStepUtils.buildOrderDataFileDtos();
        Mockito.when(orderClient.getAvailableFilesInOrder(anyLong(), any())).thenAnswer(ans -> {
            Pageable page = ans.getArgument(1);
            return ResponseEntity.ok(DeliveryStepUtils.handleOrderDataFilesDtosByPage(page.getPageNumber(),
                                                                                      page.getPageSize(),
                                                                                      simulatedOrderDataFiles));
        });
        // simulate download of file and make checksum verification fail
        Mockito.when(dataFileClient.downloadFile(anyLong()))
               .thenReturn(Response.builder()
                                   .status(HttpStatus.OK.value())
                                   .request(Request.create(Request.HttpMethod.GET,
                                                           "url",
                                                           new HashMap<>(),
                                                           Request.Body.empty(),
                                                           new RequestTemplate()))
                                   .body(new ByteArrayInputStream(("error-download").getBytes()), 100)
                                   .build());

        // --- WHEN / THEN ---
        Assertions.assertThatThrownBy(() -> deliveryDownloadService.getAndDownloadFiles(DeliveryStepUtils.buildDeliveryRequest(),
                                                                                        deliveryWorkspaceManager))
                  .isInstanceOf(DeliveryOrderException.class)
                  .hasMessageContaining("checksum");
    }

    @Test
    public void givenDelivery_whenAvailableResponseError_thenException() {
        // --- GIVEN ---
        // simulate order requested
        Mockito.when(orderClient.getAvailableFilesInOrder(anyLong(), any()))
               .thenReturn(ResponseEntity.internalServerError().build());

        // --- WHEN / THEN ---
        Assertions.assertThatThrownBy(() -> deliveryDownloadService.getAndDownloadFiles(DeliveryStepUtils.buildDeliveryRequest(),
                                                                                        deliveryWorkspaceManager))
                  .isInstanceOf(DeliveryOrderException.class)
                  .hasMessageContaining("Could not retrieve");
    }

}
