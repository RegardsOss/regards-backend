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
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
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
 * A {@link FileStorageRequestAggregation} cannot be performed and should be created as {@link FileRequestStatus#DELAYED} if :
 * <ul>
 *  <li> A {@link FileDeletionRequest} exists on the file to store</li>
 * </ul>
 * A {@link FileDeletionRequest} cannot be performed and should be created as {@link FileRequestStatus#DELAYED} if :
 * <ul>
 *  <li> A {@link FileStorageRequestAggregation} exists on the file to delete</li>
 *  <li> A {@link FileCopyRequest} exists on the file to delete</li>
 * </ul>
 * A {@link FileCopyRequest} cannot be performed and should be created as {@link FileRequestStatus#DELAYED} if :
 * <ul>
 *  <li> A {@link FileDeletionRequest} exists on the file to delete</li>
 * </ul>
 * A {@link FileCacheRequest} can always be performed. Thoses requests are never delayed.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class RequestStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestStatusService.class);

    public static final String TEMPLATE_REQUEST_HAS_BEEN_MANUALLY_CANCELED_N_TIMES = "Request has been manually canceled. %s";

    @Autowired
    private IFileDeletetionRequestRepository deletionReqRepo;

    @Autowired
    private IFileStorageRequestRepository storageReqRepo;

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
     * @param request  request to compute status for
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
        // Delayed storage request if a deletion requests already exists
        if (deletionReqRepo.existsByStorageAndFileReferenceMetaInfoChecksumAndStatusIn(storage,
                                                                                       checksum,
                                                                                       FileRequestStatus.RUNNING_STATUS)) {
            status = FileRequestStatus.DELAYED;
        }
        // Delay storage request if an other storage request is already running for the same file to store
        else if (storageReqRepo.existsByStorageAndMetaInfoChecksumAndStatusIn(storage, checksum, toDelayStatusList)) {
            status = FileRequestStatus.DELAYED;
        } else if (request.getStatus() == FileRequestStatus.DELAYED && status == FileRequestStatus.TO_DO) {
            LOGGER.info("Request {}/{} undelayed", request.getMetaInfo().getChecksum(), request.getStorage());
        }
        return status;
    }

    /**
     * Compute {@link FileRequestStatus} for new {@link FileDeletionRequest}
     *
     * @param request  request to compute status for
     * @param oDefault default status or empty
     * @return {@link FileRequestStatus}
     */
    public FileRequestStatus getNewStatus(FileDeletionRequest request, Optional<FileRequestStatus> oDefault) {
        FileRequestStatus status = oDefault.orElse(FileRequestStatus.TO_DO);
        String storage = request.getStorage();
        String checksum = request.getFileReference().getMetaInfo().getChecksum();
        // Delayed deletion request if a storage or copy request already exists
        if (storageReqRepo.existsByStorageAndMetaInfoChecksumAndStatusIn(storage,
                                                                         checksum,
                                                                         FileRequestStatus.RUNNING_STATUS)
            || copyReqRepo.existsByMetaInfoChecksumAndStatusIn(checksum, FileRequestStatus.RUNNING_STATUS)) {
            status = FileRequestStatus.DELAYED;
        }
        return status;
    }

    /**
     * Compute {@link FileRequestStatus} for new {@link FileCopyRequest}
     *
     * @param request  request to compute status for
     * @param oDefault default status or empty
     * @return {@link FileRequestStatus}
     */
    public FileRequestStatus getNewStatus(FileCopyRequest request, Optional<FileRequestStatus> oDefault) {
        FileRequestStatus status = oDefault.orElse(FileRequestStatus.TO_DO);
        String checksum = request.getMetaInfo().getChecksum();
        // Delayed storage request if a deletion requests already exists
        if (deletionReqRepo.existsByFileReferenceMetaInfoChecksumAndStatusIn(checksum,
                                                                             FileRequestStatus.RUNNING_STATUS)) {
            status = FileRequestStatus.DELAYED;
        }
        return status;
    }

    public FileRequestStatus getNewStatus(FileCacheRequest request, Optional<FileRequestStatus> oDefault) {
        FileRequestStatus status = oDefault.orElse(FileRequestStatus.TO_DO);
        String checksum = request.getChecksum();
        // Delayed storage request if a deletion requests already exists
        if (cacheReqRepo.existsByChecksumAndStatusIn(checksum, FileRequestStatus.RUNNING_STATUS)) {
            status = FileRequestStatus.DELAYED;
        }
        return status;
    }

    /**
     * Update delayed {@link FileStorageRequestAggregation}s that can be handled.
     *
     * @param fileStorageRequestService service that will handle the storage success for redundant request (whose file
     *                                  is already stored).
     */
    public void checkDelayedStorageRequests(FileStorageRequestService fileStorageRequestService) {
        int nbUpdated = 0;
        List<FileStorageRequestAggregation> undelayedRequests = new ArrayList<>();
        for (FileStorageRequestAggregation delayedRequest : storageReqRepo.findByStatus(FileRequestStatus.DELAYED,
                                                                                        PageRequest.of(0, 500))) {
            String storage = delayedRequest.getStorage();
            String checksum = delayedRequest.getMetaInfo().getChecksum();
            Optional<FileStorageRequestAggregation> sameRequest = undelayedRequests.stream()
                                                                                   .filter(ur -> ur.getStorage()
                                                                                                   .equals(storage)
                                                                                                 && ur.getMetaInfo()
                                                                                                      .getChecksum()
                                                                                                      .equals(checksum))
                                                                                   .findFirst();
            if (sameRequest.isEmpty()) {
                // Check new status for the delayed request
                FileRequestStatus newStatus = getNewStatus(delayedRequest, Optional.empty());
                if (newStatus == FileRequestStatus.TO_DO) {
                    Optional<FileReference> oStoredFile = fileReferenceRepository.findByLocationStorageAndMetaInfoChecksum(
                        delayedRequest.getStorage(),
                        delayedRequest.getMetaInfo().getChecksum());
                    if (oStoredFile.isEmpty()) {
                        undelayedRequests.add(delayedRequest);
                        delayedRequest.setStatus(FileRequestStatus.TO_DO);
                    } else {
                        FileReference storedFile = oStoredFile.get();
                        FileStorageRequestResultDto result = FileStorageRequestResultDto.build(delayedRequest.toDto(),
                                                                                               storedFile.getLocation()
                                                                                                         .getUrl(),
                                                                                               storedFile.getMetaInfo()
                                                                                                         .getFileSize(),
                                                                                               storedFile.getLocation()
                                                                                                         .isPendingActionRemaining(),
                                                                                               false);
                        fileStorageRequestService.handleSuccess(List.of(result));
                    }
                    nbUpdated++;
                }
            } else {
                // If an identical storage request has already been undelayed, just merge all two requests and delete
                // the last one.
                LOGGER.info("[STORAGE REQUEST] storage request delayed match existing one ({}/{}). Both requests are "
                            + "merged and un-delayed", storage, checksum);
                FileStorageRequestAggregation undelayedRequest = sameRequest.get();
                undelayedRequest.getGroupIds().addAll(delayedRequest.getGroupIds());
                undelayedRequest.getOwners().addAll(delayedRequest.getOwners());
                storageReqRepo.delete(delayedRequest);
            }
        }
        if (nbUpdated > 0) {
            LOGGER.debug("[STORAGE REQUEST] {} delayed requests can now be handled.", nbUpdated);
        }
    }

    /**
     * Update delayed {@link FileStorageRequestAggregation}s that can be handled.
     */
    public void checkDelayedDeleteRequests() {
        int nbUpdated = 0;
        for (FileDeletionRequest defayledRequest : deletionReqRepo.findByStatus(FileRequestStatus.DELAYED,
                                                                                PageRequest.of(0, 500))) {
            // Check new status for the delayed request
            if (getNewStatus(defayledRequest, Optional.empty()) == FileRequestStatus.TO_DO) {
                defayledRequest.setStatus(FileRequestStatus.TO_DO);
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
        for (FileCopyRequest defayledRequest : copyReqRepo.findByStatus(FileRequestStatus.DELAYED,
                                                                        PageRequest.of(0, 500))) {
            // Check new status for the delayed request
            if (getNewStatus(defayledRequest, Optional.empty()) == FileRequestStatus.TO_DO) {
                defayledRequest.setStatus(FileRequestStatus.TO_DO);
                nbUpdated++;
            }
        }
        if (nbUpdated > 0) {
            LOGGER.debug("[COPY REQUEST] {} delayed requests can be hanle now.", nbUpdated);
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
            LOGGER.debug("[COPY REQUEST] {} delayed requests can be hanle now.", nbUpdated);
        }
    }

    public void stopStorageRequests() {
        Page<FileStorageRequestAggregation> pendings = storageReqRepo.findByStatus(FileRequestStatus.PENDING,
                                                                                   PageRequest.of(0, 10_000));
        for (FileStorageRequestAggregation r : pendings) {
            if (r.getJobId() != null) {
                jobService.stopJob(UUID.fromString(r.getJobId()));
            }
            storageReqRepo.updateError(FileRequestStatus.ERROR,
                                       String.format(TEMPLATE_REQUEST_HAS_BEEN_MANUALLY_CANCELED_N_TIMES,
                                                     OffsetDateTime.now()),
                                       r.getId());
        }
        reqGrpService.deleteRequestGroups(FileRequestType.STORAGE);
        LOGGER.info("[FORCE STOP] Number of stopped storage requests : {}", pendings.getNumberOfElements());
    }

    public void stopDeletionRequests() {
        Page<FileDeletionRequest> pendings = deletionReqRepo.findByStatus(FileRequestStatus.PENDING,
                                                                          PageRequest.of(0, 10_000));
        for (FileDeletionRequest r : pendings) {
            if (r.getJobId() != null) {
                jobService.stopJob(UUID.fromString(r.getJobId()));
            }
            deletionReqRepo.updateError(FileRequestStatus.ERROR,
                                        String.format(TEMPLATE_REQUEST_HAS_BEEN_MANUALLY_CANCELED_N_TIMES,
                                                      OffsetDateTime.now()),
                                        r.getId());
        }
        reqGrpService.deleteRequestGroups(FileRequestType.DELETION);
        LOGGER.info("[FORCE STOP] Number of stopped deletion requests : {}", pendings.getNumberOfElements());
    }

    public void stopCopyRequests() {
        Page<FileCopyRequest> pendings = copyReqRepo.findByStatus(FileRequestStatus.PENDING, PageRequest.of(0, 10_000));
        for (FileCopyRequest r : pendings) {
            copyReqRepo.updateError(FileRequestStatus.ERROR,
                                    String.format(TEMPLATE_REQUEST_HAS_BEEN_MANUALLY_CANCELED_N_TIMES,
                                                  OffsetDateTime.now()),
                                    r.getId());
        }
        reqGrpService.deleteRequestGroups(FileRequestType.COPY);
        LOGGER.info("[FORCE STOP] Number of stopped copy requests : {}", pendings.getNumberOfElements());
    }

    public void stopCacheRequests() {
        Page<FileCacheRequest> pendings = cacheReqRepo.findByStatus(FileRequestStatus.PENDING,
                                                                    PageRequest.of(0, 10_000));
        for (FileCacheRequest r : pendings) {
            if (r.getJobId() != null) {
                jobService.stopJob(UUID.fromString(r.getJobId()));
            }
            cacheReqRepo.updateError(FileRequestStatus.ERROR,
                                     String.format(TEMPLATE_REQUEST_HAS_BEEN_MANUALLY_CANCELED_N_TIMES,
                                                   OffsetDateTime.now()),
                                     r.getId());
        }
        reqGrpService.deleteRequestGroups(FileRequestType.AVAILABILITY);
        LOGGER.info("[FORCE STOP] Number of stopped cache requests : {}", pendings.getNumberOfElements());
    }

}
