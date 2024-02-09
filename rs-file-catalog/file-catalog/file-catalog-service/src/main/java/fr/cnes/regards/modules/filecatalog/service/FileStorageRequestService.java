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

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent;
import fr.cnes.regards.modules.filecatalog.dao.IFileStorageRequestAggregationRepository;
import fr.cnes.regards.modules.filecatalog.domain.FileReferenceMetaInfo;
import fr.cnes.regards.modules.filecatalog.domain.request.FileStorageRequestAggregation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Thibaud Michaudel
 **/
@Service
@MultitenantTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class FileStorageRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageRequestService.class);

    private final RequestStatusService reqStatusService;

    private final SessionNotifier sessionNotifier;

    private final IFileStorageRequestAggregationRepository fileStorageRequestAggregationRepository;

    public FileStorageRequestService(RequestStatusService reqStatusService,
                                     SessionNotifier sessionNotifier,
                                     IFileStorageRequestAggregationRepository fileStorageRequestAggregationRepository) {
        this.reqStatusService = reqStatusService;
        this.sessionNotifier = sessionNotifier;
        this.fileStorageRequestAggregationRepository = fileStorageRequestAggregationRepository;
    }

    public void createStorageRequests(List<FilesStorageRequestEvent> messages) {
        for (FilesStorageRequestEvent message : messages) {
            for (FileStorageRequestDto file : message.getFiles()) {
                createNewFileStorageRequest(List.of(file.getOwner()),
                                            FileReferenceMetaInfo.buildFromDto(file.getMetaInfo()),
                                            file.getOriginUrl(),
                                            file.getStorage(),
                                            Optional.of(file.getSubDirectory()),
                                            message.getGroupId(),
                                            Optional.empty(),
                                            Optional.of(FileRequestStatus.TO_DO),
                                            file.getSessionOwner(),
                                            file.getSession());
            }
        }
    }

    /**
     * Create a new {@link FileStorageRequestAggregation}
     *
     * @param owners              owners of the file to store
     * @param fileMetaInfo        meta information of the file to store
     * @param originUrl           file origin location
     * @param storage             storage destination location
     * @param storageSubDirectory Optional sub-directory where to store file in the storage destination location
     * @param groupId             Business identifier of the deletion request
     * @param status              storage request status to be set during creation
     * @param sessionOwner        session owner to which belongs created storage request
     * @param session             session to which belongs created storage request
     */
    public FileStorageRequestAggregation createNewFileStorageRequest(Collection<String> owners,
                                                                     FileReferenceMetaInfo fileMetaInfo,
                                                                     String originUrl,
                                                                     String storage,
                                                                     Optional<String> storageSubDirectory,
                                                                     String groupId,
                                                                     Optional<String> errorCause,
                                                                     Optional<FileRequestStatus> status,
                                                                     String sessionOwner,
                                                                     String session) {
        long start = System.currentTimeMillis();
        FileStorageRequestAggregation fileStorageRequest = new FileStorageRequestAggregation(owners,
                                                                                             fileMetaInfo,
                                                                                             originUrl,
                                                                                             storage,
                                                                                             storageSubDirectory,
                                                                                             groupId,
                                                                                             sessionOwner,
                                                                                             session);
        fileStorageRequest.setStatus(reqStatusService.getNewStatus(fileStorageRequest, status));
        fileStorageRequest.setErrorCause(errorCause.orElse(null));
        // notify request is running to the session agent
        this.sessionNotifier.incrementRunningRequests(fileStorageRequest.getSessionOwner(),
                                                      fileStorageRequest.getSession());
        fileStorageRequestAggregationRepository.save(fileStorageRequest);

        LOGGER.trace(
            "[STORAGE REQUESTS] New file storage request created for file <{}> to store to {} with status {} in {}ms",
            fileStorageRequest.getMetaInfo().getFileName(),
            fileStorageRequest.getStorage(),
            fileStorageRequest.getStatus(),
            System.currentTimeMillis() - start);
        return fileStorageRequest;
    }
}
