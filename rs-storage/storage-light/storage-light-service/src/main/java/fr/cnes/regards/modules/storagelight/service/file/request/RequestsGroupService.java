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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.storagelight.dao.IFileCacheRequestRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileCopyRequestRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileDeletetionRequestRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storagelight.dao.IGroupRequestInfoRepository;
import fr.cnes.regards.modules.storagelight.dao.IRequestGroupRepository;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.RequestGroup;
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
@Service
@MultitenantTransactional
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

    @Autowired
    private IRequestGroupRepository reqGroupRepository;

    /**
     * Handle new request success for the given groupId.<br>
     * Saves requests success results in db.<br>
     * @param groupId
     * @param type
     * @param checksum
     * @param storage
     * @param fileRef
     */
    public void requestSuccess(String groupId, FileRequestType type, String checksum, String storage,
            FileReference fileRef) {
        requestDone(groupId, type, checksum, storage, fileRef, false, null);
    }

    /**
     * Handle new request error for the given groupId.<br>
     *
     * @param groupId
     * @param type
     * @param checksum
     * @param storage
     * @param errorCause
     * @param checkGroupDone
     */
    public void requestError(String groupId, FileRequestType type, String checksum, String storage, String errorCause) {
        requestDone(groupId, type, checksum, storage, null, true, errorCause);
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
        // Create new group request
        if (!reqGroupRepository.existsById(groupId)) {
            reqGroupRepository.save(RequestGroup.build(groupId, type));
        } else {
            LOGGER.error("Group request identifier already exists");
        }
        publisher.publish(FileRequestsGroupEvent.build(groupId, type, FlowItemStatus.GRANTED, Sets.newHashSet()));
    }

    /**
    *
    */
    public void checkRequestsGroupsDone() {
        PageRequest page = PageRequest.of(0, 500);
        Page<RequestGroup> response = reqGroupRepository.findAll(page);
        response.getContent().stream().forEach(this::checkRequestsGroupDone);
    }

    /**
     * Check if all requests are terminated for the given groupId.
     *
     * @param groupId
     * @param type
     */
    private void checkRequestsGroupDone(RequestGroup reqGrp) {
        boolean isDone = false;
        // Check if there is remaining request not finished
        switch (reqGrp.getType()) {
            case AVAILABILITY:
                isDone = !cacheReqRepository.existsByGroupIdAndStatusNot(reqGrp.getId(), FileRequestStatus.ERROR);
                break;
            case COPY:
                isDone = !copyReqRepository.existsByGroupIdAndStatusNot(reqGrp.getId(), FileRequestStatus.ERROR);
                break;
            case DELETION:
                isDone = !delReqRepository.existsByGroupIdAndStatusNot(reqGrp.getId(), FileRequestStatus.ERROR);
                break;
            case STORAGE:
                isDone = !storageReqRepository.existsByGroupIdsAndStatusNot(reqGrp.getId(), FileRequestStatus.ERROR);
                break;
            case REFERENCE:
                // There is no asynchronous request for reference. If the request is referenced in db, so all requests have been handled
                isDone = true;
                break;
            default:
                break;
        }
        // IF finished send a terminated group request event
        if (isDone) {
            groupDone(reqGrp);
        }
    }

    /**
     * Handle a group request done. All requests of the given group has terminated (success or error).
     * @param groupId
     * @param type
     */
    private void groupDone(RequestGroup reqGrp) {
        // 1. Get errors
        Set<RequestResultInfo> errors = groupReqInfoRepository.findByGroupIdAndError(reqGrp.getId(), true);
        // 2. Get success
        Set<RequestResultInfo> successes = groupReqInfoRepository.findByGroupIdAndError(reqGrp.getId(), false);
        // 3. Publish event
        if (errors.isEmpty()) {
            LOGGER.info("[{} GROUP SUCCESS {}] - {} requests success.", reqGrp.getType().toString().toUpperCase(),
                        reqGrp.getId(), successes.size());
            publisher.publish(FileRequestsGroupEvent.build(reqGrp.getId(), reqGrp.getType(), FlowItemStatus.SUCCESS,
                                                           successes));
        } else {
            LOGGER.info("[{} GROUP ERROR {}] - {} success / {} errors.", reqGrp.getType().toString().toUpperCase(),
                        reqGrp.getId(), successes.size(), errors.size());
            publisher.publish(FileRequestsGroupEvent.buildError(reqGrp.getId(), reqGrp.getType(), successes, errors));

        }
        // 4. Clear group info
        LOGGER.info("DELETING group {}", reqGrp.getId());
        groupReqInfoRepository.deleteByGroupId(reqGrp.getId());
        reqGroupRepository.delete(reqGrp);
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
     */
    private void requestDone(String groupId, FileRequestType type, String checksum, String storage,
            FileReference fileRef, boolean error, String errorCause) {
        // 1. Add info in database
        RequestResultInfo gInfo = new RequestResultInfo(groupId, type, checksum, storage);
        gInfo.setResultFile(fileRef);
        gInfo.setError(error);
        gInfo.setErrorCause(errorCause);
        groupReqInfoRepository.save(gInfo);
    }
}
