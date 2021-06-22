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
package fr.cnes.regards.modules.storage.service.file.request;

import fr.cnes.regards.modules.storage.service.session.SessionNotifier;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storage.dao.IFileDeletetionRequestRepository;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;
import fr.cnes.regards.modules.storage.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storage.domain.plugin.FileDeletionWorkingSubset;
import fr.cnes.regards.modules.storage.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storage.domain.plugin.PreparationResponse;
import fr.cnes.regards.modules.storage.service.JobsPriority;
import fr.cnes.regards.modules.storage.service.file.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.job.FileDeletionRequestJob;
import fr.cnes.regards.modules.storage.service.file.job.FileDeletionRequestsCreatorJob;
import fr.cnes.regards.modules.storage.service.file.job.FileStorageRequestJob;
import fr.cnes.regards.modules.storage.service.location.StoragePluginConfigurationHandler;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.TaskResult;

/**
 * Service to handle request to physically delete files thanks to {@link FileDeletionRequest}s.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileDeletionRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDeletionRequestService.class);

    @Autowired
    private IFileDeletetionRequestRepository fileDeletionRequestRepo;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private StoragePluginConfigurationHandler storageHandler;

    @Autowired
    protected FileDeletionRequestService self;

    @Autowired
    private FileReferenceEventPublisher publisher;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private RequestsGroupService reqGroupService;

    @Autowired
    private RequestStatusService reqStatusService;

    @Autowired
    private FileCopyRequestService fileCopyReqService;

    @Autowired
    private FileCacheRequestService fileCacheReqService;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Value("${regards.storage.deletion.requests.days.before.expiration:5}")
    private Integer nbDaysBeforeExpiration;

    @Value("${regards.storage.deletion.requests.per.job:100}")
    private Integer nbRequestsPerJob;

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    /**
     * Create a new {@link FileDeletionRequest}.
     * @param fileReferenceToDelete {@link FileReference} to delete
     * @param forceDelete allows to delete fileReference even if the deletion is in error.
     * @param groupId Business identifier of the deletion request
     */
    public FileDeletionRequest create(FileReference fileReferenceToDelete, boolean forceDelete, String groupId,
            Collection<FileDeletionRequest> existingRequests, FileRequestStatus status, String sessionOwner,
            String session) {
        Optional<FileDeletionRequest> existingOne = existingRequests.stream()
                .filter(r -> r.getFileReference().getId().equals(fileReferenceToDelete.getId())).findFirst();
        FileDeletionRequest request;
        if (!existingOne.isPresent()) {
            // Create new deletion request
            FileDeletionRequest newDelRequest = new FileDeletionRequest(fileReferenceToDelete, forceDelete, groupId,
                                                                        status, sessionOwner, session);
            newDelRequest.setStatus(reqStatusService.getNewStatus(newDelRequest, Optional.of(status)));
            request = fileDeletionRequestRepo.save(newDelRequest);
            existingRequests.add(request);
            // notify deletion requests to the session agent
            this.sessionNotifier.incrementDeleteRequests(sessionOwner, session);
            // notify running request to the session agent
            this.sessionNotifier.incrementRunningRequests(sessionOwner, session);
        } else {
            // Retry deletion if error
            request = retry(existingOne.get(), forceDelete);
        }
        return request;
    }
    /**
     * Update all {@link FileDeletionRequest} in error status to change status to {@link FileRequestStatus#TO_DO}.
     */
    private FileDeletionRequest retry(FileDeletionRequest request, boolean forceDelete) {
        if (request.getStatus() == FileRequestStatus.ERROR) {
            String sessionOwner = request.getSessionOwner();
            String session = request.getSession();
            // decrement error request
            this.sessionNotifier.decrementErrorRequests(sessionOwner, session);
            // notify running request to the session agent
            this.sessionNotifier.incrementRunningRequests(sessionOwner, session);
            // reset status
            request.setStatus(FileRequestStatus.TO_DO);
            request.setErrorCause(null);
            request.setForceDelete(forceDelete);
            return updateFileDeletionRequest(request);
        } else {
            return request;
        }
    }

    /**
     * Update all {@link FileDeletionRequest} in error status to change status to {@link FileRequestStatus#TO_DO}.
     */
    public void retryBySession(List<FileDeletionRequest> requestList, String sessionOwner, String session) {
        int nbRequests = requestList.size();
        for (FileDeletionRequest request : requestList) {
            // reset status
            request.setStatus(FileRequestStatus.TO_DO);
            request.setErrorCause(null);
        }
        // save changes in database
        updateFileDeletionRequestList(requestList);
        // decrement error requests
        this.sessionNotifier.decrementErrorRequests(sessionOwner, session, nbRequests);
        // notify running requests to the session agent
        this.sessionNotifier.incrementRunningRequests(sessionOwner, session, nbRequests);
    }

    /**
     * Schedule {@link FileDeletionRequestJob}s for all {@link FileDeletionRequest}s matching the given parameters.
     * @param status status of the {@link FileDeletionRequest}s to handle
     * @param storages of the {@link FileDeletionRequest}s to handle
     * @return {@link JobInfo}s scheduled
     */
    public Collection<JobInfo> scheduleJobs(FileRequestStatus status, Collection<String> storages) {
        Collection<JobInfo> jobList = Lists.newArrayList();
        try {
            TaskResult<Collection<JobInfo>> result = lockingTaskExecutors
                    .executeWithLock(new FileDeletionTask(status, storages, nbRequestsPerJob, jobList,
                            fileDeletionRequestRepo, self),
                                     new LockConfiguration(DeletionFlowItem.DELETION_LOCK,
                                             Instant.now().plusSeconds(30)));
            if (result.wasExecuted() && (result.getResult() != null)) {
                jobList = result.getResult();
            } else if (!result.wasExecuted()) {
                LOGGER.info("Deletion jobs cannot be scheduled as the process is locked by another process");
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
        }
        return jobList;
    }

    /**
     * Schedule jobs for deletion requests by using a new transaction
     * @param storage
     * @param deletionRequestPage
     * @param requestStatus
     * @return scheduled {@link JobInfo}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Collection<JobInfo> scheduleDeletionJobsByStorage(String storage,
            Page<FileDeletionRequest> deletionRequestPage, FileRequestStatus requestStatus) {
        LOGGER.debug("[DELETION REQUESTS] scheduling {} deletion jobs for storage {} ... ", deletionRequestPage.get(),
                     storage);
        // SESSION HANDLING
        List<FileDeletionRequest> storageReqList = deletionRequestPage.getContent();
        // if status is in error state decrement the number of requests in error
        if(requestStatus.equals(FileRequestStatus.ERROR)) {
            storageReqList.forEach(req -> {
                String sessionOwner = req.getSessionOwner();
                String session = req.getSession();
                sessionNotifier.decrementErrorRequests(sessionOwner, session);
                sessionNotifier.incrementRunningRequests(sessionOwner, session);
            });
        }

        // SCHEDULER - schedule jobs by storage
        if (storageHandler.isConfigured(storage)) {
            return scheduleDeletionJobsByStorage(storage, storageReqList);
        } else {
            handleStorageNotAvailable(deletionRequestPage.getContent(), Optional.empty());
        }
        return Collections.emptyList();
    }

    /**
     * Inform if for the given storage a deletion process is running
     * @param storage
     * @return boolean
     */
    public boolean isDeletionRunning(String storage) {
        boolean isRunning = false;
        // Does a deletion job exists ?
        isRunning = jobInfoService.retrieveJobsCount(FileDeletionRequestsCreatorJob.class.getName(), JobStatus.PENDING,
                                                     JobStatus.QUEUED, JobStatus.RUNNING, JobStatus.TO_BE_RUN) > 0;
        if (!isRunning) {
            isRunning = fileDeletionRequestRepo
                    .existsByStorageAndStatusIn(storage,
                                                Sets.newHashSet(FileRequestStatus.TO_DO, FileRequestStatus.PENDING));
        }
        return isRunning;
    }

    /**
     * Schedule {@link FileDeletionRequestJob}s for given {@link FileDeletionRequest}s and given storage location.
     * @param storage of the {@link FileDeletionRequest}s to handle
     * @param fileDeletionRequests {@link FileDeletionRequest}s to schedule
     * @return {@link JobInfo}s scheduled
     */
    private Collection<JobInfo> scheduleDeletionJobsByStorage(String storage,
            Collection<FileDeletionRequest> fileDeletionRequests) {
        Collection<JobInfo> jobInfoList = Sets.newHashSet();
        try {

            PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(storage);
            IStorageLocation storagePlugin = pluginService.getPlugin(conf.getBusinessId());

            PreparationResponse<FileDeletionWorkingSubset, FileDeletionRequest> response = storagePlugin
                    .prepareForDeletion(fileDeletionRequests);
            for (FileDeletionWorkingSubset ws : response.getWorkingSubsets()) {
                jobInfoList.add(scheduleJob(ws, conf.getBusinessId()));
            }
            // Handle error requests
            for (Entry<FileDeletionRequest, String> error : response.getPreparationErrors().entrySet()) {
                handleStorageNotAvailable(error.getKey(), Optional.ofNullable(error.getValue()));
            }
        } catch (ModuleException | NotAvailablePluginConfigurationException e) {
            LOGGER.error(e.getMessage(), e);
            handleStorageNotAvailable(fileDeletionRequests, Optional.empty());
        }
        return jobInfoList;
    }

    /**
     * Schedule a {@link JobInfo} for the given {@link  FileDeletionWorkingSubset}.<br/>
     * NOTE : A new transaction is created for each call at this method. It is mandatory to avoid having too long transactions.
     * @return {@link JobInfo} scheduled.
     */
    public JobInfo scheduleJob(FileDeletionWorkingSubset workingSubset, String pluginConfBusinessId) {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(FileStorageRequestJob.DATA_STORAGE_CONF_BUSINESS_ID, pluginConfBusinessId));
        parameters.add(new JobParameter(FileStorageRequestJob.WORKING_SUB_SET, workingSubset));
        JobInfo jobInfo = jobInfoService.createAsQueued(new JobInfo(false, JobsPriority.FILE_DELETION_JOB.getPriority(),
                parameters, authResolver.getUser(), FileDeletionRequestJob.class.getName()));
        workingSubset.getFileDeletionRequests().forEach(fileRefReq -> fileDeletionRequestRepo
                .updateStatusAndJobId(FileRequestStatus.PENDING, jobInfo.getId().toString(), fileRefReq.getId()));
        return jobInfo;
    }

    /**
     * Update a list of {@link FileDeletionRequest}s when the storage destination cannot be handled.
     * A storage destination cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     * @param fileDeletionRequests
     */
    private void handleStorageNotAvailable(Collection<FileDeletionRequest> fileDeletionRequests,
            Optional<String> errorCause) {
        fileDeletionRequests.forEach((r) -> this.handleStorageNotAvailable(r, errorCause));
    }

    /**
     * Update a {@link FileDeletionRequest} when the storage destination cannot be handled.
     * A storage destination cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     * @param fileDeletionRequest
     */
    private void handleStorageNotAvailable(FileDeletionRequest fileDeletionRequest, Optional<String> errorCause) {
        String lErrorCause = errorCause.orElse(String
                .format("File <%s> cannot be handle for deletion as destination storage <%s> is unknown or disabled.",
                        fileDeletionRequest.getFileReference().getMetaInfo().getFileName(),
                        fileDeletionRequest.getStorage()));
        // The storage destination is unknown, we can already set the request in error status
        fileDeletionRequest.setStatus(FileRequestStatus.ERROR);
        fileDeletionRequest.setErrorCause(lErrorCause);
        // notify request error to the session agent
        String sessionOwner = fileDeletionRequest.getSessionOwner();
        String session = fileDeletionRequest.getSession();
        this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
        this.sessionNotifier.incrementErrorRequests(sessionOwner, session);
        // update status
        updateFileDeletionRequest(fileDeletionRequest);
    }

    /**
     * Delete a {@link FileDeletionRequest}
     * @param fileDeletionRequest
     */
    public void delete(FileDeletionRequest fileDeletionRequest) {
        Assert.notNull(fileDeletionRequest, "File deletion request to delete cannot be null");
        Assert.notNull(fileDeletionRequest.getId(), "File deletion request to delete identifier cannot be null");
        if (fileDeletionRequestRepo.existsById(fileDeletionRequest.getId())) {
            fileDeletionRequestRepo.deleteById(fileDeletionRequest.getId());
        } else {
            LOGGER.warn("Unable to delete file deletion request {} cause it does not exists.",
                        fileDeletionRequest.getId());
        }
    }

    /**
     * Initialize new deletion requests from Flow items.
     * @param list
     */
    public void handle(List<DeletionFlowItem> list) {
        Set<String> checksums = list.stream().map(DeletionFlowItem::getFiles).flatMap(Set::stream)
                .map(FileDeletionRequestDTO::getChecksum).collect(Collectors.toSet());
        Set<FileReference> existingOnes = fileRefService.search(checksums);
        Set<FileDeletionRequest> fileDeletionRequests = fileDeletionRequestRepo
                .findByFileReferenceMetaInfoChecksumIn(checksums);
        for (DeletionFlowItem item : list) {
            // files to store
            Set<FileDeletionRequestDTO> files = item.getFiles();
            // if a copy process is already running on files, refuse file deletions
            if (fileCopyReqService
                    .isFileCopyRunning(files.stream().map(i -> i.getChecksum()).collect(Collectors.toSet()))) {
                reqGroupService.denied(item.getGroupId(), FileRequestType.DELETION,
                                       "Cannot delete files has a copy process is running");
                LOGGER.warn("Refused {} file deletion", files.size());

                // send refused requests to session notifier
                files.forEach(file -> {
                    String sessionOwner = file.getSessionOwner();
                    String session = file.getSession();
                    // notify a request has been taken into account but was rejected
                    this.sessionNotifier.incrementDeleteRequests(sessionOwner, session);
                    this.sessionNotifier.incrementDeniedRequests(sessionOwner, session);
                });
            } else {
                // grant file deletions
                reqGroupService
                        .granted(item.getGroupId(), FileRequestType.DELETION, files.size(), getRequestExpirationDate());
                handle(item.getFiles(), item.getGroupId(), existingOnes, fileDeletionRequests);
            }
        }
    }

    /**
     * Initialize new deletion requests for a given group identifier
     * @param requests
     * @param groupId
     */
    public void handle(Collection<FileDeletionRequestDTO> requests, String groupId) {
        Set<String> checksums = requests.stream().map(FileDeletionRequestDTO::getChecksum).collect(Collectors.toSet());
        Set<FileReference> existingOnes = fileRefService.search(checksums);
        Set<FileDeletionRequest> fileDeletionRequests = fileDeletionRequestRepo
                .findByFileReferenceMetaInfoChecksumIn(checksums);
        handle(requests, groupId, existingOnes, fileDeletionRequests);
    }

    /**
     * Initialize new deletion requests for a given group identifier. Parameter existingOnes is passed to improve performance in bulk creation to
     * avoid requesting {@link IFileReferenceRepository} on each request.
     * @param requests
     * @param groupId
     * @param existingOnes
     */
    public void handle(Collection<FileDeletionRequestDTO> requests, String groupId,
            Collection<FileReference> existingOnes, Collection<FileDeletionRequest> existingRequests) {
        for (FileDeletionRequestDTO request : requests) {
            Optional<FileReference> oFileRef = existingOnes.stream()
                    .filter(f -> f.getLocation().getStorage().equals(request.getStorage())
                            && f.getMetaInfo().getChecksum().equals(request.getChecksum()))
                    .findFirst();
            if (oFileRef.isPresent()) {
                FileReference fileRef = oFileRef.get();
                removeOwner(fileRef, request.getOwner(), request.getSessionOwner(), request.getSession(),
                            request.isForceDelete(), existingRequests, groupId);
            } else {
                // notify deletion requests to session agent
                this.sessionNotifier.incrementDeleteRequests(request.getSessionOwner(), request.getSession());
            }
            // In all case, inform caller that deletion request is success.
            reqGroupService.requestSuccess(groupId, FileRequestType.DELETION, request.getChecksum(),
                                           request.getStorage(), null, Sets.newHashSet(request.getOwner()), null);
        }
    }

    /**
     * Remove the given owner of the to the given {@link FileReference}.
     * If the owner is the last one this method tries to delete file physically if the storage location is a configured {@link IStorageLocation}.
     * @param forceDelete allows to delete fileReference even if the deletion is in error.
     * @param groupId Business identifier of the deletion request
     */
    private void removeOwner(FileReference fileReference, String owner, String sessionOwner,
            String session, boolean forceDelete,
            Collection<FileDeletionRequest> existingRequests, String groupId) {
        fileRefService.removeOwner(fileReference, owner, groupId);
        // If file reference does not belongs to anyone anymore, delete file reference
        if (!fileRefService.hasOwner(fileReference.getId())) {
            // check if storage accessibility
            if (storageHandler.isConfigured(fileReference.getLocation().getStorage())) {
                // If the file is stored on an accessible storage, create a new deletion request
                create(fileReference, forceDelete, groupId, existingRequests, FileRequestStatus.TO_DO, sessionOwner,
                       session);
            } else {
                // notify deletion requests to the session agent
                this.sessionNotifier.incrementDeleteRequests(sessionOwner, session);
                // notify running request to the session agent
                this.sessionNotifier.incrementRunningRequests(sessionOwner, session);
                // Delete associated cache request if any
                fileCacheReqService.delete(fileReference);
                // Else, directly delete the file reference
                fileRefService.delete(fileReference, groupId, sessionOwner, session);
            }
        } else {
            // notify deletion requests to the session agent
            this.sessionNotifier.incrementDeleteRequests(sessionOwner, session);
            // Notify successfully deleted file
            this.sessionNotifier.notifyDeletedFiles(sessionOwner, session);
        }
    }

    /**
     * Update a {@link FileDeletionRequest}
     * @param fileDeletionRequest
     */
    public FileDeletionRequest updateFileDeletionRequest(FileDeletionRequest fileDeletionRequest) {
        Assert.notNull(fileDeletionRequest, "File deletion request to update cannot be null");
        Assert.notNull(fileDeletionRequest.getId(), "File deletion request to update identifier cannot be null");
        return fileDeletionRequestRepo.save(fileDeletionRequest);
    }

    /**
     * Update a list {@link FileDeletionRequest}s
     * @param fileDeletionRequestList
     */
    public List<FileDeletionRequest> updateFileDeletionRequestList(List<FileDeletionRequest> fileDeletionRequestList) {
        fileDeletionRequestList.forEach(req -> {
            Assert.notNull(req, "File deletion request to update cannot be null");
            Assert.notNull(req.getId(), "File deletion request to update identifier cannot be null");
        });
        return fileDeletionRequestRepo.saveAll(fileDeletionRequestList);
    }

    /**
     * Search for a specific {@link FileDeletionRequest}
     * @param fileReference to search for
     * @return {@link FileDeletionRequest} if exists
     */
    @Transactional(readOnly = true)
    public Optional<FileDeletionRequest> search(FileReference fileReference) {
        return fileDeletionRequestRepo.findByFileReferenceId(fileReference.getId());
    }

    @Transactional(readOnly = true)
    public Set<FileDeletionRequest> searchByChecksums(Set<String> checksums) {
        return fileDeletionRequestRepo.findByFileReferenceMetaInfoChecksumIn(checksums);
    }

    @Transactional(readOnly = true)
    public Set<FileDeletionRequest> search(Set<FileReference> fileReferences) {
        return fileDeletionRequestRepo
                .findByFileReferenceIdIn(fileReferences.stream().map(FileReference::getId).collect(Collectors.toSet()));
    }

    @Transactional(readOnly = true)
    public Page<FileDeletionRequest> search(String storage, FileRequestStatus status, Pageable page) {
        return fileDeletionRequestRepo.findByStorageAndStatus(storage, status, page);
    }

    @Transactional(readOnly = true)
    public Page<FileDeletionRequest> search(String storage, Pageable page) {
        return fileDeletionRequestRepo.findByStorage(storage, page);
    }

    @Transactional(readOnly = true)
    public Optional<FileDeletionRequest> search(String checksum, String storage) {
        return fileDeletionRequestRepo.findByStorageAndFileReferenceMetaInfoChecksum(storage, checksum);
    }

    @Transactional(readOnly = true)
    public Long count(String storage, FileRequestStatus status) {
        return fileDeletionRequestRepo.countByStorageAndStatus(storage, status);
    }

    public void handleError(FileDeletionRequest fileDeletionRequest, String errorCause) {
        FileReference fileRef = fileDeletionRequest.getFileReference();
        if (!fileDeletionRequest.isForceDelete()) {
            // No force delete option. So request is in error status.
            // Update request in error status
            fileDeletionRequest.setStatus(FileRequestStatus.ERROR);
            fileDeletionRequest.setErrorCause(errorCause);
            updateFileDeletionRequest(fileDeletionRequest);
            // Publish deletion error
            publisher.deletionError(fileRef, errorCause, fileDeletionRequest.getGroupId());
            // Publish request error
            reqGroupService.requestError(fileDeletionRequest.getGroupId(), FileRequestType.DELETION,
                                         fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(), null,
                                         fileRef.getLazzyOwners(), errorCause);
            // notify request error to the session agent
            String sessionOwner = fileDeletionRequest.getSessionOwner();
            String session = fileDeletionRequest.getSession();
            this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
            this.sessionNotifier.incrementErrorRequests(sessionOwner, session);
        } else {
            // Force delete option.
            handleSuccess(fileDeletionRequest);
            // NOTE : The file reference event is published by the fileReferenceService
            LOGGER.warn(String
                    .format("File %s from %s (checksum: %s) has been removed by force from referenced files. (File may still exists on storage).",                            fileRef.getMetaInfo().getFileName(), fileRef.getLocation().toString(),
                            fileRef.getMetaInfo().getChecksum()));
        }
    }

    /**
     * Handle a {@link FileDeletionRequest} success.
     * @param fileDeletionRequest
     */
    public void handleSuccess(FileDeletionRequest fileDeletionRequest) {
        FileReference deletedFileRef = fileDeletionRequest.getFileReference();
        // 1. Delete the request in database
        delete(fileDeletionRequest);
        // 2. Delete cache request if any
        fileCacheReqService.delete(deletedFileRef);
        // 3. Delete the file reference in database
        fileRefService.delete(deletedFileRef, fileDeletionRequest.getGroupId(),
                              fileDeletionRequest.getSessionOwner(), fileDeletionRequest.getSession());
    }

    /**
     * Schedule a job to create deletion requests for all files matching the given criterion.
     * @param storageLocationId
     * @param forceDelete
     * @param sessionOwner
     * @param session
     * @throws ModuleException
     */
    public JobInfo scheduleJob(String storageLocationId, Boolean forceDelete, String sessionOwner, String session) throws ModuleException {
        // Check if a job of deletion already exists
        if (jobInfoService.retrieveJobsCount(FileDeletionRequestsCreatorJob.class.getName(), JobStatus.RUNNING) > 0) {
            throw new ModuleException("Impossible to run a files deletion process as a previous one is still running");
        } else {
            Set<JobParameter> parameters = Sets.newHashSet();
            parameters.add(new JobParameter(FileDeletionRequestsCreatorJob.STORAGE_LOCATION_ID, storageLocationId));
            parameters.add(new JobParameter(FileDeletionRequestsCreatorJob.FORCE_DELETE, forceDelete));
            parameters.add(new JobParameter(FileDeletionRequestsCreatorJob.SESSION_OWNER, sessionOwner));
            parameters.add(new JobParameter(FileDeletionRequestsCreatorJob.SESSION, session));
            JobInfo jobInfo = jobInfoService
                    .createAsQueued(new JobInfo(false, JobsPriority.FILE_DELETION_JOB.getPriority(), parameters,
                            authResolver.getUser(), FileDeletionRequestsCreatorJob.class.getName()));
            LOGGER.debug("[DELETION REQUESTS] Job scheduled to delete all files from storage location {} (force={}).",
                         storageLocationId, forceDelete);
            return jobInfo;
        }
    }

    /**
     * Delete all requests for the given storage identifier
     * @param storageLocationId
     */
    public void deleteByStorage(String storageLocationId, Optional<FileRequestStatus> status) {
        if (status.isPresent()) {
            fileDeletionRequestRepo.deleteByStorageAndStatus(storageLocationId, status.get());
        } else {
            fileDeletionRequestRepo.deleteByStorage(storageLocationId);
        }
    }

    /**
     * Retrieve expiration date for deletion request
     * @return
     */
    public OffsetDateTime getRequestExpirationDate() {
        if ((nbDaysBeforeExpiration != null) && (nbDaysBeforeExpiration > 0)) {
            return OffsetDateTime.now().plusDays(nbDaysBeforeExpiration);
        } else {
            return null;
        }
    }

}
