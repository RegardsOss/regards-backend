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
package fr.cnes.regards.modules.search.service.restoration;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.service.ExceptionCauseEnum;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.search.service.SearchException;
import fr.cnes.regards.modules.search.service.accessright.DataAccessRightService;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service that contains methods to restore of files thanks to product identifier(s).
 *
 * @author Stephane Cortine
 */
@Service
public class FileRestoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRestoreService.class);

    @Value("${regards.catalog.restoration.request.product.bulk.limit:100}")
    private int maxBulkSize;

    @Value("${regards.catalog.restoration.availability.hours.limit:24}")
    private int availabilityHours;

    private final ICatalogSearchService catalogSearchService;

    private final IStorageClient storageClient;

    private final DataAccessRightService dataAccessRightService;

    public FileRestoreService(ICatalogSearchService catalogSearchService,
                              IStorageClient storageClient,
                              DataAccessRightService dataAccessRightService) {
        this.catalogSearchService = catalogSearchService;
        this.storageClient = storageClient;
        this.dataAccessRightService = dataAccessRightService;
    }

    public void restore(String productId) throws ModuleException {
        LOGGER.info("Asking to restore files of product with urn {}", productId);
        List<DataObject> dataObjects = findProductsByIds(Set.of(productId));
        if (dataObjects.isEmpty()) {
            throw new FileRestoreException(ExceptionCauseEnum.NOT_FOUND, "Product " + productId + " cannot be found");
        }
        // 2) Filter products where access rights to FILES is not granted
        dataObjects = dataAccessRightService.removeProductsWhereAccessRightNotGranted(dataObjects);
        if (dataObjects.isEmpty()) {
            throw new FileRestoreException(ExceptionCauseEnum.FORBIDDEN,
                                           "Current user has not access to product " + productId + ".");
        }
        // 3) Retrieve files of this product
        Collection<DataFile> allFilesOfProduct = dataObjects.get(0).getFiles().values();
        // 4) Get checksums for all these files
        Set<String> checksums = allFilesOfProduct.stream()
                                                 .filter(dataFile -> !dataFile.isOnline())
                                                 .map(DataFile::getChecksum)
                                                 .collect(Collectors.toSet());
        LOGGER.debug("Asking to restore files of product with checksums {}", checksums);
        storageClient.makeAvailable(checksums, availabilityHours);
    }

    public void restore(Set<String> productIds) throws ModuleException {
        validateInput(productIds);
        LOGGER.info("Asking to restore files of {} products with their urns", productIds.size());
        // 1) Find products from their URNs, and from GROUP access rights
        List<DataObject> dataObjects = findProductsByIds(productIds);
        // 2) Filter products where access rights to FILES is not granted
        dataObjects = dataAccessRightService.removeProductsWhereAccessRightNotGranted(dataObjects);
        // 3) Retrieve all files of these products with onLine = false
        Set<DataFile> allFilesOfAllProducts = dataObjects.stream()
                                                         .flatMap(dataObject -> dataObject.getFiles().values().stream())
                                                         .filter(dataFile -> !dataFile.isOnline())
                                                         .collect(Collectors.toSet());

        // to avoid flooding datalake, avoid sending request with more than configured elements number
        if (allFilesOfAllProducts.size() > maxBulkSize) {
            String errorMessage = String.format(
                "Too many files : Cannot compute more than %s files in once : found %s files",
                maxBulkSize,
                allFilesOfAllProducts.size());
            LOGGER.error(errorMessage);
            throw new FileRestoreException(ExceptionCauseEnum.TOO_MANY_FILES, errorMessage);
        }
        // 4) Get checksums for all these files
        Set<String> checksums = allFilesOfAllProducts.stream().map(DataFile::getChecksum).collect(Collectors.toSet());
        LOGGER.debug("Asking to restore files of products with checksums {}", checksums);
        storageClient.makeAvailable(checksums, availabilityHours);
    }

    private List<DataObject> findProductsByIds(Set<String> productIds)
        throws SearchException, OpenSearchUnknownParameter, EntityOperationForbiddenException, FileRestoreException {
        try {
            Set<UniformResourceName> urns = productIds.stream()
                                                      .map(UniformResourceName::fromString)
                                                      .collect(Collectors.toSet());
            return catalogSearchService.searchByUrnIn(urns);
        } catch (IllegalArgumentException e) {
            String msg = "One of many product identifiers don't respect URN format";
            LOGGER.error(msg, e);
            throw new FileRestoreException(ExceptionCauseEnum.BAD_REQUEST, msg);
        }
    }

    private void validateInput(Set<String> productIds) throws FileRestoreException {
        if (productIds.size() > maxBulkSize) {
            throw new FileRestoreException(ExceptionCauseEnum.TOO_MUCH_PRODUCTS,
                                           "A maximum of "
                                           + maxBulkSize
                                           + " product identifiers per call is allowed. This behaviour is to avoid flooding the "
                                           + "datalake");
        }
    }

}
