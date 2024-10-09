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
package fr.cnes.regards.modules.filecatalog.service.location;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Functions;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.modules.fileaccess.client.IStorageLocationConfigurationClient;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.fileaccess.dto.StorageLocationConfigurationDto;
import fr.cnes.regards.modules.fileaccess.dto.StorageRequestStatus;
import fr.cnes.regards.modules.filecatalog.dao.IStorageLocationRepository;
import fr.cnes.regards.modules.filecatalog.domain.StorageLocation;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationDto;
import fr.cnes.regards.modules.filecatalog.service.FileStorageRequestService;
import fr.cnes.regards.modules.filecatalog.service.request.FileDeletionRequestService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.cnes.regards.modules.filecatalog.service.FileCatalogErrorType.*;

/**
 * Service managing (crud) the {@link StorageLocation}s. {@link StorageLocationConfigurationDto}s are persisted in
 * file-access, the {@link IStorageLocationConfigurationClient} is used in this service to access the configurations.
 *
 * @author Thibaud Michaudel
 **/
@Service
public class StorageLocationService {

    protected final IStorageLocationConfigurationClient storageLocationConfigClient;

    protected final IStorageLocationRepository storageLocationRepository;

    private final FileDeletionRequestService fileDeletionRequestService;

    private final FileStorageRequestService fileStorageRequestService;

    private final Cache<String, StorageLocationConfigurationDto> storageLocationConfigurationCache;

    public StorageLocationService(IStorageLocationConfigurationClient storageLocationConfigClient,
                                  IStorageLocationRepository storageLocationRepository,
                                  FileDeletionRequestService fileDeletionRequestService,
                                  FileStorageRequestService fileStorageRequestService) {
        this.storageLocationConfigClient = storageLocationConfigClient;
        this.storageLocationRepository = storageLocationRepository;
        this.fileDeletionRequestService = fileDeletionRequestService;
        this.fileStorageRequestService = fileStorageRequestService;
        this.storageLocationConfigurationCache = Caffeine.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();
    }

