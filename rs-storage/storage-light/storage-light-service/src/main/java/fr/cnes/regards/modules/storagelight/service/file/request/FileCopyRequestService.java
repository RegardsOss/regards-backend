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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storagelight.dao.IFileCopyRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.database.CacheFile;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileCopyRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import fr.cnes.regards.modules.storagelight.domain.plugin.INearlineStorageLocation;
import fr.cnes.regards.modules.storagelight.service.JobsPriority;
import fr.cnes.regards.modules.storagelight.service.cache.CacheService;
import fr.cnes.regards.modules.storagelight.service.file.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storagelight.service.file.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.job.FileCopyRequestsCreatorJob;

/**
 * Service to handle {@link FileCopyRequest}s.
 * Those requests are created when a file reference need to be restored physically thanks to an existing {@link INearlineStorageLocation} plugin.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class FileCopyRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCopyRequestService.class);

    private static final int NB_REFERENCE_BY_PAGE = 1000;

    @Autowired
    private IFileCopyRequestRepository copyRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private FileReferenceService fileRefService;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private FileReferenceEventPublisher publisher;

    @Autowired
    private FileCacheRequestService fileCacheReqService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private RequestsGroupService reqGrpService;

    @Autowired
    private IAuthenticationResolver authResolver;

    /**
     * Handle a {@link FileCopyRequestDTO}.<br>
     * If a copy request with the same parameters already exists, this method does not creates a new one.<br>
     * If the file to copy is well referenced, then a new copy request is created.<br>
     * The copy request should be handled next by the {@link FileRequestScheduler#handleFileCopyRequests()} method.
     *
     * @param requestDto {@link FileCopyRequestDTO} to handle.
     * @param groupId request group identifier.
     * @return {@link FileCopyRequest} created if any.
     */
    public Optional<FileCopyRequest> handle(FileCopyRequestDTO requestDto, String groupId) {
        // Check a same request already exists
        Optional<FileCopyRequest> request = copyRepository.findOneByMetaInfoChecksumAndStorage(requestDto.getChecksum(),
                                                                                               requestDto.getStorage());
        if (request.isPresent()) {
            return Optional.of(handleAlreadyExists(requestDto, request.get(), groupId));
        } else {
            // get file meta info to copy
            Set<FileReference> refs = fileRefService.search(requestDto.getChecksum());
            if (refs.isEmpty()) {
                String message = String
                        .format("File copy request refused for file %s to %s storage location. Cause file does not exists in any known storage location.",
                                requestDto.getChecksum(), requestDto.getStorage());
                LOGGER.warn("[COPY REQUEST] {}", message);
                notificationClient.notify(message, "File copy request refused", NotificationLevel.WARNING,
                                          DefaultRole.PROJECT_ADMIN);
            } else {
                FileCopyRequest newRequest = copyRepository
                        .save(new FileCopyRequest(groupId, refs.stream().findFirst().get().getMetaInfo(),
                                requestDto.getSubDirectory(), requestDto.getStorage()));
                request = Optional.of(newRequest);
            }
        }
        return request;
    }

    /**
     * Handle the case of a copy requests already exists.<br>
     * @param requestDto {@link FileCopyRequestDTO} new request to handle.
     * @param request {@link FileCopyRequest} existing request.
     * @param newGroupId New request group idenfitier.
     * @return updated {@link FileCopyRequest}
     */
    private FileCopyRequest handleAlreadyExists(FileCopyRequestDTO requestDto, FileCopyRequest request,
            String newGroupId) {
        if (request.getStatus() == FileRequestStatus.ERROR) {
            request.setStatus(FileRequestStatus.TO_DO);
            request.setFileCacheGroupId(newGroupId);
            return update(request);
        }
        return request;
    }

    /**
     * Schedule availability requests for all copy requests.<br>
     * @param status status of copy requests to schedule.
     */
    public void scheduleCopyRequests(FileRequestStatus status) {
        LOGGER.debug("[COPY REQUESTS] handling copy requests ...");
        long start = System.currentTimeMillis();
        Pageable page = PageRequest.of(0, NB_REFERENCE_BY_PAGE, Direction.ASC, "id");
        Page<FileCopyRequest> pageResp = null;
        OffsetDateTime expDate = OffsetDateTime.now().plusDays(1);
        do {
            String fileCacheGroupId = UUID.randomUUID().toString();
            Set<String> checksums = Sets.newHashSet();
            pageResp = copyRepository.findByStatus(status, page);
            for (FileCopyRequest request : pageResp.getContent()) {
                checksums.add(request.getMetaInfo().getChecksum());
                request.setFileCacheGroupId(fileCacheGroupId);
                request.setStatus(FileRequestStatus.PENDING);
            }

            if (!checksums.isEmpty()) {
                fileCacheReqService.makeAvailable(checksums, expDate, fileCacheGroupId);
            }
            page = page.next();
        } while (pageResp.hasNext());
        LOGGER.debug("[COPY REQUESTS] Copy requests handled in {} ms", System.currentTimeMillis() - start);
    }

    /**
     * Handle many {@link FileCopyRequestDTO} to copy files to a given storage location.
     * @param files copy requests
     * @param groupId business request identifier
     */
    public void handle(Collection<FileCopyRequestDTO> requests, String groupId) {
        for (FileCopyRequestDTO request : requests) {
            handle(request, groupId);
        }
    }

    /**
     * Handle copy request success.
     * @param request {@link FileCopyRequest} succeeded request.
     * @param newFileRef {@link FileReference} new file reference. Copy of the original file reference.
     */
    public void handleSuccess(FileCopyRequest request, FileReference newFileRef) {
        String successMessage = String.format("File %s (checksum: %s) successfully copied in %s storage location",
                                              request.getMetaInfo().getFileName(), request.getMetaInfo().getChecksum(),
                                              request.getStorage());
        LOGGER.debug("[COPY SUCCESS] {}", successMessage);
        // Delete the copy request
        copyRepository.delete(request);

        // Check if associated cache file is always present
        Optional<CacheFile> oCf = cacheService.getCacheFile(request.getMetaInfo().getChecksum());
        if (oCf.isPresent()) {
            // If it is present, check if an other availability request was used for this
            if (!oCf.get().getGroupIds().stream().anyMatch(id -> !id.equals(request.getFileCacheGroupId()))) {
                // If not, delete the cache file
                cacheService.delete(oCf.get());
            }
        }
        publisher.copySuccess(newFileRef, successMessage, request.getGroupId());
        reqGrpService.requestSuccess(request.getGroupId(), FileRequestType.COPY, request.getMetaInfo().getChecksum(),
                                     request.getStorage(), newFileRef);
    }

    /**
     * Handle copy request error.
     * @param request {@link FileCopyRequest} error request.
     * @param errorCause
     */
    public void handleError(FileCopyRequest request, String errorCause) {
        LOGGER.error("[COPY ERROR] Error copying file {} (checksum: {}) to {} storage location. Cause : {}",
                     request.getMetaInfo().getFileName(), request.getMetaInfo().getChecksum(), request.getStorage(),
                     errorCause);
        // Update copy request to error status
        request.setStatus(FileRequestStatus.ERROR);
        request.setErrorCause(errorCause);
        update(request);
        publisher.copyError(request, errorCause);
        reqGrpService.requestError(request.getGroupId(), FileRequestType.COPY, request.getMetaInfo().getChecksum(),
                                   request.getStorage(), errorCause);
    }

    /**
     * Search for a {@link FileCopyRequest} for the given checksum and given storage copy destination.
     * @param checksum
     * @param storage
     * @return {@link FileCopyRequest} if any
     */
    @Transactional(readOnly = true)
    public Optional<FileCopyRequest> search(String checksum, String storage) {
        return copyRepository.findOneByMetaInfoChecksumAndStorage(checksum, storage);
    }

    /**
     * Search for a {@link FileCopyRequest} associated to the given {@link FileReferenceEvent}.
     * @param event
     * @return {@link FileCopyRequest} if any
     */
    public Optional<FileCopyRequest> search(FileReferenceEvent event) {
        Optional<FileCopyRequest> req = Optional.empty();
        Iterator<String> it;
        switch (event.getType()) {
            case AVAILABLE:
            case AVAILABILITY_ERROR:
                it = event.getGroupIds().iterator();
                while (it.hasNext() && !req.isPresent()) {
                    req = copyRepository.findByMetaInfoChecksumAndFileCacheGroupId(event.getChecksum(), it.next());
                }
                break;
            case STORED:
            case STORE_ERROR:
                it = event.getGroupIds().iterator();
                while (it.hasNext() && !req.isPresent()) {
                    req = copyRepository.findByMetaInfoChecksumAndFileStorageGroupId(event.getChecksum(), it.next());
                }
                break;
            case DELETED_FOR_OWNER:
            case FULLY_DELETED:
            case DELETION_ERROR:
            default:
                break;
        }
        return req;
    }

    /**
     * Update in database the given {@link FileCopyRequest}.
     * @param request
     * @return
     */
    public FileCopyRequest update(FileCopyRequest request) {
        Assert.notNull(request, "FileCopyRequest to update can not be null.");
        Assert.notNull(request.getId(), "FileCopyRequestto update. Identifier can not be null.");
        return copyRepository.save(request);
    }

    /**
     * Schedule a job to create {@link FileCopyRequest}s for the given criterion
     * @param storageLocationId
     * @param destinationStorageId
     * @param pathToCopy
     * @return
     */
    public JobInfo scheduleJob(String storageLocationId, String destinationStorageId, String pathToCopy) {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(FileCopyRequestsCreatorJob.STORAGE_LOCATION_SOURCE_ID, storageLocationId));
        parameters.add(new JobParameter(FileCopyRequestsCreatorJob.STORAGE_LOCATION_DESTINATION_ID,
                destinationStorageId));
        parameters.add(new JobParameter(FileCopyRequestsCreatorJob.PATH_TO_COPY, pathToCopy));
        JobInfo jobInfo = jobInfoService.createAsQueued(new JobInfo(false, JobsPriority.FILE_COPY_JOB.getPriority(),
                parameters, authResolver.getUser(), FileCopyRequestsCreatorJob.class.getName()));
        LOGGER.info("[COPY REQUESTS] Job scheduled to copy files from {} to {} for path {}.", storageLocationId,
                    destinationStorageId, pathToCopy);
        return jobInfo;
    }
}
