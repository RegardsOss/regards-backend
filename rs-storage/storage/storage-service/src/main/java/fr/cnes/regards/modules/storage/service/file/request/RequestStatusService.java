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

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestResultDto;
import fr.cnes.regards.modules.storage.dao.*;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.*;
import fr.cnes.regards.modules.storage.domain.predicate.StoragePredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Service to handle {@link FileRequestStatus} for new requests of all types.<br/>
 * The status of request is computed here to handle conflict between all requests.<br/>
 * <br/>
 * * A {@link FileReferenceRequestAggregation} cannot be performed and should be created as {@link FileRequestStatus#DELAYED} if :
 *  <ul>
 *    <li> A {@link FileReferenceRequestAggregation} exists on the file to reference</li>
 *    <li> A {@link FileStorageRequestAggregation} exists on the file to reference</li>
 *    <li> A {@link FileDeletionRequest} exists on the file to reference</li>
 * </ul>
 * A {@link FileStorageRequestAggregation} cannot be performed and should be created as {@link FileRequestStatus#DELAYED} if :
 * <ul>
 *  <li> A {@link FileReferenceRequestAggregation} exists on the file to store</li>
 *  <li> A {@link FileStorageRequestAggregation} exists on the file to store</li>
 *  <li> A {@link FileDeletionRequest} exists on the file to store</li>
 * </ul>
 * A {@link FileDeletionRequest} cannot be performed and should be created as {@link FileRequestStatus#DELAYED} if :
 * <ul>
 *  <li> A {@link FileStorageRequestAggregation} exists on the file to delete</li>
 *  <li> A {@link FileReferenceRequestAggregation} exists on the file to delete</li>
 *  <li> A {@link FileCopyRequest} exists on the file to delete</li>
 * </ul>
 * A {@link FileCopyRequest} cannot be performed and should be created as {@link FileRequestStatus#DELAYED} if :
 * <ul>
 *  <li> A {@link FileDeletionRequest} exists on the file to copy</li>
 * </ul>
 * A {@link FileCacheRequest} can always be performed. Those requests are never delayed.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class RequestStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestStatusService.class);

    public static final String TEMPLATE_REQUEST_HAS_BEEN_MANUALLY_CANCELED_N_TIMES = "Request has been manually canceled. %s";

    /**
     * PageRequest used by stopXxxRequests methods.
     */
    public static final PageRequest STOP_PAGE_REQUEST = PageRequest.of(0, 10_000);

    /**
     * PageRequest used by checkDelayedXxxRequests methods except checkDelayedCacheRequests.
     */
    public static final PageRequest CHECK_PAGE_REQUEST = PageRequest.of(0, 500);

    @Autowired
    private IFileDeletetionRequestRepository deletionReqRepo;

    @Autowired
    private IFileStorageRequestRepository storageReqRepo;

    @Autowired
    private IFileReferenceRequestRepository referenceReqRepo;

    @Autowired
    private IFileCopyRequestRepository copyReqRepo;

    @Autowired
    private IFileCacheRequestRepository cacheReqRepo;

    @Autowired
    private RequestsGroupService reqGrpService;

    @Autowired
    private IJobInfoService jobService;

    @Autowired
    private IFileReferenceRepository fileReferenceRepository;

    /**
     * Compute {@link FileRequestStatus} for new {@link FileStorageRequestAggregation}
     *
     * @param request  storage request to compute status for
     * @param oDefault default status or empty
     * @return {@link FileRequestStatus}
     */
    public FileRequestStatus getNewStatus(FileStorageRequestAggregation request, Optional<FileRequestStatus> oDefault) {
        FileRequestStatus status = oDefault.orElse(FileRequestStatus.TO_DO);
        String storage = request.getStorage();
        String checksum = request.getMetaInfo().getChecksum();
        Set<FileRequestStatus> toDelayStatusList = FileRequestStatus.RUNNING_STATUS;
        if (request.getStatus() == FileRequestStatus.TO_DO) {
            toDelayStatusList = FileRequestStatus.RUNNING_AND_DELAYED_STATUS;
        }
        // storage request shall be delayed
        // if a deletion requests already exists
        if (deletionReqRepo.existsByStorageAndFileReferenceMetaInfoChecksum(storage, checksum)
            // or if another storage request is already running for the same file
            || storageReqRepo.existsByStorageAndMetaInfoChecksumAndStatusIn(storage, checksum, toDelayStatusList)) {
            status = FileRequestStatus.DELAYED;
        } else if (request.getStatus() == FileRequestStatus.DELAYED && status == FileRequestStatus.TO_DO) {
            LOGGER.info("Storage Request {}/{} undelayed", request.getMetaInfo().getChecksum(), request.getStorage());
        }
        return status;
    }

    /**
     * Compute {@link FileRequestStatus} for new {@link FileReferenceRequestAggregation}
     *
     * @param request reference request to compute status for
     * @return {@link FileRequestStatus}
     */
    public FileRequestStatus getNewStatus(FileReferenceRequestAggregation request) {

        FileRequestStatus status = FileRequestStatus.TO_DO;
        final String storage = request.getStorage();
        final String checksum = request.getMetaInfo().getChecksum();
        Set<FileRequestStatus> toDelayStatusList = FileRequestStatus.RUNNING_STATUS;
        if (request.getStatus() == FileRequestStatus.TO_DO) {
            toDelayStatusList = FileRequestStatus.RUNNING_AND_DELAYED_STATUS;
        }
        // if a deletion requests already exists
        if (deletionReqRepo.existsByStorageAndFileReferenceMetaInfoChecksum(storage, checksum)
            // or if another storage request is already running for the same file
            || storageReqRepo.existsByStorageAndMetaInfoChecksumAndStatusIn(storage, checksum, toDelayStatusList)
            // or if another reference request is already running for the same file
            //|| referenceReqRepo.existsByStorageAndMetaInfoChecksumAndStatusIn(storage, checksum, toDelayStatusList)
        ) {
            // the request new status is DELAYED until the other request are handled.
            status = FileRequestStatus.DELAYED;
        } else if (request.getStatus() == FileRequestStatus.DELAYED && status == FileRequestStatus.TO_DO) {
            LOGGER.info("Reference Request {}/{} undelayed", request.getMetaInfo().getChecksum(), request.getStorage());
        }
        return status;
    }

    /**
     * Compute {@link FileRequestStatus} for new {@link FileDeletionRequest}
     *
     * @param request  deletion request to compute status for
     * @param oDefault default status or empty
     * @return {@link FileRequestStatus}
     */
    public FileRequestStatus getNewStatus(FileDeletionRequest request, Optional<FileRequestStatus> oDefault) {
        FileRequestStatus status = oDefault.orElse(FileRequestStatus.TO_DO);
        String storage = request.getStorage();
        String checksum = request.getFileReference().getMetaInfo().getChecksum();
        // deletion request shall be delayed
        // if a storage request is running
        if (storageReqRepo.existsByStorageAndMetaInfoChecksumAndStatusIn(storage,
                                                                         checksum,
                                                                         FileRequestStatus.RUNNING_STATUS)
            // or a copy request is running
            || copyReqRepo.existsByMetaInfoChecksumAndStatusIn(checksum, FileRequestStatus.RUNNING_STATUS)
            // or a reference request is running
            || referenceReqRepo.existsByStorageAndMetaInfoChecksumAndStatusIn(storage,
                                                                              checksum,
                                                                              FileRequestStatus.RUNNING_STATUS)) {
            // the deletion request DELAYED until the other request are handled.
            status = FileRequestStatus.DELAYED;
        }
        return status;
    }

    /**
     * Compute {@link FileRequestStatus} for new {@link FileCopyRequest}
     *
     * @param request  copy request to compute status for
     * @param oDefault default status or empty
     * @return {@link FileRequestStatus}
     */
    public FileRequestStatus getNewStatus(FileCopyRequest request, Optional<FileRequestStatus> oDefault) {
        FileRequestStatus status = oDefault.orElse(FileRequestStatus.TO_DO);
        final String checksum = request.getMetaInfo().getChecksum();
        // Delayed storage request if a deletion requests already exists
        if (deletionReqRepo.existsByFileReferenceMetaInfoChecksumAndStatusIn(checksum,
                                                                             FileRequestStatus.RUNNING_STATUS)) {
            status = FileRequestStatus.DELAYED;
        }
        return status;
    }

    public FileRequestStatus getNewStatus(FileCacheRequest request, Optional<FileRequestStatus> oDefault) {
        FileRequestStatus status = oDefault.orElse(FileRequestStatus.TO_DO);
        final String checksum = request.getChecksum();
        // Delayed storage request if a deletion requests already exists
        if (cacheReqRepo.existsByChecksumAndStatusIn(checksum, FileRequestStatus.RUNNING_STATUS)) {
            status = FileRequestStatus.DELAYED;
        }
        return status;
    }

    /**
     * Update delayed {@link FileReferenceRequestAggregation}s that can be handled.
     *
     * @param fileReferenceRequestService service that will handle the storage success for redundant request (whose file
     *                                    is already referenced).
     */
    public void checkDelayedReferenceRequests(FileReferenceRequestService fileReferenceRequestService) {
        int nbUpdated = 0;
        final List<FileReferenceRequestAggregation> undelayedRequests = new ArrayList<>();
        final Page<FileReferenceRequestAggregation> delayedRequests = referenceReqRepo.findByStatus(FileRequestStatus.DELAYED,
                                                                                                    CHECK_PAGE_REQUEST);

        for (FileReferenceRequestAggregation delayedRequest : delayedRequests) {
            final String storage = delayedRequest.getStorage();
            final String checksum = delayedRequest.getMetaInfo().getChecksum();
            final FileReferenceRequestAggregation undelayedRequest = firstUndelayedReferenceRequest(undelayedRequests,
                                                                                                    storage,
                                                                                                    checksum);

            if (undelayedRequest == null) {
                nbUpdated += updateDelayedReferenceRequest(undelayedRequests,
                                                           delayedRequest,
                                                           fileReferenceRequestService);
            } else {

                // If an identical storage request has already been undelayed,
                // just merge the two requests and delete the last one.
                LOGGER.info("[REFERENCE REQUEST] reference request delayed match existing one ({}/{})."
                            + "Both requests are merged and un-delayed", storage, checksum);
                undelayedRequest.getGroupIds().addAll(delayedRequest.getGroupIds());
                undelayedRequest.getOwners().addAll(delayedRequest.getOwners());
                referenceReqRepo.delete(delayedRequest);
            }
        }

        if (nbUpdated > 0) {
            LOGGER.debug("[REFERENCE REQUEST] {} delayed requests can now be handled.", nbUpdated);
        }
    }

    /**
     * Update delayed {@link FileStorageRequestAggregation}s that can be handled.
     *
     * @param fileStorageRequestService service that will handle the storage success for redundant request (whose file
     *                                  is already stored).
     */
    public void checkDelayedStorageRequests(FileStorageRequestService fileStorageRequestService) {
        int nbUpdated = 0;
        final List<FileStorageRequestAggregation> undelayedRequests = new ArrayList<>();
        final Page<FileStorageRequestAggregation> delayedRequests = storageReqRepo.findByStatus(FileRequestStatus.DELAYED,
                                                                                                CHECK_PAGE_REQUEST);

        for (FileStorageRequestAggregation delayedRequest : delayedRequests) {
            final String storage = delayedRequest.getStorage();
            final String checksum = delayedRequest.getMetaInfo().getChecksum();
            final FileStorageRequestAggregation undelayedRequest = firstUndelayedStorageRequest(undelayedRequests,
                                                                                                storage,
                                                                                                checksum);

            if (undelayedRequest == null) {
                nbUpdated += updateDelayedStorageRequest(undelayedRequests, delayedRequest, fileStorageRequestService);
            } else {

                // If an identical storage request has already been undelayed,
                // just merge the two requests and delete the last one.
                LOGGER.info("[STORAGE REQUEST] storage request delayed match existing one ({}/{})."
                            + "Both requests are merged and un-delayed", storage, checksum);
                undelayedRequest.getGroupIds().addAll(delayedRequest.getGroupIds());
                undelayedRequest.getOwners().addAll(delayedRequest.getOwners());
                storageReqRepo.delete(delayedRequest);
            }
        }

        if (nbUpdated > 0) {
            LOGGER.debug("[STORAGE REQUEST] {} delayed requests can now be handled.", nbUpdated);
        }
    }

    private int updateDelayedStorageRequest(final List<FileStorageRequestAggregation> undelayedRequests,
                                            final FileStorageRequestAggregation delayedRequest,
                                            final FileStorageRequestService fileStorageRequestService) {

        // Check new status for the delayed request
        final FileRequestStatus newStatus = getNewStatus(delayedRequest, Optional.empty());
        if (newStatus != FileRequestStatus.TO_DO) {
            return 0;
        }

        final FileReference storedFile = fileReferenceRepository.findByLocationStorageAndMetaInfoChecksum(delayedRequest.getStorage(),
                                                                                                          delayedRequest.getMetaInfo()
                                                                                                                        .getChecksum())
                                                                .orElse(null);

        if (storedFile == null) {
            // new TO_DO request
            undelayedRequests.add(delayedRequest);
            delayedRequest.setStatus(FileRequestStatus.TO_DO);
        } else {
            final FileStorageRequestResultDto result = FileStorageRequestResultDto.build(delayedRequest.toDto(),
                                                                                         storedFile.getLocation()
                                                                                                   .getUrl(),
                                                                                         storedFile.getMetaInfo()
                                                                                                   .getFileSize(),
                                                                                         storedFile.getLocation()
                                                                                                   .isPendingActionRemaining(),
                                                                                         false);
            fileStorageRequestService.handleSuccess(List.of(result));
        }
        return 1;
    }

    private FileStorageRequestAggregation firstUndelayedStorageRequest(Collection<FileStorageRequestAggregation> undelayedRequests,
                                                                       String storage,
                                                                       String checksum) {
        return undelayedRequests.stream()
                                .filter(StoragePredicates.fileStorageRequestWithSameStorageAndChecksum(storage,
                                                                                                       checksum))
                                .findFirst()
                                .orElse(null);
    }

    /**
     * Update the given DELAYED reference request. Mark it as TO_DO if not handled yet.
     * Handle it as completed if the reference exists. Do not do anything if the
     */
    private int updateDelayedReferenceRequest(final List<FileReferenceRequestAggregation> undelayedRequests,
                                              final FileReferenceRequestAggregation delayedRequest,
                                              final FileReferenceRequestService fileReferenceRequestService) {

        // Check new status for the delayed request
        final FileRequestStatus newStatus = getNewStatus(delayedRequest);
        if (newStatus != FileRequestStatus.TO_DO) {
            return 0;
        }

        // does reference matching the DELAYED request exists?
        final String storage = delayedRequest.getStorage();
        final String checksum = delayedRequest.getMetaInfo().getChecksum();
        final FileReference referencedFile = fileReferenceRepository.findByLocationStorageAndMetaInfoChecksum(storage,
                                                                                                              checksum)
                                                                    .orElse(null);

        if (referencedFile == null) {
            // no reference exists yet
            // the DELAYED request is again candidate to be handled
            undelayedRequests.add(delayedRequest);
            delayedRequest.setStatus(FileRequestStatus.TO_DO);
        } else {
            // The DELAYED request is considered as COMPLETED since the reference is already present.
            // so it does not need to be handled anymore
            fileReferenceRequestService.handleSuccess(delayedRequest, referencedFile, false);
        }
        return 1;
    }

    /**
     * Return the first reference request among the given collection of reference request matching the given storage
     * qnd checksum
     *
     * @param undelayedRequests collection of reference request to be searched.
     * @param storage           storage to be matched
     * @param checksum          checksum to be matched
     * @return a {@link FileReferenceRequestAggregation} or null if none found.
     */
    private FileReferenceRequestAggregation firstUndelayedReferenceRequest(Collection<FileReferenceRequestAggregation> undelayedRequests,
                                                                           String storage,
                                                                           String checksum) {
        return undelayedRequests.stream()
                                .filter(StoragePredicates.fileReferenceRequestWithSameStorageAndChecksum(storage,
                                                                                                         checksum))
                                .findFirst()
                                .orElse(null);
    }

    /**
     * Update delayed {@link FileReferenceRequestAggregation}s that can be handled.
     */
    public void checkDelayedReferenceRequests() {
        int nbUpdated = 0;
        for (FileReferenceRequestAggregation delayedRequest : referenceReqRepo.findByStatus(FileRequestStatus.DELAYED,
                                                                                            CHECK_PAGE_REQUEST)) {
            // Check new status for the delayed request
            if (getNewStatus(delayedRequest) == FileRequestStatus.TO_DO) {
                delayedRequest.setStatus(FileRequestStatus.TO_DO);
                nbUpdated++;
            }
        }
        if (nbUpdated > 0) {
            LOGGER.debug("[REFERENCE REQUEST] {} delayed requests can be handle now.", nbUpdated);
        }
    }

    /**
     * Update delayed {@link FileStorageRequestAggregation}s that can be handled.
     */
    public void checkDelayedDeleteRequests() {
        int nbUpdated = 0;
        for (FileDeletionRequest delayedRequest : deletionReqRepo.findByStatus(FileRequestStatus.DELAYED,
                                                                               CHECK_PAGE_REQUEST)) {
            // Check new status for the delayed request
            if (getNewStatus(delayedRequest, Optional.empty()) == FileRequestStatus.TO_DO) {
                delayedRequest.setStatus(FileRequestStatus.TO_DO);
                nbUpdated++;
            }
        }
        if (nbUpdated > 0) {
            LOGGER.debug("[DELETE REQUEST] {} delayed requests can be handled now.", nbUpdated);
        }
    }

    /**
     * Update delayed {@link FileCopyRequest}s that can be handled.
     */
    public void checkDelayedCopyRequests() {
        int nbUpdated = 0;
        for (FileCopyRequest delayedRequest : copyReqRepo.findByStatus(FileRequestStatus.DELAYED, CHECK_PAGE_REQUEST)) {
            // Check new status for the delayed request
            if (getNewStatus(delayedRequest, Optional.empty()) == FileRequestStatus.TO_DO) {
                delayedRequest.setStatus(FileRequestStatus.TO_DO);
                nbUpdated++;
            }
        }
        if (nbUpdated > 0) {
            LOGGER.debug("[COPY REQUEST] {} delayed requests can be handle now.", nbUpdated);
        }
    }

    /**
     * Update delayed {@link FileCacheRequest}s that can be handled.
     */
    public void checkDelayedCacheRequests() {
        int nbUpdated = 0;
        for (FileCacheRequest delayedCacheRequest : cacheReqRepo.findByStatus(FileRequestStatus.DELAYED,
                                                                              PageRequest.of(0, 100))) {
            // Check new status for the delayed request
            if (getNewStatus(delayedCacheRequest, Optional.empty()) == FileRequestStatus.TO_DO) {
                delayedCacheRequest.setStatus(FileRequestStatus.TO_DO);
                nbUpdated++;
            }
        }
        if (nbUpdated > 0) {
            LOGGER.debug("[COPY REQUEST] {} delayed requests can be handle now.", nbUpdated);
        }
    }

    /**
     * Stop all the PENDING {@link FileReferenceRequestAggregation} reference request.
     * Note: none are in a PENDING state so far.
     */
    public void stopReferenceRequests() {
        Page<FileReferenceRequestAggregation> pendings = referenceReqRepo.findByStatus(FileRequestStatus.PENDING,
                                                                                       STOP_PAGE_REQUEST);
        for (FileReferenceRequestAggregation pendingRequest : pendings) {
            Optional.ofNullable(pendingRequest.getJobId()).map(UUID::fromString).ifPresent(jobService::stopJob);
            referenceReqRepo.updateError(pendingRequest.getId(),
                                         FileRequestStatus.ERROR,
                                         now(TEMPLATE_REQUEST_HAS_BEEN_MANUALLY_CANCELED_N_TIMES));
        }
        reqGrpService.deleteRequestGroups(FileRequestType.REFERENCE);
        LOGGER.info("[FORCE STOP] Number of stopped reference requests : {}", pendings.getNumberOfElements());
    }

    /**
     * Stop all the PENDING {@link FileStorageRequestAggregation} storage request.
     */
    public void stopStorageRequests() {
        Page<FileStorageRequestAggregation> pendings = storageReqRepo.findByStatus(FileRequestStatus.PENDING,
                                                                                   STOP_PAGE_REQUEST);
        for (FileStorageRequestAggregation r : pendings) {
            if (r.getJobId() != null) {
                jobService.stopJob(UUID.fromString(r.getJobId()));
            }
            storageReqRepo.updateError(r.getId(),
                                       FileRequestStatus.ERROR,
                                       now(TEMPLATE_REQUEST_HAS_BEEN_MANUALLY_CANCELED_N_TIMES));
        }
        reqGrpService.deleteRequestGroups(FileRequestType.STORAGE);
        LOGGER.info("[FORCE STOP] Number of stopped storage requests : {}", pendings.getNumberOfElements());
    }

    /**
     * Stop all the PENDING {@link FileDeletionRequest} deletion request.
     */
    public void stopDeletionRequests() {
        Page<FileDeletionRequest> pendings = deletionReqRepo.findByStatus(FileRequestStatus.PENDING, STOP_PAGE_REQUEST);
        for (FileDeletionRequest r : pendings) {
            if (r.getJobId() != null) {
                jobService.stopJob(UUID.fromString(r.getJobId()));
            }
            deletionReqRepo.updateError(r.getId(),
                                        FileRequestStatus.ERROR,
                                        now(TEMPLATE_REQUEST_HAS_BEEN_MANUALLY_CANCELED_N_TIMES));
        }
        reqGrpService.deleteRequestGroups(FileRequestType.DELETION);
        LOGGER.info("[FORCE STOP] Number of stopped deletion requests : {}", pendings.getNumberOfElements());
    }

    public void stopCopyRequests() {
        Page<FileCopyRequest> pendings = copyReqRepo.findByStatus(FileRequestStatus.PENDING, STOP_PAGE_REQUEST);
        for (FileCopyRequest r : pendings) {
            copyReqRepo.updateError(r.getId(),
                                    FileRequestStatus.ERROR,
                                    now(TEMPLATE_REQUEST_HAS_BEEN_MANUALLY_CANCELED_N_TIMES));
        }
        reqGrpService.deleteRequestGroups(FileRequestType.COPY);
        LOGGER.info("[FORCE STOP] Number of stopped copy requests : {}", pendings.getNumberOfElements());
    }

    public void stopCacheRequests() {
        Page<FileCacheRequest> pendings = cacheReqRepo.findByStatus(FileRequestStatus.PENDING, STOP_PAGE_REQUEST);
        for (FileCacheRequest r : pendings) {
            if (r.getJobId() != null) {
                jobService.stopJob(UUID.fromString(r.getJobId()));
            }
            cacheReqRepo.updateError(r.getId(),
                                     FileRequestStatus.ERROR,
                                     now(TEMPLATE_REQUEST_HAS_BEEN_MANUALLY_CANCELED_N_TIMES));
        }
        reqGrpService.deleteRequestGroups(FileRequestType.AVAILABILITY);
        LOGGER.info("[FORCE STOP] Number of stopped cache requests : {}", pendings.getNumberOfElements());
    }

    private static String now(String format) {
        return String.format(format, OffsetDateTime.now());
    }
}