    /**
     * Create a new StorageLocation and its StorageLocationConfiguration
     */
    @MultitenantTransactional
    public StorageLocationDto createStorageLocation(StorageLocationDto storageLocationDto) throws ModuleException {
        String alreadyExistsError = String.format(
            "[%s] Could not create storage config with name %s because it already exists",
            STORAGE_CONFIG_NOT_CREATED,
            storageLocationDto.getName());

        // Entity already exists in file-catalog
        if (storageLocationRepository.findByName(storageLocationDto.getName()).isPresent()) {
            throw new EntityAlreadyExistsException(alreadyExistsError);
        }

        ResponseEntity<EntityModel<StorageLocationConfigurationDto>> response = storageLocationConfigClient.createStorageLocationConfig(
            storageLocationDto.getConfiguration());
        // Received already exists in file-access
        if (response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT)) {
            throw new EntityAlreadyExistsException(alreadyExistsError);
        }
        // Received unknown error from file-access
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ModuleException(String.format("[%s] Could not create storage config with name %s. Got response: "
                                                    + "%s.",
                                                    STORAGE_CONFIG_NOT_CREATED,
                                                    storageLocationDto.getName(),
                                                    response));
        }
        StorageLocation storageLocation = new StorageLocation(storageLocationDto.getName());
        storageLocationRepository.save(storageLocation);
        StorageLocationConfigurationDto storageConfigDto = ResponseEntityUtils.extractContentOrThrow(response,
                                                                                                     String.format(
                                                                                                         "[%s] Could not extract storage config with name %s. Got response: "
                                                                                                         + "%s.",
                                                                                                         STORAGE_CONFIG_NOT_CREATED,
                                                                                                         storageLocationDto.getName(),
                                                                                                         response));
        storageLocationConfigurationCache.put(storageConfigDto.getName(), storageConfigDto);

        return StorageLocationDto.build(storageLocationDto.getName(), storageConfigDto);
    }

    /**
     * Find one StorageLocation using its name, does not retrieve the associated StorageLocationConfiguration
     */
    @MultitenantTransactional(readOnly = true)
    public StorageLocationDto findStorageLocationByName(String storageName) throws ModuleException {
        // Get storage location configs through feign
        StorageLocationConfigurationDto storageConfigDto = storageLocationConfigurationCache.getIfPresent(storageName);

        if (storageConfigDto == null) {
            // Get storage location configs through feign
            ResponseEntity<EntityModel<StorageLocationConfigurationDto>> response = storageLocationConfigClient.retrieveStorageLocationConfigByName(
                storageName);

            // Received not found from file access
            if (response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                throw new EntityNotFoundException("Storage location with name " + storageName + " not found");
            }

            // Received unknown error from file access
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ModuleException(String.format(
                    "[%s] Could not extract storage config with name %s. Got response: " + "%s.",
                    STORAGE_CONFIG_NOT_FOUND,
                    storageName,
                    response));
            }
            storageConfigDto = ResponseEntityUtils.extractContentOrThrow(response,
                                                                         String.format(
                                                                             "[Could not extract storage config with name %s. Got response: "
                                                                             + "%s.",
                                                                             storageName,
                                                                             response));
            storageLocationConfigurationCache.put(storageConfigDto.getName(), storageConfigDto);
        }

        // Get corresponding storage location
        StorageLocation storageLocation = storageLocationRepository.findByName(storageName)
                                                                   .orElseThrow(() -> new EntityNotFoundException(
                                                                       "Storage location with name "
                                                                       + storageName
                                                                       + " not found"));
        return getStorageLocationDto(storageName, storageConfigDto, storageLocation);
    }

    /**
     * Find all StorageLocations without the associated StorageLocationConfigurations
     */
    @MultitenantTransactional(readOnly = true)
    public List<StorageLocationDto> findAllStorageLocations() throws ModuleException {
        List<StorageLocation> locations = storageLocationRepository.findAll();

        // If any configuration is absent from the cache, retrieve them all to complete the cache
        if (!locations.stream()
                      .allMatch(location -> storageLocationConfigurationCache.getIfPresent(location.getName())
                                            != null)) {
            ResponseEntity<List<EntityModel<StorageLocationConfigurationDto>>> response = storageLocationConfigClient.retrieveAllStorageLocationConfigs();

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ModuleException(String.format("Could not extract all storages. Got response: %s", response));
            }
            List<StorageLocationConfigurationDto> configs = HateoasUtils.unwrapCollection(response.getBody());
            Map<String, StorageLocationConfigurationDto> configsMap = configs.stream()
                                                                             .collect(Collectors.toMap(
                                                                                 StorageLocationConfigurationDto::getName,
                                                                                 Functions.identity()));
            storageLocationConfigurationCache.putAll(configsMap);
        }

        return locations.stream()
                        .map(location -> getStorageLocationDto(location.getName(),
                                                               storageLocationConfigurationCache.getIfPresent(location.getName()),
                                                               location))
                        .toList();
    }

    /**
     * Delete the StorageLocation, its associated StorageLocationConfiguration and all requests on this StorageLocation
     */
    @MultitenantTransactional
    public void delete(String storageName) throws ModuleException {
        // delete associated storage location config
        ResponseEntity<Void> response = storageLocationConfigClient.deleteStorageLocationConfigByName(storageName);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ModuleException(String.format("[%s] Could not delete storage config "
                                                    + "with name %s. Got response: %s.",
                                                    STORAGE_CONFIG_NOT_DELETED,
                                                    storageName,
                                                    response));
        }
        storageLocationConfigurationCache.invalidate(storageName);
        // Delete information on storage locations
        storageLocationRepository.deleteByName(storageName);
        // Delete requests
        fileStorageRequestService.deleteByStorage(storageName, Optional.empty());
        fileDeletionRequestService.deleteByStorage(storageName, Optional.empty());
    }

    /**
     * Delete the requests (and only the requests) of the given StorageLocation
     */
    public void deleteRequests(String storageName,
                               FileRequestType type,
                               Optional<StorageRequestStatus> storageRequestStatus) {
        switch (type) {
            case DELETION -> {
                //FIXME TODO NeoStorage Lot 4
            }
            case STORAGE -> fileStorageRequestService.deleteByStorage(storageName, storageRequestStatus);
            default -> {
                // do nothing
            }
        }
    }

    private StorageLocationDto getStorageLocationDto(String storageName,
                                                     StorageLocationConfigurationDto storageConfigDto,
                                                     StorageLocation storageLocation) {
        return StorageLocationDto.build(storageName, storageConfigDto)
                                 .withFilesInformation(storageLocation.getNumberOfReferencedFiles(),
                                                       storageLocation.getNumberOfPendingFiles(),
                                                       storageLocation.getTotalSizeOfReferencedFilesInKo())
                                 .withErrorInformation(fileStorageRequestService.count(storageName,
                                                                                       StorageRequestStatus.ERROR),
                                                       fileDeletionRequestService.count(storageName,
                                                                                        FileRequestStatus.ERROR))
                                 .withRunningProcessesInformation(fileStorageRequestService.isStorageRunning(storageName),
                                                                  fileDeletionRequestService.isDeletionRunning(
                                                                      storageName),
                                                                  false)
                                 .withPendingActionRemaining(storageLocation.getPendingActionRemaining());

    }

    /**
     * Update the configuration of the given storage location.
     */
    @MultitenantTransactional
    public StorageLocationDto updateLocationConfiguration(String storageName, StorageLocationDto storageLocationDto)
        throws ModuleException {
        ResponseEntity<EntityModel<StorageLocationConfigurationDto>> response = storageLocationConfigClient.updateStorageLocationConfigByName(
            storageName,
            storageLocationDto.getConfiguration());
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ModuleException(String.format(
                "[%s] Update of storage location configuration with name '%s' failed.",
                STORAGE_CONFIG_NOT_UPDATED,
                storageName));
        }
        StorageLocationConfigurationDto updatedStorageConfigDto = ResponseEntityUtils.extractContentOrThrow(response,
                                                                                                            String.format(
                                                                                                                "[%s] Could not extract storage config with name %s. Got response: "
                                                                                                                + "%s.",
                                                                                                                STORAGE_CONFIG_NOT_UPDATED,
                                                                                                                storageName,
                                                                                                                response));
        storageLocationConfigurationCache.put(storageName, updatedStorageConfigDto);

        return StorageLocationDto.build(storageName, updatedStorageConfigDto);
    }

    /**
     * Deletes all files for the given storageName, sessionOwner and session
     */
    public void deleteFiles(String storageName, boolean forceDelete, String sessionOwner, String session) {
        //FIXME TODO NeoStorage Lot 4
    }

    /**
     * Retry all requests in error for the given storage location and the given request type.
     */
    public void retryErrors(String storageName, FileRequestType type) throws EntityOperationForbiddenException {
        switch (type) {
            case DELETION:
            case STORAGE:
                fileStorageRequestService.retryErrorsByStorage(storageName, type);
                break;
            case AVAILABILITY:
            case REFERENCE:
            default:
                throw new EntityOperationForbiddenException(storageName,
                                                            StorageLocation.class,
                                                            String.format("Retry for type %s is forbidden", type));
        }
    }

    /**
     * Retry all requests in error for the given source and session.
     */
    public void retryErrorsBySourceAndSession(String source, String session) {
        this.fileStorageRequestService.retryErrorsBySourceAndSession(source, session);
    }

    /**
     * Monitor all storage locations to calculate information about stored files.
     */
    public void monitorStorageLocations(boolean reset) {
        //FIXME TODO NeoStorage Lot 1
    }

    public void invalidateCache() {
        storageLocationConfigurationCache.invalidateAll();
    }

    public void invalidateCache(String storage) {
        storageLocationConfigurationCache.invalidate(storage);
    }
}
