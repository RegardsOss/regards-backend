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
package fr.cnes.regards.modules.ingest.service.aip;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.*;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestError;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.aip.StorageSize;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.dto.FileLocationDTO;
import fr.cnes.regards.modules.storage.domain.dto.FileReferenceDTO;
import fr.cnes.regards.modules.storage.domain.dto.FileReferenceMetaInfoDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Léo Mieulet
 * <p>
 * TODO : Handle security access for downloadable AIPs
 */
@Service
public class AIPStorageService implements IAIPStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPStorageService.class);

    public static final String AIPS_CONTROLLER_ROOT_PATH = "/aips";

    public static final String AIP_ID_PATH_PARAM = "aip_id";

    public static final String AIP_DOWNLOAD_PATH = "/{" + AIP_ID_PATH_PARAM + "}/download";

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${regards.ingest.aips.storage.location.subdirectory:AIPs}")
    private String apiStorageSubDirectory;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private IStorageClient storageClient;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Override
    public List<String> storeAIPFiles(IngestRequest request) throws ModuleException {
        // Build file storage requests
        Collection<FileStorageRequestDTO> filesToStore = new ArrayList<>();
        // Build file reference requests
        Collection<FileReferenceRequestDTO> filesToRefer = new ArrayList<>();

        List<StorageMetadata> storages = request.getMetadata().getStorages();
        // Check if request contains errors. If true retry error requests, else create new storage requests
        if (!request.isErrorInformation()) {
            // Iterate over AIPs
            for (AIPEntity aipEntity : request.getAips()) {
                AIP aip = aipEntity.getAip();
                // Iterate over Data Objects
                for (ContentInformation contentInformation : aip.getProperties().getContentInformations()) {
                    dispatchOAISDataObjectForStorage(contentInformation,
                                                     aipEntity,
                                                     storages,
                                                     filesToStore,
                                                     filesToRefer);
                }
            }
        } else {
            // Iterate over list of errors
            for (IngestRequestError error : request.getErrorInformation()) {
                // Get the storage in error in the available list of storage in request
                StorageMetadata storageError = storages.stream()
                                                       .filter(storage -> storage.getPluginBusinessId()
                                                                                 .equals(error.getRequestStorage()))
                                                       .findFirst()
                                                       .orElseThrow(() -> new IllegalArgumentException(
                                                           "List of storage in ingest request doesn't contain the request storage in error : "
                                                           + error.getRequestStorage()));
                // Iterate over AIPs
                for (AIPEntity aipEntity : request.getAips()) {
                    AIP aip = aipEntity.getAip();
                    // Iterate over Data Objects
                    for (ContentInformation contentInformation : aip.getProperties().getContentInformations()) {
                        dispatchOAISDataObjectForStorageInError(error,
                                                                contentInformation,
                                                                aipEntity,
                                                                storageError,
                                                                filesToStore,
                                                                filesToRefer);
                    }
                }
            }
            // Clear errors after processed them
            request.clearErrorInformation();
        }
        // Keep reference to requests sent to Storage
        List<String> remoteStepGroupIds = new ArrayList<>();
        // Send storage request
        if (!filesToStore.isEmpty()) {
            remoteStepGroupIds.addAll(storageClient.store(filesToStore).stream().map(RequestInfo::getGroupId).toList());
        }
        // Send reference request
        if (!filesToRefer.isEmpty()) {
            remoteStepGroupIds.addAll(storageClient.reference(filesToRefer)
                                                   .stream()
                                                   .map(RequestInfo::getGroupId)
                                                   .toList());
        }
        return remoteStepGroupIds;
    }

    private FileStorageRequestDTO createFileStorageRequestDTO(OAISDataObject dataObject,
                                                              RepresentationInformation representationInformation,
                                                              AIPEntity aipEntity,
                                                              OAISDataObjectLocation location,
                                                              StorageMetadata storage) {
        FileStorageRequestDTO storageRequest = FileStorageRequestDTO.build(dataObject.getFilename(),
                                                                           dataObject.getChecksum(),
                                                                           dataObject.getAlgorithm(),
                                                                           representationInformation.getSyntax()
                                                                                                    .getMimeType()
                                                                                                    .toString(),
                                                                           aipEntity.getAip().getId().toString(),
                                                                           aipEntity.getSessionOwner(),
                                                                           aipEntity.getSession(),
                                                                           location.getUrl(),
                                                                           storage.getPluginBusinessId(),
                                                                           Optional.ofNullable(storage.getStorePath()));
        storageRequest.withType(dataObject.getRegardsDataType().toString());

        return storageRequest;
    }

    private FileReferenceRequestDTO createFileReferenceRequestDTO(OAISDataObject dataObject,
                                                                  RepresentationInformation representationInformation,
                                                                  AIPEntity aipEntity,
                                                                  OAISDataObjectLocation location)
        throws ModuleException {
        validateForReference(dataObject);

        FileReferenceRequestDTO referenceRequest = FileReferenceRequestDTO.build(dataObject.getFilename(),
                                                                                 dataObject.getChecksum(),
                                                                                 dataObject.getAlgorithm(),
                                                                                 representationInformation.getSyntax()
                                                                                                          .getMimeType()
                                                                                                          .toString(),
                                                                                 dataObject.getFileSize(),
                                                                                 aipEntity.getAip().getId().toString(),
                                                                                 location.getStorage(),
                                                                                 location.getUrl(),
                                                                                 aipEntity.getSessionOwner(),
                                                                                 aipEntity.getSession());
        referenceRequest.withType(dataObject.getRegardsDataType().toString());

        return referenceRequest;
    }

    /**
     * Dispatch for the given {@link ContentInformation} the files to store and the files to reference on storage microservice when an error occurs (useful for the Retry action by user).
     * Update the list of files to store or the list of files to reference.
     *
     * @param error              {@link IngestRequestError}
     * @param contentInformation {@link ContentInformation}
     * @param aipEntity          {@link AIPEntity} associated to the {@link ContentInformation}
     * @param storageError       storage in error where to store/reference files
     * @param filesToStore       dispatched files to store
     * @param filesToRefer       dispatched files to reference
     * @throws ModuleException
     */
    private void dispatchOAISDataObjectForStorageInError(IngestRequestError error,
                                                         ContentInformation contentInformation,
                                                         AIPEntity aipEntity,
                                                         StorageMetadata storageError,
                                                         Collection<FileStorageRequestDTO> filesToStore,
                                                         Collection<FileReferenceRequestDTO> filesToRefer)
        throws ModuleException {
        OAISDataObject dataObject = contentInformation.getDataObject();
        if (dataObject == null) {
            return;
        }
        // Check the checksum of file in error
        if (error.getRequestFileChecksum().equals(dataObject.getChecksum())) {
            RepresentationInformation representationInformation = contentInformation.getRepresentationInformation();

            for (OAISDataObjectLocation location : dataObject.getLocations()) {
                // Check if error occured during the storage of file ou the referencing of file
                switch (error.getStorageType()) {
                    case STORED_FILE:
                        filesToStore.add(createFileStorageRequestDTO(dataObject,
                                                                     representationInformation,
                                                                     aipEntity,
                                                                     location,
                                                                     storageError));
                        break;
                    case REFERENCED_FILE:
                        filesToRefer.add(createFileReferenceRequestDTO(dataObject,
                                                                       representationInformation,
                                                                       aipEntity,
                                                                       location));
                        break;
                    default:
                        throw new IllegalArgumentException("Ingest request error doesn't contain a known storage type :"
                                                           + error.getStorageType());
                }
            }
        }
    }

    /**
     * Dispatch for the given {@link ContentInformation} the files to store and the files to reference on storage microservice.
     * Update the list of files to store or the list of files to reference.
     *
     * @param contentInformation {@link ContentInformation}
     * @param aipEntity          {@link AIPEntity} associated to the {@link ContentInformation}
     * @param requestedStorages  storages where to store/reference files
     * @param filesToStore       dispatched files to store
     * @param filesToRefer       dispatched files to reference
     * @throws ModuleException
     */
    private void dispatchOAISDataObjectForStorage(ContentInformation contentInformation,
                                                  AIPEntity aipEntity,
                                                  List<StorageMetadata> requestedStorages,
                                                  Collection<FileStorageRequestDTO> filesToStore,
                                                  Collection<FileReferenceRequestDTO> filesToRefer)
        throws ModuleException {
        OAISDataObject dataObject = contentInformation.getDataObject();
        if (dataObject == null) {
            return;
        }
        RepresentationInformation representationInformation = contentInformation.getRepresentationInformation();

        // At this step, locations can be in 2 situations
        // Either its storage is empty, which means it's a file that should be store on each storage location
        // Either its storage is defined, the file should only be referenced

        // Let's check if the AIP is correct, with at least 1 location of file to store/refer
        if (dataObject.getLocations().isEmpty()) {
            throw new ModuleException(String.format(
                "No location provided in the AIP (location of dataobject empty) for aip id[%s]",
                aipEntity.getAipId()));
        }
        // Check if the AIP have only one location to store
        long nbLocationWithNoStorage = dataObject.getLocations().stream().filter(l -> l.getStorage() == null).count();
        if (nbLocationWithNoStorage > 1) {
            throw new ModuleException(String.format("Too many files to store in a single dataobject for aip id[%s]",
                                                    aipEntity.getAipId()));
        }

        for (OAISDataObjectLocation location : dataObject.getLocations()) {
            // Check : should be store on each storage location or should only be referenced
            if (location.getStorage() == null) {
                for (StorageMetadata storage : getDistinctStorageForDataObject(dataObject, requestedStorages)) {
                    LOGGER.debug("New storage request for file={} and storage={}",
                                 dataObject.getFilename(),
                                 storage.getPluginBusinessId());
                    filesToStore.add(createFileStorageRequestDTO(dataObject,
                                                                 representationInformation,
                                                                 aipEntity,
                                                                 location,
                                                                 storage));
                }
            } else {
                filesToRefer.add(createFileReferenceRequestDTO(dataObject,
                                                               representationInformation,
                                                               aipEntity,
                                                               location));
            }
        }
    }

    /**
     * Calculates distinct storage destination for the given feature file (or data object).
     * Check into all given available {@link StorageMetadata} to find distinct ones to match the file to store.
     * Note :{@link StorageMetadata} unique identifier is the businessId.
     *
     * @param dataObject {@link OAISDataObject} data object do caluclate distinct storage destination
     * @param storages   {@link StorageMetadata}s available storage destinations cofiguration.
     * @return distinct {@link StorageMetadata}s
     */
    private List<StorageMetadata> getDistinctStorageForDataObject(OAISDataObject dataObject,
                                                                  List<StorageMetadata> storages) {
        return storages.stream().filter(s -> matchStorage(s, dataObject)).distinct().toList();
    }

    /**
     * Determine if the dataObject should be stored on the storage location. There are two conditions :
     * <ul>
     *     <li>the storage accepts all types or the target dataObject type.</li>
     *     <li>if the storage size is provided, the file size should be included in the limits.</li>
     * </ul>
     *
     * @param storage    metadata about the storage location
     * @param dataObject file to store
     */
    private boolean matchStorage(StorageMetadata storage, OAISDataObject dataObject) {
        // targetTypes empty = this storage accepts all types
        boolean isMatch = storage.getTargetTypes().isEmpty() || storage.getTargetTypes()
                                                                       .contains(dataObject.getRegardsDataType());
        StorageSize storageAcceptedSize = storage.getSize();
        if (storageAcceptedSize != null && dataObject.getFileSize() != null) {
            if (storageAcceptedSize.getMin() != null) {
                isMatch &= dataObject.getFileSize() >= storageAcceptedSize.getMin();
            }
            if (storageAcceptedSize.getMax() != null) {
                isMatch &= dataObject.getFileSize() <= storageAcceptedSize.getMax();
            }
        } else if (storageAcceptedSize != null && dataObject.getFileSize() == null) {
            isMatch = false;
        }
        return isMatch;
    }

    /**
     * @param dataObject
     * @throws ModuleException
     */
    private void validateForReference(OAISDataObject dataObject) throws ModuleException {
        Set<String> errors = Sets.newHashSet();
        if ((dataObject.getAlgorithm() == null) || dataObject.getAlgorithm().isEmpty()) {
            errors.add("Invalid checksum algorithm");
        }
        if ((dataObject.getChecksum() == null) || dataObject.getChecksum().isEmpty()) {
            errors.add("Invalid checksum");
        }
        if (dataObject.getFileSize() == null) {
            errors.add("Invalid filesize");
        }
        if ((dataObject.getFilename() == null) || dataObject.getFilename().isEmpty()) {
            errors.add("Invalid filename");
        }
        if (!errors.isEmpty()) {
            throw new ModuleException(String.format("Invalid entity {}. Information are missing : %s",
                                                    String.join(", ", errors)));
        }
    }

    @Override
    public void updateAIPsContentInfosAndLocations(List<AIPEntity> aips,
                                                   Collection<RequestResultInfoDTO> storeRequestInfos) {

        // Iterate over AIPs
        for (AIPEntity aipEntity : aips) {
            // Filter ResultInfos for the current aip to handle
            Set<RequestResultInfoDTO> aipRequests = storeRequestInfos.stream()
                                                                     .filter(r -> r.getRequestOwners()
                                                                                   .contains(aipEntity.getAipId()))
                                                                     .collect(Collectors.toSet());
            // Iterate over AIP data objects
            List<ContentInformation> contentInfos = aipEntity.getAip().getProperties().getContentInformations();
            for (ContentInformation ci : contentInfos) {
                OAISDataObject dataObject = ci.getDataObject();

                // Filter the request result list to only keep whose referring to the current data object
                Set<RequestResultInfoDTO> storeRequestInfosForCurrentAIP = aipRequests.stream()
                                                                                      .filter(r -> r.getRequestChecksum()
                                                                                                    .equals(dataObject.getChecksum()))
                                                                                      .collect(Collectors.toSet());

                // Iterate over request results
                for (RequestResultInfoDTO storeRequestInfo : storeRequestInfosForCurrentAIP) {
                    FileReferenceDTO resultFile = storeRequestInfo.getResultFile();
                    FileReferenceMetaInfoDTO metaInfo = resultFile.getMetaInfo();
                    FileLocationDTO fileLocation = resultFile.getLocation();
                    // Update AIP data object metas
                    dataObject.setFileSize(metaInfo.getFileSize());
                    // It's safe to patch the checksum here
                    dataObject.setChecksum(metaInfo.getChecksum());
                    // Update representational info
                    if (metaInfo.getHeight() != null) {
                        ci.getRepresentationInformation().getSyntax().setHeight(metaInfo.getHeight().doubleValue());
                    }
                    if (metaInfo.getWidth() != null) {
                        ci.getRepresentationInformation().getSyntax().setWidth(metaInfo.getWidth().doubleValue());
                    }
                    ci.getRepresentationInformation().getSyntax().setMimeType(metaInfo.getMimeType());
                    // Exclude from the location list any null storage
                    Set<OAISDataObjectLocation> newLocations = dataObject.getLocations()
                                                                         .stream()
                                                                         .filter(l -> l.getStorage() != null)
                                                                         .collect(Collectors.toSet());
                    newLocations.add(OAISDataObjectLocation.build(fileLocation.getUrl(),
                                                                  storeRequestInfo.getRequestStorage(),
                                                                  storeRequestInfo.getRequestStorePath()));
                    dataObject.setLocations(newLocations);

                    // Ensure the AIP storage list is updated
                    aipEntity.getStorages().add(storeRequestInfo.getRequestStorage());

                    String eventMessage = String.format("Data file %s stored on %s at %s.",
                                                        metaInfo.getFileName(),
                                                        fileLocation.getStorage(),
                                                        fileLocation.getUrl());
                    aipEntity.getAip()
                             .withEvent(EventType.STORAGE.toString(), eventMessage, resultFile.getStorageDate());
                }
            }
        }
    }

    @Override
    public AIPUpdateResult addAIPLocations(AIPEntity aip, Collection<RequestResultInfoDTO> storeRequestInfos) {
        boolean aipEdited = false;
        boolean edited = false;
        // Iterate over events (we already know they concerns the provided aip)
        for (RequestResultInfoDTO eventInfo : storeRequestInfos) {
            String storageLocation = eventInfo.getRequestStorage();
            List<ContentInformation> contentInfos = aip.getAip().getProperties().getContentInformations();

            // Extract from aip the ContentInfo referenced by the event, otherwise it does not concern AIP files
            Optional<ContentInformation> ciOp = contentInfos.stream()
                                                            .filter(ci -> ci.getDataObject()
                                                                            .getChecksum()
                                                                            .equals(eventInfo.getRequestChecksum()))
                                                            .findFirst();
            if (ciOp.isPresent()) {

                ContentInformation ci = ciOp.get();

                // Ensure the AIP storage list contains this storage location
                aip.getStorages().add(storageLocation);

                // Check if the event storage location is not already existing in ContentInfo locations
                boolean dataObjectLocationExists = ci.getDataObject()
                                                     .getLocations()
                                                     .stream()
                                                     .anyMatch(l -> l.getStorage().equals(storageLocation));

                if (!dataObjectLocationExists) {
                    aipEdited = true;
                    edited = true;
                    // Add this new location to the ContentInfo locations list
                    ci.getDataObject()
                      .getLocations()
                      .add(OAISDataObjectLocation.build(eventInfo.getResultFile().getLocation().getUrl(),
                                                        storageLocation,
                                                        eventInfo.getRequestStorePath()));
                    aip.getAip()
                       .withEvent(EventType.UPDATE.toString(),
                                  String.format("File %s [%s] is now stored on %s at %s.",
                                                eventInfo.getResultFile().getMetaInfo().getFileName(),
                                                eventInfo.getResultFile().getMetaInfo().getChecksum(),
                                                storageLocation,
                                                eventInfo.getResultFile().getLocation().getUrl()));
                    LOGGER.debug("[AIP {}] New location {} for file {}",
                                 aip.getAipId(),
                                 storageLocation,
                                 eventInfo.getResultFile().getMetaInfo().getFileName());
                } else {
                    LOGGER.debug("[AIP {}] Location {} for file {} already exists",
                                 aip.getAipId(),
                                 storageLocation,
                                 ci.getDataObject().getFilename());
                }
            }
        }
        return AIPUpdateResult.build(edited, aipEdited);
    }

    @Override
    public AIPUpdateResult removeAIPLocations(AIPEntity aip, Collection<RequestResultInfoDTO> storeRequestInfos) {
        boolean aipEdited = false;
        boolean edited = false;
        // Iterate over events (we already know they concerns the provided aip)
        for (RequestResultInfoDTO eventInfo : storeRequestInfos) {
            String storageLocation = eventInfo.getRequestStorage();
            List<ContentInformation> contentInfos = aip.getAip().getProperties().getContentInformations();

            // Extract from aip the ContentInfo referenced by the event, otherwise it does not concern AIP files
            Optional<ContentInformation> ciOp = contentInfos.stream()
                                                            .filter(ci -> ci.getDataObject()
                                                                            .getChecksum()
                                                                            .equals(eventInfo.getRequestChecksum()))
                                                            .findFirst();
            if (ciOp.isPresent()) {
                ContentInformation ci = ciOp.get();

                // Check if the event storage location exists in ContentInfo locations
                boolean dataObjectLocationExists = ci.getDataObject()
                                                     .getLocations()
                                                     .stream()
                                                     .anyMatch(l -> l.getStorage().equals(storageLocation));

                if (dataObjectLocationExists) {
                    aipEdited = true;
                    edited = true;
                    // Remove the location from ContentInfo locations
                    Set<OAISDataObjectLocation> updatedDataObject = ci.getDataObject()
                                                                      .getLocations()
                                                                      .stream()
                                                                      .filter(l -> !l.getStorage()
                                                                                     .equals(storageLocation))
                                                                      .collect(Collectors.toSet());
                    ci.getDataObject().setLocations(updatedDataObject);
                    aip.getAip()
                       .withEvent(EventType.UPDATE.toString(),
                                  String.format("File %s [%s] is not stored anymore on %s.",
                                                eventInfo.getResultFile().getMetaInfo().getFileName(),
                                                eventInfo.getResultFile().getMetaInfo().getChecksum(),
                                                storageLocation));
                }

                // Check if the event storage location still appears in some file referenced by this AIP
                boolean shouldKeepStorage = contentInfos.stream()
                                                        .anyMatch(contentInfo -> contentInfo.getDataObject()
                                                                                            .getLocations()
                                                                                            .stream()
                                                                                            .anyMatch(loc -> loc.getStorage()
                                                                                                                .equals(
                                                                                                                    storageLocation)));

                // Remove the location from the storage list
                if (!shouldKeepStorage) {
                    edited = true;
                    Set<String> updatedStorages = aip.getStorages()
                                                     .stream()
                                                     .filter(s -> !s.equals(storageLocation))
                                                     .collect(Collectors.toSet());
                    aip.setStorages(updatedStorages);
                }
            }
        }
        return AIPUpdateResult.build(edited, aipEdited);
    }

    @Override
    public Collection<FileDeletionRequestDTO> removeStorages(AIPEntity aip, List<String> removedStorages) {

        // Build file reference requests
        Collection<FileDeletionRequestDTO> filesToRemove = new ArrayList<>();

        // Compute the new list of storage location (for files)
        Set<String> currentStorages = aip.getStorages();
        Set<String> newStorages = new HashSet<>();
        currentStorages.forEach(s -> {
            if (!removedStorages.contains(s)) {
                newStorages.add(s);
            }
        });
        // Check if some storage location have been removed
        if (newStorages.size() < currentStorages.size()) {
            // Update the list of storage location
            aip.setStorages(newStorages);

            // Iterate over Data Objects
            for (ContentInformation ci : aip.getAip().getProperties().getContentInformations()) {
                OAISDataObject dataObject = ci.getDataObject();
                Optional<OAISDataObjectLocation> locationToRemove = dataObject.getLocations()
                                                                              .stream()
                                                                              .filter(l -> removedStorages.contains(l.getStorage()))
                                                                              .findFirst();
                if (locationToRemove.isPresent()) {
                    OAISDataObjectLocation loc = locationToRemove.get();
                    LOGGER.debug("Removing location {} from dataObject {} of AIP provider id {}",
                                 loc.getStorage(),
                                 dataObject.getFilename(),
                                 aip.getProviderId());
                    filesToRemove.add(FileDeletionRequestDTO.build(dataObject.getChecksum(),
                                                                   loc.getStorage(),
                                                                   aip.getAipId(),
                                                                   aip.getSessionOwner(),
                                                                   aip.getSession(),
                                                                   false));
                    // Remove location from AIPs.
                    // If storage deletion fails, the deletion can be rerun manually from storage interface.
                    dataObject.getLocations().remove(loc);
                    aip.getAip()
                       .withEvent(EventType.STORAGE.toString(),
                                  String.format("All files stored on location %s have been removed", loc.getStorage()));
                }
            }
        }
        return filesToRemove;
    }

    /**
     * Generate a public download URL for the file associated to the given Checksum
     *
     * @param aipId aip id
     * @return a public URL to retrieve the AIP manifest
     * @throws ModuleException if the Eureka server is not reachable
     */
    public URL generateDownloadUrl(OaisUniformResourceName aipId, ServiceInstance instance) throws ModuleException {
        String host = instance.getUri().toString();
        String path = Paths.get(AIPS_CONTROLLER_ROOT_PATH, AIP_DOWNLOAD_PATH).toString();
        String p = path.toString().replace("{" + AIP_ID_PATH_PARAM + "}", aipId.toString());
        p = p.charAt(0) == '/' ? p.replaceFirst("/", "") : p;
        String urlStr = String.format("%s/%s?scope=%s", host, p, tenantResolver.getTenant());
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage(), e);
            throw new ModuleException(String.format(
                "Error generating AIP download url. Invalid calculated url %s. Cause : %s",
                urlStr,
                e.getMessage()), e);
        }
    }

}


