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
import fr.cnes.regards.modules.fileaccess.dto.request.FileGroupRequestStatus;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileRequestsGroupEvent;
import fr.cnes.regards.modules.filecatalog.dao.IGroupRequestInfoRepository;
import fr.cnes.regards.modules.filecatalog.dao.IRequestGroupRepository;
import fr.cnes.regards.modules.filecatalog.domain.FileReference;
import fr.cnes.regards.modules.filecatalog.domain.RequestResultInfo;
import fr.cnes.regards.modules.filecatalog.domain.request.RequestGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collection;

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

    private IGroupRequestInfoRepository groupReqInfoRepository;

    private final IPublisher publisher;

    private final IRequestGroupRepository reqGroupRepository;

    public RequestsGroupService(IPublisher publisher,
                                IGroupRequestInfoRepository groupReqInfoRepository,
                                IRequestGroupRepository reqGroupRepository) {
        this.publisher = publisher;
        this.groupReqInfoRepository = groupReqInfoRepository;
        this.reqGroupRepository = reqGroupRepository;
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
}
