/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationMetadataEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.dto.*;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.service.logger.FeatureLogger;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;
import fr.cnes.regards.modules.storage.domain.flow.ReferenceFlowItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to handle actions on {@link Feature} files
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FeatureFilesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureFilesService.class);

    private final IStorageClient storageClient;

    private final IFeatureEntityRepository featureRepository;

    public FeatureFilesService(IStorageClient storageClient, IFeatureEntityRepository featureRepository) {
        this.storageClient = storageClient;
        this.featureRepository = featureRepository;
    }

    /**
     * Handle files update between given {@link FeatureUpdateRequest} and given existing associated {@link FeatureEntity}.
     * If needed, this method sends storage or reference requests to storage microservice associated to the update
     * requested.
     *
     * @param request {@link FeatureUpdateRequest} update requested
     * @param entity  {@link FeatureEntity} associated feature
     * @throws ModuleException if error occurs sending requests to storage microservice
     */
    @Transactional(noRollbackFor = ModuleException.class)
    public void handleFeatureUpdateFiles(FeatureUpdateRequest request, FeatureEntity entity) throws ModuleException {
        if (!CollectionUtils.isEmpty(request.getFeature().getFiles())) {
            Set<FileReferenceRequestDTO> referenceRequests = Sets.newHashSet();
            Set<FileStorageRequestDTO> storageRequests = Sets.newHashSet();

            // For each file from feature update information, check if it's a new file or if the file already exists if there
            // is some new locations.
            for (FeatureFile fileToUpdate : request.getFeature().getFiles()) {
                List<StorageMetadata> storageLocations = new ArrayList<>();
                if (request.getMetadata() != null && !CollectionUtils.isEmpty(request.getMetadata().getStorages())) {
                    storageLocations = request.getMetadata().getStorages();
                }
                handleFeatureUpdateFile(entity,
                                        fileToUpdate,
                                        storageLocations,
                                        request.getRequestOwner(),
                                        referenceRequests,
                                        storageRequests);
            }

            if (!referenceRequests.isEmpty() && storageRequests.isEmpty()) {
                sendReferenceRequestsToStorage(request, referenceRequests);
            } else if (!storageRequests.isEmpty() && referenceRequests.isEmpty()) {
                sendStorageRequestsToStorage(request, storageRequests);
            } else if (!storageRequests.isEmpty() && !referenceRequests.isEmpty()) {
                throw new ModuleException(
                    "Update request cannot be handled as both storage and reference files are provided");
            }
        }
    }

    /**
     * Generate storage/reference requests if given {@link FeatureFile} does not exists in the
     * current {@link FeatureEntity} or if any location does exist.
     *
     * @param feature           {@link FeatureEntity} current feature to check for new file locations
     * @param fileToUpdate      {@link FeatureFile} new file to add to current feature
     * @param storageLocations  {@link String}s storage locations to store new files
     * @param requestOwner      {@link String} update request owner
     * @param referenceRequests {@link FileReferenceRequestDTO}s containing all reference requests to send to storage
     * @param storageRequests   {@link FileStorageRequestDTO}s containing all storage requests to send to storage
     */
    private void handleFeatureUpdateFile(FeatureEntity feature,
                                         FeatureFile fileToUpdate,
                                         List<StorageMetadata> storageLocations,
                                         String requestOwner,
                                         Set<FileReferenceRequestDTO> referenceRequests,
                                         Set<FileStorageRequestDTO> storageRequests) {

        // Check if file to update in given feature is a new file or if it contains a new location.
        Optional<FeatureFile> existingFile = fileAlreadyExists(fileToUpdate, feature);

        Set<FeatureFileLocation> newLocations;
        if (existingFile.isPresent()) {
            // Update feature with new file locations for the existing file
            newLocations = getNewLocations(fileToUpdate, existingFile.get(), storageLocations);
            existingFile.get().getLocations().addAll(newLocations);
        } else {
            // Update feature with new file and its locations
            // If new location does not contains storage name, storageLocations is mandatory
            newLocations = fileToUpdate.getLocations()
                                       .stream()
                                       .filter(l -> l.getStorage() != null
                                                    || !CollectionUtils.isEmpty(storageLocations))
                                       .collect(Collectors.toSet());
        }

        FeatureFileAttributes attributes = fileToUpdate.getAttributes();
        // For each new locations, create the associated storage requests (reference or storage)
        for (FeatureFileLocation loc : newLocations) {
            if (loc.getStorage() != null) {
                referenceRequests.add(FileReferenceRequestDTO.build(attributes.getFilename(),
                                                                    attributes.getChecksum(),
                                                                    attributes.getAlgorithm(),
                                                                    attributes.getMimeType().toString(),
                                                                    attributes.getFilesize(),
                                                                    feature.getUrn().toString(),
                                                                    loc.getStorage(),
                                                                    loc.getUrl(),
                                                                    requestOwner,
                                                                    feature.getSession()));
            } else {
                // No storage location, means that we have to store the given file from local url
                // to given storageLocations
                for (StorageMetadata storage : storageLocations) {
                    // Create one storage request for each storageLocation
                    storageRequests.add(FileStorageRequestDTO.build(attributes.getFilename(),
                                                                    attributes.getChecksum(),
                                                                    attributes.getAlgorithm(),
                                                                    attributes.getMimeType().toString(),
                                                                    feature.getUrn().toString(),
                                                                    requestOwner,
                                                                    feature.getSession(),
                                                                    loc.getUrl(),
                                                                    storage.getPluginBusinessId(),
                                                                    Optional.ofNullable(storage.getStorePath())));
                }
            }
        }
    }

    /**
     * Calculate new {@link FeatureFileLocation}s by comparing two given {@link FeatureFile}s.
     *
     * @param newFile     new {@link FeatureFile} to extract new locations from the other one.
     * @param file        existing {@link FeatureFile} to compare newFile with.
     * @param newStorages list of storage location for new files to store
     * @return Set<FeatureFileLocation>
     */
    private Set<FeatureFileLocation> getNewLocations(FeatureFile newFile,
                                                     FeatureFile file,
                                                     List<StorageMetadata> newStorages) {
        Set<FeatureFileLocation> newLocations = Sets.newHashSet();
        for (FeatureFileLocation newFileLocation : newFile.getLocations()) {
            // For each file location to update, compare with the original feature files if location is a new location.
            boolean isNewLocation = file.getLocations().stream().anyMatch(location -> {
                if (newFileLocation.getStorage() == null) {
                    if (newStorages.isEmpty()) {
                        // No new location as is no new destination storages
                        return false;
                    } else {
                        // A new location storage null, means new storage requests for all given new storages
                        // Location. So check if one of the new storages is not already associated to the current file.
                        return newStorages.stream()
                                          .map(StorageMetadata::getPluginBusinessId)
                                          .noneMatch(storage -> storage.equals(location.getStorage()));
                    }
                } else {
                    // A new location not null means a reference request, so only check if the given storage is not
                    // already associated to the current file
                    return !location.getStorage().equals(newFileLocation.getStorage());
                }
            });
            if (isNewLocation) {
                newLocations.add(newFileLocation);
            }
        }
        return newLocations;
    }

    /**
     * Check if {@link FeatureFile} match an existing file (by checksum comparaison) in the given {@link FeatureEntity}
     *
     * @param fileToCheck {@link FeatureFile} to check existence
     * @param entity      {@link FeatureEntity} to check file existence in.
     * @return FeatureFile if file is found.
     */
    private Optional<FeatureFile> fileAlreadyExists(FeatureFile fileToCheck, FeatureEntity entity) {
        return entity.getFeature()
                     .getFiles()
                     .stream()
                     .filter(file -> file.getAttributes()
                                         .getChecksum()
                                         .equals(fileToCheck.getAttributes().getChecksum()))
                     .findFirst();
    }

    /**
     * Update given {@link FeatureEntity} files if needed by reading storage responses.
     *
     * @param originalFeature  {@link Feature} to update
     * @param storageResponses {@link RequestResultInfoDTO}s storage requests responses
     * @param requestedFiles   original request files
     * @return FeatureEntity updated (or not) feature
     */
    public FeatureEntity updateFeatureLocations(FeatureEntity originalFeature,
                                                List<RequestResultInfoDTO> storageResponses,
                                                List<FeatureFile> requestedFiles) {
        originalFeature.setLastUpdate(OffsetDateTime.now());
        // For each feature, handle each request info from storage
        for (RequestResultInfoDTO info : storageResponses) {
            String checksum = info.getResultFile().getMetaInfo().getChecksum();
            Optional<FeatureFile> oFileToUpdate = requestedFiles.stream()
                                                                .filter(f -> f.getAttributes()
                                                                              .getChecksum()
                                                                              .equals(checksum))
                                                                .findFirst();
            String newUrl = info.getResultFile().getLocation().getUrl();
            String newStorage = info.getResultFile().getLocation().getStorage();
            String filename = info.getResultFile().getMetaInfo().getFileName();
            Long newFileSize = info.getResultFile().getMetaInfo().getFileSize();
            String newAlgorithm = info.getResultFile().getMetaInfo().getAlgorithm();
            MimeType mimeType = info.getResultFile().getMetaInfo().getMimeType();
            DataType newDataType = DataType.parse(info.getResultFile().getMetaInfo().getType(), DataType.RAWDATA);
            // If file is present on request original files use specific metadata.
            // Else use the response metadata
            if (oFileToUpdate.isPresent()) {
                mimeType = oFileToUpdate.get().getAttributes().getMimeType();
                filename = oFileToUpdate.get().getAttributes().getFilename();
                newDataType = oFileToUpdate.get().getAttributes().getDataType();
            }
            boolean fileFound = false;
            for (FeatureFile file : originalFeature.getFeature().getFiles()) {
                // For each request info from storage, find associated file in the feature
                if (file.getAttributes().getChecksum().equals(checksum)) {
                    // If file is found in original feature, so it is a new location for and existing fil.
                    // Then update file location with new storage/location
                    updateFileLocation(file, newUrl, newStorage);
                    FeatureLogger.updateNewLocation(originalFeature.getFeature().getId(),
                                                    originalFeature.getFeature().getUrn(),
                                                    newStorage,
                                                    newUrl);
                    fileFound = true;
                }
            }
            // If file is not found in original feature, it is a new file so add it.
            if (!fileFound) {
                FeatureFileAttributes newFileAttributes = FeatureFileAttributes.build(newDataType,
                                                                                      mimeType,
                                                                                      filename,
                                                                                      newFileSize,
                                                                                      newAlgorithm,
                                                                                      checksum);
                originalFeature.getFeature()
                               .getFiles()
                               .add(FeatureFile.build(newFileAttributes,
                                                      FeatureFileLocation.build(newUrl, newStorage)));
                FeatureLogger.updateNewLocation(originalFeature.getFeature().getId(),
                                                originalFeature.getFeature().getUrn(),
                                                newStorage,
                                                newUrl);
            }
        }
        return originalFeature;
    }

    /**
     * Handle {@link FeatureCreationRequest} with files to be stored or referenced by storage microservice:
     * <ul>
     *     <li>No storage metadata at all -> feature files are to be referenced</li>
     *     <li>for each metadata without any data storage identifier specified -> feature files are to be stored</li>
     *     <li>for each metadata with a data storage identifier specified -> feature files are to be referenced</li>
     * </ul>
     *
     * @param fcr currently creating feature
     */
    public FeatureCreationRequest handleRequestFiles(FeatureCreationRequest fcr) {

        long subProcessStart = System.currentTimeMillis();

        Set<FileReferenceRequestDTO> referenceRequests = Sets.newHashSet();
        Set<FileStorageRequestDTO> storageRequests = Sets.newHashSet();

        for (FeatureFile file : fcr.getFeature().getFiles()) {
            FeatureFileAttributes attributes = file.getAttributes();
            for (FeatureFileLocation loc : file.getLocations()) {
                FeatureCreationMetadataEntity metadata = fcr.getMetadata();
                // there is no metadata but a file location so we will update reference
                if (!metadata.hasStorage()) {
                    referenceRequests.add(FileReferenceRequestDTO.build(attributes.getFilename(),
                                                                        attributes.getChecksum(),
                                                                        attributes.getAlgorithm(),
                                                                        attributes.getMimeType().toString(),
                                                                        attributes.getFilesize(),
                                                                        fcr.getFeature().getUrn().toString(),
                                                                        loc.getStorage(),
                                                                        loc.getUrl(),
                                                                        metadata.getSessionOwner(),
                                                                        metadata.getSession()));
                }
                for (StorageMetadata storageMetadata : metadata.getStorages()) {
                    if (loc.getStorage() == null) {
                        storageRequests.add(FileStorageRequestDTO.build(attributes.getFilename(),
                                                                        attributes.getChecksum(),
                                                                        attributes.getAlgorithm(),
                                                                        attributes.getMimeType().toString(),
                                                                        fcr.getFeature().getUrn().toString(),
                                                                        metadata.getSessionOwner(),
                                                                        metadata.getSession(),
                                                                        loc.getUrl(),
                                                                        storageMetadata.getPluginBusinessId(),
                                                                        Optional.ofNullable(storageMetadata.getStorePath())));
                    } else {
                        referenceRequests.add(FileReferenceRequestDTO.build(attributes.getFilename(),
                                                                            attributes.getChecksum(),
                                                                            attributes.getAlgorithm(),
                                                                            attributes.getMimeType().toString(),
                                                                            attributes.getFilesize(),
                                                                            fcr.getFeature().getUrn().toString(),
                                                                            loc.getStorage(),
                                                                            loc.getUrl(),
                                                                            metadata.getSessionOwner(),
                                                                            metadata.getSession()));
                    }
                }
            }
        }

        LOGGER.trace("------------->>> {} storage / {} ref requests calculated for creationRequest {} in {} ms",
                     storageRequests.size(),
                     referenceRequests.size(),
                     fcr.getRequestId(),
                     System.currentTimeMillis() - subProcessStart);

        try {
            if (!referenceRequests.isEmpty() && storageRequests.isEmpty()) {
                sendReferenceRequestsToStorage(fcr, referenceRequests);
            } else if (!storageRequests.isEmpty() && referenceRequests.isEmpty()) {
                sendStorageRequestsToStorage(fcr, storageRequests);
            } else {
                throw new ModuleException(
                    "Creation request cannot be handled as both storage and reference files are provided");
            }
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            fcr.setState(RequestState.ERROR);
            fcr.setStep(FeatureRequestStep.LOCAL_ERROR);
            fcr.addError(e.getMessage());
        }
        return fcr;
    }

    /**
     * Update {@link FeatureFile} locations to handle storage response for the given storage/url.
     * If the location already exists for the given storage, url is updated if needed.
     * If location does not exist, it is added.
     *
     * @param file       {@link FeatureFile} to update
     * @param newUrl     {@link String} new url from storage response
     * @param newStorage {@link String} storage location name from storage response
     * @return boolean True if file has been updated.
     */
    private boolean updateFileLocation(FeatureFile file, String newUrl, String newStorage) {
        boolean featureUpdated = false;
        // remove all locations without any storage defined. It will be replaced by the storage results
        file.getLocations().removeIf(location -> location.getStorage() == null);
        Optional<FeatureFileLocation> updatedLocation = file.getLocations()
                                                            .stream()
                                                            .filter(l -> l.getStorage().equals(newStorage))
                                                            .findFirst();
        if (updatedLocation.isPresent() && !updatedLocation.get().getUrl().equals(newUrl)) {
            updatedLocation.get().setUrl(newUrl);
            featureUpdated = true;
        } else if (!updatedLocation.isPresent()) {
            file.getLocations().add(FeatureFileLocation.build(newUrl, newStorage));
            featureUpdated = true;
        }
        return featureUpdated;
    }

    /**
     * Send the given requests to storage and update the associated request with storage request groupId and new status
     *
     * @param request               request to update
     * @param storageRequests{@link FileStorageRequestDTO}s to  send
     * @throws ModuleException thrown if too much storage requests needs to be sent
     */
    private void sendStorageRequestsToStorage(AbstractFeatureRequest request,
                                              Collection<FileStorageRequestDTO> storageRequests)
        throws ModuleException {
        if (storageRequests != null && !storageRequests.isEmpty()) {
            if (storageRequests.size() > ReferenceFlowItem.MAX_REQUEST_PER_GROUP) {
                throw new ModuleException(String.format("Error storing feature files. Too much files for a request (%s)",
                                                        storageRequests.size()));
            }
            request.setGroupId(storageClient.store(storageRequests)
                                            .stream()
                                            .findFirst()
                                            .map(RequestInfo::getGroupId)
                                            .orElse(null));
        }
        request.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
    }

    /**
     * Send the given requests to storage and update the associated request with reference request groupId and new status
     *
     * @param request                 Request to update
     * @param referenceRequests{@link FileStorageRequestDTO}s to  send
     * @throws ModuleException thrown if too much storage requests needs to be sent
     */
    private void sendReferenceRequestsToStorage(AbstractFeatureRequest request,
                                                Collection<FileReferenceRequestDTO> referenceRequests)
        throws ModuleException {
        if (referenceRequests != null && !referenceRequests.isEmpty()) {
            if (referenceRequests.size() > ReferenceFlowItem.MAX_REQUEST_PER_GROUP) {
                throw new ModuleException(String.format(
                    "Error referencing feature files. Too much files for a request (%s)",
                    referenceRequests.size()));
            }
            request.setGroupId(storageClient.reference(referenceRequests)
                                            .stream()
                                            .findFirst()
                                            .map(RequestInfo::getGroupId)
                                            .orElse(null));
        }
        request.setStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED);
    }

}
