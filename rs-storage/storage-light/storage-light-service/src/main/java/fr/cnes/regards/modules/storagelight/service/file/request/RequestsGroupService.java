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
import fr.cnes.regards.modules.storagelight.domain.database.request.group.GroupRequestsInfo;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestsGroupEvent;
import fr.cnes.regards.modules.storagelight.domain.flow.FlowItemStatus;

/**
 * @author sbinda
 *
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

    public void requestSuccess(String groupId, FileRequestType type, String checksum, String storage,
            FileReference fileRef) {
        requestSuccess(groupId, type, checksum, storage, fileRef, true);
    }

    public void requestSuccess(String groupId, FileRequestType type, String checksum, String storage,
            FileReference fileRef, boolean checkGroupDone) {
        requestDone(groupId, type, checksum, storage, fileRef, false, null, checkGroupDone);
    }

    public void requestError(String groupId, FileRequestType type, String checksum, String storage, String errorCause,
            boolean checkGroupDone) {
        requestDone(groupId, type, checksum, storage, null, true, errorCause, checkGroupDone);
    }

    public void requestError(String groupId, FileRequestType type, String checksum, String storage, String errorCause) {
        requestError(groupId, type, checksum, storage, errorCause, true);
    }

    private void requestDone(String groupId, FileRequestType type, String checksum, String storage,
            FileReference fileRef, boolean error, String errorCause, boolean checkGroupDone) {
        // 1. Add info in database
        GroupRequestsInfo gInfo = new GroupRequestsInfo(groupId, type, checksum, storage);
        gInfo.setFileReference(fileRef);
        gInfo.setError(error);
        gInfo.setErrorCause(errorCause);
        groupReqInfoRepository.save(gInfo);
        if (checkGroupDone) {
            checkRequestsGroupDone(groupId, type);
        }
    }

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
            default:
                break;
        }
        // IF finished send a terminated group request event
        if (isDone) {
            done(groupId, type);
        }
    }

    public void denied(String groupId, FileRequestType type, String denyCause) {
        LOGGER.error("[{} GROUP DENIED {}] - Group request denied. Cause : {}", type.toString().toUpperCase(), groupId,
                     denyCause);
        publisher.publish(FileRequestsGroupEvent.build(groupId, type, FlowItemStatus.DENIED, Sets.newHashSet())
                .withMessage(denyCause));
    }

    public void granted(String groupId, FileRequestType type, int nbRequestInGroup) {
        LOGGER.debug("[{} GROUP GRANTED {}] - Group request granted with {} requests.", type.toString().toUpperCase(),
                     groupId, nbRequestInGroup);
        publisher.publish(FileRequestsGroupEvent.build(groupId, type, FlowItemStatus.GRANTED, Sets.newHashSet()));
    }

    public void done(String groupId, FileRequestType type) {
        // 1. Get errors
        Set<GroupRequestsInfo> errors = groupReqInfoRepository.findByGroupIdAndError(groupId, true);
        // 2. Get success
        Set<GroupRequestsInfo> successes = groupReqInfoRepository.findByGroupIdAndError(groupId, false);
        // 3. Publish event
        if (errors.isEmpty()) {
            LOGGER.debug("[{} GROUP SUCCESS {}] - Request group done with {} success requests",
                         type.toString().toUpperCase(), groupId, successes.size());
            publisher.publish(FileRequestsGroupEvent.build(groupId, type, FlowItemStatus.SUCCESS, successes));
        } else {
            LOGGER.error("[{} GROUP ERROR {}] - Request group terminated in error with {} success requests and {} error requests",
                         type.toString().toUpperCase(), groupId, successes.size(), errors.size());
            publisher.publish(FileRequestsGroupEvent.buildError(groupId, type, successes, errors));
        }
        // 4. Clear group info
        groupReqInfoRepository.deleteByGroupId(groupId);
    }

}
