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

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.delivery.domain.exception.DeliveryOrderException;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.service.order.zip.workspace.DeliveryDownloadWorkspaceManager;
import fr.cnes.regards.modules.order.client.feign.IOrderDataFileAvailableClient;
import fr.cnes.regards.modules.order.client.feign.IOrderDataFileClient;
import fr.cnes.regards.modules.order.domain.dto.OrderDataFileDTO;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

/**
 * Download all files requested in a {@link DeliveryRequest} in a configured workspace.
 *
 * @author Iliana Ghazali
 **/
@MultitenantTransactional
@Service
public class DeliveryDownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryDownloadService.class);

    /**
     * Pattern to build the parent folder of a file ordered.
     */
    private static final String PRODUCT_FOLDER_PATTERN = "%s_%d"; // <productId>_<version>

    // SERVICES

    private final IOrderDataFileAvailableClient orderClient;

    private final IOrderDataFileClient dataFileClient;

    private final int availablePageSize;

    public DeliveryDownloadService(IOrderDataFileAvailableClient orderClient,
                                   IOrderDataFileClient dataFileClient,
                                   @Value("${regards.delivery.available.files.bulk.size:100}") int availablePageSize) {
        this.orderClient = orderClient;
        this.dataFileClient = dataFileClient;
        this.availablePageSize = availablePageSize;
    }

    /**
     * Download files requested in the {@link DeliveryRequest} in the configured workspace by slice.
     *
     * @param deliveryRequest   client request to process
     * @param downloadWorkspace where to download files
     * @throws DeliveryOrderException if files could not be downloaded
     */
    public void getAndDownloadFiles(DeliveryRequest deliveryRequest, DeliveryDownloadWorkspaceManager downloadWorkspace)
        throws DeliveryOrderException {
        String correlationId = deliveryRequest.getCorrelationId();
        long start = System.currentTimeMillis();
        LOGGER.debug("Starting downloading files requested in delivery with correlation id '{}'.", correlationId);
        Long orderId = deliveryRequest.getOrderId();
        Assert.notNull(orderId,
                       String.format("An unexpected error occurred orderId should not be null for "
                                     + "delivery request with correlation id '%s'!", correlationId));

        int nbFilesProcessed = 0;
        Pageable pageable = PageRequest.of(0, availablePageSize, Sort.by("id"));
        PagedModel<EntityModel<OrderDataFileDTO>> pageAvailableFiles;
        boolean hasNextPage = true;
        while (hasNextPage) {
            // 1. Get available files by page
            pageAvailableFiles = retrieveAvailableFilePage(orderId, correlationId, pageable);
            // 2. Download files in the unique microservice workspace
            nbFilesProcessed += downloadPageOfFiles(correlationId, pageAvailableFiles, downloadWorkspace);
            hasNextPage = pageAvailableFiles.getNextLink().isPresent();
            if (hasNextPage) {
                pageable = pageable.next();
            }
        }
        LOGGER.debug("""
                         Successfully completed downloading of files requested in delivery with correlationId '{}'.
                         Local download folder path: '{}'.
                         Total number of files downloaded: {}.
                         Total duration: {} ms.""",
                     correlationId,
                     downloadWorkspace.getDownloadSubfolder(),
                     nbFilesProcessed,
                     System.currentTimeMillis() - start);
    }

    /**
     * Retrieve references of files requested from rs-order with {@link DeliveryRequest#getOrderId()}.
     *
     * @param orderId       order reference identifier
     * @param correlationId unique identifier to monitor the request
     * @param pageable      page requested
     * @return page model of {@link OrderDataFileDTO}
     * @throws DeliveryOrderException if files could not be retrieved.
     */
    @NotNull
    private PagedModel<EntityModel<OrderDataFileDTO>> retrieveAvailableFilePage(Long orderId,
                                                                                String correlationId,
                                                                                Pageable pageable)
        throws DeliveryOrderException {

        ResponseEntity<PagedModel<EntityModel<OrderDataFileDTO>>> availableFilesResponse = orderClient.getAvailableFilesInOrder(
            orderId,
            pageable);
        if (availableFilesResponse == null
            || !availableFilesResponse.getStatusCode().is2xxSuccessful()
            || availableFilesResponse.getBody() == null) {
            throw new DeliveryOrderException(String.format("Could not retrieve available files from "
                                                           + "delivery with correlation id '%s'. Got response '%s'.",
                                                           correlationId,
                                                           availableFilesResponse));
        }

        return availableFilesResponse.getBody();
    }

    /**
     * Download a page of files in the configured workspace.
     *
     * @param correlationId      unique identifier to monitor the request
     * @param pageAvailableFiles metadata containing information about the files to download
     * @param downloadWorkspace  where to download files
     * @return number of files downloaded
     * @throws DeliveryOrderException if files could not be downloaded.
     */
    private int downloadPageOfFiles(String correlationId,
                                    PagedModel<EntityModel<OrderDataFileDTO>> pageAvailableFiles,
                                    DeliveryDownloadWorkspaceManager downloadWorkspace) throws DeliveryOrderException {
        int nbDownloadedFiles = 0;
        Collection<EntityModel<OrderDataFileDTO>> availableFiles = pageAvailableFiles.getContent();
        LOGGER.debug("Starting downloading {} files for page {}.",
                     availableFiles.size(),
                     pageAvailableFiles.getMetadata());

        for (EntityModel<OrderDataFileDTO> availableFileModel : availableFiles) {
            OrderDataFileDTO availableFile = availableFileModel.getContent();
            if (availableFile == null) {
                throw new DeliveryOrderException(String.format("Could not extract available file from delivery with "
                                                               + "correlation id '%s'", correlationId));
            }
            downloadFile(availableFile, downloadWorkspace);
            nbDownloadedFiles++;
        }

        LOGGER.debug("{} files downloaded for page {}.", nbDownloadedFiles, pageAvailableFiles.getMetadata());
        return nbDownloadedFiles;
    }

    /**
     * Download a file in the configured workspace by streaming bytes from another accessible location.
     *
     * @param availableFile     metadata about the file to download
     * @param downloadWorkspace where to download the file
     */
    private void downloadFile(OrderDataFileDTO availableFile, DeliveryDownloadWorkspaceManager downloadWorkspace)
        throws DeliveryOrderException {
        LOGGER.trace("Starting downloading file with name '{}' and md5Checksum '{}'.",
                     availableFile.getFilename(),
                     availableFile.getChecksum());

        InputStreamResource fileStreamResource = getInputStreamResource(availableFile);
        Path downloadedPath = writeFileToWorkspace(availableFile, downloadWorkspace, fileStreamResource);

        LOGGER.trace("Successfully downloaded file with name '{}' and md5Checksum '{}' at '{}'.",
                     availableFile.getFilename(),
                     availableFile.getChecksum(),
                     downloadedPath);
    }

    /**
     * Get the file to download by requesting rs-order, which will return the file input stream.
     * Linked to {@link this#downloadFile}
     *
     * @param availableFile metadata about the file to stream
     */
    @NotNull
    private InputStreamResource getInputStreamResource(OrderDataFileDTO availableFile) throws DeliveryOrderException {
        ResponseEntity<InputStreamResource> inputStreamRes = dataFileClient.downloadFile(availableFile.getId());
        if (inputStreamRes == null
            || !inputStreamRes.getStatusCode().is2xxSuccessful()
            || inputStreamRes.getBody() == null) {
            throw new DeliveryOrderException(String.format("Could not retrieve file with name '%s' from order service"
                                                           + ". Got response '%s'.",
                                                           availableFile.getFilename(),
                                                           inputStreamRes));
        }
        return inputStreamRes.getBody();
    }

    /**
     * Write the file in a local directory provided by the delivery workspace manager.
     * Linked to {@link this#downloadFile}
     */
    private Path writeFileToWorkspace(OrderDataFileDTO availableFile,
                                      DeliveryDownloadWorkspaceManager deliveryWorkspace,
                                      InputStreamResource fileStreamResource) throws DeliveryOrderException {
        Path fileDownloadPath = deliveryWorkspace.getDownloadSubfolder()
                                                 .resolve(String.format(PRODUCT_FOLDER_PATTERN,
                                                                        availableFile.getProductId(),
                                                                        availableFile.getVersion()))
                                                 .resolve(availableFile.getFilename());
        try (InputStream fileInputStream = fileStreamResource.getInputStream()) {
            Files.createDirectories(fileDownloadPath.getParent());
            Files.createFile(fileDownloadPath);
            // copy the input stream to the file
            FileUtils.copyInputStreamToFile(fileInputStream, fileDownloadPath.toFile());
            checkFileIntegrity(availableFile, fileDownloadPath);
            return fileDownloadPath;
        } catch (IOException e) {
            throw new DeliveryOrderException(String.format("Could not write file with name '%s' at '%s'.",
                                                           availableFile.getFilename(),
                                                           fileDownloadPath), e);
        }
    }

    /**
     * Check the integrity of the file downloaded by comparing its md5 checksum with the one expected (provided by
     * the file reference).
     *
     * @param availableFile    metadata about the file to download
     * @param fileDownloadPath downloaded file
     * @throws DeliveryOrderException if checksums do not match
     */
    private void checkFileIntegrity(OrderDataFileDTO availableFile, Path fileDownloadPath)
        throws DeliveryOrderException {
        try {
            String computedMd5Checksum = ChecksumUtils.computeHexChecksum(fileDownloadPath, "MD5");
            if (!availableFile.getChecksum().equals(computedMd5Checksum)) {
                throw new DeliveryOrderException(String.format("Downloaded file MD5 checksum '%s' does not match "
                                                               + "the expected one '%s' (file with name '%s' located "
                                                               + "at '%s'). File transfer is considered incomplete.",
                                                               computedMd5Checksum,
                                                               availableFile.getChecksum(),
                                                               availableFile.getFilename(),
                                                               fileDownloadPath));
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new DeliveryOrderException(String.format("Could not compute MD5 checksum from file located at '%s'.",
                                                           fileDownloadPath), e);
        }
    }
}