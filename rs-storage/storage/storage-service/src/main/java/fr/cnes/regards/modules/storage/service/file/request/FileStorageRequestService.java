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
import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestResultDTO;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;
import fr.cnes.regards.modules.storage.domain.flow.StorageFlowItem;
import fr.cnes.regards.modules.storage.domain.plugin.FileStorageWorkingSubset;
import fr.cnes.regards.modules.storage.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storage.domain.plugin.PreparationResponse;
import fr.cnes.regards.modules.storage.service.JobsPriority;
import fr.cnes.regards.modules.storage.service.file.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.job.FileStorageRequestJob;
import fr.cnes.regards.modules.storage.service.location.StoragePluginConfigurationHandler;

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

    @Autowired
    private RequestStatusService reqStatusService;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Value("${regards.storage.storage.requests.days.before.expiration:5}")
    private Integer nbDaysBeforeExpiration;

    @Value("${regards.storage.storage.requests.per.job:100}")
    private Integer nbRequestsPerJob;

    /**
     * Initialize new storage requests from Flow items.
     * @param list
     */
    public void store(List<StorageFlowItem> list) {
        Set<String> checksums = list.stream().map(StorageFlowItem::getFiles).flatMap(Set::stream)
                .map(FileStorageRequestDTO::getChecksum).collect(Collectors.toSet());
        Set<FileReference> existingOnes = fileRefService.search(checksums);
        Set<FileStorageRequest> existingRequests = fileStorageRequestRepo.findByMetaInfoChecksumIn(checksums);
        Set<FileDeletionRequest> existingDeletionRequests = fileDelReqService.searchByChecksums(checksums);
        for (StorageFlowItem item : list) {
            doStore(item.getFiles(), item.getGroupId(), existingOnes, existingRequests, existingDeletionRequests);
            reqGroupService.granted(item.getGroupId(), FileRequestType.STORAGE, item.getFiles().size(),
                                    getRequestExpirationDate());
        }
    }

    /**
     * Initialize new storage requests for a given group identifier. Parameter existingOnes is passed to improve performance in bulk creation to
     * avoid requesting {@link IFileReferenceRepository} on each request.
     * @param requests requests to handle
     * @param groupId requests group identifier
     * @param existingOnes Already existing file references
     * @param existingRequests Already existing requests
     */
    private void doStore(Collection<FileStorageRequestDTO> requests, String groupId,
            Collection<FileReference> existingOnes, Set<FileStorageRequest> existingRequests,
            Set<FileDeletionRequest> existingDeletionRequests) {
        // Retrieve already existing ones by checksum only to improve performance. The associated storage location is checked later
        LOGGER.trace("[STORAGE REQUESTS] Handling {} requests ...", requests.size());
        for (FileStorageRequestDTO request : requests) {
            long start = System.currentTimeMillis();
            // Check if the file already exists for the storage destination
            Optional<FileReference> oFileRef = existingOnes.stream()
                    .filter(f -> f.getMetaInfo().getChecksum().equals(request.getChecksum())
                            && f.getLocation().getStorage().equals(request.getStorage()))
                    .findFirst();
            Optional<FileStorageRequest> oReq = existingRequests.stream().filter(f -> f.getMetaInfo().getChecksum()
                    .equals(request.getChecksum()) && f.getStorage().equals(request.getStorage())
                    && ((f.getStatus() == FileRequestStatus.TO_DO) || (f.getStatus() == FileRequestStatus.ERROR)))
                    .findFirst();
            Optional<FileDeletionRequest> oDelReq = existingDeletionRequests.stream()
                    .filter(f -> f.getFileReference().getMetaInfo().getChecksum().equals(request.getChecksum())
                            && f.getStorage().equals(request.getStorage())
                            && f.getStatus().equals(FileRequestStatus.TO_DO))
                    .findFirst();
            RequestResult result = handleRequest(request, oFileRef, oReq, oDelReq, groupId);
            if (result.getFileReference().isPresent()) {
                // Update file reference in the list of file references existing
                existingOnes.removeIf(f -> f.getId().equals(result.getFileReference().get().getId()));
                existingOnes.add(result.getFileReference().get());
            }
            if (result.getStorageRequest().isPresent()) {
                // Update file reference in the list of file references existing
                existingRequests.removeIf(f -> f.getId().equals(result.getStorageRequest().get().getId()));
                existingRequests.add(result.getStorageRequest().get());
            }
            LOGGER.trace("[STORAGE REQUESTS] New request ({}) handled in {} ms", request.getFileName(),
                         System.currentTimeMillis() - start);
        }
    }

    /**
     * Store a new file to a given storage destination
     * @param owner Owner of the new file
     * @param sessionOwner Session information owner
     * @param session Session information name
     * @param metaInfo information about file
     * @param originUrl current location of file. This URL must be locally accessible to be copied.
     * @param storage name of the storage destination. Must be a existing plugin configuration of a {@link IStorageLocation}
     * @param subDirectory where to store file in the destination location.
     * @param groupId business request identifier
     * @return {@link FileReference} if the file is already referenced.
     */
    public Optional<FileReference> handleRequest(String owner, String sessionOwner, String session,
            FileReferenceMetaInfo metaInfo, String originUrl, String storage, Optional<String> subDirectory,
            String groupId) {
        Optional<FileReference> oFileRef = fileRefService.search(storage, metaInfo.getChecksum());
        Optional<FileStorageRequest> oReq = fileStorageRequestRepo.findByMetaInfoChecksum(metaInfo.getChecksum());
        Optional<FileDeletionRequest> oDeletionReq = fileDelReqService.search(metaInfo.getChecksum(), storage);
        FileStorageRequestDTO request = FileStorageRequestDTO
                .build(metaInfo.getFileName(), metaInfo.getChecksum(), metaInfo.getAlgorithm(),
                       metaInfo.getMimeType().toString(), owner, sessionOwner, session, originUrl, storage,
                       subDirectory);
        request.withType(metaInfo.getType());
        return handleRequest(request, oFileRef, oReq, oDeletionReq, groupId).getFileReference();
    }

    /**
     * Internal private class to regroup information about handle {@link FileStorageRequestDTO} result.
     * The result can be : <ul>
     * <li> {@link FileReference} : If the request is associated to a file already referenced.</li>
     * <li> {@link FileStorageRequest} : If the request is a new or updated storage request </li>
     *
     */
    private static class RequestResult {

        Optional<FileReference> fileReference = Optional.empty();

        Optional<FileStorageRequest> storageRequest = Optional.empty();

        public static RequestResult build(FileReference fileReference) {
            RequestResult res = new RequestResult();
            res.fileReference = Optional.ofNullable(fileReference);
            return res;
        }

        public static RequestResult build(FileStorageRequest storageRequest) {
            RequestResult res = new RequestResult();
            res.storageRequest = Optional.ofNullable(storageRequest);
            return res;
        }

        public Optional<FileReference> getFileReference() {
            return fileReference;
        }

        public Optional<FileStorageRequest> getStorageRequest() {
            return storageRequest;
        }

    }

    /**
     * Store a new file to a given storage destination
     * @param request {@link FileStorageRequestDTO} info about file to store
     * @param fileRef {@link FileReference} of associated file if already exists
     * @param oReq {@link FileStorageRequest} associated to given {@link FileStorageRequestDTO} if already exists
     * @param groupId business request identifier
     * @return {@link FileReference} if the file is already referenced.
     * @throws MalformedURLException
     */
    private RequestResult handleRequest(FileStorageRequestDTO request, Optional<FileReference> fileRef,
            Optional<FileStorageRequest> oReq, Optional<FileDeletionRequest> oDeletionReq, String groupId) {
        // init storage requester
        String sessionOwner = request.getSessionOwner();
        String session = request.getSession();
        // increment store request to the session agent
        this.sessionNotifier.incrementStoreRequests(sessionOwner, session);
        // Check if fileReference is present
        if (fileRef.isPresent()) {
            // handle file
            return handleFileToStoreAlreadyExists(fileRef.get(), request, oDeletionReq, groupId);
        } else if (oReq.isPresent() && oReq.get().getStatus() != FileRequestStatus.PENDING) {
            // If request already exists and is not handled yet, do not create a new request, just add
            FileStorageRequest existingReq = oReq.get();
            existingReq.update(request, groupId);
            if (existingReq.getStatus() == FileRequestStatus.ERROR) {
                // Allow retry of error requests when the same request is sent
                existingReq.setStatus(FileRequestStatus.TO_DO);
                // decrement errors to the session agent for the previous request session
                this.sessionNotifier.decrementErrorRequests(existingReq.getSessionOwner(), existingReq.getSession());
                // increment new request running for the new session
                this.sessionNotifier.incrementRunningRequests(sessionOwner, session);
            }
            LOGGER.trace("[STORAGE REQUESTS] Existing request ({}) updated to handle same file of request ({})",
                         existingReq.getMetaInfo().getFileName(), request.getFileName());
            return RequestResult.build(existingReq);
        } else {
            if (oReq.isPresent() && oReq.get().getStatus() == FileRequestStatus.PENDING) {
                LOGGER.debug("Request already exists but is already pending. Create a new storage request");
            }
            Optional<String> cause = Optional.empty();
            Optional<FileRequestStatus> status = Optional.empty();
            // Check that URL is a valid
            try {
                new URL(request.getOriginUrl());
            } catch (MalformedURLException e) {
                String errorMessage = "Invalid URL for file " + request.getFileName() + "storage. Cause : "
                        + e.getMessage();
                LOGGER.error(errorMessage);
                status = Optional.of(FileRequestStatus.ERROR);
                cause = Optional.of(errorMessage);
            }
            return RequestResult
                    .build(createNewFileStorageRequest(Sets.newHashSet(request.getOwner()), request.buildMetaInfo(),
                                                       request.getOriginUrl(), request.getStorage(),
                                                       request.getOptionalSubDirectory(), groupId, cause, status,
                                                       sessionOwner, session));
        }
    }

    /**
     * Search for {@link FileStorageRequest}s matching the given destination storage and checksum
     * @param destinationStorage
     * @param checksum
     * @return {@link FileStorageRequest}
     */
    @Transactional(readOnly = true)
    public Collection<FileStorageRequest> search(String destinationStorage, String checksum) {
        return fileStorageRequestRepo.findByMetaInfoChecksumAndStorage(checksum, destinationStorage);
    }

    /**
     * Search for {@link FileStorageRequest}s matching the given destination storage and checksum
     * @return {@link FileStorageRequest}
     */
    @Transactional(readOnly = true)
    public Page<FileStorageRequest> search(String destinationStorage, FileRequestStatus status, Pageable page) {
        return fileStorageRequestRepo.findAllByStorageAndStatus(destinationStorage, status, page);
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
        if (fileStorageRequestRepo.existsById(fileStorageRequest.getId())) {
            fileStorageRequestRepo.deleteById(fileStorageRequest.getId());
        } else {
            LOGGER.debug("Unable to delete file storage request {} cause it does not exists",
                         fileStorageRequest.getId());
        }
    }

    /**
     * Update all {@link FileStorageRequest} in error status to change status to {@link FileRequestStatus#TO_DO}.
     * @param groupId request business identifier to retry
     */
    public void retryRequest(String groupId) {
        for (FileStorageRequest request : fileStorageRequestRepo.findByGroupIdsAndStatus(groupId,
                                                                                         FileRequestStatus.ERROR)) {
            request.setStatus(reqStatusService.getNewStatus(request, Optional.empty()));
            request.setErrorCause(null);
            update(request);
            // Session handling
            String sessionOwner = request.getSessionOwner();
            String session = request.getSession();
            // decrement number of errors to the session agent
            this.sessionNotifier.decrementErrorRequests(sessionOwner, session);
            // increment number of running to the session agent
            this.sessionNotifier.incrementRunningRequests(sessionOwner, session);
        }
    }

    /**
     * Update all {@link FileStorageRequest} in error status to change status to {@link FileRequestStatus#TO_DO} for the given owners.
     */
    public void retry(Collection<String> owners) {
        Pageable page = PageRequest.of(0, nbRequestsPerJob, Sort.by(Direction.ASC, "id"));
        Page<FileStorageRequest> results;
        do {
            results = fileStorageRequestRepo.findByOwnersInAndStatus(owners, FileRequestStatus.ERROR, page);
            for (FileStorageRequest request : results) {
                request.setStatus(reqStatusService.getNewStatus(request, Optional.empty()));
                request.setErrorCause(null);
                update(request);
                // decrement number of errors to the session agent
                this.sessionNotifier.decrementErrorRequests(request.getSessionOwner(), request.getSession());
                // increment number of running to the session agent
                this.sessionNotifier.incrementRunningRequests(request.getSessionOwner(), request.getSession());
            }
            // Always retrieve the first page has we modify each element of the results.
            // All element are handled when result is empty.
        } while (results.hasNext());
    }

    /**
     * Update all {@link FileStorageRequest} in error status to change status to {@link FileRequestStatus#TO_DO} or
     * {@link FileRequestStatus#DELAYED}.
     */
    public void retryBySession(List<FileStorageRequest> requestList, String sessionOwner, String session) {
        int nbRequests = requestList.size();
        for (FileStorageRequest request : requestList) {
            // reset status
            request.setStatus(reqStatusService.getNewStatus(request, Optional.empty()));
            request.setErrorCause(null);
        }
        // save changes in database
        updateListRequests(requestList);
        // decrement error requests
        this.sessionNotifier.decrementErrorRequests(sessionOwner, session, nbRequests);
        // notify running requests to the session agent
        this.sessionNotifier.incrementRunningRequests(sessionOwner, session, nbRequests);
    }

    /**
     * Update a {@link FileStorageRequest}
     * @param fileStorageRequest to delete
     */
    public FileStorageRequest update(FileStorageRequest fileStorageRequest) {
        return fileStorageRequestRepo.save(fileStorageRequest);
    }

    /**
     * Update a list {@link FileStorageRequest}
     * @param fileStorageRequestList to delete
     */
    public List<FileStorageRequest> updateListRequests(List<FileStorageRequest> fileStorageRequestList) {
        return fileStorageRequestRepo.saveAll(fileStorageRequestList);
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
        LOGGER.trace("[STORAGE REQUESTS] Scheduling storage jobs ...");
        for (String storage : storagesToSchedule) {
            boolean productRemains;
            do {
                productRemains = self.scheduleJobsByStorage(jobList, storage, owners, status);
            } while (productRemains);
        }
        LOGGER.debug("[STORAGE REQUESTS] {} jobs scheduled in {} ms", jobList.size(),
                     System.currentTimeMillis() - start);
        return jobList;
    }

    /**
     * @param jobList
     * @param storage
     * @param owners
     * @param status
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean scheduleJobsByStorage(Collection<JobInfo> jobList, String storage,
            Collection<String> owners, FileRequestStatus status) {
        Page<FileStorageRequest> filesPage;
        Long maxId = 0L;
        // Always search the first page of requests until there is no requests anymore.
        // To do so, we order on id to ensure to not handle same requests multiple times.
        Pageable page = PageRequest.of(0, nbRequestsPerJob, Sort.by("id"));
        // Always retrieve first page, as request status are updated during job scheduling method.
        if ((owners != null) && !owners.isEmpty()) {
            filesPage = fileStorageRequestRepo
                    .findAllByStorageAndStatusAndOwnersInAndIdGreaterThan(storage, status, owners, maxId, page);
        } else {
            filesPage = fileStorageRequestRepo.findAllByStorageAndStatusAndIdGreaterThan(storage, status, maxId,
                                                                                         page);
        }
        if (filesPage.hasContent()) {
            maxId = filesPage.stream().max(Comparator.comparing(FileStorageRequest::getId)).get().getId();
            // SESSION HANDLING
            // if status is in error state decrement the number of requests in error
            if(status.equals(FileRequestStatus.ERROR)) {
                filesPage.getContent().forEach(req -> {
                    String sessionOwner = req.getSessionOwner();
                    String session = req.getSession();
                    sessionNotifier.decrementErrorRequests(sessionOwner, session);
                    sessionNotifier.incrementRunningRequests(sessionOwner, session);
                });
            }

            // SCHEDULER - schedule jobs by storage
            if (storageHandler.isConfigured(storage)) {
                jobList.addAll(scheduleJobsByStorage(storage,  filesPage.getContent()));
            } else {
                handleStorageNotAvailable(filesPage.getContent(), Optional.empty());
            }
        }
        return filesPage.hasContent();
    }

    /**
     * Schedule {@link FileStorageRequestJob}s for all given {@link FileStorageRequest}s and a given storage location.
     * @param storage
     * @param fileStorageRequests
     * @return {@link JobInfo}s scheduled
     */
    private Collection<JobInfo> scheduleJobsByStorage(String storage,
            Collection<FileStorageRequest> fileStorageRequests) {
        LOGGER.debug("Nb requests to schedule for storage {} = {}",storage,fileStorageRequests.size());
        Collection<JobInfo> jobInfoList = Sets.newHashSet();
        Collection<FileStorageRequest> remainingRequests = Sets.newHashSet();
        remainingRequests.addAll(fileStorageRequests);
        try {
            PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(storage);
            IStorageLocation storagePlugin = pluginService.getPlugin(conf.getBusinessId());
            PreparationResponse<FileStorageWorkingSubset, FileStorageRequest> response = storagePlugin
                    .prepareForStorage(fileStorageRequests);
            for (FileStorageWorkingSubset ws : response.getWorkingSubsets()) {
                if (!ws.getFileReferenceRequests().isEmpty()) {
                    LOGGER.debug("Scheduling 1 storage job for {} requests.",ws.getFileReferenceRequests().size());
                    jobInfoList.add(scheduleJob(ws, conf.getBusinessId(), storage));
                    remainingRequests.removeAll(ws.getFileReferenceRequests());
                }
            }
            // Handle preparation errors
            for (Entry<FileStorageRequest, String> request : response.getPreparationErrors().entrySet()) {
                this.handleStorageNotAvailable(request.getKey(), Optional.ofNullable(request.getValue()));
            }
            // Handle request not handled by the plugin preparation step.
            for (FileStorageRequest req : remainingRequests) {
                this.handleStorageNotAvailable(req, Optional.of("Request has not been handled by plugin."));
            }
        } catch (ModuleException | PluginUtilsRuntimeException | NotAvailablePluginConfigurationException e) {
            LOGGER.error(e.getMessage(), e);
            this.handleStorageNotAvailable(fileStorageRequests, Optional.of(e.getMessage()));
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
    private JobInfo scheduleJob(FileStorageWorkingSubset workingSubset, String plgBusinessId, String storage) {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(FileStorageRequestJob.DATA_STORAGE_CONF_BUSINESS_ID, plgBusinessId));
        parameters.add(new JobParameter(FileStorageRequestJob.WORKING_SUB_SET, workingSubset));
        JobInfo jobInfo = jobInfoService.createAsQueued(new JobInfo(false, JobsPriority.FILE_STORAGE_JOB.getPriority(),
                parameters, authResolver.getUser(), FileStorageRequestJob.class.getName()));
        workingSubset.getFileReferenceRequests().forEach(fr -> fileStorageRequestRepo
                .updateStatusAndJobId(FileRequestStatus.PENDING, jobInfo.getId().toString(), fr.getId()));
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
     * @param groupId Business identifier of the deletion request
     * @param status
     * @param sessionOwner
     * @param session
     */
    public FileStorageRequest createNewFileStorageRequest(Collection<String> owners, FileReferenceMetaInfo fileMetaInfo,
            String originUrl, String storage, Optional<String> storageSubDirectory, String groupId,
            Optional<String> errorCause, Optional<FileRequestStatus> status, String sessionOwner, String session) {
        long start = System.currentTimeMillis();
        FileStorageRequest fileStorageRequest = new FileStorageRequest(owners, fileMetaInfo, originUrl, storage,
                storageSubDirectory, groupId, sessionOwner, session);
        fileStorageRequest.setStatus(reqStatusService.getNewStatus(fileStorageRequest, status));
        fileStorageRequest.setErrorCause(errorCause.orElse(null));
        // notify request is running to the session agent
        this.sessionNotifier.incrementRunningRequests(fileStorageRequest.getSessionOwner(), fileStorageRequest.getSession());
        // check if a storage is configured
        if (!storageHandler.isConfigured(storage)) {
            // The storage destination is unknown, we can already set the request in error status
            handleStorageNotAvailable(fileStorageRequest, Optional.empty());
        } else {
            // save request
            fileStorageRequestRepo.save(fileStorageRequest);

            LOGGER.trace("[STORAGE REQUESTS] New file storage request created for file <{}> to store to {} with status {} in {}ms",
                         fileStorageRequest.getMetaInfo().getFileName(), fileStorageRequest.getStorage(),
                         fileStorageRequest.getStatus(), System.currentTimeMillis() - start);
        }
        return fileStorageRequest;
    }

    /**
     * Update a list of {@link FileStorageRequest}s when the storage destination cannot be handled.
     * A storage destination cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     * @param fileStorageRequests
     */
    private void handleStorageNotAvailable(Collection<FileStorageRequest> fileStorageRequests,
            Optional<String> errorCause) {
        fileStorageRequests.forEach(r -> handleStorageNotAvailable(r, errorCause));
    }

    /**
     * Update a {@link FileStorageRequest} when the storage destination cannot be handled.
     * A storage destination cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     * @param fileStorageRequest
     */
    private void handleStorageNotAvailable(FileStorageRequest fileStorageRequest, Optional<String> errorCause) {
        long start = System.currentTimeMillis();
        // The storage destination is unknown, we can already set the request in error status
        String lErrorCause = errorCause.orElse(String
                .format("Storage request <%s> cannot be handle as destination storage <%s> is unknown or not accessible (offline).",
                        fileStorageRequest.getMetaInfo().getFileName(), fileStorageRequest.getStorage()));
        fileStorageRequest.setStatus(FileRequestStatus.ERROR);
        fileStorageRequest.setErrorCause(lErrorCause);
        update(fileStorageRequest);
        LOGGER.error(fileStorageRequest.getErrorCause());
        // increment error requests to the session agent
        String sessionOwner = fileStorageRequest.getSessionOwner();
        String session = fileStorageRequest.getSession();
        this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
        sessionNotifier.incrementErrorRequests(sessionOwner, session);
        // Send notification for file storage error
        eventPublisher.storeError(fileStorageRequest.getMetaInfo().getChecksum(), fileStorageRequest.getOwners(),
                                  fileStorageRequest.getStorage(), fileStorageRequest.getErrorCause(),
                                  fileStorageRequest.getGroupIds());
        // Inform request groups that the request is in error
        for (String groupId : fileStorageRequest.getGroupIds()) {
            reqGroupService.requestError(groupId, FileRequestType.STORAGE,
                                         fileStorageRequest.getMetaInfo().getChecksum(),
                                         fileStorageRequest.getStorage(), fileStorageRequest.getStorageSubDirectory(),
                                         fileStorageRequest.getOwners(), lErrorCause);
        }
        LOGGER.debug("[STORAGE REQUESTS] Request {} set as error in {} ms. Cause : {}",
                     fileStorageRequest.getMetaInfo().getFileName(), System.currentTimeMillis() - start, lErrorCause);
    }

    public void handleSuccess(Collection<FileStorageRequestResultDTO> results) {
        for (FileStorageRequestResultDTO result : results) {
            FileStorageRequest request = result.getRequest();
            FileReferenceMetaInfo reqMetaInfos = request.getMetaInfo();
            Set<FileReference> fileRefs = Sets.newHashSet();
            // parameters for session notification
            String sessionOwner = request.getSessionOwner();
            String session = request.getSession();
            int nbFilesStored = 0;

            for (String owner : result.getRequest().getOwners()) {
                try {
                    FileReferenceMetaInfo fileMeta = new FileReferenceMetaInfo(reqMetaInfos.getChecksum(),
                            reqMetaInfos.getAlgorithm(), reqMetaInfos.getFileName(), result.getFileSize(),
                            reqMetaInfos.getMimeType());
                    fileMeta.setHeight(reqMetaInfos.getHeight());
                    fileMeta.setWidth(reqMetaInfos.getWidth());
                    fileMeta.setType(reqMetaInfos.getType());
                    fileRefs.add(fileRefReqService
                            .reference(owner, fileMeta, new FileLocation(request.getStorage(), result.getStoredUrl()),
                                       request.getGroupIds(), sessionOwner, session));
                    nbFilesStored++;
                } catch (ModuleException e) {
                    LOGGER.error(e.getMessage(), e);
                    handleError(request, e.getMessage());
                }
            }

            for (String groupId : request.getGroupIds()) {
                for (FileReference fileRef : fileRefs) {
                    reqGroupService.requestSuccess(groupId, FileRequestType.STORAGE,
                                                   fileRef.getMetaInfo().getChecksum(),
                                                   fileRef.getLocation().getStorage(), request.getStorageSubDirectory(),
                                                   request.getOwners(), fileRef);
                }
            }

            // Session handling
            // decrement the number of running requests
            this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
            // notify the number of successful created files
            this.sessionNotifier.incrementStoredFiles(sessionOwner, session, nbFilesStored);

            // Delete the FileRefRequest as it has been handled
            delete(request);
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
                                         request.getStorage(), request.getStorageSubDirectory(), request.getOwners(),
                                         errorCause);
        }
        // notify error to the session agent
        String sessionOwner = request.getSessionOwner();
        String session = request.getSession();
        this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
        this.sessionNotifier.incrementErrorRequests(sessionOwner, session);
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
    private RequestResult handleFileToStoreAlreadyExists(FileReference fileReference, FileStorageRequestDTO request,
            Optional<FileDeletionRequest> oDeletionRequest, String groupId) {
        long start = System.currentTimeMillis();
        FileReferenceMetaInfo newMetaInfo = request.buildMetaInfo();
        if (oDeletionRequest.isPresent()) {
            // Deletion is running write now, so delay the new file reference creation with a FileReferenceRequest
            return RequestResult.build(createNewFileStorageRequest(Sets.newHashSet(request.getOwner()), newMetaInfo,
                                                                   request.getOriginUrl(), request.getStorage(),
                                                                   request.getOptionalSubDirectory(), groupId,
                                                                   Optional.empty(), Optional.empty(),
                                                                   request.getSessionOwner(), request.getSession()));
        } else {
            String message = String
                    .format("New owner <%s> added to existing referenced file <%s> at <%s> (checksum: %s) ",
                            request.getOwner(), fileReference.getMetaInfo().getFileName(),
                            fileReference.getLocation().toString(), fileReference.getMetaInfo().getChecksum());
            eventPublisher.storeSuccess(fileReference, message, Sets.newHashSet(groupId),
                                        Lists.newArrayList(request.getOwner()));
            fileReference.getLazzyOwners().add(request.getOwner());
            reqGroupService.requestSuccess(groupId, FileRequestType.STORAGE, request.getChecksum(),
                                           request.getStorage(), request.getOptionalSubDirectory().orElse(null),
                                           Sets.newHashSet(request.getOwner()), fileReference);
            LOGGER.trace("[STORAGE REQUESTS] Storage request {} succeded for existing reference {} in {}ms.",
                         request.getFileName(), fileReference.getId(), System.currentTimeMillis() - start);
            // notify the number of successful created files
            this.sessionNotifier.incrementStoredFiles(request.getSessionOwner(), request.getSession(), 1);
            return RequestResult.build(fileReference);
        }
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

    /**
     * @param storageId
     * @return
     */
    public boolean isStorageRunning(String storageId) {
        return fileStorageRequestRepo
                .existsByStorageAndStatusIn(storageId,
                                            Sets.newHashSet(FileRequestStatus.TO_DO, FileRequestStatus.PENDING));
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
