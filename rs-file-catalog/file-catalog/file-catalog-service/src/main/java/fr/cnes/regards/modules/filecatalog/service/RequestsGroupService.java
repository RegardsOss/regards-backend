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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.fileaccess.dto.StorageRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.request.FileGroupRequestStatus;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileRequestsGroupEvent;
import fr.cnes.regards.modules.filecatalog.dao.IFileStorageRequestAggregationRepository;
import fr.cnes.regards.modules.filecatalog.dao.IRequestGroupRepository;
import fr.cnes.regards.modules.filecatalog.dao.RequestResultInfoRepository;
import fr.cnes.regards.modules.filecatalog.domain.FileReference;
import fr.cnes.regards.modules.filecatalog.domain.RequestResultInfo;
import fr.cnes.regards.modules.filecatalog.domain.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.filecatalog.domain.request.RequestGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    private RequestResultInfoRepository groupReqInfoRepository;

    private final IPublisher publisher;

    private final IRequestGroupRepository reqGroupRepository;

    private final IFileStorageRequestAggregationRepository storageRequestRepository;

    private final FileReferenceEventPublisher fileReferenceEventPublisher;

    public RequestsGroupService(IPublisher publisher,
                                RequestResultInfoRepository groupReqInfoRepository,
                                IRequestGroupRepository reqGroupRepository,
                                IFileStorageRequestAggregationRepository storageRequestRepository,
                                FileReferenceEventPublisher fileReferenceEventPublisher) {
        this.publisher = publisher;
        this.groupReqInfoRepository = groupReqInfoRepository;
        this.reqGroupRepository = reqGroupRepository;
        this.storageRequestRepository = storageRequestRepository;
        this.fileReferenceEventPublisher = fileReferenceEventPublisher;
    }

    public void deleteRequestInfoForFile(Long fileId) {
        groupReqInfoRepository.deleteByResultFileId(fileId);
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
                        OffsetDateTime expirationDate) throws ModuleException {

        long start = System.currentTimeMillis();
        // Create new group request
        if (!reqGroupRepository.existsById(groupId)) {
            reqGroupRepository.save(RequestGroup.build(groupId, type, expirationDate));
        } else {
            throw new ModuleException(String.format("Identifier %s already exists", groupId));
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
    public void granted(String groupId, FileRequestType type, int nbRequestInGroup, OffsetDateTime expirationDate)
        throws ModuleException {
        granted(groupId, type, nbRequestInGroup, false, expirationDate);
    }

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

    /**
     * Check for all current request groups if all requests are terminated. If so send a SUCCESS or ERROR event on the bus message.
     */
    public void checkRequestsDoneGroups() {
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
        List<RequestGroup> groupsDone = reqGroupRepository.findDoneGroups(maxRequestPerTransaction);
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

    private void groupExpired(RequestGroup requestGroup) {
        LOGGER.warn(
            "[REQUEST GROUP {} EXPIRED] . Group {} is expired, it will be deleted and all associated requests will be set in ERROR status",
            requestGroup.getType(),
            requestGroup.getId());
        String errorCause = "Associated request group expired.";
        // If a request group is pending from more than 2 days, delete the group and set all requests in pending to error.
        switch (requestGroup.getType()) {
            case AVAILABILITY:
                //TODO FIXME LOT 2
                break;
            case DELETION:
                //TODO FIXME LOT 4
                break;
            case STORAGE:
            case REFERENCE:
                Set<FileStorageRequestAggregation> requests = storageRequestRepository.findAllByGroupIdsContaining(
                    requestGroup.getId());
                for (FileStorageRequestAggregation request : requests) {
                    request.setStatus(StorageRequestStatus.ERROR);
                    request.setErrorCause(errorCause);
                    fileReferenceEventPublisher.storeError(request.getMetaInfo().getChecksum(),
                                                           List.of(request.getOwner()),
                                                           request.getStorage(),
                                                           errorCause,
                                                           requestGroup.getId());
                }
                storageRequestRepository.saveAll(requests);
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
}
