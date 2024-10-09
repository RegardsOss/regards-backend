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
package fr.cnes.regards.modules.filecatalog.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.fileaccess.amqp.input.FileStorageRequestReadyToProcessEvent;
import fr.cnes.regards.modules.fileaccess.dto.FileArchiveStatus;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.fileaccess.dto.StorageRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.input.FileStorageMetaInfoDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FileArchiveResponseEvent;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent;
import fr.cnes.regards.modules.filecatalog.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.filecatalog.dao.IFileStorageRequestAggregationRepository;
import fr.cnes.regards.modules.filecatalog.dao.result.RequestAndMaxStatus;
import fr.cnes.regards.modules.filecatalog.domain.*;
import fr.cnes.regards.modules.filecatalog.domain.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.filecatalog.dto.FileArchiveResponseDto;
import fr.cnes.regards.modules.filecatalog.service.template.StorageTemplatesConf;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service to handle {@link FileStorageRequestAggregation}s.
 * Those requests are created when a file reference need to be stored physically.
 *
 * @author Thibaud Michaudel
 **/
@Service
public class FileStorageRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageRequestService.class);

    public static final int PAGE_SIZE = 1000;

    public static final int SMALL_PAGE_SIZE = 200;

    private final RequestStatusService requestStatusService;

    private final FileReferenceRequestService fileReferenceRequestService;

    private final RequestsGroupService requestsGroupService;

    private final ITemplateService templateService;

    private final SessionNotifier sessionNotifier;

    private final INotificationClient notificationClient;

    private final FileReferenceEventPublisher eventPublisher;

    private final IFileStorageRequestAggregationRepository fileStorageRequestAggregationRepository;

    private final IFileReferenceRepository fileReferenceRepository;

    private final IPublisher publisher;

    @Value("${regards.file.catalog.requests.retry.page:1000}")
    private int pageRetrySize;

    public FileStorageRequestService(RequestStatusService requestStatusService,
                                     FileReferenceRequestService fileReferenceRequestService,
                                     RequestsGroupService requestsGroupService,
                                     ITemplateService templateService,
                                     SessionNotifier sessionNotifier,
                                     INotificationClient notificationClient,
                                     FileReferenceEventPublisher eventPublisher,
                                     IFileStorageRequestAggregationRepository fileStorageRequestAggregationRepository,
                                     IFileReferenceRepository fileReferenceRepository,
                                     IPublisher publisher) {
        this.requestStatusService = requestStatusService;
        this.fileReferenceRequestService = fileReferenceRequestService;
        this.requestsGroupService = requestsGroupService;
        this.templateService = templateService;
        this.sessionNotifier = sessionNotifier;
        this.notificationClient = notificationClient;
        this.eventPublisher = eventPublisher;
        this.fileStorageRequestAggregationRepository = fileStorageRequestAggregationRepository;
        this.fileReferenceRepository = fileReferenceRepository;
        this.publisher = publisher;
    }

    public void createStorageRequests(List<FilesStorageRequestEvent> messages) {
        for (FilesStorageRequestEvent message : messages) {
            for (FileStorageRequestDto file : message.getFiles()) {
                createNewFileStorageRequest(file.getOwner(),
                                            FileReferenceMetaInfo.buildFromDto(file.getMetaInfo()),
                                            file.getOriginUrl(),
                                            file.getStorage(),
                                            Optional.of(file.getSubDirectory()),
                                            message.getGroupId(),
                                            Optional.empty(),
                                            Optional.of(StorageRequestStatus.GRANTED),
                                            file.getSessionOwner(),
                                            file.getSession());
            }
        }
    }

    /**
     * Create a new {@link FileStorageRequestAggregation}
     *
     * @param owner               owner of the file to store
     * @param fileMetaInfo        meta information of the file to store
     * @param originUrl           file origin location
     * @param storage             storage destination location
     * @param storageSubDirectory Optional sub-directory where to store file in the storage destination location
     * @param groupId             Business identifier of the deletion request
     * @param status              storage request status to be set during creation
     * @param sessionOwner        session owner to which belongs created storage request
     * @param session             session to which belongs created storage request
     */
    @MultitenantTransactional
    public FileStorageRequestAggregation createNewFileStorageRequest(String owner,
                                                                     FileReferenceMetaInfo fileMetaInfo,
                                                                     String originUrl,
                                                                     String storage,
                                                                     Optional<String> storageSubDirectory,
                                                                     String groupId,
                                                                     Optional<String> errorCause,
                                                                     Optional<StorageRequestStatus> status,
                                                                     String sessionOwner,
                                                                     String session) {
        long start = System.currentTimeMillis();
        FileStorageRequestAggregation fileStorageRequest = new FileStorageRequestAggregation(owner,
                                                                                             fileMetaInfo,
                                                                                             originUrl,
                                                                                             storage,
                                                                                             storageSubDirectory,
                                                                                             groupId,
                                                                                             sessionOwner,
                                                                                             session,
                                                                                             false);
        fileStorageRequest.setStatus(requestStatusService.getNewStatus(fileStorageRequest, status));
        fileStorageRequest.setErrorCause(errorCause.orElse(null));
        // notify request is running to the session agent
        this.sessionNotifier.incrementRunningRequests(fileStorageRequest.getSessionOwner(),
                                                      fileStorageRequest.getSession());
        fileStorageRequestAggregationRepository.save(fileStorageRequest);

        LOGGER.trace(
            "[STORAGE REQUESTS] New file storage request created for file <{}> to store to {} with status {} in {}ms",
            fileStorageRequest.getMetaInfo().getFileName(),
            fileStorageRequest.getStorage(),
            fileStorageRequest.getStatus(),
            System.currentTimeMillis() - start);
        return fileStorageRequest;
    }

    /**
     * Check requests in {@link StorageRequestStatus#GRANTED} status.
     * Il the file reference associated with the request already exists, delete the request and add the owner to
     * the reference. Change the request status to {@link StorageRequestStatus#TO_HANDLE} otherwise.
     * <p>
     * This method is not transactional as each step will need a new transaction
     */
    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    public Pageable doCheckRequests(Pageable page) {
        // List of requests to delete (use a list in order to do only one delete request)
        List<Long> requestsToDeleteIds = new ArrayList<>();

        // List of requests to update to TO_HANDLE (use a list in order to do only one update request)
        List<Long> requestsToUpdateIds = new ArrayList<>();

        Page<FileStorageRequestAggregation> filesPage = fileStorageRequestAggregationRepository.findAllByStatusOrderByStorageAsc(
            StorageRequestStatus.GRANTED,
            page);
        Map<String, List<FileStorageRequestAggregation>> requestsByStorage = filesPage.getContent()
                                                                                      .stream()
                                                                                      .collect(Collectors.groupingBy(
                                                                                          FileStorageRequestAggregation::getStorage));
        for (String storage : requestsByStorage.keySet()) {
            List<FileStorageRequestAggregation> requests = requestsByStorage.get(storage);
            // List of already existing File References with the same checksum as one of the requests
            Set<FileReference> existingFileRefs = fileReferenceRepository.findByLocationStorageAndMetaInfoChecksumIn(
                storage,
                requests.stream().map(request -> request.getMetaInfo().getChecksum()).toList());
            for (FileStorageRequestAggregation request : requests) {
                Optional<FileReference> oExistingFileReference = existingFileRefs.stream()
                                                                                 .filter(fileRef -> request.getMetaInfo()
                                                                                                           .getChecksum()
                                                                                                           .equals(
                                                                                                               fileRef.getMetaInfo()
                                                                                                                      .getChecksum()))
                                                                                 .findAny();
                if (oExistingFileReference.isPresent()) {
                    // The file doesn't need to be stored, just add the owner to the existing file reference
                    fileReferenceRepository.addOwner(oExistingFileReference.get().getId(), request.getOwner());
                    requestsToDeleteIds.add(request.getId());
                } else {
                    // The file need to be stored
                    requestsToUpdateIds.add(request.getId());
                }
            }
        }
        page = filesPage.nextPageable();
        fileStorageRequestAggregationRepository.deleteAllByIdInBatch(requestsToDeleteIds);
        fileStorageRequestAggregationRepository.updateStatusByIdIn(StorageRequestStatus.TO_HANDLE, requestsToUpdateIds);
        return page;
    }

    /**
     * Delete Storage requests in status {@link StorageRequestStatus#TO_DELETE}
     */
    @MultitenantTransactional
    public void deleteRequests() {
        fileStorageRequestAggregationRepository.deleteAllByStatus(StorageRequestStatus.TO_DELETE);
    }

    /**
     * Check if requests in {@link StorageRequestStatus#HANDLED} are completed
     */
    @MultitenantTransactional
    public Pageable doHandleCompleteRequests(Pageable page) {
        // List of requests to set to TO_DELETE (use a list in order to do only one update request)
        List<Long> requestsToDeleteIds = new ArrayList<>();
        Page<FileStorageRequestAggregation> filesPage = fileStorageRequestAggregationRepository.findAllByStatusOrderByStorageAsc(
            StorageRequestStatus.HANDLED,
            page);
        Map<String, List<FileStorageRequestAggregation>> requestsByStorage = filesPage.getContent()
                                                                                      .stream()
                                                                                      .collect(Collectors.groupingBy(
                                                                                          FileStorageRequestAggregation::getStorage));
        for (String storage : requestsByStorage.keySet()) {
            List<FileStorageRequestAggregation> requests = requestsByStorage.get(storage);
            // List of already existing File References with the same checksum as one of the requests
            Set<FileReference> existingFileRefs = fileReferenceRepository.findByLocationStorageAndMetaInfoChecksumIn(
                storage,
                requests.stream().map(request -> request.getMetaInfo().getChecksum()).toList());
            for (FileStorageRequestAggregation request : requests) {
                Optional<FileReference> oExistingFileReference = existingFileRefs.stream()
                                                                                 .filter(fileRef -> request.getMetaInfo()
                                                                                                           .getChecksum()
                                                                                                           .equals(
                                                                                                               fileRef.getMetaInfo()
                                                                                                                      .getChecksum()))
                                                                                 .findAny();
                if (oExistingFileReference.isPresent()) {
                    // The file has been stored by another request, just add the owner to the existing file
                    // reference
                    fileReferenceRepository.addOwner(oExistingFileReference.get().getId(), request.getOwner());
                    requestsToDeleteIds.add(request.getId());
                } else {
                    // The file has not yet be stored, nothing to do
                }
            }
        }
        page = filesPage.nextPageable();
        fileStorageRequestAggregationRepository.updateStatusByIdIn(StorageRequestStatus.TO_DELETE, requestsToDeleteIds);
        return page;
    }

    /**
     * Create and send request creation events to file access for the given storage
     */
    @MultitenantTransactional
    public Pageable doDispatchRequestsByStorage(String storage, Pageable page) {
        // List of checksum for the given storage where a request is TO_HANDLE
        List<String> requestChecksumsToUpdate = new ArrayList<>();

        // List of checksum for the given storage where we need to send an event to handle the request
        List<String> checksumsToHandle = new ArrayList<>();

        // Events that will be sent
        List<FileStorageRequestReadyToProcessEvent> eventsToSend = new ArrayList<>();

        // The requests we need to handle are the one in TO_HANDLE status.
        // We don't want multiple process to store the same file (checksum) on the same storage.
        // To prevent this, we will send at most one event to the storage worker per checksum and storage.
        // They might also be a request already running or completed for this file. In that case, no new event will
        // be sent.
        // In any case, the requests in TO_HANDLE status must be updated to HANDLED status (even if no new event was
        // sent).

        // Firstly, get the distinct checksums of the requests to update and the maximum status of requests with this
        // checksum. The maximum status is computed using status ordinal (In fact, we only need for TO_HANDLE <
        // HANDLED and TO_HANDLE < DELETED, if the maximum status is not TO_HANDLE, it means that a request is
        // already being processed or has been processed for this checksum).
        Page<RequestAndMaxStatus> checksumsPageable = fileStorageRequestAggregationRepository.findRequestChecksumToHandle(
            storage,
            page);
        for (RequestAndMaxStatus checksumAndStatus : checksumsPageable.getContent()) {
            if (checksumAndStatus.maxStatus() == StorageRequestStatus.TO_HANDLE) {
                checksumsToHandle.add(checksumAndStatus.requestChecksum());
            }
            requestChecksumsToUpdate.add(checksumAndStatus.requestChecksum());
        }
        // Secondly, retrieve the requests that necessitate sending an event

        Pageable requestsPageable = PageRequest.of(0, FileStorageRequestService.PAGE_SIZE, Sort.by("id"));

        List<String> handledChecksums = new ArrayList<>();
        do {
            Page<FileStorageRequestAggregation> requestsPage = fileStorageRequestAggregationRepository.findAllByStorageAndStatusAndMetaInfoChecksumIn(
                storage,
                StorageRequestStatus.TO_HANDLE,
                checksumsToHandle,
                requestsPageable);
            for (FileStorageRequestAggregation request : requestsPage.getContent()) {
                if (!handledChecksums.contains(request.getMetaInfo().getChecksum())) {
                    eventsToSend.add(createFileAccessEvent(request));
                    handledChecksums.add(request.getMetaInfo().getChecksum());
                }
            }
            requestsPageable = checksumsPageable.nextPageable();

            // Lastly, send the event and update the requests
            if (!eventsToSend.isEmpty()) {
                publisher.publish(eventsToSend);
            }

            if (!requestChecksumsToUpdate.isEmpty()) {
                fileStorageRequestAggregationRepository.updateStatusByStorageAndMetaInfoChecksumIn(StorageRequestStatus.HANDLED,
                                                                                                   storage,
                                                                                                   requestChecksumsToUpdate);
            }
        } while (requestsPageable.isPaged());

        return checksumsPageable.nextPageable();
    }

    /**
     * Create the event that will be sent to file access for the given request
     */
    private FileStorageRequestReadyToProcessEvent createFileAccessEvent(FileStorageRequestAggregation request) {
        FileStorageMetaInfoDto metaData = new FileStorageMetaInfoDto(request.getMetaInfo().getMimeType().toString(),
                                                                     request.getMetaInfo().getType(),
                                                                     Optional.ofNullable(request.getMetaInfo()
                                                                                                .getHeight()).orElse(0),
                                                                     Optional.ofNullable(request.getMetaInfo()
                                                                                                .getWidth()).orElse(0));
        return new FileStorageRequestReadyToProcessEvent(request.getId(),
                                                         request.getMetaInfo().getChecksum(),
                                                         request.getMetaInfo().getAlgorithm(),
                                                         request.getOriginUrl(),
                                                         request.getStorage(),
                                                         request.getStorageSubDirectory(),
                                                         request.getOwner(),
                                                         request.getSession(),
                                                         false,
                                                         metaData,
                                                         request.isReference());
    }

    /**
     * Search for {@link FileStorageRequestAggregation}s matching the given destination storage and checksum
     *
     * @return {@link FileStorageRequestAggregation}
     */
    @Transactional(readOnly = true)
    public Collection<FileStorageRequestAggregation> search(String destinationStorage, String checksum) {
        return fileStorageRequestAggregationRepository.findByMetaInfoChecksumAndStorage(checksum, destinationStorage);
    }

    private void handleSuccess(Collection<FileStorageResult> results) {
        Set<String> filesWithActionsRemaining = new HashSet<>();
        for (FileStorageResult result : results) {
            boolean isHandleSuccess = true;
            FileStorageRequestAggregation request = result.request();
            FileReferenceMetaInfo reqMetaInfos = request.getMetaInfo();
            Set<FileReference> fileRefs = Sets.newHashSet();
            // parameters for session notification
            String sessionOwner = request.getSessionOwner();
            String session = request.getSession();
            int nbFilesStored = 0;

            try {
                FileReferenceMetaInfo fileMeta = new FileReferenceMetaInfo(reqMetaInfos.getChecksum(),
                                                                           reqMetaInfos.getAlgorithm(),
                                                                           reqMetaInfos.getFileName(),
                                                                           request.getMetaInfo().getFileSize(),
                                                                           reqMetaInfos.getMimeType());
                fileMeta.setHeight(reqMetaInfos.getHeight());
                fileMeta.setWidth(reqMetaInfos.getWidth());
                fileMeta.setType(reqMetaInfos.getType());
                FileReferenceResult fileReferenceResult = fileReferenceRequestService.reference(request.getOwner(),
                                                                                                fileMeta,
                                                                                                new FileLocation(request.getStorage(),
                                                                                                                 result.fileUrl(),
                                                                                                                 result.storageStatus()),
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

            for (String groupId : request.getGroupIds()) {
                for (FileReference fileRef : fileRefs) {
                    requestsGroupService.requestSuccess(groupId,
                                                        FileRequestType.STORAGE,
                                                        fileRef.getMetaInfo().getChecksum(),
                                                        fileRef.getLocation().getStorage(),
                                                        request.getStorageSubDirectory(),
                                                        List.of(request.getOwner()),
                                                        fileRef);
                }
            }

            // Session handling
            // decrement the number of running requests
            this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
            // notify the number of successful created files
            this.sessionNotifier.incrementStoredFiles(sessionOwner, session, nbFilesStored);

            if (result.notifyActionRemainingToAdmin()) {
                filesWithActionsRemaining.add(result.fileUrl());
            }

            // Delete the FileRefRequest as it has been handled
            if (isHandleSuccess) {
                delete(request);
            }
        }

        /**
         * Notify the admin that some files are temporarily stored locally before they are fully stored on the target
         * storage
         */
        if (!filesWithActionsRemaining.isEmpty()) {
            notificationClient.notifyRoles(createStorageActionPendingNotification(filesWithActionsRemaining),
                                           "Storage not completed",
                                           NotificationLevel.ERROR,
                                           MimeTypeUtils.TEXT_HTML,
                                           Sets.newHashSet(DefaultRole.PROJECT_ADMIN.toString()));
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
        request.setStatus(StorageRequestStatus.ERROR);
        request.setErrorCause(errorCause);
        save(request);
        eventPublisher.storeError(request.getMetaInfo().getChecksum(),
                                  List.of(request.getOwner()),
                                  request.getStorage(),
                                  errorCause,
                                  request.getGroupIds());
        for (String groupId : request.getGroupIds()) {
            requestsGroupService.requestError(groupId,
                                              FileRequestType.STORAGE,
                                              request.getMetaInfo().getChecksum(),
                                              request.getStorage(),
                                              request.getStorageSubDirectory(),
                                              List.of(request.getOwner()),
                                              errorCause);
        }
        // notify error to the session agent
        String sessionOwner = request.getSessionOwner();
        String session = request.getSession();
        this.sessionNotifier.decrementRunningRequests(sessionOwner, session);
        this.sessionNotifier.incrementErrorRequests(sessionOwner, session);
    }

    /**
     * Create a new {@link FileStorageRequestAggregation} or update it if it exists
     *
     * @param fileStorageRequest to save
     */
    public FileStorageRequestAggregation save(FileStorageRequestAggregation fileStorageRequest) {
        return fileStorageRequestAggregationRepository.save(fileStorageRequest);
    }

    /**
     * Delete a {@link FileStorageRequestAggregation}
     *
     * @param fileStorageRequest to delete
     */
    public void delete(FileStorageRequestAggregation fileStorageRequest) {
        if (fileStorageRequestAggregationRepository.existsById(fileStorageRequest.getId())) {
            fileStorageRequestAggregationRepository.deleteById(fileStorageRequest.getId());
        } else {
            LOGGER.debug("Unable to delete file storage request {} cause it does not exists",
                         fileStorageRequest.getId());
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

    /**
     * Create the file reference for each successful request
     */
    public void handleSuccess(List<FileArchiveResponseEvent> messages) {
        List<FileStorageRequestAggregation> requestList = fileStorageRequestAggregationRepository.findAllById(messages.stream()
                                                                                                                      .map(
                                                                                                                          FileArchiveResponseDto::getRequestId)
                                                                                                                      .toList());

        Map<Long, FileStorageRequestAggregation> requestMap = requestList.stream()
                                                                         .collect(Collectors.toMap(
                                                                             FileStorageRequestAggregation::getId,
                                                                             Function.identity()));

        List<FileStorageResult> resultList = messages.stream()
                                                     .map(message -> new FileStorageResult(requestMap.get(message.getRequestId()),
                                                                                           message.getFileUrl(),
                                                                                           FileArchiveStatus.TO_STORE,
                                                                                           true))
                                                     .toList();

        this.handleSuccess(resultList);

    }

    private record FileStorageResult(FileStorageRequestAggregation request,
                                     String fileUrl,
                                     FileArchiveStatus storageStatus,
                                     boolean notifyActionRemainingToAdmin) {

    }

    @MultitenantTransactional(readOnly = true)
    public Long count(String storage, StorageRequestStatus status) {
        return fileStorageRequestAggregationRepository.countByStorageAndStatus(storage, status);
    }

    @MultitenantTransactional(readOnly = true)
    public boolean isStorageRunning(String storageId) {
        return fileStorageRequestAggregationRepository.existsByStorageAndStatusIn(storageId,
                                                                                  Sets.newHashSet(StorageRequestStatus.GRANTED,
                                                                                                  StorageRequestStatus.TO_HANDLE,
                                                                                                  StorageRequestStatus.DELAYED));
    }

    /**
     * Delete all requests for the given storage identifier
     */
    @MultitenantTransactional
    public void deleteByStorage(String storageLocationId, Optional<StorageRequestStatus> status) {
        decrementSessionBeforeDeletion(storageLocationId, status);
        if (status.isPresent()) {
            fileStorageRequestAggregationRepository.deleteByStorageAndStatus(storageLocationId, status.get());
        } else {
            fileStorageRequestAggregationRepository.deleteByStorage(storageLocationId);
        }
    }

    /**
     * Decrement session counts before deletion of storage requests.
     *
     * @param storageLocationId storage identifier of requests to delete
     * @param status            Optional status of requests to delete
     */
    private void decrementSessionBeforeDeletion(String storageLocationId, Optional<StorageRequestStatus> status) {
        Pageable page = PageRequest.ofSize(100);
        Page<FileStorageRequestAggregation> pageRequests;
        do {
            if (status.isPresent()) {
                pageRequests = fileStorageRequestAggregationRepository.findAllByStorageAndStatus(storageLocationId,
                                                                                                 status.get(),
                                                                                                 page);
            } else {
                pageRequests = fileStorageRequestAggregationRepository.findAllByStorage(storageLocationId, page);
            }
            pageRequests.stream().forEach(r -> {
                sessionNotifier.decrementStoreRequests(r.getSessionOwner(), r.getSession());
                if (r.getStatus() == StorageRequestStatus.ERROR) {
                    sessionNotifier.decrementErrorRequests(r.getSessionOwner(), r.getSession());
                }
            });
            page = page.next();
        } while (pageRequests.hasNext());
    }

    /**
     * Retry errors requests (Storage and Deletion) for the given storage by setting the request status to TO_HANDLE
     * (instead of ERROR).
     */
    public void retryErrorsByStorage(String storage, FileRequestType type) {
        Pageable pageToRequest = PageRequest.of(0, pageRetrySize, Sort.by("id"));

        if (type.equals(FileRequestType.STORAGE)) {
            // Retry all storage requests in error
            Page<FileStorageRequestAggregation> storageReqPage;
            do {
                storageReqPage = fileStorageRequestAggregationRepository.findAllByStorageAndStatus(storage,
                                                                                                   StorageRequestStatus.ERROR,
                                                                                                   pageToRequest);
                List<FileStorageRequestAggregation> storageReqList = storageReqPage.getContent();
                Map<SessionAndOwner, List<FileStorageRequestAggregation>> requestMap = storageReqList.stream()
                                                                                                     .collect(Collectors.groupingBy(
                                                                                                         request -> new SessionAndOwner(
                                                                                                             request.getSession(),
                                                                                                             request.getSessionOwner())));
                // update all requests status and decrement errors to the session agent
                requestMap.forEach((key, value) -> retry(value, key.sessionOwner(), key.session()));
            } while (storageReqPage.hasNext());
        }

        // Retry all deletion requests in error
        // FIXME TODO NeoStorage Lot 4
    }

    public void retryErrorsBySourceAndSession(String sessionOwner, String session) {
        Pageable pageToRequest = PageRequest.of(0, pageRetrySize, Sort.by("id"));

        // Retry all storage requests in error
        Page<FileStorageRequestAggregation> storageReqPage;
        do {
            storageReqPage = fileStorageRequestAggregationRepository.findByStatusAndSessionOwnerAndSession(
                StorageRequestStatus.ERROR,
                sessionOwner,
                session,
                pageToRequest);
            List<FileStorageRequestAggregation> storageReqList = storageReqPage.getContent();
            // update all requests status and decrement errors to the session agent
            if (!storageReqList.isEmpty()) {
                this.retry(storageReqList, sessionOwner, session);
            }
        } while (storageReqPage.hasNext());

        // Retry all deletion requests in error
        // FIXME TODO NeoStorage Lot 4
    }

    /**
     * Update all {@link FileStorageRequestAggregation} in error status to change status to {@link FileRequestStatus#TO_DO} or
     * {@link FileRequestStatus#DELAYED}.
     */
    private void retry(List<FileStorageRequestAggregation> requestList, String sessionOwner, String session) {
        int nbRequests = requestList.size();
        for (FileStorageRequestAggregation request : requestList) {
            // reset status
            request.setStatus(requestStatusService.getNewStatus(request, Optional.empty()));
            request.setErrorCause(null);
        }
        // save changes in database
        this.fileStorageRequestAggregationRepository.saveAll(requestList);
        // decrement error requests
        this.sessionNotifier.decrementErrorRequests(sessionOwner, session, nbRequests);
        // notify running requests to the session agent
        this.sessionNotifier.incrementRunningRequests(sessionOwner, session, nbRequests);
    }

    private record SessionAndOwner(String sessionOwner,
                                   String session) {

    }

}
