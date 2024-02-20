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

package fr.cnes.regards.modules.search.service.accessright;

import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.metadata.DataObjectGroup;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.search.service.AccessStatus;
import fr.cnes.regards.modules.search.service.LicenseAccessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Service containing access right verification to file, dataObject, or dataset, of the current user.
 *
 * @author tguillou
 */
@Service
public class DataAccessRightService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataAccessRightService.class);

    private static final List<DataType> PUBLIC_FILES_DATA_TYPES = Arrays.asList(DataType.QUICKLOOK_SD,
                                                                                DataType.QUICKLOOK_MD,
                                                                                DataType.QUICKLOOK_HD,
                                                                                DataType.THUMBNAIL,
                                                                                DataType.DESCRIPTION);

    /**
     * Service handling the access groups in criterion.
     */
    private final IAccessRightFilter accessRightFilter;

    private final LicenseAccessorService licenseAccessorService;

    public DataAccessRightService(IAccessRightFilter accessRightFilter, LicenseAccessorService licenseAccessorService) {
        this.accessRightFilter = accessRightFilter;
        this.licenseAccessorService = licenseAccessorService;
    }

    /**
     * Check if current user has right to access data objects of given dataset.
     * Given entity can be Dataset or DataObject
     *
     * @return either true or false
     */
    public boolean isDatasetDataObjectsAccessGranted(AbstractEntity<?> entity) throws AccessRightFilterException {
        Map<String, DataObjectGroup> datasetObjectsGroupsMap;
        final Set<String> userAccessGroups = accessRightFilter.getUserAccessGroups();
        if (userAccessGroups == null) {
            // access groups is null for admin users. Admin have always access
            return true;
        }
        if (entity instanceof Dataset dataset) {
            datasetObjectsGroupsMap = dataset.getMetadata().getDataObjectsGroups();
            List<DataObjectGroup> dataObjectGroups = userAccessGroups.stream()
                                                                     .filter(datasetObjectsGroupsMap::containsKey)
                                                                     .map(datasetObjectsGroupsMap::get)
                                                                     .toList();
            return dataObjectGroups.stream().anyMatch(DataObjectGroup::getDataObjectAccess);
        }
        return false;
    }

    /**
     * Check if current user has right to access content (files) of given entity.
     * If checksum is indicated, only this file is checked
     *
     * @param entity       can be Dataset or DataObject
     * @param fileChecksum an optional checksum of file of given dataObject to check access rights
     * @return either true or false
     */
    private AccessStatus checkContentAccess(AbstractEntity<?> entity, Optional<String> fileChecksum)
        throws AccessRightFilterException, ExecutionException {

        // First check if file is public or if license is not accepted by current user.
        if (fileChecksum.isPresent()) {
            if (!isPrivateFile(entity, fileChecksum.get())) {
                // If file is public access is granted
                return AccessStatus.GRANTED;
            } else if (!licenseAccessorService.currentUserHasAcceptedLicense()) {
                // If license is not accepted status is locked no matter what.
                return AccessStatus.LOCKED;
            }
        }

        // If file is private and license accepted check user groups for file access status
        final Set<String> userAccessGroups = accessRightFilter.getUserAccessGroups();
        if (userAccessGroups != null) {
            if (entity instanceof Dataset dataset) {
                return getContentAccessOfDataset(dataset, userAccessGroups);
            } else if (entity instanceof DataObject dataObject) {
                return getContentAccessOfDataObject(dataObject, userAccessGroups);
            } else {
                LOGGER.error("Not managed entity type " + entity.getClass());
                return AccessStatus.ERROR;
            }
        } else {
            // Access groups is null for admin users. Admin have always access
            return AccessStatus.GRANTED;
        }
    }

    /**
     * Check if the current logged in ser as access to the given file
     */
    public AccessStatus checkFileAccess(AbstractEntity<?> entity, String fileChecksum)
        throws AccessRightFilterException, ExecutionException {
        return checkContentAccess(entity, Optional.of(fileChecksum));
    }

    /**
     * Check if the current logged in ser as access to the given entity
     */
    public AccessStatus checkContentAccess(AbstractEntity<?> entity)
        throws AccessRightFilterException, ExecutionException {
        return checkContentAccess(entity, Optional.empty());
    }

    public List<DataObject> removeProductsWhereAccessRightNotGranted(List<DataObject> dataObjects) {
        return dataObjects.stream().filter(product -> {
            try {
                return checkContentAccess(product).isGranted();
            } catch (AccessRightFilterException | ExecutionException e) {
                LOGGER.error("Error while trying to calculate access rights of product with provideId {}",
                             product.getProviderId(),
                             e);
                return false;
            }
        }).toList();
    }

    /**
     * Check if file is not restricted to entity access rights
     */
    private boolean isPrivateFile(AbstractEntity<?> entity, String checksum) {
        return entity.getFiles()
                     .values()
                     .stream()
                     .filter(file -> file.getChecksum().equals(checksum))
                     .findFirst()
                     .map(DataFile::getDataType)
                     .filter(PUBLIC_FILES_DATA_TYPES::contains)
                     .isEmpty();
    }

    private AccessStatus getContentAccessOfDataObject(DataObject dataObject, Set<String> userAccessGroups) {
        boolean isAccessGranted = dataObject.getMetadata()
                                            .getGroupsAccessRightsMap()
                                            .entrySet()
                                            .stream()
                                            .anyMatch(entry -> userAccessGroups.contains(entry.getKey())
                                                               && entry.getValue());
        return isAccessGranted ? AccessStatus.GRANTED : AccessStatus.FORBIDDEN;
    }

    private AccessStatus getContentAccessOfDataset(Dataset dataset, Set<String> userAccessGroups) {
        Map<String, DataObjectGroup> datasetObjectsGroupsMap;
        datasetObjectsGroupsMap = dataset.getMetadata().getDataObjectsGroups();
        List<DataObjectGroup> dataObjectGroups = userAccessGroups.stream()
                                                                 .filter(datasetObjectsGroupsMap::containsKey)
                                                                 .map(datasetObjectsGroupsMap::get)
                                                                 .toList();
        boolean isDatasetAccessGranted = dataObjectGroups.stream()
                                                         .anyMatch(dataobjectGroup -> dataobjectGroup.getDataObjectAccess()
                                                                                      && dataobjectGroup.getDataFileAccess());
        return isDatasetAccessGranted ? AccessStatus.GRANTED : AccessStatus.FORBIDDEN;
    }
}
