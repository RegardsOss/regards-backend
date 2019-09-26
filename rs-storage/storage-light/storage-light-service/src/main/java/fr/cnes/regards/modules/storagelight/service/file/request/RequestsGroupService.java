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

import java.util.Set;

import org.apache.commons.compress.utils.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.storagelight.dao.IFileCacheRequestRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileCopyRequestRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileDeletetionRequestRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storagelight.dao.IGroupRequestInfoRepository;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.RequestResultInfo;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestsGroupEvent;
import fr.cnes.regards.modules.storagelight.domain.flow.FlowItemStatus;

/**
 * Service to handle actions on requests group.<br>
 * A requests group is an business association between many FileRequests of the same type.<br>
 * All requests of a same groups are associated thanks to a group identifier.<br>
 * When all requests of a group has been handled by the associated service, then a {@link FileRequestsGroupEvent} is published
 *  with {@link FlowItemStatus#GRANTED} status.<br>
 * When all requests of a group has been rejected by the associated service, then a {@link FileRequestsGroupEvent} is published
 *  with {@link FlowItemStatus#DENIED} status.<br>
 * When all requests of a group are done (successfully or with errors), a {@link FileRequestsGroupEvent} is published
 * with {@link FlowItemStatus#DONE} or with {@link FlowItemStatus#ERROR} status.<br>
 *
 * @author Sébastien Binda
 */
