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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.storage.dao.IFileCacheRequestRepository;
import fr.cnes.regards.modules.storage.dao.IFileCopyRequestRepository;
import fr.cnes.regards.modules.storage.dao.IFileDeletetionRequestRepository;
import fr.cnes.regards.modules.storage.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storage.dao.IGroupRequestInfoRepository;
import fr.cnes.regards.modules.storage.dao.IRequestGroupRepository;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.database.request.RequestGroup;
import fr.cnes.regards.modules.storage.domain.database.request.RequestResultInfo;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;
import fr.cnes.regards.modules.storage.domain.event.FileRequestsGroupEvent;
import fr.cnes.regards.modules.storage.domain.flow.FlowItemStatus;

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

    /**
     * Maximum number of request group to handle in one transaction. This is limited to avoid issue one too much
     * amqp message to send at a time.
     */
    private static final int MAX_REQUEST_PER_TRANSACTION = 100;

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

    @Value("${regards.storage.requests.days.before.expiration:2}")
    private Integer nbDaysBeforeExpiration;

    /**
     * Handle new request success for the given groupId.<br>
     *
     * @param groupId
     * @param type
     * @param checksum
     * @param storage
     * @param storePath
     * @param fileRef
     */
    public void requestSuccess(String groupId, FileRequestType type, String checksum, String storage, String storePath,
            Collection<String> owners, FileReference fileRef) {
        requestDone(groupId, type, checksum, storage, storePath, owners, fileRef, false, null);
    }

    /**
     * Handle new request error for the given groupId.<br>
     *
     * @param groupId
     * @param type
     * @param checksum
     * @param storage
     * @param storePath
     * @param owners
     * @param errorCause
     * @param checkGroupDone
     */
    public void requestError(String groupId, FileRequestType type, String checksum, String storage, String storePath,
            Set<String> owners, String errorCause) {
        requestDone(groupId, type, checksum, storage, storePath, owners, null, true, errorCause);
    }

    /**
     * Send a bus message to inform that the given groupId is denied.
     *
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
     * Save new granted request group and send a bus message to inform that the given groupId is granted.
     *
     * @param groupId
     * @param type
     * @param nbRequestInGroup
     * @param silent True to avoid sending bus message about group granted. Used internally in storage microservice.
     */
    public void granted(String groupId, FileRequestType type, int nbRequestInGroup, boolean silent) {
        LOGGER.debug("[{} GROUP GRANTED {}] - Group request granted with {} requests.", type.toString().toUpperCase(),
                     groupId, nbRequestInGroup);
        // Create new group request
        if (!reqGroupRepository.existsById(groupId)) {
            reqGroupRepository.save(RequestGroup.build(groupId, type));
        } else {
            LOGGER.error("Group request identifier already exists");
        }
        if (!silent) {
            publisher.publish(FileRequestsGroupEvent.build(groupId, type, FlowItemStatus.GRANTED, Sets.newHashSet()));
        }
    }

    /**
     * Save new granted request group and send a bus message to inform that the given groupId is granted.
     *
     * @param groupId
     * @param type
     * @param nbRequestInGroup
     */
    public void granted(String groupId, FileRequestType type, int nbRequestInGroup) {
        granted(groupId, type, nbRequestInGroup, false);
    }

    /**
     * Save new granted request groups and send bus messages to inform that the given groupIds are granted.
     *
     * @param groupId
     * @param type
     */
    public void granted(Set<String> groupIds, FileRequestType type) {
        // Create new group request
        List<RequestGroup> existings = reqGroupRepository.findAllById(groupIds);
        List<String> existingGrpIds = existings.stream().map(RequestGroup::getId).collect(Collectors.toList());
        Set<RequestGroup> toSave = Sets.newHashSet();
        for (String groupId : groupIds) {
            if (!existingGrpIds.contains(groupId)) {
                toSave.add(RequestGroup.build(groupId, type));
                publisher.publish(FileRequestsGroupEvent.build(groupId, type, FlowItemStatus.GRANTED,
                                                               Sets.newHashSet()));
            } else {
                LOGGER.error("Group request identifier already exists");
            }
        }
        reqGroupRepository.saveAll(toSave);
    }

    /**
    * Check for all current request groups if all requests are terminated. If so send a SUCCESS or ERROR event on the bus message.
    */
    public void checkRequestsGroupsDone() {
        long start = System.currentTimeMillis();
        LOGGER.info("[REQUEST GROUPS] Start checking request groups ... ");
        Page<RequestGroup> response = reqGroupRepository.findAll(PageRequest.of(0, 500));
        LOGGER.info("[REQUEST GROUPS] {} request groups found", response.getTotalElements());
        long totalChecked = response.getTotalElements();
        int nbGroupsDone = 0;
        if (totalChecked > 0) {
            do {
                Iterator<RequestGroup> it = response.getContent().iterator();
                do {
                    nbGroupsDone += checkRequestsGroupDone(it.next()) ? 1 : 0;
                } while (it.hasNext());
                response = reqGroupRepository.findAll(response.getPageable().next());
            } while (response.hasNext() && (nbGroupsDone < MAX_REQUEST_PER_TRANSACTION));
        }
        LOGGER.info("[REQUEST GROUPS] Checking request groups done in {}ms. Terminated groups {}/{}",
                    System.currentTimeMillis() - start, nbGroupsDone, totalChecked);
    }

    /**
     * Check if all requests are terminated for the given groupId.
     *
     * @param groupId
     * @param type
     */
    private boolean checkRequestsGroupDone(RequestGroup reqGrp) {
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
            return true;
        } else {
            return checkRequestGroupExpired(reqGrp);
        }
    }

    /**
     * Check if the given requests group has expired (too old) and can be deleted.
     * @param reqGrp to check for
     */
    private boolean checkRequestGroupExpired(RequestGroup reqGrp) {
        boolean expired = false;
        if ((nbDaysBeforeExpiration > 0)
                && reqGrp.getCreationDate().isBefore(OffsetDateTime.now().minusDays(nbDaysBeforeExpiration))) {
            LOGGER.warn("Request group {} is expired. It will be deleted and all associated requests will be set in ERROR status");
            String errorCause = "Associated group request expired.";
            // If a request group is pending from more than 2 days, delete the group and set all requests in pending to error.
            switch (reqGrp.getType()) {
                case AVAILABILITY:
                    cacheReqRepository.findByGroupId(reqGrp.getId()).forEach(req -> cacheReqRepository
                            .updateError(FileRequestStatus.ERROR, errorCause, req.getId()));
                    break;
                case COPY:
                    copyReqRepository.findByGroupId(reqGrp.getId()).forEach(req -> copyReqRepository
                            .updateError(FileRequestStatus.ERROR, errorCause, req.getId()));
                    break;
                case DELETION:
                    delReqRepository.findByGroupId(reqGrp.getId()).forEach(req -> delReqRepository
                            .updateError(FileRequestStatus.ERROR, errorCause, req.getId()));
                    break;
                case STORAGE:
                    storageReqRepository.findByGroupIds(reqGrp.getId()).forEach(req -> storageReqRepository
                            .updateError(FileRequestStatus.ERROR, errorCause, req.getId()));
                    break;
                case REFERENCE:
                    // There is no asynchronous request for reference. If the request is referenced in db, so all requests have been handled
                    break;
                default:
                    break;
            }
            // Clear group info
            groupReqInfoRepository.deleteByGroupId(reqGrp.getId());
            reqGroupRepository.delete(reqGrp);
            expired = true;
        }
        return expired;
    }

    /**
     * Handle a group request done. All requests of the given group has terminated (success or error).
     * @param groupId
     * @param type
     */
    public void groupDone(RequestGroup reqGrp) {
        // 1. Do only one database request, then dispatch results between success and errors
        Set<RequestResultInfo> resultInfos = groupReqInfoRepository.findByGroupId(reqGrp.getId());
        Set<RequestResultInfo> errors = Sets.newHashSet();
        Set<RequestResultInfo> successes = Sets.newHashSet();
        for (RequestResultInfo info : resultInfos) {
            if (info.isError()) {
                errors.add(info);
            } else {
                successes.add(info);
            }
        }
        // 2. Publish events
        if (errors.isEmpty()) {
            LOGGER.debug("[{} GROUP SUCCESS {}] - {} requests success.", reqGrp.getType().toString().toUpperCase(),
                         reqGrp.getId(), successes.size());
            publisher.publish(FileRequestsGroupEvent.build(reqGrp.getId(), reqGrp.getType(), FlowItemStatus.SUCCESS,
                                                           successes));
            if (successes.isEmpty()) {
                LOGGER.error("[{} GROUP SUCCESS {}] No success requests associated to terminated group {}",
                             reqGrp.getType(), reqGrp.getId());
            }
        } else {
            LOGGER.error("[{} GROUP ERROR {}] - {} success / {} errors.", reqGrp.getType().toString().toUpperCase(),
                         reqGrp.getId(), successes.size(), errors.size());
            publisher.publish(FileRequestsGroupEvent.buildError(reqGrp.getId(), reqGrp.getType(), successes, errors));
        }
        // 3. Clear
        groupReqInfoRepository.deleteByGroupId(reqGrp.getId());
        reqGroupRepository.delete(reqGrp);
        resultInfos.clear();
    }

    /**
     * Handle result of a requests terminated.
     * @param groupId
     * @param type
     * @param checksum
     * @param storage
     * @param storePath
     * @param owners
     * @param fileRef
     * @param error
     * @param errorCause
     */
    private void requestDone(String groupId, FileRequestType type, String checksum, String storage, String storePath,
            Collection<String> owners, FileReference fileRef, boolean error, String errorCause) {
        // Check if associated group exists
        if (reqGroupRepository.existsById(groupId)) {
            RequestResultInfo gInfo = new RequestResultInfo(groupId, type, checksum, storage, storePath, owners);
            gInfo.setResultFile(fileRef);
            gInfo.setError(error);
            gInfo.setErrorCause(errorCause);
            groupReqInfoRepository.save(gInfo);
        } else {
            LOGGER.warn("A {} request is terminated with error={} for the request group {} but this group does not exists anymore !",
                        type.toString(), Boolean.valueOf(error).toString(), groupId);
        }
    }
}
