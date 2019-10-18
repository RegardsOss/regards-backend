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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.storagelight.client.IStorageClient;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import fr.cnes.regards.modules.storagelight.domain.dto.FileReferenceDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileStorageRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.request.RequestResultInfoDTO;

/**
 * @author Léo Mieulet
 */
@Service
public class AIPStorageService implements IAIPStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPStorageService.class);

    public static final String AIPS_CONTROLLER_ROOT_PATH = "/aips";

    public static final String AIP_ID_PATH_PARAM = "aip_id";

    public static final String AIP_DOWNLOAD_PATH = "/{" + AIP_ID_PATH_PARAM + "}/download";

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private IStorageClient storageClient;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Override
    public List<String> storeAIPFiles(List<AIPEntity> aipEntities) throws ModuleException {
        // Build file storage requests
        Collection<FileStorageRequestDTO> filesToStore = new ArrayList<>();

        // Build file reference requests
        Collection<FileReferenceRequestDTO> filesToRefer = new ArrayList<>();

        // Iterate over AIPs
        for (AIPEntity aipEntity : aipEntities) {

            AIP aip = aipEntity.getAip();
            List<StorageMetadata> storages = aipEntity.getIngestMetadata().getStorages();
            // Iterate over Data Objects
            for (ContentInformation ci : aip.getProperties().getContentInformations()) {

                OAISDataObject dataObject = ci.getDataObject();
                // At this step, locations can be in 2 situations
                // Either its storage is empty, which means it's a file that should be store on each storage location
                // Either its storage is defined, the file should only be referenced

                // Let's check if the AIP is correct, with at least 1 location of file to store/refer
                if (dataObject.getLocations().isEmpty()) {
                    throw new ModuleException(String
                            .format("No location provided in the AIP (location of dataobject empty) for aip id[%s]",
                                    aipEntity.getAipId()));
                }
                // Check if the AIP have only one location to store
                long nbLocationWithNoStorage = dataObject.getLocations().stream().filter(l -> l.getStorage() == null)
                        .count();
                if (nbLocationWithNoStorage > 1) {
                    throw new ModuleException(
                            String.format("Too many files to store in a single dataobject for aip id[%s]",
                                          aipEntity.getAipId()));
                }

                for (OAISDataObjectLocation l : dataObject.getLocations()) {
                    if (l.getStorage() == null) {
                        // Storage is empty for this dataobject, create a storage request for each storage
                        for (StorageMetadata storage : storages) {
                            // Check if this storage contains this target type or is empty, which means
                            // this storage accepts everything
                            if (storage.getTargetTypes().isEmpty()
                                    || storage.getTargetTypes().contains(dataObject.getRegardsDataType())) {
                                filesToStore.add(FileStorageRequestDTO
                                        .build(dataObject.getFilename(), dataObject.getChecksum(),
                                               dataObject.getAlgorithm(),
                                               ci.getRepresentationInformation().getSyntax().getMimeType().toString(),
                                               aip.getId().toString(), l.getUrl(), storage.getPluginBusinessId(),
                                               Optional.ofNullable(storage.getStorePath())));
                            }
                        }
                    } else {
                        // Create a storage reference
                        filesToRefer.add(FileReferenceRequestDTO
                                .build(dataObject.getFilename(), dataObject.getChecksum(), dataObject.getAlgorithm(),
                                       ci.getRepresentationInformation().getSyntax().getMimeType().toString(),
                                       dataObject.getFileSize(), aip.getId().toString(), l.getStorage(), l.getUrl()));
                    }
                }
            }
        }
        // Keep reference to requests sent to Storage
        List<String> remoteStepGroupIds = new ArrayList<>();

        // Send storage request
        if (!filesToStore.isEmpty()) {
            RequestInfo info = storageClient.store(filesToStore);
            remoteStepGroupIds.add(info.getGroupId());
        }
        // Send reference request
        if (!filesToRefer.isEmpty()) {
            RequestInfo info = storageClient.reference(filesToRefer);
            remoteStepGroupIds.add(info.getGroupId());
        }
        return remoteStepGroupIds;
    }

    @Override
    public void updateAIPsUsingStorageResult(List<AIPEntity> aips, Collection<RequestResultInfoDTO> storeRequestInfos) {
        // Iterate over AIPs
        for (AIPEntity aipEntity : aips) {
            // Iterate over AIP data objects
            List<ContentInformation> contentInfos = aipEntity.getAip().getProperties().getContentInformations();
            for (ContentInformation ci : contentInfos) {
                OAISDataObject dataObject = ci.getDataObject();

                // Filter the request result list to only keep whose referring to the current data object
                Set<RequestResultInfoDTO> storeRequestInfosForCurrentAIP = storeRequestInfos.stream()
                        .filter(r -> r.getRequestChecksum().equals(dataObject.getChecksum()))
                        .collect(Collectors.toSet());

                // Iterate over request results
                for (RequestResultInfoDTO storeRequestInfo : storeRequestInfosForCurrentAIP) {
                    FileReferenceDTO resultFile = storeRequestInfo.getResultFile();
                    // Update AIP data object metas
                    dataObject.setFileSize(resultFile.getMetaInfo().getFileSize());
                    // It's safe to patch the checksum here
                    dataObject.setChecksum(resultFile.getMetaInfo().getChecksum());
                    // Update representational info
                    ci.getRepresentationInformation().getSyntax().setHeight(resultFile.getMetaInfo().getHeight());
                    ci.getRepresentationInformation().getSyntax().setWidth(resultFile.getMetaInfo().getWidth());
                    ci.getRepresentationInformation().getSyntax().setMimeType(resultFile.getMetaInfo().getMimeType());
                    // Exclude from the location list any null storage
                    Set<OAISDataObjectLocation> newLocations = dataObject.getLocations().stream()
                            .filter(l -> l.getStorage() != null).collect(Collectors.toSet());
                    newLocations
                            .add(OAISDataObjectLocation.build(storeRequestInfo.getResultFile().getLocation().getUrl(),
                                                              storeRequestInfo.getRequestStorage()));
                    dataObject.setLocations(newLocations);
                }
            }
        }
    }

    @Override
    public String storeAIPs(List<AIPEntity> aips) throws ModuleException {

        // Build file storage requests
        Collection<FileStorageRequestDTO> files = new ArrayList<>();

        for (AIPEntity aipEntity : aips) {
            // Create a request for each storage
            // TODO préciser le répertoire de sauvegarde des AIPs
            Collection<FileStorageRequestDTO> requests = buildAIPStorageRequest(aipEntity.getAip(), aipEntity
                    .getChecksum(), aipEntity.getIngestMetadata().getStorages());
            files.addAll(requests);
        }

        // Request storage for all AIPs of the ingest request
        RequestInfo info = storageClient.store(files);
        return info.getGroupId();
    }

    /**
     * Generate a public download URL for the file associated to the given Checksum
     * @param checksum
     * @return
     * @throws ModuleException if the Eureka server is not reachable
     */
    public URL generateDownloadUrl(String checksum) throws ModuleException {
        Optional<ServiceInstance> instance = discoveryClient.getInstances(applicationName).stream().findFirst();
        if (instance.isPresent()) {
            String host = instance.get().getUri().toString();
            String path = Paths.get(AIPS_CONTROLLER_ROOT_PATH, AIP_DOWNLOAD_PATH).toString();
            String p = path.toString().replace("{" + AIP_ID_PATH_PARAM + "}", checksum);
            p = (p.charAt(0) == '/') ? p.replaceFirst("/", "") : p;
            // TODO : Handle security access for downloadable AIPs
            String urlStr = String.format("%s/%s?scope=%s", host, p, tenantResolver.getTenant());
            try {
                return new URL(urlStr);
            } catch (MalformedURLException e) {
                LOGGER.error(e.getMessage(), e);
                throw new ModuleException(
                        String.format("Error generating AIP download url. Invalid calculated url %s. Cause : %s",
                                      urlStr, e.getMessage()),
                        e);
            }
        } else {
            String message = "Error getting storage microservice address from eureka client";
            LOGGER.error(message);
            throw new ModuleException(message);
        }
    }

    /**
     * Build storage request for AIP file itself!
     */
    private Collection<FileStorageRequestDTO> buildAIPStorageRequest(AIP aip, String checksum,
            List<StorageMetadata> storages) throws ModuleException {

        // Build file storage requests
        Collection<FileStorageRequestDTO> files = new ArrayList<>();

        // Build origin(s) URL
        URL originUrl = generateDownloadUrl(checksum);

        // Create a request for each storage
        for (StorageMetadata storage : storages) {

            if (storage.getTargetTypes().isEmpty() || storage.getTargetTypes().contains(DataType.AIP)) {
                files.add(FileStorageRequestDTO.build(aip.getId().toString(), checksum, AIPService.MD5_ALGORITHM,
                                                      MediaType.APPLICATION_JSON_UTF8_VALUE, aip.getId().toString(),
                                                      originUrl.toString(), storage.getPluginBusinessId(),
                                                      Optional.ofNullable(storage.getStorePath())));
            }
        }

        return files;
    }
}