@Component
public class RequestsGroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestsGroupService.class);

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IGroupRequestInfoRepository groupReqInfoRepository;

    @Autowired
    private IFileStorageRequestRepository storageReqRepository;

    @Autowired
    private IFileCacheRequestRepository cacheReqRepository;

    @Autowired
    private IFileCopyRequestRepository copyReqRepository;

    @Autowired
    private IFileDeletetionRequestRepository delReqRepository;

    /**
     * Handle new request success for the given groupId.<br>
     * Saves requests success results in db.<br>
     * If all requests are terminated for the given groupId, then a message is sent.<br>
     * NOTE : Transactional new : Ensure new transaction is created and all commits are handle to calculate number of requests pending.
     * @param groupId
     * @param type
     * @param checksum
     * @param storage
     * @param fileRef
     */
    public void requestSuccess(String groupId, FileRequestType type, String checksum, String storage,
            FileReference fileRef) {
        requestSuccess(groupId, type, checksum, storage, fileRef, true);
    }

    /**
     * Handle new request success for the given groupId.<br>
     * Saves requests success results in db.<br>
     * Set checkGroupDone to true to check  if all requests are terminated for the given groupId, then a message is sent.<br>
     * NOTE : Transactional new : Ensure new transaction is created and all commits are handle to calculate number of requests pending.
     * @param groupId
     * @param type
     * @param checksum
     * @param storage
     * @param fileRef
     * @param checkGroupDone
     */
    public void requestSuccess(String groupId, FileRequestType type, String checksum, String storage,
            FileReference fileRef, boolean checkGroupDone) {
        requestDone(groupId, type, checksum, storage, fileRef, false, null, checkGroupDone);
    }

    /**
     * Handle new request success for the given groupId.<br>
     * Set checkGroupDone to true to check if all requests are terminated for the given groupId and then a message is sent.<br>
     * NOTE : Transactional new : Ensure new transaction is created and all commits are handle to calculate number of requests pending.
     *
     * @param groupId
     * @param type
     * @param checksum
     * @param storage
     * @param errorCause
     * @param checkGroupDone
     */
    public void requestError(String groupId, FileRequestType type, String checksum, String storage, String errorCause,
            boolean checkGroupDone) {
        requestDone(groupId, type, checksum, storage, null, true, errorCause, checkGroupDone);
    }

    /**
     * Handle new request success for the given groupId.<br>
     * Check if all requests are terminated for the given groupId and then a message is sent.<br>
     * NOTE : Transactional new : Ensure new transaction is created and all commits are handle to calculate number of requests pending.
     *
     * @param groupId
     * @param type
     * @param checksum
     * @param storage
     * @param errorCause
     */
    public void requestError(String groupId, FileRequestType type, String checksum, String storage, String errorCause) {
        requestError(groupId, type, checksum, storage, errorCause, true);
    }

    /**
     * Check if all requests are terminated for the given groupId.
     *
     * NOTE : Transactional new : Ensure new transaction is created and all commits are handle to calculate number of requests pending.
     * @param groupId
     * @param type
     */
    public void checkRequestsGroupDone(String groupId, FileRequestType type) {
        boolean isDone = false;
        // Check if there is remaining request not finished
        switch (type) {
            case AVAILABILITY:
                isDone = !cacheReqRepository.existsByGroupIdAndStatusNot(groupId, FileRequestStatus.ERROR);
                break;
            case COPY:
                isDone = !copyReqRepository.existsByGroupIdAndStatusNot(groupId, FileRequestStatus.ERROR);
                break;
            case DELETION:
                isDone = !delReqRepository.existsByGroupIdAndStatusNot(groupId, FileRequestStatus.ERROR);
                break;
            case STORAGE:
                isDone = !storageReqRepository.existsByGroupIdsAndStatusNot(groupId, FileRequestStatus.ERROR);
                break;
            case REFERENCE:
                LOGGER.warn("There is no requests for type REFERENCE. It is impossible to automaticly determine if group requests is done.");
                break;
            default:
                break;
        }
        // IF finished send a terminated group request event
        if (isDone) {
            done(groupId, type);
        }
    }

    /**
     * Send a bus message to inform that the given groupId is denied.
     * @param groupId
     * @param type
     * @param denyCause
     */
    public void denied(String groupId, FileRequestType type, String denyCause) {
        LOGGER.error("[{} GROUP DENIED {}] - Group request denied. Cause : {}", type.toString().toUpperCase(), groupId,
                     denyCause);
        publisher.publish(FileRequestsGroupEvent.build(groupId, type, FlowItemStatus.DENIED, Sets.newHashSet())
                .withMessage(denyCause));
    }

    /**
     * Send a bus message to inform that the given groupId is granted.
     * @param groupId
     * @param type
     * @param nbRequestInGroup
     */
    public void granted(String groupId, FileRequestType type, int nbRequestInGroup) {
        LOGGER.debug("[{} GROUP GRANTED {}] - Group request granted with {} requests.", type.toString().toUpperCase(),
                     groupId, nbRequestInGroup);
        publisher.publish(FileRequestsGroupEvent.build(groupId, type, FlowItemStatus.GRANTED, Sets.newHashSet()));
    }

    /**
     * Handle a group request done. All requests of the given group has terminated (success or error).
     * @param groupId
     * @param type
     */
    public void done(String groupId, FileRequestType type) {
        // 1. Get errors
        Set<RequestResultInfo> errors = groupReqInfoRepository.findByGroupIdAndError(groupId, true);
        // 2. Get success
        Set<RequestResultInfo> successes = groupReqInfoRepository.findByGroupIdAndError(groupId, false);
        // 3. Publish event
        if (errors.isEmpty()) {
            LOGGER.debug("[{} GROUP SUCCESS {}] - {} requests success.", type.toString().toUpperCase(), groupId,
                         successes.size());
            publisher.publish(FileRequestsGroupEvent.build(groupId, type, FlowItemStatus.SUCCESS, successes));
        } else {
            RequestResultInfo error = errors.stream().findFirst().get();
            String firstError = String.format("Location %s - Error : %s", error.getRequestStorage(),
                                              error.getErrorCause());
            LOGGER.error("[{} GROUP ERROR {}] - {} success / {} errors. First error : {}.",
                         type.toString().toUpperCase(), groupId, successes.size(), errors.size(), firstError);
            publisher.publish(FileRequestsGroupEvent.buildError(groupId, type, successes, errors));
        }
        // 4. Clear group info
        groupReqInfoRepository.deleteByGroupId(groupId);
    }

    /**
     * Handle result of a requests terminated.
     * @param groupId
     * @param type
     * @param checksum
     * @param storage
     * @param fileRef
     * @param error
     * @param errorCause
     * @param checkGroupDone
     */
    private void requestDone(String groupId, FileRequestType type, String checksum, String storage,
            FileReference fileRef, boolean error, String errorCause, boolean checkGroupDone) {
        // 1. Add info in database
        RequestResultInfo gInfo = new RequestResultInfo(groupId, type, checksum, storage);
        gInfo.setResultFile(fileRef);
        gInfo.setError(error);
        gInfo.setErrorCause(errorCause);
        groupReqInfoRepository.save(gInfo);
        if (checkGroupDone) {
            checkRequestsGroupDone(groupId, type);
        }
    }

}
