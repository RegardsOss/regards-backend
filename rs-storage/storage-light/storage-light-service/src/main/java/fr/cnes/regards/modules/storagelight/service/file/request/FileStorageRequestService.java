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
package fr.cnes.regards.modules.storagelight.service.file.request;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileStorageRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileStorageRequestResultDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import fr.cnes.regards.modules.storagelight.domain.flow.StorageFlowItem;
import fr.cnes.regards.modules.storagelight.domain.plugin.FileStorageWorkingSubset;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storagelight.service.JobsPriority;
import fr.cnes.regards.modules.storagelight.service.file.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storagelight.service.file.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.job.FileStorageRequestJob;
import fr.cnes.regards.modules.storagelight.service.location.StoragePluginConfigurationHandler;

/**
 * Service to handle {@link FileStorageRequest}s.
 * Those requests are created when a file reference need to be stored physically thanks to an existing {@link IStorageLocation} plugin.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileStorageRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageRequestService.class);

    private static final int NB_REFERENCE_BY_PAGE = 1000;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IFileStorageRequestRepository fileStorageRequestRepo;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private FileReferenceEventPublisher eventPublisher;

    @Autowired
    private StoragePluginConfigurationHandler storageHandler;

    @Autowired
    protected FileStorageRequestService self;

    @Autowired
    private RequestsGroupService reqGroupService;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private FileDeletionRequestService fileDelReqService;

    @Autowired
    private FileReferenceRequestService fileRefReqService;

    /**
     * Initialize new storage requests from Flow items.
     * @param list
     */
    public void store(List<StorageFlowItem> list) {
        Set<FileReference> existingOnes = fileRefService.search(list.stream().map(StorageFlowItem::getFiles)
                .flatMap(Set::stream).map(FileStorageRequestDTO::getChecksum).collect(Collectors.toSet()));
        Set<String> groupsToGrant = Sets.newHashSet();
        for (StorageFlowItem item : list) {
            store(item.getFiles(), item.getGroupId(), existingOnes);
            groupsToGrant.add(item.getGroupId());
        }
        reqGroupService.granted(groupsToGrant, FileRequestType.STORAGE);
    }

    /**
     * Initialize new storage requests for a given group identifier
     * @param requests
     * @param groupId
     */
    public void store(Collection<FileStorageRequestDTO> requests, String groupId) {
        Set<FileReference> existingOnes = fileRefService
                .search(requests.stream().map(FileStorageRequestDTO::getChecksum).collect(Collectors.toSet()));
        store(requests, groupId, existingOnes);
    }

    /**
     * Initialize new storage requests for a given group identifier. Parameter existingOnes is passed to improve performance in bulk creation to
     * avoid requesting {@link IFileReferenceRepository} on each request.
     * @param requests
     * @param groupId
     * @param existingOnes
     */
    public void store(Collection<FileStorageRequestDTO> requests, String groupId,
            Collection<FileReference> existingOnes) {
        // Retrieve already existing ones by checksum only to improve performance. The associated storage location is checked later
        LOGGER.debug("[STORAGE REQUESTS] Handling {} requests ...", requests.size());

        for (FileStorageRequestDTO request : requests) {
            // Check if the file already exists for the storage destination
            Optional<FileReference> oFileRef = existingOnes.stream()
                    .filter(f -> f.getMetaInfo().getChecksum().equals(request.getChecksum())
                            && f.getLocation().getStorage().contentEquals(request.getStorage()))
                    .findFirst();
            handleRequest(request, oFileRef, groupId);
        }
    }

    /**
     * Store a new file to a given storage destination
     * @param owner Owner of the new file
     * @param metaInfo information about file
     * @param originUrl current location of file. This URL must be locally accessible to be copied.
     * @param storage name of the storage destination. Must be a existing plugin configuration of a {@link IStorageLocation}
     * @param subDirectory where to store file in the destination location.
     * @param groupId business request identifier
     * @return {@link FileReference} if the file is already referenced.
     */
    public Optional<FileReference> handleRequest(String owner, FileReferenceMetaInfo metaInfo, String originUrl,
            String storage, Optional<String> subDirectory, String groupId) {
        Optional<FileReference> oFileRef = fileRefService.search(storage, metaInfo.getChecksum());
        return handleRequest(FileStorageRequestDTO.build(metaInfo.getFileName(), metaInfo.getChecksum(),
                                                         metaInfo.getAlgorithm(), metaInfo.getMimeType().toString(),
                                                         owner, originUrl, storage, subDirectory),
                             oFileRef, groupId);
    }

    /**
     * Store a new file to a given storage destination
     * @param request {@link FileStorageRequestDTO} info about file to store
     * @param fileRef {@link FileReference} of associated file if already exists
     * @param groupId business request identifier
     * @return {@link FileReference} if the file is already referenced.
     */
    private Optional<FileReference> handleRequest(FileStorageRequestDTO request, Optional<FileReference> fileRef,
            String groupId) {
        if (fileRef.isPresent()) {
            return handleAlreadyExists(fileRef.get(), request, groupId);
        } else {
            create(Sets.newHashSet(request.getOwner()), request.buildMetaInfo(), request.getOriginUrl(),
                   request.getStorage(), request.getSubDirectory(), FileRequestStatus.TO_DO, groupId);
            return Optional.empty();
        }
    }

    /**
     * Search for {@link FileStorageRequest}s matching the given destination storage and checksum
     * @param destinationStorage
     * @param checksum
     * @return {@link FileStorageRequest}
     */
    @Transactional(readOnly = true)
    public Optional<FileStorageRequest> search(String destinationStorage, String checksum) {
        return fileStorageRequestRepo.findByMetaInfoChecksumAndStorage(checksum, destinationStorage);
    }

    /**
     * Search for all {@link FileStorageRequest}s
     * @param pageable
     * @return {@link FileStorageRequest}s by page
     */
    @Transactional(readOnly = true)
    public Page<FileStorageRequest> search(Pageable pageable) {
        return fileStorageRequestRepo.findAll(pageable);
    }

    /**
     * Search for {@link FileStorageRequest}s associated to the given destination storage location.
     * @param pageable
     * @return {@link FileStorageRequest}s by page
     */
    @Transactional(readOnly = true)
    public Page<FileStorageRequest> search(String destinationStorage, Pageable pageable) {
        return fileStorageRequestRepo.findByStorage(destinationStorage, pageable);
    }

    @Transactional(readOnly = true)
    public Long count(String storage, FileRequestStatus status) {
        return fileStorageRequestRepo.countByStorageAndStatus(storage, status);
    }

    /**
     * Delete a {@link FileStorageRequest}
     * @param fileStorageRequest to delete
     */
    public void delete(FileStorageRequest fileStorageRequest) {
        fileStorageRequestRepo.deleteById(fileStorageRequest.getId());
    }

    /**
     * Update all {@link FileStorageRequest} in error status to change status to {@link FileRequestStatus#TO_DO}.
     * @param groupId request business identifier to retry
     */
    public void retryRequest(String groupId) {
        for (FileStorageRequest request : fileStorageRequestRepo.findByGroupIdsAndStatus(groupId,
                                                                                         FileRequestStatus.ERROR)) {
            request.setStatus(FileRequestStatus.TO_DO);
            request.setErrorCause(null);
            update(request);
        }
    }

    /**
     * Update all {@link FileStorageRequest} in error status to change status to {@link FileRequestStatus#TO_DO} for the given owners.
     */
    public void retry(Collection<String> owners) {
        Pageable page = PageRequest.of(0, NB_REFERENCE_BY_PAGE, Sort.by(Direction.ASC, "id"));
        Page<FileStorageRequest> results;
        do {
            results = fileStorageRequestRepo.findByOwnersInAndStatus(owners, FileRequestStatus.ERROR, page);
            for (FileStorageRequest request : results) {
                request.setStatus(FileRequestStatus.TO_DO);
                request.setErrorCause(null);
                update(request);
            }
            // Always retrieve the first page has we modify each element of the results.
            // All element are handled when result is empty.
        } while (results.hasNext());
    }

    /**
     * Update a {@link FileStorageRequest}
     * @param fileStorageRequest to delete
     */
    public void update(FileStorageRequest fileStorageRequest) {
        fileStorageRequestRepo.save(fileStorageRequest);
    }

    /**
     * Schedule {@link FileStorageRequestJob}s for all {@link FileStorageRequest}s matching the given parameters
     * @param status of the request to handle
     * @param storages of the request to handle
     * @param owners of the request to handle
     * @return {@link JobInfo}s scheduled
     */
    public Collection<JobInfo> scheduleJobs(FileRequestStatus status, Collection<String> storages,
            Collection<String> owners) {
        Collection<JobInfo> jobList = Lists.newArrayList();
        Set<String> allStorages = fileStorageRequestRepo.findStoragesByStatus(status);
        Set<String> storagesToSchedule = (storages != null) && !storages.isEmpty()
                ? allStorages.stream().filter(storages::contains).collect(Collectors.toSet())
                : allStorages;
        long start = System.currentTimeMillis();
        LOGGER.debug("[STORAGE REQUESTS] Scheduling storage jobs ...");
        for (String storage : storagesToSchedule) {
            Page<FileStorageRequest> filesPage;
            Pageable page = PageRequest.of(0, NB_REFERENCE_BY_PAGE, Sort.by("id"));
            do {
                if ((owners != null) && !owners.isEmpty()) {
                    filesPage = fileStorageRequestRepo.findAllByStorageAndStatusAndOwnersIn(storage, status, owners,
                                                                                            page);
                } else {
                    filesPage = fileStorageRequestRepo.findAllByStorageAndStatus(storage, status, page);
                }
                List<FileStorageRequest> fileStorageRequests = filesPage.getContent();

                if (storageHandler.getConfiguredStorages().contains(storage)) {
                    jobList.addAll(scheduleJobsByStorage(storage, fileStorageRequests));
                } else {
                    handleStorageNotAvailable(fileStorageRequests);
                }
                // page = filesPage.nextPageable();
            } while (filesPage.hasContent());
        }
        LOGGER.debug("[STORAGE REQUESTS] {} jobs scheduled in {} ms", jobList.size(),
                     System.currentTimeMillis() - start);
        return jobList;
    }

    /**
     * Schedule {@link FileStorageRequestJob}s for all given {@link FileStorageRequest}s and a given storage location.
     * @param storage
     * @param fileStorageRequests
     * @return {@link JobInfo}s scheduled
     */
    private Collection<JobInfo> scheduleJobsByStorage(String storage,
            Collection<FileStorageRequest> fileStorageRequests) {
        Collection<JobInfo> jobInfoList = Sets.newHashSet();
        try {
            PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(storage);
            IStorageLocation storagePlugin = pluginService.getPlugin(conf.getBusinessId());
            Collection<FileStorageWorkingSubset> workingSubSets = storagePlugin.prepareForStorage(fileStorageRequests);
            for (FileStorageWorkingSubset ws : workingSubSets) {
                if (!ws.getFileReferenceRequests().isEmpty()) {
                    jobInfoList.add(self.scheduleJob(ws, conf.getBusinessId(), storage));
                }
            }
        } catch (ModuleException | NotAvailablePluginConfigurationException e) {
            LOGGER.error(e.getMessage(), e);
            this.handleStorageNotAvailable(fileStorageRequests);
        }
        return jobInfoList;
    }

    /**
     * Schedule a {@link JobInfo} for the given {@link  FileStorageWorkingSubset}.<br/>
     * NOTE : A new transaction is created for each call at this method. It is mandatory to avoid having too long transactions.
     * @param workingSubset
     * @param plgBusinessId
     * @return {@link JobInfo} scheduled.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobInfo scheduleJob(FileStorageWorkingSubset workingSubset, String plgBusinessId, String storage) {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(FileStorageRequestJob.DATA_STORAGE_CONF_BUSINESS_ID, plgBusinessId));
        parameters.add(new JobParameter(FileStorageRequestJob.WORKING_SUB_SET, workingSubset));
        workingSubset.getFileReferenceRequests().forEach(fileStorageRequest -> fileStorageRequestRepo
                .updateStatus(FileRequestStatus.PENDING, fileStorageRequest.getId()));
        JobInfo jobInfo = jobInfoService.createAsQueued(new JobInfo(false, JobsPriority.FILE_STORAGE_JOB.getPriority(),
                parameters, authResolver.getUser(), FileStorageRequestJob.class.getName()));
        LOGGER.debug("[STORAGE REQUESTS] Job scheduled for {} requests on storage {}",
                     workingSubset.getFileReferenceRequests().size(), storage);
        return jobInfo;
    }

    /**
     * Create a new {@link FileStorageRequest}
     * @param owners owners of the file to store
     * @param fileMetaInfo meta information of the file to store
     * @param originUrl file origin location
     * @param storage storage destination location
     * @param storageSubDirectory Optional sub directory where to store file in the storage destination location
     * @param status
     * @param groupId Business identifier of the deletion request
     */
    public Optional<FileStorageRequest> create(Collection<String> owners, FileReferenceMetaInfo fileMetaInfo,
            String originUrl, String storage, Optional<String> storageSubDirectory, FileRequestStatus status,
            String groupId) {
        // Check if file storage request already exists
        Optional<FileStorageRequest> oFileRefRequest = search(storage, fileMetaInfo.getChecksum());
        if (oFileRefRequest.isPresent()) {
            handleAlreadyExists(oFileRefRequest.get(), originUrl, fileMetaInfo, owners, groupId);
        } else {
            FileStorageRequest fileStorageRequest = new FileStorageRequest(owners, fileMetaInfo, originUrl, storage,
                    storageSubDirectory, groupId);
            fileStorageRequest.setStatus(status);
            if (!storageHandler.getConfiguredStorages().contains(storage)) {
                // The storage destination is unknown, we can already set the request in error status
                handleStorageNotAvailable(fileStorageRequest);
            } else {
                fileStorageRequestRepo.save(fileStorageRequest);
                LOGGER.trace("New file storage request created for file <{}> to store to {} with status {}",
                             fileStorageRequest.getMetaInfo().getFileName(), fileStorageRequest.getStorage(),
                             fileStorageRequest.getStatus());
            }
            return Optional.of(fileStorageRequest);
        }
        return oFileRefRequest;
    }

    /**
     * Method to update a {@link FileStorageRequest} when a new request is sent for the same associated {@link FileReference}.<br>
     * If the existing file request is in error state, update the state to {@link FileRequestStatus#TO_DO} to allow store request retry.<br>
     * The existing request is also updated to add new owners of the future stored and referenced {@link FileReference}.
     *
     * @param fileStorageRequest
     * @param newMetaInfo
     * @param owners
     * @param newGroupId business requests group identifier
     */
    private void handleAlreadyExists(FileStorageRequest fileStorageRequest, String originUrl,
            FileReferenceMetaInfo newMetaInfo, Collection<String> owners, String newGroupId) {
        LOGGER.trace("Storage request already exists for file {}", newMetaInfo.getFileName());
        fileStorageRequest.getOwners().addAll(owners);
        fileStorageRequest.getGroupIds().add(newGroupId);
        if (!newMetaInfo.equals(fileStorageRequest.getMetaInfo())) {
            LOGGER.warn("New storage request received for the same file {} but meta informations differs. using new  ones.");
            fileStorageRequest.setMetaInfo(newMetaInfo);
        }
        if (!fileStorageRequest.getOriginUrl().equals(originUrl)) {
            LOGGER.warn("New storage request received for the same file {} but origin url differs. using new  one.");
            fileStorageRequest.setOriginUrl(originUrl);
        }

        switch (fileStorageRequest.getStatus()) {
            case ERROR:
                // Allows storage retry.
                fileStorageRequest.setStatus(FileRequestStatus.TO_DO);
                break;
            case PENDING:
                // A storage is already in progress for this request.
            case DELAYED:
            case TO_DO:
            default:
                // Request has not been handled yet, we can update it.
                break;
        }
        fileStorageRequestRepo.save(fileStorageRequest);
    }

    /**
     * Update a list of {@link FileStorageRequest}s when the storage destination cannot be handled.
     * A storage destination cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     * @param fileStorageRequests
     */
    private void handleStorageNotAvailable(Collection<FileStorageRequest> fileStorageRequests) {
        fileStorageRequests.forEach(r -> handleStorageNotAvailable(r));
    }

    /**
     * Update a {@link FileStorageRequest} when the storage destination cannot be handled.
     * A storage destination cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     * @param fileStorageRequest
     */
    private void handleStorageNotAvailable(FileStorageRequest fileStorageRequest) {
        // The storage destination is unknown, we can already set the request in error status
        String errorCause = String
                .format("File <%s> cannot be handle for storage as destination storage <%s> is unknown or disabled.",
                        fileStorageRequest.getMetaInfo().getFileName(), fileStorageRequest.getStorage());
        fileStorageRequest.setStatus(FileRequestStatus.ERROR);
        fileStorageRequest.setErrorCause(errorCause);
        update(fileStorageRequest);
        LOGGER.error(fileStorageRequest.getErrorCause());
        // Send notification for file storage error
        eventPublisher.storeError(fileStorageRequest.getMetaInfo().getChecksum(), fileStorageRequest.getOwners(),
                                  fileStorageRequest.getStorage(), fileStorageRequest.getErrorCause(),
                                  fileStorageRequest.getGroupIds());
        // Inform request groups that the request is in error
        for (String groupId : fileStorageRequest.getGroupIds()) {
            reqGroupService.requestError(groupId, FileRequestType.STORAGE,
                                         fileStorageRequest.getMetaInfo().getChecksum(),
                                         fileStorageRequest.getStorage(), errorCause);
        }
    }

    public void handleSuccess(Collection<FileStorageRequestResultDTO> results) {
        for (FileStorageRequestResultDTO result : results) {
            FileStorageRequest request = result.getRequest();
            FileReferenceMetaInfo reqMetaInfos = request.getMetaInfo();
            for (String owner : result.getRequest().getOwners()) {
                try {
                    FileReferenceMetaInfo fileMeta = new FileReferenceMetaInfo(reqMetaInfos.getChecksum(),
                            reqMetaInfos.getAlgorithm(), reqMetaInfos.getFileName(), result.getFileSize(),
                            reqMetaInfos.getMimeType());
                    fileMeta.setHeight(reqMetaInfos.getHeight());
                    fileMeta.setWidth(reqMetaInfos.getWidth());
                    FileReference fileRef = fileRefReqService
                            .reference(owner, fileMeta, new FileLocation(request.getStorage(), result.getStoredUrl()),
                                       request.getGroupIds());
                    try {
                        // Delete the FileRefRequest as it has been handled
                        delete(request);
                    } catch (EmptyResultDataAccessException e) {
                        LOGGER.warn(String.format("Unable to delete storage request with id %s. Cause : %s",
                                                  request.getId(), e.getMessage()),
                                    e);
                    }
                    for (String groupId : request.getGroupIds()) {
                        reqGroupService.requestSuccess(groupId, FileRequestType.STORAGE,
                                                       fileRef.getMetaInfo().getChecksum(),
                                                       fileRef.getLocation().getStorage(), fileRef);
                    }
                } catch (ModuleException e) {
                    handleError(request, e.getMessage());
                }
            }
        }

    }

    public void handleError(Collection<FileStorageRequestResultDTO> results) {
        for (FileStorageRequestResultDTO result : results) {
            handleError(result.getRequest(), result.getErrorCause());
        }
    }

    /**
     * Handle a {@link FileStorageRequest} error.
     * <ul>
     * <li> Update the request into database </li>
     * <li> Send bus message information about storage error </li>
     * <li> Update group with the error request </li>
     * </ul>
     */
    private void handleError(FileStorageRequest request, String errorCause) {
        // The file is not really referenced so handle reference error by modifying request to be retry later
        request.setStatus(FileRequestStatus.ERROR);
        request.setErrorCause(errorCause);
        update(request);
        eventPublisher.storeError(request.getMetaInfo().getChecksum(), request.getOwners(), request.getStorage(),
                                  errorCause, request.getGroupIds());
        for (String groupId : request.getGroupIds()) {
            reqGroupService.requestError(groupId, FileRequestType.STORAGE, request.getMetaInfo().getChecksum(),
                                         request.getStorage(), errorCause);
        }
    }

    /**
     * Update if needed an already existing {@link FileReference} associated to a
     * new {@link FileStorageRequestDTO} request received.<br/>
     * <br/>
     * If a deletion request is running on the existing {@link FileReference} then a new {@link FileStorageRequest}
     * request is created as DELAYED.<br/>
     *
     * @param fileReference {@link FileReference} to update
     * @param request associated {@link FileStorageRequestDTO} new request
     * @param groupId new business request identifier
     * @return {@link FileReference} updated or null.
     */
    private Optional<FileReference> handleAlreadyExists(FileReference fileReference, FileStorageRequestDTO request,
            String groupId) {
        FileReference updatedFileRef = null;
        FileReferenceMetaInfo newMetaInfo = request.buildMetaInfo();
        Optional<FileDeletionRequest> deletionRequest = fileDelReqService.search(fileReference);
        if (deletionRequest.isPresent() && (deletionRequest.get().getStatus() == FileRequestStatus.PENDING)) {
            // Deletion is running write now, so delay the new file reference creation with a FileReferenceRequest
            create(Sets.newHashSet(request.getOwner()), newMetaInfo, request.getOriginUrl(), request.getStorage(),
                   request.getSubDirectory(), FileRequestStatus.DELAYED, groupId);
        } else {
            if (deletionRequest.isPresent()) {
                // Delete not running deletion request to add the new owner
                fileDelReqService.delete(deletionRequest.get());
            }
            String message = String
                    .format("New owner <%s> added to existing referenced file <%s> at <%s> (checksum: %s) ",
                            request.getOwner(), fileReference.getMetaInfo().getFileName(),
                            fileReference.getLocation().toString(), fileReference.getMetaInfo().getChecksum());
            eventPublisher.storeSuccess(fileReference, message, Sets.newHashSet(groupId));
            updatedFileRef = fileRefService.addOwner(fileReference, request.getOwner());
            reqGroupService.requestSuccess(groupId, FileRequestType.STORAGE, request.getChecksum(),
                                           request.getStorage(), updatedFileRef);
        }
        return Optional.ofNullable(updatedFileRef);
    }

    /**
     * Delete all requests for the given storage identifier
     * @param storageLocationId
     */
    public void deleteByStorage(String storageLocationId, Optional<FileRequestStatus> status) {
        if (status.isPresent()) {
            fileStorageRequestRepo.deleteByStorageAndStatus(storageLocationId, status.get());
        } else {
            fileStorageRequestRepo.deleteByStorage(storageLocationId);
        }
    }

}
