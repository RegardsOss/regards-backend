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
package fr.cnes.regards.modules.search.service.availability;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import feign.FeignException;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.fileaccess.dto.availability.FileAvailabilityStatusDto;
import fr.cnes.regards.modules.fileaccess.dto.availability.FilesAvailabilityRequestDto;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.dto.availability.FilesAvailabilityResponseDto;
import fr.cnes.regards.modules.search.dto.availability.ProductFilesStatusDto;
import fr.cnes.regards.modules.search.service.CatalogSearchService;
import fr.cnes.regards.modules.search.service.ExceptionCauseEnum;
import fr.cnes.regards.modules.search.service.SearchException;
import fr.cnes.regards.modules.search.service.accessright.DataAccessRightService;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service that contains methods to check availability of files thanks to product identifier(s).
 *
 * @author Thomas GUILLOU
 **/
@Service
public class FileAvailabilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileAvailabilityService.class);

    private static final String ERROR_AVAILABILITY_MESSAGE = "Error occurred while asking file availability to storage service";

    @Value("${regards.catalog.availability.request.product.bulk.limit:100}")
    private int maxBulkSize;

    private final CatalogSearchService catalogSearchService;

    private final DataAccessRightService dataAccessRightService;

    private final IStorageRestClient storageClient;

    public FileAvailabilityService(CatalogSearchService catalogSearchService,
                                   DataAccessRightService dataAccessRightService,
                                   IStorageRestClient storageClient) {
        this.catalogSearchService = catalogSearchService;
        this.dataAccessRightService = dataAccessRightService;
        this.storageClient = storageClient;
    }

    /**
     * Compute availability of all files of the input product.
     */
    public ProductFilesStatusDto checkAvailability(String productId) throws ModuleException {
        LOGGER.info("Checking files availability of product with urn {}.", productId);
        // 1) Find products from their Ids, and from GROUP access rights
        List<DataObject> dataObjects = findProductsByIds(Set.of(productId));
        if (dataObjects.isEmpty()) {
            throw new FileAvailabilityException(ExceptionCauseEnum.NOT_FOUND,
                                                "Product " + productId + " cannot be found");
        }
        // 2) Filter products where access rights to FILES is not granted
        dataObjects = dataAccessRightService.removeProductsWhereAccessRightNotGranted(dataObjects);
        if (dataObjects.isEmpty()) {
            throw new FileAvailabilityException(ExceptionCauseEnum.FORBIDDEN,
                                                "Current user has not access to product " + productId + ".");
        }
        // 3) Retrieve files of this product
        Collection<DataFile> allFilesOfProduct = dataObjects.get(0).getFiles().values();
        // 4) get checksums for all these files and create availability request
        Set<String> checksums = allFilesOfProduct.stream().map(DataFile::getChecksum).collect(Collectors.toSet());
        FilesAvailabilityRequestDto availabilityRequest = new FilesAvailabilityRequestDto(checksums);
        // 5) send availability request to storage
        List<FileAvailabilityStatusDto> fileAvailabilityStatusDtos = sendAvailabilityRequestToStorage(
            availabilityRequest);
        // 6) build results from responses
        return new ProductFilesStatusDto(productId, fileAvailabilityStatusDtos);
    }

    /**
     * Compute availability of all files included in input products.
     * File availability is asked to storage service, and then catalog service group them by product.
     */
    public FilesAvailabilityResponseDto checkAvailability(Set<String> productIds) throws ModuleException {
        validateInput(productIds);
        if (productIds.isEmpty()) {
            return new FilesAvailabilityResponseDto(List.of());
        }
        LOGGER.info("Checking files availability of {} products", productIds.size());
        // 1) Find products from their Ids, and from GROUP access rights
        List<DataObject> dataObjects = findProductsByIds(productIds);
        // 2) Filter products where access rights to FILES is not granted
        dataObjects = dataAccessRightService.removeProductsWhereAccessRightNotGranted(dataObjects);
        // 3) Retrieve all files of these products
        Set<DataFile> allFilesOfAllProducts = dataObjects.stream()
                                                         .flatMap(dataObject -> dataObject.getFiles().values().stream())
                                                         .collect(Collectors.toSet());

        // to avoid flooding datalake, avoid sending request with more than configured elements number
        if (allFilesOfAllProducts.size() > maxBulkSize) {
            String errorMessage = String.format(
                "Too many files : Cannot compute more than %s files in once : found %s files",
                maxBulkSize,
                allFilesOfAllProducts.size());
            LOGGER.error(errorMessage);
            throw new FileAvailabilityException(ExceptionCauseEnum.TOO_MANY_FILES, errorMessage);
        }
        // 4) get checksums for all these files and create availability request
        Set<String> checksums = allFilesOfAllProducts.stream().map(DataFile::getChecksum).collect(Collectors.toSet());
        FilesAvailabilityRequestDto availabilityRequest = new FilesAvailabilityRequestDto(checksums);
        // 5) send availability request to storage
        List<FileAvailabilityStatusDto> fileAvailabilityStatusDtos = sendAvailabilityRequestToStorage(
            availabilityRequest);
        // 6) build results from responses
        return new FilesAvailabilityResponseDto(buildFilesAvailabilityResponses(dataObjects,
                                                                                fileAvailabilityStatusDtos));
    }

    private List<DataObject> findProductsByIds(Set<String> productIds)
        throws SearchException, OpenSearchUnknownParameter, EntityOperationForbiddenException,
        FileAvailabilityException {
        try {
            Set<UniformResourceName> urns = productIds.stream()
                                                      .map(UniformResourceName::fromString)
                                                      .collect(Collectors.toSet());
            return catalogSearchService.searchByUrnIn(urns);
        } catch (IllegalArgumentException e) {
            LOGGER.error("One of many productIds don't respect URN format", e);
            throw new FileAvailabilityException(ExceptionCauseEnum.BAD_REQUEST,
                                                "One of many productIds don't respect URN format");
        }
    }

    private List<FileAvailabilityStatusDto> sendAvailabilityRequestToStorage(FilesAvailabilityRequestDto availabilityRequest)
        throws ModuleException {
        ResponseEntity<List<FileAvailabilityStatusDto>> storageResponse;
        try {
            FeignSecurityManager.asSystem();
            storageResponse = storageClient.checkFileAvailability(availabilityRequest);
        } catch (HttpClientErrorException | HttpServerErrorException | FeignException e) {
            String msg = ERROR_AVAILABILITY_MESSAGE + " : access to rs-storage failed";
            LOGGER.error(msg, e);
            throw new ModuleException(msg, e);
        } finally {
            FeignSecurityManager.reset();
        }
        return ResponseEntityUtils.extractBodyOrThrow(storageResponse, ERROR_AVAILABILITY_MESSAGE);
    }

    /**
     * Check if the product list size doesn't exceed the max configured.
     */
    private void validateInput(Set<String> productIds) throws FileAvailabilityException {
        // Need to validate manually (without @Size of javax in payload dto) because size is configurable.
        if (productIds.size() > maxBulkSize) {
            throw new FileAvailabilityException(ExceptionCauseEnum.TOO_MUCH_PRODUCTS,
                                                "A maximum of "
                                                + maxBulkSize
                                                + " productIds per call is allowed. This behaviour is to avoid flooding the datalake");
        }
    }

    /**
     * Group file availability by their products.
     *
     * @param dataObjects                data returned by elastic search, use to map files to product
     * @param fileAvailabilityStatusDtos response of rs-storage, which contains availability of files
     */
    private static List<ProductFilesStatusDto> buildFilesAvailabilityResponses(List<DataObject> dataObjects,
                                                                               List<FileAvailabilityStatusDto> fileAvailabilityStatusDtos) {
        // Map<checksum, List<productId>> : a file can be in multiple product
        Multimap<String, String> groupFileChecksumToProductId = ArrayListMultimap.create();
        for (DataObject dataObject : dataObjects) {
            for (DataFile dataFile : dataObject.getFiles().values()) {
                groupFileChecksumToProductId.put(dataFile.getChecksum(), dataObject.getIpId().toString());
            }
        }

        // Map<productId, List<AvailabilityStatus>: group file availabilities from their productId
        Multimap<String, FileAvailabilityStatusDto> groupByProductId = ArrayListMultimap.create();
        for (FileAvailabilityStatusDto fileAvailabilityStatusDto : fileAvailabilityStatusDtos) {
            Collection<String> productIds = groupFileChecksumToProductId.get(fileAvailabilityStatusDto.getChecksum());
            for (String productId : productIds) {
                groupByProductId.put(productId, fileAvailabilityStatusDto);
            }
        }

        return groupByProductId.asMap()
                               .entrySet()
                               .stream()
                               .map(entry -> new ProductFilesStatusDto(entry.getKey(), entry.getValue()))
                               .toList();
    }
}
