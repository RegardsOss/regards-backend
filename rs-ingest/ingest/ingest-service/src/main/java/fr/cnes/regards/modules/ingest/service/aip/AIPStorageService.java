/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.framework.oais.RepresentationInformation;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.domain.request.manifest.StoreLocation;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.dto.FileLocationDTO;
import fr.cnes.regards.modules.storage.domain.dto.FileReferenceDTO;
import fr.cnes.regards.modules.storage.domain.dto.FileReferenceMetaInfoDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;

/**
 * @author Léo Mieulet
 *
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
    public List<String> storeAIPFiles(List<AIPEntity> aipEntities, IngestMetadata metadata) throws ModuleException {
        // Build file storage requests
        Collection<FileStorageRequestDTO> filesToStore = new ArrayList<>();

        // Build file reference requests
        Collection<FileReferenceRequestDTO> filesToRefer = new ArrayList<>();

        // Iterate over AIPs
        for (AIPEntity aipEntity : aipEntities) {
            AIP aip = aipEntity.getAip();
            Set<StorageMetadata> storages = metadata.getStorages();
            // Iterate over Data Objects
            for (ContentInformation ci : aip.getProperties().getContentInformations()) {
                dispatchOAISDataObjectForStorage(ci, aipEntity, storages, filesToStore, filesToRefer);
            }
        }
        // Keep reference to requests sent to Storage
        List<String> remoteStepGroupIds = new ArrayList<>();

        // Send storage request
        if (!filesToStore.isEmpty()) {
            Collection<RequestInfo> infos = storageClient.store(filesToStore);
            remoteStepGroupIds.addAll(infos.stream().map(RequestInfo::getGroupId).collect(Collectors.toList()));
        }
        // Send reference request
        if (!filesToRefer.isEmpty()) {
            Collection<RequestInfo> infos = storageClient.reference(filesToRefer);
            remoteStepGroupIds.addAll(infos.stream().map(RequestInfo::getGroupId).collect(Collectors.toList()));
        }
        return remoteStepGroupIds;
    }

    /**
     * Dispatch for the given {@link ContentInformation} the files to store and the files to reference on storage microservice.
     * @param contentInformation {@link ContentInformation}
     * @param aipEntity {@link AIPEntity} associated to the {@link ContentInformation}
     * @param requestedStorages storages where to store/reference files
     * @param filesToStore dispatched files to store
     * @param filesToRefer dispatched files to reference
     * @throws ModuleException
     */
    private void dispatchOAISDataObjectForStorage(ContentInformation contentInformation, AIPEntity aipEntity,
            Set<StorageMetadata> requestedStorages, Collection<FileStorageRequestDTO> filesToStore,
            Collection<FileReferenceRequestDTO> filesToRefer) throws ModuleException {
        OAISDataObject dataObject = contentInformation.getDataObject();
        RepresentationInformation representationInformation = contentInformation.getRepresentationInformation();
        AIP aip = aipEntity.getAip();
        if (dataObject != null) {
            // At this step, locations can be in 2 situations
            // Either its storage is empty, which means it's a file that should be store on each storage location
            // Either its storage is defined, the file should only be referenced

            // Let's check if the AIP is correct, with at least 1 location of file to store/refer
            if (dataObject.getLocations().isEmpty()) {
                throw new ModuleException(
                        String.format("No location provided in the AIP (location of dataobject empty) for aip id[%s]",
                                      aipEntity.getAipId()));
            }
            // Check if the AIP have only one location to store
            long nbLocationWithNoStorage = dataObject.getLocations().stream().filter(l -> l.getStorage() == null)
                    .count();
            if (nbLocationWithNoStorage > 1) {
                throw new ModuleException(String.format("Too many files to store in a single dataobject for aip id[%s]",
                                                        aipEntity.getAipId()));
            }

            for (OAISDataObjectLocation l : dataObject.getLocations()) {
                if (l.getStorage() == null) {
                    // Storage is empty for this dataobject, create a storage request for each storage
                    for (StorageMetadata storage : requestedStorages) {
                        // Check if this storage contains this target type or is empty, which means
                        // this storage accepts everything
                        if (storage.getTargetTypes().isEmpty()
                                || storage.getTargetTypes().contains(dataObject.getRegardsDataType())) {
                            FileStorageRequestDTO storageRequest = FileStorageRequestDTO
                                    .build(dataObject.getFilename(), dataObject.getChecksum(),
                                           dataObject.getAlgorithm(),
                                           representationInformation.getSyntax().getMimeType().toString(),
                                           aip.getId().toString(), l.getUrl(), storage.getPluginBusinessId(),
                                           Optional.ofNullable(storage.getStorePath()));
                            storageRequest.withType(dataObject.getRegardsDataType().toString());
                            filesToStore.add(storageRequest);
                        }
                    }
                } else {
                    // Create a storage reference
                    validateForReference(dataObject);
                    FileReferenceRequestDTO referenceRequest = FileReferenceRequestDTO
                            .build(dataObject.getFilename(), dataObject.getChecksum(), dataObject.getAlgorithm(),
                                   representationInformation.getSyntax().getMimeType().toString(),
                                   dataObject.getFileSize(), aip.getId().toString(), l.getStorage(), l.getUrl());
                    referenceRequest.withType(dataObject.getRegardsDataType().toString());
                    filesToRefer.add(referenceRequest);
                }
            }
        }
    }

    /**
     * @param dataObject
     * @return
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
            throw new ModuleException(
                    String.format("Invalid entity {}. Information are missing : %s", String.join(", ", errors)));
        }
    }

    @Override
    public void updateAIPsContentInfosAndLocations(List<AIPEntity> aips,
            Collection<RequestResultInfoDTO> storeRequestInfos) {

        // Iterate over AIPs
        for (AIPEntity aipEntity : aips) {
            // Filter ResultInfos for the current aip to handle
            Set<RequestResultInfoDTO> aipRequests = storeRequestInfos.stream()
                    .filter(r -> r.getRequestOwners().contains(aipEntity.getAipId())).collect(Collectors.toSet());
            // Iterate over AIP data objects
            List<ContentInformation> contentInfos = aipEntity.getAip().getProperties().getContentInformations();
            for (ContentInformation ci : contentInfos) {
                OAISDataObject dataObject = ci.getDataObject();

                // Filter the request result list to only keep whose referring to the current data object
                Set<RequestResultInfoDTO> storeRequestInfosForCurrentAIP = aipRequests.stream()
                        .filter(r -> r.getRequestChecksum().equals(dataObject.getChecksum()))
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
                    Set<OAISDataObjectLocation> newLocations = dataObject.getLocations().stream()
                            .filter(l -> l.getStorage() != null).collect(Collectors.toSet());
                    newLocations.add(OAISDataObjectLocation.build(fileLocation.getUrl(),
                                                                  storeRequestInfo.getRequestStorage(),
                                                                  storeRequestInfo.getRequestStorePath()));
                    dataObject.setLocations(newLocations);

                    // Ensure the AIP storage list is updated
                    aipEntity.getStorages().add(storeRequestInfo.getRequestStorage());

                    String eventMessage = String.format("Data file %s stored on %s at %s.", metaInfo.getFileName(),
                                                        fileLocation.getStorage(), fileLocation.getUrl());
                    aipEntity.getAip().withEvent(EventType.STORAGE.toString(), eventMessage,
                                                 resultFile.getStorageDate());
                }
            }

            // Check if some storeRequests are concerning the AIP itself
            Set<RequestResultInfoDTO> storeRequestInfosForAIPManifest = aipRequests.stream()
                    .filter(AIPStorageService::isManifest).collect(Collectors.toSet());
            for (RequestResultInfoDTO storeRequestInfo : storeRequestInfosForAIPManifest) {
                if (storeRequestInfo.getRequestOwners().contains(aipEntity.getAipId())) {
                    FileReferenceDTO resultFile = storeRequestInfo.getResultFile();
                    FileLocationDTO fileLocation = resultFile.getLocation();

                    Set<OAISDataObjectLocation> manifestLocations = aipEntity.getManifestLocations();
                    // Remove any old reference to this storage
                    Set<OAISDataObjectLocation> newManifestLocations = manifestLocations.stream()
                            .filter(ml -> !ml.getStorage().equals(storeRequestInfo.getRequestStorage()))
                            .collect(Collectors.toSet());
                    // Generate the new ObjectLocation
                    newManifestLocations.add(OAISDataObjectLocation.build(fileLocation.getUrl(),
                                                                          storeRequestInfo.getRequestStorage(),
                                                                          storeRequestInfo.getRequestStorePath()));
                    // Save it
                    aipEntity.setManifestLocations(newManifestLocations);

                    // Ensure the AIP storage list is updated
                    aipEntity.getStorages().add(storeRequestInfo.getRequestStorage());
                }
            }
        }
    }

    /**
     * Check if a storage result info {@link RequestResultInfoDTO} is about a manifest file
     * @param result {@link RequestResultInfoDTO}
     * @return boolean
     */
    public static boolean isManifest(RequestResultInfoDTO result) {
        return ((result != null) && (result.getResultFile() != null) && (result.getResultFile().getMetaInfo() != null)
                && (result.getResultFile().getMetaInfo().getType() != null)
                && DataType.AIP.toString().equals(result.getResultFile().getMetaInfo().getType()));
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
                    .filter(ci -> ci.getDataObject().getChecksum().equals(eventInfo.getRequestChecksum())).findFirst();
            if (ciOp.isPresent()) {

                ContentInformation ci = ciOp.get();

                // Ensure the AIP storage list contains this storage location
                aip.getStorages().add(storageLocation);

                // Check if the event storage location is not already existing in ContentInfo locations
                boolean dataObjectLocationExists = ci.getDataObject().getLocations().stream()
                        .anyMatch(l -> l.getStorage().equals(storageLocation));

                if (!dataObjectLocationExists) {
                    aipEdited = true;
                    edited = true;
                    // Add this new location to the ContentInfo locations list
                    ci.getDataObject().getLocations()
                            .add(OAISDataObjectLocation.build(eventInfo.getResultFile().getLocation().getUrl(),
                                                              storageLocation, eventInfo.getRequestStorePath()));
                    aip.getAip().withEvent(EventType.UPDATE.toString(),
                                           String.format("File %s [%s] is now stored on %s at %s.",
                                                         eventInfo.getResultFile().getMetaInfo().getFileName(),
                                                         eventInfo.getResultFile().getMetaInfo().getChecksum(),
                                                         storageLocation,
                                                         eventInfo.getResultFile().getLocation().getUrl()));
                    LOGGER.debug("[AIP {}] New location {} for file {}", aip.getAipId(), storageLocation,
                                 eventInfo.getResultFile().getMetaInfo().getFileName());
                } else {
                    LOGGER.debug("[AIP {}] Location {} for file {} already exists", aip.getAipId(), storageLocation,
                                 ci.getDataObject().getFilename());
                }
            } else if (isManifest(eventInfo)) {
                // The event concern the AIP manifest itself
                // Check if manifest location is already referenced
                if (!aip.getManifestLocations().stream().anyMatch(l -> l.getStorage().equals(storageLocation))) {
                    // Add this new location to the manifest locations
                    Set<OAISDataObjectLocation> newManifestLocation = aip.getManifestLocations();
                    edited = true;
                    newManifestLocation
                            .add(OAISDataObjectLocation.build(eventInfo.getResultFile().getLocation().getUrl(),
                                                              storageLocation, eventInfo.getRequestStorePath()));
                    aip.setManifestLocations(newManifestLocation);
                } else {
                    LOGGER.debug("[AIP {}] Manifest location {} already exists", aip.getAipId(), storageLocation);
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
                    .filter(ci -> ci.getDataObject().getChecksum().equals(eventInfo.getRequestChecksum())).findFirst();
            if (ciOp.isPresent()) {
                ContentInformation ci = ciOp.get();

                // Check if the event storage location exists in ContentInfo locations
                boolean dataObjectLocationExists = ci.getDataObject().getLocations().stream()
                        .anyMatch(l -> l.getStorage().equals(storageLocation));

                if (dataObjectLocationExists) {
                    aipEdited = true;
                    edited = true;
                    // Remove the location from ContentInfo locations
                    Set<OAISDataObjectLocation> updatedDataObject = ci.getDataObject().getLocations().stream()
                            .filter(l -> !l.getStorage().equals(storageLocation)).collect(Collectors.toSet());
                    ci.getDataObject().setLocations(updatedDataObject);
                    aip.getAip().withEvent(EventType.UPDATE.toString(),
                                           String.format("File %s [%s] is not stored anymore on %s.",
                                                         eventInfo.getResultFile().getMetaInfo().getFileName(),
                                                         eventInfo.getResultFile().getMetaInfo().getChecksum(),
                                                         storageLocation));
                }

                // Check if the event storage location still appears in some file referenced by this AIP
                boolean shouldKeepStorage = contentInfos.stream().anyMatch(contentInfo -> contentInfo.getDataObject()
                        .getLocations().stream().anyMatch(loc -> loc.getStorage().equals(storageLocation)));

                // Remove the location from the storage list
                if (!shouldKeepStorage) {
                    edited = true;
                    Set<String> updatedStorages = aip.getStorages().stream().filter(s -> !s.equals(storageLocation))
                            .collect(Collectors.toSet());
                    aip.setStorages(updatedStorages);
                }
            } else if (AIPStorageService.isManifest(eventInfo)
                    && eventInfo.getRequestChecksum().equals(aip.getChecksum())) {
                // The event concern the AIP manifest itself
                // Check if the event storage location exists in manifest locations
                boolean manifestLocationExists = aip.getManifestLocations().stream()
                        .anyMatch(l -> l.getStorage().equals(storageLocation));
                if (manifestLocationExists) {
                    edited = true;
                    // Remove this location from the manifest locations
                    Set<OAISDataObjectLocation> updatedStorages = aip.getManifestLocations().stream()
                            .filter(s -> !s.getStorage().equals(storageLocation)).collect(Collectors.toSet());
                    aip.setManifestLocations(updatedStorages);
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
                // Iterate over data object localisations
                for (OAISDataObjectLocation l : dataObject.getLocations()) {
                    // Check if the current storage is still there
                    if (removedStorages.contains(l.getStorage())) {
                        // Create a storage deletion request
                        filesToRemove.add(FileDeletionRequestDTO.build(dataObject.getChecksum(), l.getStorage(),
                                                                       aip.getAipId(), false));
                    }
                }
            }
        }

        // Compute the new list of storage location (for manifest)
        Set<OAISDataObjectLocation> manifestLocations = aip.getManifestLocations();
        Set<OAISDataObjectLocation> newManifestLocations = new HashSet<>();
        manifestLocations.forEach(ml -> {
            if (!removedStorages.contains(ml.getStorage())) {
                newManifestLocations.add(ml);
            }
        });
        // Check if some manifest location have been removed
        if (newManifestLocations.size() < manifestLocations.size()) {
            for (OAISDataObjectLocation location : manifestLocations) {
                for (String removeStorage : removedStorages) {
                    if (location.getStorage().equals(removeStorage)) {
                        // Create a storage deletion request
                        filesToRemove.add(FileDeletionRequestDTO.build(aip.getChecksum(), removeStorage, aip.getAipId(),
                                                                       false));
                    }
                }
            }
            aip.setManifestLocations(newManifestLocations);
        }
        return filesToRemove;
    }

    @Override
    public Set<StoreLocation> getManifestStoreLocationsByStorageMetadata(Set<StorageMetadata> storages) {
        Set<StoreLocation> result = new HashSet<>();
        for (StorageMetadata storage : storages) {
            if (storage.getTargetTypes().isEmpty() || storage.getTargetTypes().contains(DataType.AIP)) {
                result.add(StoreLocation.build(storage.getPluginBusinessId(), storage.getStorePath()));
            }
        }
        return result;
    }

    @Override
    public Set<StoreLocation> getManifestStoreLocationsByLocation(Set<OAISDataObjectLocation> manifestLocations) {
        Set<StoreLocation> result = new HashSet<>();
        for (OAISDataObjectLocation location : manifestLocations) {
            result.add(StoreLocation.build(location.getStorage(), location.getStorePath()));
        }
        return result;
    }

    @Override
    public List<String> storeAIPs(List<AIPStoreMetaDataRequest> requests) throws ModuleException {

        Optional<ServiceInstance> instance = discoveryClient.getInstances(applicationName).stream().findFirst();
        if (!instance.isPresent()) {
            throw new ModuleException("Unable to retrieve an accessible instance for ingest microservice");
        }

        // Build file storage requests
        Collection<FileStorageRequestDTO> files = new ArrayList<>();

        for (AIPStoreMetaDataRequest request : requests) {
            // Create a request for each storage
            Collection<FileStorageRequestDTO> fileStorageRequests = buildAIPStorageRequest(request.getAip()
                    .getAip(), request.getAip().getChecksum(), request.getStoreLocations(), instance.get());
            files.addAll(fileStorageRequests);
        }

        // Make a request group for all these aips
        LOGGER.info("[AIP STORE META REQUEST] Sending {} storage requests to storage client.", files.size());
        Collection<RequestInfo> infos = storageClient.store(files);
        return infos.stream().map(RequestInfo::getGroupId).collect(Collectors.toList());
    }

    /**
     * Generate a public download URL for the file associated to the given Checksum
     * @param aipId aip id
     * @return a public URL to retrieve the AIP manifest
     * @throws ModuleException if the Eureka server is not reachable
     */
    public URL generateDownloadUrl(UniformResourceName aipId, ServiceInstance instance) throws ModuleException {
        String host = instance.getUri().toString();
        String path = Paths.get(AIPS_CONTROLLER_ROOT_PATH, AIP_DOWNLOAD_PATH).toString();
        String p = path.toString().replace("{" + AIP_ID_PATH_PARAM + "}", aipId.toString());
        p = (p.charAt(0) == '/') ? p.replaceFirst("/", "") : p;
        String urlStr = String.format("%s/%s?scope=%s", host, p, tenantResolver.getTenant());
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage(), e);
            throw new ModuleException(
                    String.format("Error generating AIP download url. Invalid calculated url %s. Cause : %s", urlStr,
                                  e.getMessage()),
                    e);
        }
    }

    /**
     * Build storage request for AIP file itself!
     */
    private Collection<FileStorageRequestDTO> buildAIPStorageRequest(AIP aip, String checksum,
            Set<StoreLocation> storeLocations, ServiceInstance instance) throws ModuleException {

        // Build file storage requests
        Collection<FileStorageRequestDTO> files = new ArrayList<>();

        // Build origin(s) URL
        URL originUrl = generateDownloadUrl(aip.getId(), instance);

        // Create a request for each storage
        for (StoreLocation storeLocation : storeLocations) {
            // If storage location sub directory is given, add a specific one for AIPs
            String storePath = null;
            if (storeLocation.getStorePath() != null) {
                if (!storeLocation.getStorePath().endsWith(apiStorageSubDirectory)) {
                    storePath = Paths.get(storeLocation.getStorePath(), apiStorageSubDirectory).toString();
                } else {
                    storePath = storeLocation.getStorePath();
                }
            }
            FileStorageRequestDTO storageRequest = FileStorageRequestDTO
                    .build(aip.getId().toString(), checksum, AIPService.MD5_ALGORITHM,
                           MediaType.APPLICATION_JSON_UTF8_VALUE, aip.getId().toString(), originUrl.toString(),
                           storeLocation.getStorage(), Optional.ofNullable(storePath));
            storageRequest.withType(DataType.AIP.toString());
            files.add(storageRequest);
        }

        return files;
    }

}
