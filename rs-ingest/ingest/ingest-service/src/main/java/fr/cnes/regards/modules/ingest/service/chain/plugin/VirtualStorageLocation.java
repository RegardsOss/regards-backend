/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.chain.plugin;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.exception.PluginInitException;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.ingest.domain.plugin.IAIPStorageMetadataUpdate;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storage.domain.plugin.StorageType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Léo Mieulet
 */
@Plugin(author = "REGARDS Team", description = "VirtualStorageLocation updates AIP storage metadata, "
                                               + "to replace from request a virtual storage location into a"
                                               + " real list of storage location ", id = "VirtualStorageLocation",
    version = "1.0.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
    url = "https://regardsoss.github.io/")
public class VirtualStorageLocation implements IAIPStorageMetadataUpdate {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualStorageLocation.class);

    private static final String VIRTUAL_STORAGE_NAME = "virtual_storage_name";

    private static final String REAL_STORAGE_LOCATIONS = "real_storage_locations";

    @PluginParameter(name = VIRTUAL_STORAGE_NAME, label = "Virtual storage name", description =
        "The name of the storage location this plugin is configured to react on. "
        + "When detected inside an IngestRequest, this plugin replaces the virtual location by real locations (from plugin conf).")
    private String virtualStorageName;

    @PluginParameter(name = REAL_STORAGE_LOCATIONS, label = "Real storage locations", description =
        "Real storage locations we add inside an Ingest Request when the virtual storage is present. "
        + "Allowed target types are: RAWDATA, QUICKLOOK_SD, QUICKLOOK_MD, QUICKLOOK_HD, DOCUMENT, THUMBNAIL, OTHER, AIP, DESCRIPTION")
    private Set<StorageMetadata> realStorageLocations;

    @Autowired
    private IStorageRestClient storageRestClient;

    /**
     * Plugin init method
     */
    @PluginInit
    public void init() throws PluginInitException {
        // validation
        validateRealStorageLocations();
        validateStorageLocationsUsingStorageMicroservice();
    }

    private void validateStorageLocationsUsingStorageMicroservice() throws PluginInitException {
        FeignSecurityManager.asSystem();
        ResponseEntity<List<EntityModel<StorageLocationDTO>>> response = storageRestClient.retrieve();
        if (!response.hasBody() || !response.getStatusCode().is2xxSuccessful()) {
            throw new PluginInitException("Failed to retrieve Storage location list from Storage microservice");
        } else {
            List<StorageLocationDTO> storageLocationList = HateoasUtils.unwrapList(response.getBody());
            validateVirtualStorageLocation(storageLocationList);
            validateRealStorageLocation(storageLocationList);
        }
    }

    private void validateRealStorageLocation(List<StorageLocationDTO> storageLocationList) throws PluginInitException {
        for (StorageMetadata realStorageLocation : realStorageLocations) {
            String realStorageLocationName = realStorageLocation.getPluginBusinessId();
            Optional<StorageLocationDTO> storageLocationDTO = storageLocationList.stream()
                                                                                 .filter(storageLocation -> realStorageLocationName.equals(
                                                                                     storageLocation.getName()))
                                                                                 .findAny();
            if (storageLocationDTO.isEmpty()
                || storageLocationDTO.get().getConfiguration().getStorageType() == StorageType.OFFLINE) {
                throw new PluginInitException(String.format(
                    "The storage location %s cannot be used as a real storage location: such storage location does not exist on Storage or is offline",
                    realStorageLocationName));
            }
        }
    }

    private void validateVirtualStorageLocation(List<StorageLocationDTO> storageLocationList)
        throws PluginInitException {
        boolean virtualStorageIsARealStorageLocation = storageLocationList.stream()
                                                                          .anyMatch(storageLocationDTO -> storageLocationDTO.getName()
                                                                                                                            .equals(
                                                                                                                                virtualStorageName));
        if (virtualStorageIsARealStorageLocation) {
            throw new PluginInitException(String.format(
                "The storage location named %s cannot be used as a virtual storage location as it already exists on Storage",
                virtualStorageName));
        }
    }

    /**
     * Throw a {@link PluginInitException} if {@link VirtualStorageLocation#realStorageLocations} does not handle all existing DataTypes
     */
    private void validateRealStorageLocations() throws PluginInitException {
        boolean storageLocationAcceptingAllDataTypes = realStorageLocations.stream()
                                                                           .anyMatch(storageLocation -> storageLocation.getTargetTypes()
                                                                                                                       .isEmpty());
        Set<DataType> targetTypes = realStorageLocations.stream()
                                                        .map(StorageMetadata::getTargetTypes)
                                                        .flatMap(Collection::stream)
                                                        .collect(Collectors.toSet());
        boolean storageLocationsContainsAllDataTypes = targetTypes.containsAll(Arrays.asList(DataType.values()));
        if (!storageLocationAcceptingAllDataTypes && !storageLocationsContainsAllDataTypes) {
            throw new PluginInitException("Invalid storage location configuration: some datatypes are not handled");
        }
    }

    @Override
    public Set<StorageMetadata> getStorageMetadata(Set<StorageMetadata> requestStorageMetadataList)
        throws ModuleException {
        Optional<StorageMetadata> virtualStorageMetadataOpt = requestStorageMetadataList.stream()
                                                                                        .filter(storageMetadata -> virtualStorageName.equals(
                                                                                            storageMetadata.getPluginBusinessId()))
                                                                                        .findAny();
        if (virtualStorageMetadataOpt.isPresent()) {
            // validation
            StorageMetadata virtualStorageMetadata = virtualStorageMetadataOpt.get();
            validateVirtualStorageMetadata(virtualStorageMetadata);
            validateNoDuplicateStorageMetadata(requestStorageMetadataList);

            // Take the list of StorageMetadata from the request and remove the virtual one
            List<StorageMetadata> realStorageLocationsFromRequest = requestStorageMetadataList.stream()
                                                                                              .filter(storageMetadata -> !virtualStorageName.equals(
                                                                                                  storageMetadata.getPluginBusinessId()))
                                                                                              .toList();
            // Create a list of StorageMetadata using the plugin conf and the storePath associated to the request.
            HashSet<StorageMetadata> newRequestStorageMetadataList = getRealStorageLocations(virtualStorageMetadata.getStorePath());
            newRequestStorageMetadataList.addAll(realStorageLocationsFromRequest);
            return newRequestStorageMetadataList;
        }
        return requestStorageMetadataList;
    }

    /**
     * Create storage metadata for current request that replaces the Virtual StorageMetadata.
     * If the virtual StorageMetadata from the request contains a storage path, we use it
     *
     * @param virtualStorageStorePath the request store path associated to the virtual storage metadata
     * @return the list of {@link StorageMetadata} to save inside the request
     */
    private HashSet<StorageMetadata> getRealStorageLocations(String virtualStorageStorePath) {
        HashSet<StorageMetadata> newRequestStorageMetadataList = Sets.newHashSet(realStorageLocations);
        if (StringUtils.isBlank(virtualStorageStorePath)) {
            for (StorageMetadata storageMetadata : newRequestStorageMetadataList) {
                storageMetadata.setStorePath(virtualStorageStorePath);
            }
        }
        return newRequestStorageMetadataList;
    }

    /**
     * Throw a {@link ModuleException} if we detect one of the storage metadata from the request contains a storage
     * location id that this plugin would like to add in the returned list of storage locations
     */
    private void validateNoDuplicateStorageMetadata(Set<StorageMetadata> requestStorageMetadataList)
        throws ModuleException {
        Set<String> conflictingStorageLocations = realStorageLocations.stream()
                                                                      .map(StorageMetadata::getPluginBusinessId)
                                                                      .filter(realStorageLocationName -> requestStorageMetadataList.stream()
                                                                                                                                   .anyMatch(
                                                                                                                                       requestStorageMetadata -> requestStorageMetadata.getPluginBusinessId()
                                                                                                                                                                                       .equals(
                                                                                                                                                                                           realStorageLocationName)))
                                                                      .collect(Collectors.toSet());
        if (!conflictingStorageLocations.isEmpty()) {
            String message = String.format("Invalid request : cannot add storage locations %s to the request "
                                           + "as this storage location identifier is already used in this plugin configuration",
                                           conflictingStorageLocations);
            LOGGER.warn(message);
            throw new ModuleException(message);
        }
    }

    /**
     * Throw a {@link ModuleException} if the virtual storage metadata coming from the request
     * defines some attributes like file size or target types. As we cannot use properly these values when
     * it's a virtual storage metadata inside the {@link IngestRequest}, we decided to block the request as it's a bad usage
     */
    private void validateVirtualStorageMetadata(StorageMetadata requestVirtualStorageMetadata) throws ModuleException {
        if (!requestVirtualStorageMetadata.getTargetTypes().isEmpty()
            || requestVirtualStorageMetadata.getSize() != null) {
            String message = String.format(
                "Invalid virtual storage metadata %s inside your request : targetTypes and/or file size must be empty or null",
                requestVirtualStorageMetadata.getPluginBusinessId());
            LOGGER.warn(message);
            throw new ModuleException(message);
        }
    }
}
