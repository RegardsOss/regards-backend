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
package fr.cnes.regards.modules.storage.service.file.request;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.storage.dao.IFileCacheRequestRepository;
import fr.cnes.regards.modules.storage.dao.IFileCopyRequestRepository;
import fr.cnes.regards.modules.storage.dao.IFileDeletetionRequestRepository;
import fr.cnes.regards.modules.storage.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;

/**
 * Service to handle {@link FileRequestStatus} for new requests of all types.<br/>
 * The status of request is computed here to handle conflict between all requests.<br/>
 * <br/>
 * A {@link FileStorageRequest} cannot be performed and should be created as {@link FileRequestStatus#DELAYED} if :
 * <ul>
 *  <li> A {@link FileDeletionRequest} exists on the file to store</li>
 * </ul>
 * A {@link FileDeletionRequest} cannot be performed and should be created as {@link FileRequestStatus#DELAYED} if :
 * <ul>
 *  <li> A {@link FileStorageRequest} exists on the file to delete</li>
 *  <li> A {@link FileCopyRequest} exists on the file to delete</li>
 * </ul>
 * A {@link FileCopyRequest} cannot be performed and should be created as {@link FileRequestStatus#DELAYED} if :
 * <ul>
 *  <li> A {@link FileDeletionRequest} exists on the file to delete</li>
 * </ul>
 * A {@link FileCacheRequest} can always be performed. Thoses requests are never delayed.
 *
 * @author Sébastien Binda
 *
 */
