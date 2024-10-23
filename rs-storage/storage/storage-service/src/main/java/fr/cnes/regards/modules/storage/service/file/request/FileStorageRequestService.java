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
package fr.cnes.regards.modules.storage.service.file.request;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.fileaccess.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestAggregationDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestResultDto;
import fr.cnes.regards.modules.fileaccess.plugin.domain.FileStorageWorkingSubset;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IStorageLocation;
import fr.cnes.regards.modules.fileaccess.plugin.domain.PreparationResponse;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storage.domain.FileReferenceResult;
import fr.cnes.regards.modules.storage.domain.FileReferenceResultStatusEnum;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.StorageJobsPriority;
import fr.cnes.regards.modules.storage.service.file.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.job.FileStorageRequestJob;
import fr.cnes.regards.modules.storage.service.file.job.PeriodicStorageLocationJob;
import fr.cnes.regards.modules.storage.service.location.StoragePluginConfigurationHandler;
import fr.cnes.regards.modules.storage.service.session.SessionNotifier;
import fr.cnes.regards.modules.storage.service.template.StorageTemplatesConf;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import freemarker.template.TemplateException;
import jakarta.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Service to handle {@link FileStorageRequestAggregation}s.
 * Those requests are created when a file reference need to be stored physically thanks to an existing {@link IStorageLocation} plugin.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class FileStorageRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageRequestService.class);

    private static final List<FileRequestStatus> STORED_REQUEST_STATUSES = List.of(FileRequestStatus.TO_DO,
                                                                                   FileRequestStatus.ERROR,
                                                                                   FileRequestStatus.PENDING);

    private final IPluginService pluginService;

    private final IFileStorageRequestRepository fileStorageRequestRepo;

    private final IJobInfoService jobInfoService;

    private final IAuthenticationResolver authResolver;

    private final FileReferenceEventPublisher eventPublisher;

    private final StoragePluginConfigurationHandler storageHandler;

    private final RequestsGroupService reqGroupService;

    private final FileReferenceService fileRefService;

    private final FileDeletionRequestService fileDelReqService;

    private final FileReferenceRequestService fileRefReqService;

    private final RequestStatusService reqStatusService;

    private final SessionNotifier sessionNotifier;

    protected FileStorageRequestService self;

    private final IJobInfoRepository jobInfoRepo;

    private final INotificationClient notificationClient;

    private final ITemplateService templateService;

    private final Validator validator;

    @Value("${regards.storage.storage.requests.days.before.expiration:5}")
    private Integer nbDaysBeforeExpiration;

    @Value("${regards.storage.storage.requests.per.job:100}")
    private Integer nbRequestsPerJob;

    public FileStorageRequestService(IPluginService pluginService,
                                     IFileStorageRequestRepository fileStorageRequestRepo,
                                     IJobInfoService jobInfoService,
                                     IAuthenticationResolver authResolver,
                                     FileReferenceEventPublisher eventPublisher,
                                     StoragePluginConfigurationHandler storageHandler,
                                     FileStorageRequestService fileStorageRequestService,
                                     RequestsGroupService reqGroupService,
                                     FileReferenceService fileRefService,
                                     FileDeletionRequestService fileDelReqService,
                                     FileReferenceRequestService fileRefReqService,
                                     RequestStatusService reqStatusService,
                                     SessionNotifier sessionNotifier,
                                     IJobInfoRepository jobInfoRepo,
                                     INotificationClient notificationClient,
                                     ITemplateService templateService,
                                     Validator validator) {
        this.pluginService = pluginService;
        this.fileStorageRequestRepo = fileStorageRequestRepo;
        this.jobInfoService = jobInfoService;
        this.authResolver = authResolver;
        this.eventPublisher = eventPublisher;
        this.storageHandler = storageHandler;
        this.self = fileStorageRequestService;
        this.reqGroupService = reqGroupService;
        this.fileRefService = fileRefService;
        this.fileDelReqService = fileDelReqService;
        this.fileRefReqService = fileRefReqService;
        this.reqStatusService = reqStatusService;
        this.sessionNotifier = sessionNotifier;
        this.jobInfoRepo = jobInfoRepo;
        this.notificationClient = notificationClient;
        this.templateService = templateService;
        this.validator = validator;
    }

    /**
     * Initialize new storage requests from events.
     *
     * @param list storage requests
     */
    public void store(List<FilesStorageRequestEvent> list) {
        Set<String> checksums = list.stream()
                                    .map(FilesStorageRequestEvent::getFiles)
                                    .flatMap(Set::stream)
                                    .map(FileStorageRequestDto::getChecksum)
                                    .collect(Collectors.toSet());
        Set<FileReference> existingFiles = fileRefService.search(checksums);
        Set<FileStorageRequestAggregation> existingRequests = fileStorageRequestRepo.findByMetaInfoChecksumIn(checksums);
        Set<FileDeletionRequest> existingDeletionRequests = fileDelReqService.searchByChecksums(checksums);
        for (FilesStorageRequestEvent item : list) {

            Errors errors = new MapBindingResult(new HashMap<>(), FileReferenceRequestDto.class.getName());
            validator.validate(item, errors);
            if (errors.hasErrors()) {
                reqGroupService.denied(item.getGroupId(),
                                       FileRequestType.REFERENCE,
                                       ErrorTranslator.getErrorsAsString(errors));
                // notify denied requests to the session agent
                item.getFiles().forEach(file -> {
                    String sessionOwner = file.getSessionOwner();
                    String session = file.getSession();
                    this.sessionNotifier.incrementReferenceRequests(sessionOwner, session);
                    this.sessionNotifier.incrementDeniedRequests(sessionOwner, session);
                });
            } else {
                doStore(item.getFiles(), item.getGroupId(), existingFiles, existingRequests, existingDeletionRequests);
                reqGroupService.granted(item.getGroupId(),
                                        FileRequestType.STORAGE,
                                        item.getFiles().size(),
                                        getRequestExpirationDate());
            }
        }
    }

    /**
     * Compare {@link FileStorageRequestDto} and {@link FileStorageRequestAggregation} to check if the two requests handle the
     * same file with identical checksum and storage location.
     */
    private boolean isIdenticalRequest(FileStorageRequestDto requestDto, FileStorageRequestAggregation request) {
        return StringUtils.equals(request.getMetaInfo().getChecksum(), requestDto.getChecksum())
               && StringUtils.equals(request.getStorage(), requestDto.getStorage());
    }

    /**
     * Find more valuable request form list of existing requests and matching the request to handle.
     */
    private Optional<FileStorageRequestAggregation> findMoreDiscriminantRequest(FileStorageRequestDto requestToHandle,
                                                                                Collection<FileStorageRequestAggregation> existingRequests) {
        return Optional.ofNullable(existingRequests.stream()
                                                   .filter(existingRequest -> isIdenticalRequest(requestToHandle,
                                                                                                 existingRequest))
                                                   .reduce(null, this::findMoreDiscriminantRequestByStatus));
    }

    /**
     * Find more valuable request between the given ones by status order like ERROR > DELAYED > TO_DO > PENDING.
     */
    private FileStorageRequestAggregation findMoreDiscriminantRequestByStatus(FileStorageRequestAggregation request1,
                                                                              FileStorageRequestAggregation request2) {
        // We are trying to retrieve the more discriminant request from all existing requests with the same checksum
        // and storage that the current handling request
        if (request1 == null) {
            return request2;
        }
        return switch (request1.getStatus()) {
            case ERROR, DELAYED, TO_DO -> request1;
            case PENDING -> request2;
        };
    }

    /**
     * Initialize new storage requests for a given group identifier. Parameter existingFiles is passed to improve performance in bulk creation to
     * avoid requesting {@link IFileReferenceRepository} on each request.
     *
     * @param requests         requests to handle
     * @param groupId          requests group identifier
     * @param existingFiles    Already existing file references
     * @param existingRequests Already existing requests
     */
    private void doStore(Collection<FileStorageRequestDto> requests,
                         String groupId,
                         Collection<FileReference> existingFiles,
                         Set<FileStorageRequestAggregation> existingRequests,
                         Set<FileDeletionRequest> existingDeletionRequests) {
        // Retrieve already existing ones by checksum only to improve performance. The associated storage location is checked later
        LOGGER.trace("[STORAGE REQUESTS] Handling {} requests ...", requests.size());
        for (FileStorageRequestDto request : requests) {
            long start = System.currentTimeMillis();
            // Check if the file already exists for the storage destination
            Optional<FileReference> oFileRef = existingFiles.stream()
                                                            .filter(f -> f.getMetaInfo()
                                                                          .getChecksum()
                                                                          .equals(request.getChecksum())
                                                                         && f.getLocation()
                                                                             .getStorage()
                                                                             .equals(request.getStorage()))
                                                            .findFirst();
            Optional<FileStorageRequestAggregation> oReq = findMoreDiscriminantRequest(request, existingRequests);
            Optional<FileDeletionRequest> oDelReq = existingDeletionRequests.stream()
                                                                            .filter(f -> f.getFileReference()
                                                                                          .getMetaInfo()
                                                                                          .getChecksum()
                                                                                          .equals(request.getChecksum())
                                                                                         && f.getStorage()
                                                                                             .equals(request.getStorage())
                                                                                         && f.getStatus()
                                                                                             .equals(FileRequestStatus.TO_DO))
                                                                            .findFirst();
            RequestResult result = handleRequest(request, oFileRef, oReq, oDelReq, groupId);
            Optional<FileReference> optionalFileReference = result.getFileReference();
            if (optionalFileReference.isPresent()) {
                // Update file reference in the list of file references existing
                existingFiles.removeIf(f -> f.getId().equals(optionalFileReference.get().getId()));
                existingFiles.add(optionalFileReference.get());
            }
            Optional<FileStorageRequestAggregation> optionalStorageRequest = result.getStorageRequest();
            if (optionalStorageRequest.isPresent()) {
                // Update storage request in the list of existing storage requests
                existingRequests.removeIf(storageRequest -> storageRequest.getId()
                                                                          .equals(optionalStorageRequest.get()
                                                                                                        .getId()));
                existingRequests.add(optionalStorageRequest.get());
            }
            LOGGER.trace("[STORAGE REQUESTS] New request ({}) handled in {} ms",
                         request.getFileName(),
                         System.currentTimeMillis() - start);
        }
    }

    /**
     * Store a new file to a given storage destination
     *
     * @param owner        Owner of the new file
     * @param sessionOwner Session information owner
     * @param session      Session information name
     * @param metaInfo     information about file
     * @param originUrl    current location of file. This URL must be locally accessible to be copied.
     * @param storage      name of the storage destination. Must be a existing plugin configuration of a {@link IStorageLocation}
     * @param subDirectory where to store file in the destination location.
     * @param groupId      business request identifier
     * @return {@link FileReference} if the file is already referenced.
     */
    public Optional<FileReference> handleRequest(String owner,
                                                 String sessionOwner,
                                                 String session,
                                                 FileReferenceMetaInfo metaInfo,
                                                 String originUrl,
                                                 String storage,
                                                 Optional<String> subDirectory,
                                                 String groupId) {
        Optional<FileReference> oFileRef = fileRefService.search(storage, metaInfo.getChecksum());
        Optional<FileStorageRequestAggregation> oReq = fileStorageRequestRepo.findByMetaInfoChecksum(metaInfo.getChecksum());
        Optional<FileDeletionRequest> oDeletionReq = fileDelReqService.search(metaInfo.getChecksum(), storage);
        FileStorageRequestDto request = FileStorageRequestDto.build(metaInfo.getFileName(),
                                                                    metaInfo.getChecksum(),
                                                                    metaInfo.getAlgorithm(),
                                                                    metaInfo.getMimeType().toString(),
                                                                    owner,
                                                                    sessionOwner,
                                                                    session,
                                                                    originUrl,
                                                                    storage,
                                                                    metaInfo.toDto(),
                                                                    subDirectory);
        request.withType(metaInfo.getType());
        return handleRequest(request, oFileRef, oReq, oDeletionReq, groupId).getFileReference();
    }

    /**
     * Store a new file to a given storage destination
     *
     * @param request {@link FileStorageRequestDto} info about file to store
     * @param fileRef {@link FileReference} of associated file if already exists
     * @param oReq    {@link FileStorageRequestAggregation} associated to given {@link FileStorageRequestDto} if already exists
     * @param groupId business request identifier
     * @return {@link FileReference} if the file is already referenced.
     */
    private RequestResult handleRequest(FileStorageRequestDto request,
                                        Optional<FileReference> fileRef,
                                        Optional<FileStorageRequestAggregation> oReq,
                                        Optional<FileDeletionRequest> oDeletionReq,
                                        String groupId) {
        // init storage requester
        String sessionOwner = request.getSessionOwner();
        String session = request.getSession();
        // increment store request to the session agent
        this.sessionNotifier.incrementStoreRequests(sessionOwner, session);
        // Check if fileReference is present
        if (fileRef.isPresent()) {
            LOGGER.debug("Handling incoming request for file {} : File already exists", request.getFileName());
            // handle file
            return handleFileToStoreAlreadyExists(fileRef.get(), request, oDeletionReq, groupId);
        } else if (oReq.isPresent()) {
            FileStorageRequestAggregation existingReq = oReq.get();
            LOGGER.debug("Handling incoming request for file {} : A request on this file already exists in status {}",
                         request.getFileName(),
                         existingReq.getStatus());
            switch (existingReq.getStatus()) {
                case DELAYED:
                case TO_DO:
                case PENDING:
                    // Create new request in DELAYED state so it can be handled once the other one is over.
                    // Delayed identical requests are un-delayed and merge if possible during the un-delay task
                    // see reqStatusService.checkDelayedStorageRequests
                    LOGGER.info("Storage request for file {}/{} (checksum={}) already pending, create a new request in "
                                + "delayed status.",
                                request.getFileName(),
                                request.getStorage(),
                                request.getChecksum());
                    return saveNewFileStorageRequest(request,
                                                     groupId,
                                                     sessionOwner,
                                                     session,
                                                     FileRequestStatus.DELAYED);
                case ERROR:
                    // If request already exists and in ERROR status retry exiting one instead of create a new request
                    existingReq.update(request, groupId);
                    // retry
                    // Allow retry of error requests when the same request is sent
                    existingReq.setStatus(FileRequestStatus.TO_DO);
                    // decrement errors to the session agent for the previous request session
                    this.sessionNotifier.decrementErrorRequests(existingReq.getSessionOwner(),
                                                                existingReq.getSession());
                    // increment new request running for the new session
                    this.sessionNotifier.incrementRunningRequests(sessionOwner, session);
                    LOGGER.trace(
                        "[STORAGE REQUESTS] Existing request ({}) in ERROR state updated to handle same file of "
                        + "request ({})",
                        existingReq.getId(),
                        request.getFileName());
                    break;
                default:
                    throw new IllegalStateException("Unknown request state " + existingReq.getStatus());
            }
            return RequestResult.build(existingReq);
        } else {
            LOGGER.debug("Handling incoming request for file {} : Creating new request ", request.getFileName());
            return saveNewFileStorageRequest(request, groupId, sessionOwner, session);
        }
    }

    /**
     * Creates a new {@link FileStorageRequestAggregation} for the given dto
     */
    private RequestResult saveNewFileStorageRequest(FileStorageRequestDto request,
                                                    String groupId,
                                                    String sessionOwner,
                                                    String session) {
        return saveNewFileStorageRequest(request, groupId, sessionOwner, session, null);
    }

    /**
     * Creates a new {@link FileStorageRequestAggregation} for the given dto with given status.
     */
    private RequestResult saveNewFileStorageRequest(FileStorageRequestDto requestDto,
                                                    String groupId,
                                                    String sessionOwner,
                                                    String session,
                                                    @Nullable FileRequestStatus status) {
        Optional<String> cause = Optional.empty();
        Optional<FileRequestStatus> oStatus = Optional.ofNullable(status);
        // Check that URL is a valid
        try {
            new URL(requestDto.getOriginUrl());
        } catch (MalformedURLException e) {
            String errorMessage = "Invalid URL for file "
                                  + requestDto.getFileName()
                                  + "storage. Cause : "
                                  + e.getMessage();
            LOGGER.error(errorMessage);
            oStatus = Optional.of(FileRequestStatus.ERROR);
            cause = Optional.of(errorMessage);
        }
        return RequestResult.build(createNewFileStorageRequest(Sets.newHashSet(requestDto.getOwner()),
                                                               FileReferenceMetaInfo.buildFromDto(requestDto.getMetaInfo()),
                                                               requestDto.getOriginUrl(),
                                                               requestDto.getStorage(),
                                                               requestDto.getOptionalSubDirectory(),
                                                               groupId,
                                                               cause,
                                                               oStatus,
                                                               sessionOwner,
                                                               session));
    }

    /**
     * Search for {@link FileStorageRequestAggregation}s matching the given destination storage and checksum
     *
     * @return {@link FileStorageRequestAggregation}
     */
    @Transactional(readOnly = true)
    public Collection<FileStorageRequestAggregation> search(String destinationStorage, String checksum) {
        return fileStorageRequestRepo.findByMetaInfoChecksumAndStorage(checksum, destinationStorage);
    }

    /**
     * Search for {@link FileStorageRequestAggregation}s matching the given destination storage and checksum
     *
     * @return {@link FileStorageRequestAggregation}
     */
    @Transactional(readOnly = true)
    public Page<FileStorageRequestAggregation> search(String destinationStorage,
                                                      FileRequestStatus status,
                                                      Pageable page) {
        return fileStorageRequestRepo.findAllByStorageAndStatus(destinationStorage, status, page);
    }

    /**
     * Search for all {@link FileStorageRequestAggregation}s
     *
     * @return {@link FileStorageRequestAggregation}s by page
     */
    @Transactional(readOnly = true)
    public Page<FileStorageRequestAggregation> search(Pageable pageable) {
        return fileStorageRequestRepo.findAll(pageable);
    }

    /**
     * Search for {@link FileStorageRequestAggregation}s associated to the given destination storage location.
     *
     * @return {@link FileStorageRequestAggregation}s by page
     */
    @Transactional(readOnly = true)
    public Page<FileStorageRequestAggregation> search(String destinationStorage, Pageable pageable) {
        return fileStorageRequestRepo.findByStorage(destinationStorage, pageable);
    }

    @Transactional(readOnly = true)
    public Long count(String storage, FileRequestStatus status) {
        return fileStorageRequestRepo.countByStorageAndStatus(storage, status);
    }

    /**
     * Delete a {@link FileStorageRequestAggregation}
     *
     * @param fileStorageRequest to delete
     */
    public void delete(FileStorageRequestAggregation fileStorageRequest) {
        if (fileStorageRequestRepo.existsById(fileStorageRequest.getId())) {
            fileStorageRequestRepo.deleteById(fileStorageRequest.getId());
        } else {
            LOGGER.debug("Unable to delete file storage request {} cause it does not exists",
                         fileStorageRequest.getId());
        }
    }

    /**
     * Update all {@link FileStorageRequestAggregation} in error status to change status to {@link FileRequestStatus#TO_DO}.
     *
     * @param groupId request business identifier to retry
     */
    public void retryRequest(String groupId) {
        for (FileStorageRequestAggregation request : fileStorageRequestRepo.findByGroupIdsAndStatus(groupId,
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
     * Update all {@link FileStorageRequestAggregation} in error status to change status to {@link FileRequestStatus#TO_DO} for the given owners.
     */
    public void retry(Collection<String> owners) {
        Pageable page = PageRequest.of(0, nbRequestsPerJob, Sort.by(Direction.ASC, "id"));
        Page<FileStorageRequestAggregation> results;
        do {
            results = fileStorageRequestRepo.findByOwnersInAndStatus(owners, FileRequestStatus.ERROR, page);
            for (FileStorageRequestAggregation request : results) {
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
     * Update all {@link FileStorageRequestAggregation} in error status to change status to {@link FileRequestStatus#TO_DO} or
     * {@link FileRequestStatus#DELAYED}.
     */
    public void retryBySession(List<FileStorageRequestAggregation> requestList, String sessionOwner, String session) {
        int nbRequests = requestList.size();
        for (FileStorageRequestAggregation request : requestList) {
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
     * Update a {@link FileStorageRequestAggregation}
     *
     * @param fileStorageRequest to delete
     */
    public FileStorageRequestAggregation update(FileStorageRequestAggregation fileStorageRequest) {
        return fileStorageRequestRepo.save(fileStorageRequest);
    }

    /**
     * Update a list {@link FileStorageRequestAggregation}
     *
     * @param fileStorageRequestList to delete
     */
    public List<FileStorageRequestAggregation> updateListRequests(List<FileStorageRequestAggregation> fileStorageRequestList) {
        return fileStorageRequestRepo.saveAll(fileStorageRequestList);
    }

    /**
     * Schedule {@link FileStorageRequestJob}s for all {@link FileStorageRequestAggregation}s matching the given parameters
     *
     * @param status   of the request to handle
     * @param storages of the request to handle
     * @param owners   of the request to handle
     * @return {@link JobInfo}s scheduled
     */
    public Collection<JobInfo> scheduleJobs(FileRequestStatus status,
                                            Collection<String> storages,
                                            Collection<String> owners) {
        Collection<JobInfo> jobList = Lists.newArrayList();
        Set<String> allStorages = fileStorageRequestRepo.findStoragesByStatus(status);
        Set<String> storagesToSchedule = (storages != null) && !storages.isEmpty() ?
            allStorages.stream().filter(storages::contains).collect(Collectors.toSet()) :
            allStorages;
        long start = System.currentTimeMillis();
        LOGGER.trace("[STORAGE REQUESTS] Scheduling storage jobs ...");
        for (String storage : storagesToSchedule) {
            boolean productRemains;
            do {
                productRemains = self.scheduleJobsByStorage(jobList, storage, owners, status);
            } while (productRemains);
        }
        LOGGER.debug("[STORAGE REQUESTS] {} jobs scheduled in {} ms",
                     jobList.size(),
                     System.currentTimeMillis() - start);
        return jobList;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean scheduleJobsByStorage(Collection<JobInfo> jobList,
                                         String storage,
                                         Collection<String> owners,
                                         FileRequestStatus status) {
        Page<FileStorageRequestAggregation> filesPage;
        Long maxId = 0L;
        // Always search the first page of requests until there is no requests anymore.
        // To do so, we order on id to ensure to not handle same requests multiple times.
        Pageable page = PageRequest.of(0, nbRequestsPerJob, Sort.by("id"));
        // Always retrieve first page, as request status are updated during job scheduling method.
        if ((owners != null) && !owners.isEmpty()) {
            filesPage = fileStorageRequestRepo.findAllByStorageAndStatusAndOwnersInAndIdGreaterThan(storage,
                                                                                                    status,
                                                                                                    owners,
                                                                                                    maxId,
                                                                                                    page);
        } else {
            filesPage = fileStorageRequestRepo.findAllByStorageAndStatusAndIdGreaterThan(storage, status, maxId, page);
        }
        if (filesPage.hasContent()) {
            // SESSION HANDLING
            // if status is in error state decrement the number of requests in error
            if (status.equals(FileRequestStatus.ERROR)) {
                filesPage.getContent().forEach(req -> {
                    String sessionOwner = req.getSessionOwner();
                    String session = req.getSession();
                    sessionNotifier.decrementErrorRequests(sessionOwner, session);
                    sessionNotifier.incrementRunningRequests(sessionOwner, session);
                });
            }

            // SCHEDULER - schedule jobs by storage
            if (storageHandler.isConfigured(storage)) {
                jobList.addAll(scheduleJobsByStorage(storage, filesPage.getContent()));
            } else {
                handleStorageNotAvailable(filesPage.getContent(), Optional.empty());
            }
        }
        return filesPage.hasContent();
    }

    /**
     * Schedule {@link FileStorageRequestJob}s for all given {@link FileStorageRequestAggregation}s and a given storage location.
     *
     * @return {@link JobInfo}s scheduled
     */
    private Collection<JobInfo> scheduleJobsByStorage(String storage,
                                                      Collection<FileStorageRequestAggregation> fileStorageRequests) {
        LOGGER.debug("Nb requests to schedule for storage {} = {}", storage, fileStorageRequests.size());
        Collection<JobInfo> jobInfoList = Sets.newHashSet();
        Collection<FileStorageRequestAggregationDto> remainingRequests = Sets.newHashSet();
        List<FileStorageRequestAggregationDto> dtoList = fileStorageRequests.stream()
                                                                            .map(FileStorageRequestAggregation::toDto)
                                                                            .toList();
        remainingRequests.addAll(dtoList);
        try {
            PluginConfiguration conf = pluginService.getPluginConfigurationByLabel(storage);
            IStorageLocation storagePlugin = pluginService.getPlugin(conf.getBusinessId());
            PreparationResponse<FileStorageWorkingSubset, FileStorageRequestAggregationDto> response = storagePlugin.prepareForStorage(
                dtoList);
            for (FileStorageWorkingSubset ws : response.getWorkingSubsets()) {
                if (!ws.getFileReferenceRequests().isEmpty()) {
                    LOGGER.debug("Scheduling 1 storage job for {} requests.", ws.getFileReferenceRequests().size());
                    jobInfoList.add(scheduleJob(ws, conf.getBusinessId(), storage));
                    remainingRequests.removeAll(ws.getFileReferenceRequests());
                }
            }
            // Handle preparation errors
            for (Entry<FileStorageRequestAggregationDto, String> request : response.getPreparationErrors().entrySet()) {
                this.handleStorageNotAvailable(FileStorageRequestAggregation.fromDto(request.getKey()),
                                               Optional.ofNullable(request.getValue()));
            }
            // Handle request not handled by the plugin preparation step.
            for (FileStorageRequestAggregationDto req : remainingRequests) {
                this.handleStorageNotAvailable(FileStorageRequestAggregation.fromDto(req),
                                               Optional.of("Request has not been handled by plugin."));
            }
        } catch (ModuleException | PluginUtilsRuntimeException e) {
            LOGGER.error(e.getMessage(), e);
            this.handleStorageNotAvailable(fileStorageRequests, Optional.of(e.getMessage()));
        }
        return jobInfoList;
    }

    /**
     * Schedule a {@link JobInfo} for the given {@link  FileStorageWorkingSubset}.<br/>
     * NOTE : A new transaction is created for each call at this method. It is mandatory to avoid having too long transactions.
     *
     * @return {@link JobInfo} scheduled.
     */
    private JobInfo scheduleJob(FileStorageWorkingSubset workingSubset, String plgBusinessId, String storage) {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(FileStorageRequestJob.DATA_STORAGE_CONF_BUSINESS_ID, plgBusinessId));
        parameters.add(new JobParameter(FileStorageRequestJob.WORKING_SUB_SET, workingSubset));
        JobInfo jobInfo = jobInfoService.createAsQueued(new JobInfo(false,
                                                                    StorageJobsPriority.FILE_STORAGE_JOB,
                                                                    parameters,
                                                                    authResolver.getUser(),
                                                                    FileStorageRequestJob.class.getName()));
        workingSubset.getFileReferenceRequests()
                     .forEach(fr -> fileStorageRequestRepo.updateStatusAndJobId(FileRequestStatus.PENDING,
                                                                                jobInfo.getId().toString(),
                                                                                fr.getId()));
        LOGGER.debug("[STORAGE REQUESTS] Job scheduled for {} requests on storage {}",
                     workingSubset.getFileReferenceRequests().size(),
                     storage);
        return jobInfo;
    }

    /**
     * Create a new {@link FileStorageRequestAggregation}
     *
     * @param owners              owners of the file to store
     * @param fileMetaInfo        meta information of the file to store
     * @param originUrl           file origin location
     * @param storage             storage destination location
     * @param storageSubDirectory Optional sub-directory where to store file in the storage destination location
     * @param groupId             Business identifier of the deletion request
     * @param status              storage request status to be set during creation
     * @param sessionOwner        session owner to which belongs created storage request
     * @param session             session to which belongs created storage request
     */
    public FileStorageRequestAggregation createNewFileStorageRequest(Collection<String> owners,
                                                                     FileReferenceMetaInfo fileMetaInfo,
                                                                     String originUrl,
                                                                     String storage,
                                                                     Optional<String> storageSubDirectory,
                                                                     String groupId,
                                                                     Optional<String> errorCause,
                                                                     Optional<FileRequestStatus> status,
                                                                     String sessionOwner,
                                                                     String session) {
        long start = System.currentTimeMillis();
        FileStorageRequestAggregation fileStorageRequest = new FileStorageRequestAggregation(owners,
                                                                                             fileMetaInfo,
                                                                                             originUrl,
                                                                                             storage,
                                                                                             storageSubDirectory,
                                                                                             groupId,
                                                                                             sessionOwner,
                                                                                             session);
        fileStorageRequest.setStatus(reqStatusService.getNewStatus(fileStorageRequest, status));
        fileStorageRequest.setErrorCause(errorCause.orElse(null));
        // notify request is running to the session agent
        this.sessionNotifier.incrementRunningRequests(fileStorageRequest.getSessionOwner(),
                                                      fileStorageRequest.getSession());
        // check if a storage is configured
        if (!storageHandler.isConfigured(storage)) {
            // The storage destination is unknown, we can already set the request in error status
            handleStorageNotAvailable(fileStorageRequest, Optional.empty());
        } else {
            // save request
            fileStorageRequestRepo.save(fileStorageRequest);

            LOGGER.trace(
                "[STORAGE REQUESTS] New file storage request created for file <{}> to store to {} with status {} in {}ms",
                fileStorageRequest.getMetaInfo().getFileName(),
                fileStorageRequest.getStorage(),
                fileStorageRequest.getStatus(),
                System.currentTimeMillis() - start);
        }
        return fileStorageRequest;
    }

    /**
     * Create a new {@link FileStorageRequestAggregation} by copying the owners of another
     *
     * @param fileCopyRequest the copy request
     * @param metaInfo        meta information of the file to store
     * @param originUrl       file origin location
     * @param groupId         Business identifier of the deletion request
     * @param sessionOwner    session owner to which belongs created storage request
     * @param session         session to which belongs created storage request
     */
    public FileStorageRequestAggregation createNewFileStorageRequestFromCopy(FileCopyRequest fileCopyRequest,
                                                                             String originStorage,
                                                                             FileReferenceMetaInfo metaInfo,
                                                                             String originUrl,
                                                                             String groupId,
                                                                             String sessionOwner,
                                                                             String session) {

        Collection<String> owners = fileRefService.findWithOwnersByLocationStorageAndMetaInfoChecksum(originStorage,
                                                                                                      metaInfo.getChecksum())
                                                  .orElseThrow()
                                                  .getLazzyOwners();
        return createNewFileStorageRequest(owners,
                                           metaInfo,
                                           originUrl,
                                           fileCopyRequest.getStorage(),
                                           Optional.ofNullable(fileCopyRequest.getStorageSubDirectory()),
                                           groupId,
                                           Optional.empty(),
                                           Optional.empty(),
                                           sessionOwner,
                                           session);
    }

    /**
     * Update a list of {@link FileStorageRequestAggregation}s when the storage destination cannot be handled.
     * A storage destination cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     *
     * @param fileStorageRequests storage request not available
     */
    private void handleStorageNotAvailable(Collection<FileStorageRequestAggregation> fileStorageRequests,
                                           Optional<String> errorCause) {
        fileStorageRequests.forEach(r -> handleStorageNotAvailable(r, errorCause));
    }

    /**
     * Update a {@link FileStorageRequestAggregation} when the storage destination cannot be handled.
     * A storage destination cannot be handled if <ul>
     * <li> No plugin configuration of {@link IStorageLocation} exists for the storage</li>
     * <li> the plugin configuration is disabled </li>
     * </ul>
     */
    private void handleStorageNotAvailable(FileStorageRequestAggregation fileStorageRequest,
                                           Optional<String> errorCause) {
        long start = System.currentTimeMillis();
        // The storage destination is unknown, we can already set the request in error status
        String lErrorCause = errorCause.orElse(String.format(
            "Storage request <%s> cannot be handle as destination storage <%s> is unknown or not accessible (offline).",
            fileStorageRequest.getMetaInfo().getFileName(),
            fileStorageRequest.getStorage()));
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
        eventPublisher.storeError(fileStorageRequest.getMetaInfo().getChecksum(),
                                  fileStorageRequest.getOwners(),
                                  fileStorageRequest.getStorage(),
                                  fileStorageRequest.getErrorCause(),
                                  fileStorageRequest.getGroupIds());
        // Inform request groups that the request is in error
        for (String groupId : fileStorageRequest.getGroupIds()) {
            reqGroupService.requestError(groupId,
                                         FileRequestType.STORAGE,
                                         fileStorageRequest.getMetaInfo().getChecksum(),
                                         fileStorageRequest.getStorage(),
                                         fileStorageRequest.getStorageSubDirectory(),
                                         fileStorageRequest.getOwners(),
                                         lErrorCause);
        }
        LOGGER.debug("[STORAGE REQUESTS] Request {} set as error in {} ms. Cause : {}",
                     fileStorageRequest.getMetaInfo().getFileName(),
                     System.currentTimeMillis() - start,
                     lErrorCause);
    }

    public void handleSuccess(Collection<FileStorageRequestResultDto> results) {
        Set<String> files = new HashSet<>();
        for (FileStorageRequestResultDto result : results) {
            boolean isHandleSuccess = true;
            // As the request is rebuilt from the dto, the status information is not available and the request need
            // to be recovered from the database if the status is needed
            FileStorageRequestAggregation request = FileStorageRequestAggregation.fromDto(result.getRequest());
            FileReferenceMetaInfo reqMetaInfos = request.getMetaInfo();
            Set<FileReference> fileRefs = Sets.newHashSet();
            // parameters for session notification
            String sessionOwner = request.getSessionOwner();
            String session = request.getSession();
            int nbFilesStored = 0;

            for (String owner : request.getOwners()) {
                try {
                    FileReferenceMetaInfo fileMeta = new FileReferenceMetaInfo(reqMetaInfos.getChecksum(),
                                                                               reqMetaInfos.getAlgorithm(),
                                                                               reqMetaInfos.getFileName(),
                                                                               result.getFileSize(),
                                                                               reqMetaInfos.getMimeType());
                    fileMeta.setHeight(reqMetaInfos.getHeight());
                    fileMeta.setWidth(reqMetaInfos.getWidth());
                    fileMeta.setType(reqMetaInfos.getType());
                    FileReferenceResult fileReferenceResult = fileRefReqService.reference(owner,
                                                                                          fileMeta,
                                                                                          new FileLocation(request.getStorage(),
                                                                                                           result.getStoredUrl(),
                                                                                                           result.isPendingActionRemaining()),
                                                                                          request.getGroupIds(),
                                                                                          sessionOwner,
                                                                                          session);
                    fileRefs.add(fileReferenceResult.getFileReference());
                    if (fileReferenceResult.getStatus() != FileReferenceResultStatusEnum.UNMODIFIED) {
                        // Only increment count of stored files if referenced file is new or updated.
                        // If reference file already exists for the given owner (unmodified), total of stored files already contains this one.
                        nbFilesStored++;
                    }
                } catch (ModuleException e) {
                    LOGGER.error(e.getMessage(), e);
                    handleError(request, e.getMessage());
                    isHandleSuccess = false;
                }
            }

            for (String groupId : request.getGroupIds()) {
                for (FileReference fileRef : fileRefs) {
                    reqGroupService.requestSuccess(groupId,
                                                   FileRequestType.STORAGE,
                                                   fileRef.getMetaInfo().getChecksum(),
                                                   fileRef.getLocation().getStorage(),
                                                   request.getStorageSubDirectory(),
                                                   request.getOwners(),
                                                   fileRef);
                }
            }

            // Session handling
            // decrement the number of running requests
            this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
            // notify the number of successful created files
            this.sessionNotifier.incrementStoredFiles(sessionOwner, session, nbFilesStored);

            if (result.isNotifyActionRemainingToAdmin()) {
                files.add(result.getStoredUrl());
            }

            // Delete the FileRefRequest as it has been handled
            if (isHandleSuccess) {
                delete(request);
            }
        }

        if (!files.isEmpty()) {
            notificationClient.notifyRoles(createStorageActionPendingNotification(files),
                                           "Storage not completed",
                                           NotificationLevel.ERROR,
                                           MimeTypeUtils.TEXT_HTML,
                                           Sets.newHashSet(DefaultRole.PROJECT_ADMIN.toString()));
        }
    }

    /**
     * Creates notification for project administrators to inform action pending is remaining on stored files
     */
    private String createStorageActionPendingNotification(Set<String> files) {
        final Map<String, Object> data = new HashMap<>();
        data.put("files", files);
        try {
            return templateService.render(StorageTemplatesConf.ACTION_REMAINING_TEMPLATE_NAME, data);
        } catch (TemplateException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    public void handleError(Collection<FileStorageRequestResultDto> results) {
        for (FileStorageRequestResultDto result : results) {
            handleError(FileStorageRequestAggregation.fromDto(result.getRequest()), result.getErrorCause());
        }
    }

    /**
     * Handle a {@link FileStorageRequestAggregation} error.
     * <ul>
     * <li> Update the request into database </li>
     * <li> Send bus message information about storage error </li>
     * <li> Update group with the error request </li>
     * </ul>
     */
    private void handleError(FileStorageRequestAggregation request, String errorCause) {
        // The file is not really referenced so handle reference error by modifying request to be retry later
        request.setStatus(FileRequestStatus.ERROR);
        request.setErrorCause(errorCause);
        update(request);
        eventPublisher.storeError(request.getMetaInfo().getChecksum(),
                                  request.getOwners(),
                                  request.getStorage(),
                                  errorCause,
                                  request.getGroupIds());
        for (String groupId : request.getGroupIds()) {
            reqGroupService.requestError(groupId,
                                         FileRequestType.STORAGE,
                                         request.getMetaInfo().getChecksum(),
                                         request.getStorage(),
                                         request.getStorageSubDirectory(),
                                         request.getOwners(),
                                         errorCause);
        }
        // notify error to the session agent
        String sessionOwner = request.getSessionOwner();
        String session = request.getSession();
        this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
        this.sessionNotifier.incrementErrorRequests(sessionOwner, session);
    }

    public boolean handleJobCrash(JobInfo jobInfo) {
        boolean isFileStorageRequestJob = FileStorageRequestJob.class.getName().equals(jobInfo.getClassName());
        if (isFileStorageRequestJob) {
            try {
                FileStorageWorkingSubset workingSubset = IJob.getValue(jobInfo.getParametersAsMap(),
                                                                       FileStorageRequestJob.WORKING_SUB_SET);
                List<FileStorageRequestAggregation> fileStorageRequests = fileStorageRequestRepo.findAllById(
                    workingSubset.getFileReferenceRequests()
                                 .stream()
                                 .map(FileStorageRequestAggregationDto::getId)
                                 .toList());
                fileStorageRequests.stream()
                                   .filter(fileStorageRequest -> FileRequestStatus.RUNNING_STATUS.contains(
                                       fileStorageRequest.getStatus()))
                                   .forEach(r -> handleError(r, jobInfo.getStatus().getStackTrace()));
            } catch (JobParameterMissingException | JobParameterInvalidException e) {
                String message = String.format("Storage file storage request job with id \"%s\" fails with status "
                                               + "\"%s\"", jobInfo.getId(), jobInfo.getStatus().getStatus());
                LOGGER.error(message, e);
                notificationClient.notify(message, "Storage job failure", NotificationLevel.ERROR, DefaultRole.ADMIN);
            }
        }
        return isFileStorageRequestJob;
    }

    /**
     * Update if needed an already existing {@link FileReference} associated to a
     * new {@link FileStorageRequestDto} request received.<br/>
     * <br/>
     * If a deletion request is running on the existing {@link FileReference} then a new {@link FileStorageRequestAggregation}
     * request is created as DELAYED.<br/>
     *
     * @param fileReference {@link FileReference} to update
     * @param request       associated {@link FileStorageRequestDto} new request
     * @param groupId       new business request identifier
     * @return {@link FileReference} updated or null.
     */
    private RequestResult handleFileToStoreAlreadyExists(FileReference fileReference,
                                                         FileStorageRequestDto request,
                                                         Optional<FileDeletionRequest> oDeletionRequest,
                                                         String groupId) {
        long start = System.currentTimeMillis();
        FileReferenceMetaInfo newMetaInfo = FileReferenceMetaInfo.buildFromDto(request.getMetaInfo());
        if (oDeletionRequest.isPresent()) {
            // Deletion is running right now, so delay the new file reference creation with a FileReferenceRequest
            return RequestResult.build(createNewFileStorageRequest(Sets.newHashSet(request.getOwner()),
                                                                   newMetaInfo,
                                                                   request.getOriginUrl(),
                                                                   request.getStorage(),
                                                                   request.getOptionalSubDirectory(),
                                                                   groupId,
                                                                   Optional.empty(),
                                                                   Optional.empty(),
                                                                   request.getSessionOwner(),
                                                                   request.getSession()));
        } else {
            String message = String.format(
                "New owner <%s> added to existing referenced file <%s> at <%s> (checksum: %s) ",
                request.getOwner(),
                fileReference.getMetaInfo().getFileName(),
                fileReference.getLocation().toString(),
                fileReference.getMetaInfo().getChecksum());
            eventPublisher.storeSuccess(fileReference,
                                        message,
                                        Sets.newHashSet(groupId),
                                        Lists.newArrayList(request.getOwner()));
            fileReference.getLazzyOwners().add(request.getOwner());
            reqGroupService.requestSuccess(groupId,
                                           FileRequestType.STORAGE,
                                           request.getChecksum(),
                                           request.getStorage(),
                                           request.getOptionalSubDirectory().orElse(null),
                                           Sets.newHashSet(request.getOwner()),
                                           fileReference);
            LOGGER.trace("[STORAGE REQUESTS] Storage request {} succeded for existing reference {} in {}ms.",
                         request.getFileName(),
                         fileReference.getId(),
                         System.currentTimeMillis() - start);
            // notify the number of successful created files
            this.sessionNotifier.incrementStoredFiles(request.getSessionOwner(), request.getSession(), 1);
            return RequestResult.build(fileReference);
        }
    }

    /**
     * Delete all requests for the given storage identifier
     */
    public void deleteByStorage(String storageLocationId, Optional<FileRequestStatus> status) {
        decrementSessionBeforeDeletion(storageLocationId, status);
        if (status.isPresent()) {
            fileStorageRequestRepo.deleteByStorageAndStatus(storageLocationId, status.get());
        } else {
            fileStorageRequestRepo.deleteByStorage(storageLocationId);
        }
    }

    /**
     * Decrement session counts before deletion of storage requests.
     *
     * @param storageLocationId storage identifier of requests to delete
     * @param status            Optional status of requests to delete
     */
    private void decrementSessionBeforeDeletion(String storageLocationId, Optional<FileRequestStatus> status) {
        Pageable page = PageRequest.ofSize(100);
        Page<FileStorageRequestAggregation> pageRequests;
        do {
            if (status.isPresent()) {
                pageRequests = fileStorageRequestRepo.findAllByStorageAndStatus(storageLocationId, status.get(), page);
            } else {
                pageRequests = fileStorageRequestRepo.findAllByStorage(storageLocationId, page);
            }
            pageRequests.stream().forEach(r -> {
                sessionNotifier.decrementStoreRequests(r.getSessionOwner(), r.getSession());
                if (r.getStatus() == FileRequestStatus.ERROR) {
                    sessionNotifier.decrementErrorRequests(r.getSessionOwner(), r.getSession());
                }
            });
            page = page.next();
        } while (pageRequests.hasNext());
    }

    public boolean isStorageRunning(String storageId) {
        return fileStorageRequestRepo.existsByStorageAndStatusIn(storageId,
                                                                 Sets.newHashSet(FileRequestStatus.TO_DO,
                                                                                 FileRequestStatus.PENDING));
    }

    /**
     * Check if a {@link PeriodicStorageLocationJob} is running for the given storage location.
     */
    public boolean isPendingActionRunning(String storageId) {
        Page<JobInfo> jobs = jobInfoService.retrieveJobs(PeriodicStorageLocationJob.class.getName(),
                                                         Pageable.ofSize(100),
                                                         JobStatus.getAllNotFinishedStatus());
        return jobs.stream()
                   .map(job -> job.getParametersAsMap().get(PeriodicStorageLocationJob.DATA_STORAGE_CONF_BUSINESS_ID))
                   .filter(Objects::nonNull)
                   .anyMatch(storageIdParameter -> storageId.equals(storageIdParameter.getValue()));
    }

    /**
     * Retrieve expiration date for deletion request
     */
    public OffsetDateTime getRequestExpirationDate() {
        if ((nbDaysBeforeExpiration != null) && (nbDaysBeforeExpiration > 0)) {
            return OffsetDateTime.now().plusDays(nbDaysBeforeExpiration);
        } else {
            return null;
        }
    }

    /**
     * Internal private class to regroup information about handle {@link FileStorageRequestDto} result.
     * The result can be : <ul>
     * <li> {@link FileReference} : If the request is associated to a file already referenced.</li>
     * <li> {@link FileStorageRequestAggregation} : If the request is a new or updated storage request </li>
     */
    private static class RequestResult {

        Optional<FileReference> fileReference = Optional.empty();

        Optional<FileStorageRequestAggregation> storageRequest = Optional.empty();

        public static RequestResult build(FileReference fileReference) {
            RequestResult res = new RequestResult();
            res.fileReference = Optional.ofNullable(fileReference);
            return res;
        }

        public static RequestResult build(FileStorageRequestAggregation storageRequest) {
            RequestResult res = new RequestResult();
            res.storageRequest = Optional.ofNullable(storageRequest);
            return res;
        }

        public Optional<FileReference> getFileReference() {
            return fileReference;
        }

        public Optional<FileStorageRequestAggregation> getStorageRequest() {
            return storageRequest;
        }

    }

}
