/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileRequestsGroupEvent;
import fr.cnes.regards.modules.filecatalog.dto.FileRequestStatus;
import fr.cnes.regards.modules.filecatalog.dto.FileRequestType;
import fr.cnes.regards.modules.filecatalog.dto.request.FileGroupRequestStatus;
import fr.cnes.regards.modules.storage.dao.*;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.*;
import fr.cnes.regards.modules.storage.service.file.FileReferenceEventPublisher;
import fr.cnes.regards.modules.storage.service.session.SessionNotifier;
import org.apache.commons.compress.utils.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to handle actions on requests group.<br>
 * A requests group is an business association between many FileRequests of the same type.<br>
 * All requests of a same groups are associated thanks to a group identifier.<br>
 * When all requests of a group has been handled by the associated service, then a {@link FileRequestsGroupEvent} is published
 * with {@link FileGroupRequestStatus#GRANTED} status.<br>
 * When all requests of a group has been rejected by the associated service, then a {@link FileRequestsGroupEvent} is published
 * with {@link FileGroupRequestStatus#DENIED} status.<br>
 * When all requests of a group are done (successfully or with errors), a {@link FileRequestsGroupEvent} is published
 * with {@link FileGroupRequestStatus#SUCCESS} or with {@link FileGroupRequestStatus#ERROR} status.<br>
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
    @Value("${regards.storage.groups.requests.bulk:500}")
    private final Integer maxRequestPerTransaction = 500;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private FileReferenceEventPublisher eventPublisher;

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

    @Autowired
    private SessionNotifier sessionNotifier;

    /**
     * Handle new request success for the given groupId.<br>
     */
    public void requestSuccess(String groupId,
                               FileRequestType type,
                               String checksum,
                               String storage,
                               String storePath,
                               Collection<String> owners,
                               FileReference fileRef) {
        requestDone(groupId, type, checksum, storage, storePath, owners, fileRef, false, null);
    }

    public void availibilityRequestSuccess(String groupId, String checksum, Collection<String> owners) {
        requestSuccess(groupId, FileRequestType.AVAILABILITY, checksum, null, null, owners, null);
    }

    /**
     * Handle new request error for the given groupId.<br>
     */
    public void requestError(String groupId,
                             FileRequestType type,
                             String checksum,
                             String storage,
                             String storePath,
                             Collection<String> owners,
                             String errorCause) {
        requestDone(groupId, type, checksum, storage, storePath, owners, null, true, errorCause);
    }

    /**
     * Send a bus message to inform that the given groupId is denied.
     */
    public void denied(String groupId, FileRequestType type, String denyCause) {
        LOGGER.error("[{} GROUP DENIED {}] - Group request denied. Cause : {}",
                     type.toString().toUpperCase(),
                     groupId,
                     denyCause);
        publisher.publish(FileRequestsGroupEvent.build(groupId, type, FileGroupRequestStatus.DENIED, Sets.newHashSet())
                                                .withMessage(denyCause));
    }

    /**
     * Save new granted request group and send a bus message to inform that the given groupId is granted.
     *
     * @param silent True to avoid sending bus message about group granted. Used internally in storage microservice.
     */
    public void granted(String groupId,
                        FileRequestType type,
                        int nbRequestInGroup,
                        boolean silent,
                        OffsetDateTime expirationDate) {

        long start = System.currentTimeMillis();
        // Create new group request
        if (!reqGroupRepository.existsById(groupId)) {
            reqGroupRepository.save(RequestGroup.build(groupId, type, expirationDate));
        } else {
            LOGGER.error("[{} Group request] Identifier {} already exists", type.toString(), groupId);
        }
        if (!silent) {
            publisher.publish(FileRequestsGroupEvent.build(groupId,
                                                           type,
                                                           FileGroupRequestStatus.GRANTED,
                                                           Sets.newHashSet()));
        }
        LOGGER.trace("[{} GROUP GRANTED {}] - Group request granted with {} requests. ({}ms)",
                     type.toString().toUpperCase(),
                     groupId,
                     nbRequestInGroup,
                     System.currentTimeMillis() - start);
    }

    /**
     * Save new granted request group and send a bus message to inform that the given groupId is granted.
     */
    public void granted(String groupId, FileRequestType type, int nbRequestInGroup, OffsetDateTime expirationDate) {
        granted(groupId, type, nbRequestInGroup, false, expirationDate);
    }

    /**
     * Save new granted request groups and send bus messages to inform that the given groupIds are granted.
     */
    public void granted(Set<String> groupIds, FileRequestType type, OffsetDateTime expirationDate) {
        // Create new group request
        List<RequestGroup> existings = reqGroupRepository.findAllById(groupIds);
        List<String> existingGrpIds = existings.stream().map(RequestGroup::getId).collect(Collectors.toList());
        Set<RequestGroup> toSave = Sets.newHashSet();
        for (String groupId : groupIds) {
            if (!existingGrpIds.contains(groupId)) {
                toSave.add(RequestGroup.build(groupId, type, expirationDate));
                publisher.publish(FileRequestsGroupEvent.build(groupId,
                                                               type,
                                                               FileGroupRequestStatus.GRANTED,
                                                               Sets.newHashSet()));
            } else {
                LOGGER.error("Group request identifier already exists");
            }
        }
        reqGroupRepository.saveAll(toSave);
    }

    /**
     * Delete all requests associated to the given group ids if not running.
     *
     * @param group group identifier
     */
    public void cancelRequestGroup(String group) {
        // Cancel storage requests
        List<FileStorageRequestAggregation> storageRequests = storageReqRepository.findByGroupIdsAndStatusNotIn(group,
                                                                                                                FileRequestStatus.RUNNING_STATUS);
        storageRequests.forEach(r -> {
            sessionNotifier.decrementStoreRequests(r.getSessionOwner(), r.getSession());
            if (r.getStatus() == FileRequestStatus.ERROR) {
                sessionNotifier.decrementErrorRequests(r.getSessionOwner(), r.getSession());
            }
        });
        storageReqRepository.deleteAll(storageRequests);

        // Cancel deletion requests
        Set<FileDeletionRequest> delRequests = delReqRepository.findByGroupIdAndStatusNotIn(group,
                                                                                            FileRequestStatus.RUNNING_STATUS);
        delRequests.forEach(r -> {
            sessionNotifier.decrementDeleteRequests(r.getSessionOwner(), r.getSession());
            if (r.getStatus() == FileRequestStatus.ERROR) {
                sessionNotifier.decrementErrorRequests(r.getSessionOwner(), r.getSession());
            }
        });
        delReqRepository.deleteAll(delRequests);

        // Cancel copy requests
        Set<FileCopyRequest> copyRequests = copyReqRepository.findByGroupIdAndStatusNotIn(group,
                                                                                          FileRequestStatus.RUNNING_STATUS);
        copyRequests.forEach(r -> {
            sessionNotifier.decrementCopyRequests(r.getSessionOwner(), r.getSession());
            if (r.getStatus() == FileRequestStatus.ERROR) {
                sessionNotifier.decrementErrorRequests(r.getSessionOwner(), r.getSession());
            }
        });
        copyReqRepository.deleteAll(copyRequests);

        // Cancel cache requests
        cacheReqRepository.deleteByGroupIdAndStatusNotIn(group, FileRequestStatus.RUNNING_STATUS);

    }

    /**
     * Check for all current request groups if all requests are terminated. If so send a SUCCESS or ERROR event on the bus message.
     */
    public void checkRequestsGroupsDone() {
        LOGGER.debug("[REQUEST GROUPS] Start checking request groups expired ... ");
        long start = System.currentTimeMillis();
        // Handle expired groups
        Page<RequestGroup> expiredGroups = reqGroupRepository.findByExpirationDateLessThanEqual(OffsetDateTime.now(),
                                                                                                PageRequest.of(0,
                                                                                                               maxRequestPerTransaction));
        expiredGroups.forEach(this::groupExpired);
        long expiredGroupsCount = expiredGroups.getTotalElements();
        int expiredGroupsHandledCount = expiredGroups.getNumberOfElements();
        if (expiredGroupsCount > 0) {
            reqGroupRepository.deleteAllInBatch(expiredGroups);
            groupReqInfoRepository.deleteByGroupIdIn(expiredGroups.stream()
                                                                  .map(RequestGroup::getId)
                                                                  .collect(Collectors.toSet()));
            LOGGER.info("[REQUEST GROUPS] {}/{} expired groups done in {}ms ",
                        expiredGroupsHandledCount,
                        expiredGroupsCount,
                        System.currentTimeMillis() - start);
        }
        start = System.currentTimeMillis();
        LOGGER.debug("[REQUEST GROUPS] Start checking request groups done ... ");
        // Handle done groups
        List<RequestGroup> groupsDone = reqGroupRepository.findGroupDones(maxRequestPerTransaction);
        List<String> groupsDoneIds = groupsDone.stream().map(RequestGroup::getId).collect(Collectors.toList());
        Set<RequestResultInfo> requestsInfo = groupReqInfoRepository.findByGroupIdIn(groupsDoneIds);
        if (!groupsDone.isEmpty()) {
            for (RequestGroup group : groupsDone) {
                groupDone(group,
                          requestsInfo.stream()
                                      .filter(i -> i.getGroupId().equals(group.getId()))
                                      .collect(Collectors.toSet()));
            }
            groupReqInfoRepository.deleteByGroupIdIn(groupsDoneIds);
            reqGroupRepository.deleteAll(groupsDone);
            LOGGER.info("[REQUEST GROUPS] Checking request groups done in {}ms. Terminated groups {}.",
                        System.currentTimeMillis() - start,
                        groupsDone.size());
        } else {
            LOGGER.debug("[REQUEST GROUPS] Checking request groups done in {}ms. No groups done.",
                         System.currentTimeMillis() - start);
        }
    }

    private void groupExpired(RequestGroup reqGrp) {
        LOGGER.warn(
            "[REQUEST GROUP {} EXPIRED] . Group {} is expired, it will be deleted and all associated requests will be set in ERROR status",
            reqGrp.getType(),
            reqGrp.getId());
        String errorCause = "Associated group request expired.";
        // If a request group is pending from more than 2 days, delete the group and set all requests in pending to error.
        switch (reqGrp.getType()) {
            case AVAILABILITY:
                cacheReqRepository.findByGroupId(reqGrp.getId()).forEach(req -> {
                    cacheReqRepository.updateError(FileRequestStatus.ERROR, errorCause, req.getId());
                    eventPublisher.notAvailable(req.getChecksum(), null, errorCause, reqGrp.getId());
                });
                break;
            case COPY:
                copyReqRepository.findByGroupId(reqGrp.getId()).forEach(req -> {
                    copyReqRepository.updateError(FileRequestStatus.ERROR, errorCause, req.getId());
                    eventPublisher.copyError(req, errorCause);
                });
                break;
            case DELETION:
                delReqRepository.findByGroupId(reqGrp.getId()).forEach(req -> {
                    delReqRepository.updateError(FileRequestStatus.ERROR, errorCause, req.getId());
                    eventPublisher.deletionError(req.getFileReference(), errorCause, reqGrp.getId());
                });
                break;
            case STORAGE:
                storageReqRepository.findByGroupIds(reqGrp.getId()).forEach(req -> {
                    storageReqRepository.updateError(FileRequestStatus.ERROR, errorCause, req.getId());
                    eventPublisher.storeError(req.getMetaInfo().getChecksum(),
                                              req.getOwners(),
                                              req.getStorage(),
                                              errorCause,
                                              reqGrp.getId());
                });
                break;
            case REFERENCE:
                // There is no asynchronous request for reference. If the request is referenced in db, so all requests have been handled
                break;
            default:
                break;
        }
    }

    private void groupDone(RequestGroup reqGrp, Set<RequestResultInfo> infos) {
        groupDone(reqGrp, infos, Optional.empty());
    }

    /**
     * Handle a group request done. All requests of the given group has terminated (success or error).
     */
    private void groupDone(RequestGroup reqGrp,
                           Set<RequestResultInfo> resultInfos,
                           Optional<FileGroupRequestStatus> forcedStatus) {
        Set<RequestResultInfo> errors = Sets.newHashSet();
        Set<RequestResultInfo> successes = Sets.newHashSet();
        for (RequestResultInfo info : resultInfos) {
            if (info.isError()) {
                errors.add(info);
            } else {
                successes.add(info);
            }
        }
        // 1. Publish events
        if (errors.isEmpty()) {
            LOGGER.trace("[{} GROUP {} {}] - {} requests success.",
                         reqGrp.getType().toString().toUpperCase(),
                         forcedStatus.orElse(FileGroupRequestStatus.SUCCESS),
                         reqGrp.getId(),
                         successes.size());
            publisher.publish(FileRequestsGroupEvent.build(reqGrp.getId(),
                                                           reqGrp.getType(),
                                                           forcedStatus.orElse(FileGroupRequestStatus.SUCCESS),
                                                           successes.stream().map(RequestResultInfo::toDto).toList()));
            if (successes.isEmpty()) {
                LOGGER.debug("[{} GROUP {} {}] No success requests associated to terminated group",
                             forcedStatus.orElse(FileGroupRequestStatus.SUCCESS),
                             reqGrp.getType(),
                             reqGrp.getId());
            }
        } else {
            LOGGER.error("[{} GROUP ERROR {}] - {} success / {} errors.",
                         reqGrp.getType().toString().toUpperCase(),
                         reqGrp.getId(),
                         successes.size(),
                         errors.size());
            publisher.publish(FileRequestsGroupEvent.buildError(reqGrp.getId(),
                                                                reqGrp.getType(),
                                                                successes.stream()
                                                                         .map(RequestResultInfo::toDto)
                                                                         .toList(),
                                                                errors.stream()
                                                                      .map(RequestResultInfo::toDto)
                                                                      .toList()));
        }
    }

    public void deleteRequestInfoForFile(Long fileId) {
        groupReqInfoRepository.deleteByResultFileId(fileId);
    }

    /**
     * Handle result of a requests terminated.
     */
    private void requestDone(String groupId,
                             FileRequestType type,
                             String checksum,
                             String storage,
                             String storePath,
                             Collection<String> owners,
                             FileReference fileRef,
                             boolean error,
                             String errorCause) {
        RequestResultInfo gInfo = new RequestResultInfo(groupId, type, checksum, storage, storePath, owners);
        gInfo.setResultFile(fileRef);
        gInfo.setError(error);
        gInfo.setErrorCause(errorCause);

        groupReqInfoRepository.save(gInfo);
    }

    public void deleteRequestGroups(FileRequestType type) {
        Pageable page = PageRequest.of(0, 500, Direction.ASC, "id");
        Page<RequestGroup> groups = reqGroupRepository.findByType(type, page);
        Set<RequestResultInfo> infos = groupReqInfoRepository.findByGroupIdIn(groups.stream()
                                                                                    .map(g -> g.getId())
                                                                                    .collect(Collectors.toSet()));
        if (!groups.isEmpty()) {
            for (RequestGroup group : groups) {
                groupDone(group,
                          infos.stream().filter(i -> i.getGroupId().equals(group.getId())).collect(Collectors.toSet()),
                          Optional.of(FileGroupRequestStatus.ERROR));
            }
            groupReqInfoRepository.deleteByGroupIdIn(groups.stream()
                                                           .map(RequestGroup::getId)
                                                           .collect(Collectors.toSet()));
            reqGroupRepository.deleteAll(groups);
        }
    }
}