@Service
@MultitenantTransactional
public class RequestStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestStatusService.class);

    @Autowired
    private IFileDeletetionRequestRepository deletionReqRepo;

    @Autowired
    private IFileStorageRequestRepository storageReqRepo;

    @Autowired
    private IFileCopyRequestRepository copyReqRepo;

    @Autowired
    private IFileCacheRequestRepository cacheReqRepo;

    @Autowired
    private IJobInfoService jobService;

    /**
     * Compute {@link FileRequestStatus} for new {@link FileStorageRequest}
     *
     * @param request request to compute status for
     * @param oDefault default status or empty
     * @return {@link FileRequestStatus}
     */
    public FileRequestStatus getNewStatus(FileStorageRequest request, Optional<FileRequestStatus> oDefault) {
        FileRequestStatus status = oDefault.orElse(FileRequestStatus.TO_DO);
        String storage = request.getStorage();
        String checksum = request.getMetaInfo().getChecksum();
        // Delayed storage request if a deletion requests already exists
        if (deletionReqRepo
                .existsByStorageAndFileReferenceMetaInfoChecksumAndStatusIn(storage, checksum,
                                                                            FileRequestStatus.RUNNING_STATUS)) {
            status = FileRequestStatus.DELAYED;
        }
        // Delay storage request if an other storage request is already running for the same file to store
        else if (storageReqRepo
                .existsByStorageAndMetaInfoChecksumAndStatusIn(storage, checksum,
                                                               Sets.newHashSet(FileRequestStatus.RUNNING_STATUS))) {
            status = FileRequestStatus.DELAYED;
        }
        return status;
    }

    /**
     * Compute {@link FileRequestStatus} for new {@link FileCacheRequest}
     *
     * @param request request to compute status for
     * @return {@link FileRequestStatus}
     */
    public FileRequestStatus getNewStatus(FileCacheRequest request) {
        return FileRequestStatus.TO_DO;
    }

    /**
     * Compute {@link FileRequestStatus} for new {@link FileDeletionRequest}
     *
     * @param request request to compute status for
     * @param oDefault default status or empty
     * @return {@link FileRequestStatus}
     */
    public FileRequestStatus getNewStatus(FileDeletionRequest request, Optional<FileRequestStatus> oDefault) {
        FileRequestStatus status = oDefault.orElse(FileRequestStatus.TO_DO);
        String storage = request.getStorage();
        String checksum = request.getFileReference().getMetaInfo().getChecksum();
        // Delayed deletion request if a storage or copy request already exists
        if (storageReqRepo.existsByStorageAndMetaInfoChecksumAndStatusIn(storage, checksum,
                                                                         FileRequestStatus.RUNNING_STATUS)
                || copyReqRepo.existsByMetaInfoChecksumAndStatusIn(checksum, FileRequestStatus.RUNNING_STATUS)) {
            status = FileRequestStatus.DELAYED;
        }
        return status;
    }

    /**
     * Compute {@link FileRequestStatus} for new {@link FileCopyRequest}
     *
     * @param request request to compute status for
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

    /**
     * Update delayed {@link FileStorageRequest}s that can be handled.
     */
    public void checkDelayedStorageRequests() {
        int nbDelayedrequestUpdated = 0;
        for (FileStorageRequest defayledRequest : storageReqRepo.findByStatus(FileRequestStatus.DELAYED,
                                                                              PageRequest.of(0, 500))) {
            // Check new status for the delayed request
            if (getNewStatus(defayledRequest, Optional.empty()) == FileRequestStatus.TO_DO) {
                defayledRequest.setStatus(FileRequestStatus.TO_DO);
                storageReqRepo.save(defayledRequest);
                nbDelayedrequestUpdated++;
            }
        }
        if (nbDelayedrequestUpdated > 0) {
            LOGGER.debug("[STORAGE REQUEST] {} delayed requests can be hanle now.", nbDelayedrequestUpdated);
        }
    }

    /**
     * Update delayed {@link FileStorageRequest}s that can be handled.
     */
    public void checkDelayedDeleteRequests() {
        int nbDelayedrequestUpdated = 0;
        for (FileDeletionRequest defayledRequest : deletionReqRepo.findByStatus(FileRequestStatus.DELAYED,
                                                                                PageRequest.of(0, 500))) {
            // Check new status for the delayed request
            if (getNewStatus(defayledRequest, Optional.empty()) == FileRequestStatus.TO_DO) {
                defayledRequest.setStatus(FileRequestStatus.TO_DO);
                deletionReqRepo.save(defayledRequest);
                nbDelayedrequestUpdated++;
            }
        }
        if (nbDelayedrequestUpdated > 0) {
            LOGGER.debug("[DELETE REQUEST] {} delayed requests can be hanle now.", nbDelayedrequestUpdated);
        }
    }

    /**
     * Update delayed {@link FileCopyRequest}s that can be handled.
     */
    public void checkDelayedCopyRequests() {
        int nbDelayedrequestUpdated = 0;
        for (FileCopyRequest defayledRequest : copyReqRepo.findByStatus(FileRequestStatus.DELAYED,
                                                                        PageRequest.of(0, 500))) {
            // Check new status for the delayed request
            if (getNewStatus(defayledRequest, Optional.empty()) == FileRequestStatus.TO_DO) {
                defayledRequest.setStatus(FileRequestStatus.TO_DO);
                copyReqRepo.save(defayledRequest);
                nbDelayedrequestUpdated++;
            }
        }
        if (nbDelayedrequestUpdated > 0) {
            LOGGER.debug("[COPY REQUEST] {} delayed requests can be hanle now.", nbDelayedrequestUpdated);
        }
    }

    /**
     * Update delayed {@link FileCacheRequest}s that can be handled.
     */
    public void checkDelayedCacheRequests() {
        // Nothing to do. No cache requests can be delayed.
    }

    public void stopStorageRequests() {
        Page<FileStorageRequest> pendings = storageReqRepo.findByStatus(FileRequestStatus.PENDING,
                                                                        PageRequest.of(0, 10_000));
        for (FileStorageRequest r : pendings) {
            if (r.getJobId() != null) {
                jobService.stopJob(UUID.fromString(r.getJobId()));
            }
            storageReqRepo.updateError(
                                       FileRequestStatus.ERROR, String.format("Request has been manually canceled. %s",
                                                                              OffsetDateTime.now().toString()),
                                       r.getId());
        }
    }

    public void stopDeletionRequests() {
        Page<FileDeletionRequest> pendings = deletionReqRepo.findByStatus(FileRequestStatus.PENDING,
                                                                          PageRequest.of(0, 10_000));
        for (FileDeletionRequest r : pendings) {
            if (r.getJobId() != null) {
                jobService.stopJob(UUID.fromString(r.getJobId()));
            }
            deletionReqRepo.updateError(
                                        FileRequestStatus.ERROR, String.format("Request has been manually canceled. %s",
                                                                               OffsetDateTime.now().toString()),
                                        r.getId());
        }
    }

    public void stopCopyRequests() {
        Page<FileCopyRequest> pendings = copyReqRepo.findByStatus(FileRequestStatus.PENDING, PageRequest.of(0, 10_000));
        for (FileCopyRequest r : pendings) {
            copyReqRepo.updateError(FileRequestStatus.ERROR, String.format("Request has been manually canceled. %s",
                                                                           OffsetDateTime.now().toString()),
                                    r.getId());
        }
    }

    public void stopCacheRequests() {
        Page<FileCacheRequest> pendings = cacheReqRepo.findByStatus(FileRequestStatus.PENDING,
                                                                    PageRequest.of(0, 10_000));
        for (FileCacheRequest r : pendings) {
            if (r.getJobId() != null) {
                jobService.stopJob(UUID.fromString(r.getJobId()));
            }
            cacheReqRepo.updateError(
                                     FileRequestStatus.ERROR, String.format("Request has been manually canceled. %s",
                                                                            OffsetDateTime.now().toString()),
                                     r.getId());
        }
    }

}
