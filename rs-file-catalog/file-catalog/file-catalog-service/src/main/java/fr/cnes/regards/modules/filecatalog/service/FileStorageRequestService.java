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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.fileaccess.amqp.input.FileStorageRequestReadyToProcessEvent;
import fr.cnes.regards.modules.fileaccess.dto.StorageRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.input.FileStorageMetaInfoDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent;
import fr.cnes.regards.modules.filecatalog.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.filecatalog.dao.IFileStorageRequestAggregationRepository;
import fr.cnes.regards.modules.filecatalog.dao.result.RequestAndMaxStatus;
import fr.cnes.regards.modules.filecatalog.domain.FileReference;
import fr.cnes.regards.modules.filecatalog.domain.FileReferenceMetaInfo;
import fr.cnes.regards.modules.filecatalog.domain.request.FileStorageRequestAggregation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.util.*;
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

    private final RequestStatusService reqStatusService;

    private final SessionNotifier sessionNotifier;

    private final IFileStorageRequestAggregationRepository fileStorageRequestAggregationRepository;

    private final IFileReferenceRepository fileReferenceRepository;

    private final IPublisher publisher;

    public FileStorageRequestService(RequestStatusService reqStatusService,
                                     SessionNotifier sessionNotifier,
                                     IFileStorageRequestAggregationRepository fileStorageRequestAggregationRepository,
                                     IFileReferenceRepository fileReferenceRepository,
                                     IPublisher publisher) {
        this.reqStatusService = reqStatusService;
        this.sessionNotifier = sessionNotifier;
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
                                                                                             session);
        fileStorageRequest.setStatus(reqStatusService.getNewStatus(fileStorageRequest, status));
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
                                                         metaData);
    }
}
