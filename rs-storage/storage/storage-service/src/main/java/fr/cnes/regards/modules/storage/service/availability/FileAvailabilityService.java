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
package fr.cnes.regards.modules.storage.service.availability;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.fileaccess.dto.availability.FileAvailabilityStatusDto;
import fr.cnes.regards.modules.fileaccess.dto.availability.FilesAvailabilityRequestDto;
import fr.cnes.regards.modules.fileaccess.dto.availability.NearlineFileStatusDto;
import fr.cnes.regards.modules.fileaccess.plugin.domain.INearlineStorageLocation;
import fr.cnes.regards.modules.storage.domain.database.CacheFile;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.service.cache.CacheService;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service that contains method utils to download, and other utils, on a S3 storage.
 *
 * @author Thomas GUILLOU
 **/
@Service
public class FileAvailabilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileAvailabilityService.class);

    @Value("${regards.storage.availability.request.product.bulk.limit:100}")
    private int maxBulkSize;

    private final FileReferenceService fileReferenceService;

    private final CacheService fileCacheService;

    private final StorageLocationConfigurationService storageConfigurationService;

    private final PluginService pluginService;

    public FileAvailabilityService(FileReferenceService fileReferenceService,
                                   CacheService fileCacheService,
                                   StorageLocationConfigurationService storageLocationConfigurationService,
                                   PluginService pluginService) {
        this.fileReferenceService = fileReferenceService;
        this.fileCacheService = fileCacheService;
        this.storageConfigurationService = storageLocationConfigurationService;
        this.pluginService = pluginService;
    }

    /**
     * Compute file availability for all input files.
     */
    public List<FileAvailabilityStatusDto> checkFileAvailability(FilesAvailabilityRequestDto filesAvailabilityRequestDto)
        throws EntityInvalidException {
        long start = Instant.now().toEpochMilli();
        validateRequest(filesAvailabilityRequestDto);
        List<FileAvailabilityStatusDto> fileAvailabilityResponse = new ArrayList<>();
        Set<String> allInputChecksums = filesAvailabilityRequestDto.getChecksums();
        LOGGER.info("Check availability of {} files", allInputChecksums.size());
        // firstly, get files in cache, and build response available for each
        fileAvailabilityResponse.addAll(buildAvailabilityStatusForCachedFiles(allInputChecksums));

        // secondly, get file references for remaining files (files not cached)
        List<String> remainingFileChecksums = getChecksumNotIncludeIn(allInputChecksums, fileAvailabilityResponse);
        Set<FileReference> fileReferences = fileReferenceService.search(remainingFileChecksums);
        // now build response for each file reference found
        fileAvailabilityResponse.addAll(buildAvailabilityStatusForFileReferences(fileReferences));
        // file not found in fileReference table is not managed (no availability status returned).
        LOGGER.info("[Availability Monitoring] Check availability request for {} files processed in {} ms",
                    fileReferences.size(),
                    Instant.now().toEpochMilli() - start);
        return fileAvailabilityResponse;
    }

    private void validateRequest(FilesAvailabilityRequestDto filesAvailabilityRequestDto)
        throws EntityInvalidException {
        if (filesAvailabilityRequestDto.getChecksums().size() > maxBulkSize) {
            throw new EntityInvalidException("A maximum of "
                                             + maxBulkSize
                                             + " products per call is allowed. This behaviour is to avoid flooding the datalake");
        }
    }

    public Collection<FileAvailabilityStatusDto> buildAvailabilityStatusForFileReferences(Set<FileReference> fileReferences) {
        Set<StorageLocationConfiguration> storagesUsed = searchAndComputeStoragesUsedOf(fileReferences);
        // retrieve storage configuration grouped by names
        Map<StorageType, List<FileReference>> fileReferenceGrouped = groupFileByStorageType(fileReferences,
                                                                                            storagesUsed);
        List<FileAvailabilityStatusDto> fileAvailabilities = new ArrayList<>();
        fileAvailabilities.addAll(manageAvailabilityStatusForOnlineFiles(fileReferenceGrouped.get(StorageType.ONLINE)));
        fileAvailabilities.addAll(manageAvailabilityStatusForNearlineFiles(fileReferenceGrouped.get(StorageType.NEARLINE),
                                                                           storagesUsed));
        fileAvailabilities.addAll(manageAvailabilityStatusForOfflineFiles(fileReferenceGrouped.get(StorageType.OFFLINE)));
        return fileAvailabilities;
    }

    private Set<StorageLocationConfiguration> searchAndComputeStoragesUsedOf(Set<FileReference> fileReferences) {
        // get storage name used for these files
        List<String> allStoragesReferenced = fileReferences.stream()
                                                           .map(FileReference::getLocation)
                                                           .map(FileLocation::getStorage)
                                                           .distinct()
                                                           .toList();
        // retrieve storages
        Set<StorageLocationConfiguration> storagesUsed = storageConfigurationService.searchByNames(allStoragesReferenced);
        if (allStoragesReferenced.size() != storagesUsed.size()) {
            List<String> absentStorages = new ArrayList<>(allStoragesReferenced);
            absentStorages.removeAll(storagesUsed.stream().map(StorageLocationConfiguration::getName).toList());
            LOGGER.warn("One or more storages used in availability requests are not present in the storage "
                        + "configurations : {}", absentStorages);
            LOGGER.debug("Storages extracted from fileReference : " + allStoragesReferenced);
            LOGGER.debug("Storages computed from storage repository (except for web) : " + storagesUsed);
        }
        return storagesUsed;
    }

    public List<FileAvailabilityStatusDto> manageAvailabilityStatusForNearlineFiles(List<FileReference> fileReferencesNearline,
                                                                                    Set<StorageLocationConfiguration> storageUsed) {
        long start = Instant.now().toEpochMilli();
        List<FileAvailabilityStatusDto> fileAvailabilities = new ArrayList<>();
        if (fileReferencesNearline.isEmpty()) {
            return fileAvailabilities;
        }
        Map<String, Optional<INearlineStorageLocation>> storagePluginsIndexed = instantiateAndIndexStoragePlugins(
            storageUsed);

        // separate nearlineConfirmed files and not nearlineConfirmed
        Map<Boolean, List<FileReference>> groups = fileReferencesNearline.stream()
                                                                         .collect(Collectors.groupingBy(FileReference::isNearlineConfirmed));
        // nearlineConfirmed to True means that file is stored in Tier3 and need to be restored before downloaded.
        // so that means the file is not available
        fileAvailabilities.addAll(groups.getOrDefault(Boolean.TRUE, List.of())
                                        .stream()
                                        .map(FileAvailabilityBuilder::buildNotAvailable)
                                        .toList());
        // for nearlineConfirmed to False or null, we don't know if file is store in Tier3, Tier2, or cache.
        // if a file is stored in Tier2 or cache, that means the file is available.
        // call the plugin to get this information.
        List<FileReference> filesToConfirm = groups.getOrDefault(Boolean.FALSE, List.of());
        fileAvailabilities.addAll(manageNotNearlineConfirmedFiles(filesToConfirm, storagePluginsIndexed));
        LOGGER.info("[Availability Monitoring] Manager availability for {} nearline files processed in {} ms",
                    fileReferencesNearline.size(),
                    Instant.now().toEpochMilli() - start);
        return fileAvailabilities;
    }

    private List<FileAvailabilityStatusDto> manageNotNearlineConfirmedFiles(List<FileReference> files,
                                                                            Map<String, Optional<INearlineStorageLocation>> storagePluginsIndexed) {

        List<FileAvailabilityStatusDto> results = new ArrayList<>();
        Map<Optional<INearlineStorageLocation>, List<FileReference>> filesByStorage = files.stream()
                                                                                           .collect(Collectors.groupingBy(
                                                                                               f -> (storagePluginsIndexed.get(
                                                                                                   f.getLocation()
                                                                                                    .getStorage()))));

        for (Map.Entry<Optional<INearlineStorageLocation>, List<FileReference>> entry : filesByStorage.entrySet()) {
            if (entry.getKey().isEmpty()) {
                LOGGER.warn("Trying to access to access unknown plugins : {}",
                            entry.getValue().stream().map(f -> f.getLocation().getStorage()).distinct().toList());
                results.addAll(entry.getValue().stream().map(FileAvailabilityBuilder::buildNotAvailable).toList());
            } else {
                results.addAll(processFilesAvailability(entry));

            }
        }
        return results;
    }

    private List<FileAvailabilityStatusDto> processFilesAvailability(Map.Entry<Optional<INearlineStorageLocation>, List<FileReference>> entry) {
        List<FileAvailabilityStatusDto> results = new ArrayList<>();
        Map<String, FileReference> checksumToFileMap = entry.getValue()
                                                            .stream()
                                                            .collect(Collectors.toMap(file -> file.getMetaInfo()
                                                                                                  .getChecksum(),
                                                                                      Function.identity()));

        try {
            List<NearlineFileStatusDto> filesAvailability = entry.getKey()
                                                                 .get()
                                                                 .checkAvailability(entry.getValue()
                                                                                         .stream()
                                                                                         .map(FileReference::toDtoWithoutOwners)
                                                                                         .toList());
            List<FileReference> filesToUpdate = new ArrayList<>();
            for (NearlineFileStatusDto availability : filesAvailability) {
                switch (availability.getAvailable()) {
                    case AVAILABLE:
                        results.add(FileAvailabilityBuilder.buildAvailable(availability.getChecksum(),
                                                                           availability.getExpirationDate()));
                        break;
                    case UNAVAILABLE:
                        // file is not available from storage, that means file is stored on T3 and need restoration
                        // that means file is now nearline.
                        FileReference fileReference = checksumToFileMap.get(availability.getChecksum());
                        fileReference.setNearlineConfirmed(true);
                        filesToUpdate.add(fileReference);
                        results.add(FileAvailabilityBuilder.buildNotAvailable(fileReference));
                        break;
                    case ERROR:
                        // There was an error, return notAvailable result but don't update the file reference in
                        // database
                        results.add(FileAvailabilityBuilder.buildNotAvailable(availability.getChecksum()));
                        break;
                }
            }
            fileReferenceService.storeAll(filesToUpdate);
        } catch (Exception e) {
            LOGGER.error("An error occurred while checking availability of files on storage : {}", entry.getKey(), e);
            results.addAll(entry.getValue().stream().map(FileAvailabilityBuilder::buildNotAvailable).toList());
        }
        return results;
    }

    public Collection<? extends FileAvailabilityStatusDto> manageAvailabilityStatusForOfflineFiles(List<FileReference> fileReferencesOffline) {
        return fileReferencesOffline.stream().map(FileAvailabilityBuilder::buildNotAvailable).toList();
    }

    public List<FileAvailabilityStatusDto> manageAvailabilityStatusForOnlineFiles(List<FileReference> fileReferencesOnline) {
        return fileReferencesOnline.stream().map(FileAvailabilityBuilder::buildAvailableWithoutExpiration).toList();
    }

    private List<String> getChecksumNotIncludeIn(Set<String> checksums,
                                                 List<FileAvailabilityStatusDto> checksumsToCheck) {
        List<String> checksumsToRemove = checksumsToCheck.stream().map(FileAvailabilityStatusDto::getChecksum).toList();
        return checksums.stream().filter(checksum -> !checksumsToRemove.contains(checksum)).toList();
    }

    /**
     * Build AvailabilityStatus for each file that is stored in cache. All cached files not expired are available.
     * Cached files expired are deleted.
     *
     * @param allInputChecksums input file checksums
     */
    private List<FileAvailabilityStatusDto> buildAvailabilityStatusForCachedFiles(Set<String> allInputChecksums) {
        Set<CacheFile> cacheFiles = fileCacheService.getCacheFiles(allInputChecksums);
        List<FileAvailabilityStatusDto> results = new ArrayList<>();
        for (CacheFile cacheFile : cacheFiles) {
            if (cacheFile.getExpirationDate().isBefore(OffsetDateTime.now())) {
                fileCacheService.delete(cacheFile);
                results.add(FileAvailabilityBuilder.buildNotAvailable(cacheFile));
            } else {
                results.add(FileAvailabilityBuilder.buildAvailable(cacheFile));
            }
        }
        return results;
    }

    /**
     * Group FileReference by their storage type.
     * If a file is stored in different storages, and only the file stored in the most priority storage is kep.
     */
    private Map<StorageType, List<FileReference>> groupFileByStorageType(Set<FileReference> fileReferenceSet,
                                                                         Set<StorageLocationConfiguration> storagesUsed) {
        Map<String, StorageLocationConfiguration> storagesUsedIndexed = indexStorageConfigurations(storagesUsed);
        // manage the special case of "web" storage.This storage is not real,
        // and all files in "web" are considered unavailable
        List<FileReference> fileReferences = fileReferenceSet.stream()
                                                             .filter(file -> !"web".equalsIgnoreCase(file.getLocation()
                                                                                                         .getStorage()))
                                                             .toList();
        // keep only files with the highest storage type priority
        fileReferences = sortFileReferenceByStoragePriority(fileReferences, storagesUsedIndexed);
        fileReferences = deleteDuplicatedFileReference(fileReferences);

        // init map with an empty list by storage type
        Map<StorageType, List<FileReference>> groupFileByStorageType = Arrays.stream(StorageType.values())
                                                                             .collect(Collectors.toMap(type -> type,
                                                                                                       type -> new ArrayList<>()));

        // proceed the grouping
        for (FileReference file : fileReferences) {
            String storageNameOfCurrentFile = file.getLocation().getStorage();
            StorageType storageTypeOfCurrentFile = storagesUsedIndexed.get(storageNameOfCurrentFile).getStorageType();
            groupFileByStorageType.get(storageTypeOfCurrentFile).add(file);
        }

        return groupFileByStorageType;
    }

    /**
     * Index StorageLocation by names.
     */
    private Map<String, StorageLocationConfiguration> indexStorageConfigurations(Set<StorageLocationConfiguration> storagesUsed) {
        // group them by name to easily retrieve a storage from their name
        return storagesUsed.stream()
                           .collect(Collectors.toMap(StorageLocationConfiguration::getName, storage -> storage));
    }

    /**
     * Get plugins and index them by their storage names.
     */
    private Map<String, Optional<INearlineStorageLocation>> instantiateAndIndexStoragePlugins(Set<StorageLocationConfiguration> storagesUsed) {
        // group them by name to easily retrieve a storage from their name
        Map<String, Optional<INearlineStorageLocation>> result = new HashMap<>();
        for (StorageLocationConfiguration storageLocationConfiguration : storagesUsed) {
            if (storageLocationConfiguration.getStorageType().equals(StorageType.NEARLINE)) {
                try {
                    INearlineStorageLocation plugin = pluginService.getPlugin(storageLocationConfiguration.getPluginConfiguration()
                                                                                                          .getBusinessId());
                    result.put(storageLocationConfiguration.getName(), Optional.of(plugin));
                } catch (ModuleException e) {
                    LOGGER.warn("Plugin {} not available", storageLocationConfiguration.getName(), e);
                    result.put(storageLocationConfiguration.getName(), Optional.empty());
                }
            }
        }
        return result;
    }

    /**
     * Walkthrough the input list and remove item that are already encountered
     * Due to sorting, if the same file is referenced to different storages, the file with most priority storage is kep
     */
    private List<FileReference> deleteDuplicatedFileReference(List<FileReference> fileReferencesSortedByPriority) {
        List<FileReference> fileReferenceResult = new ArrayList<>();
        for (FileReference fileReference : fileReferencesSortedByPriority) {
            boolean fileStoredInOtherStorage = fileReferenceResult.stream()
                                                                  .anyMatch(file -> file.getMetaInfo()
                                                                                        .getChecksum()
                                                                                        .equals(fileReference.getMetaInfo()
                                                                                                             .getChecksum()));
            if (!fileStoredInOtherStorage) {
                fileReferenceResult.add(fileReference);
            }
        }
        return fileReferenceResult;
    }

    /**
     * Sort file reference depending on their storage type priority, most priority first
     */
    private static List<FileReference> sortFileReferenceByStoragePriority(List<FileReference> fileReferences,
                                                                          Map<String, StorageLocationConfiguration> mapStoragesByName) {
        List<FileReference> fileReferencesAsList = new ArrayList<>(fileReferences);
        try {
            fileReferencesAsList.sort((o1, o2) -> {
                StorageType storageType1 = mapStoragesByName.get(o1.getLocation().getStorage()).getStorageType();
                StorageType storageType2 = mapStoragesByName.get(o2.getLocation().getStorage()).getStorageType();
                return storageType2.comparePriorityWith(storageType1);
            });
        } catch (NullPointerException e) {
            throw new RsRuntimeException("Error in storage configuration : ", e);
        }

        return fileReferencesAsList;
    }

}
