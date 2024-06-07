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
import fr.cnes.regards.modules.fileaccess.dto.StorageRequestStatus;
import fr.cnes.regards.modules.filecatalog.dao.IFileDeletionRequestRepository;
import fr.cnes.regards.modules.filecatalog.dao.IFileStorageRequestAggregationRepository;
import fr.cnes.regards.modules.filecatalog.domain.request.FileStorageRequestAggregation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * @author Thibaud Michaudel
 **/
@Service
public class RequestStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestStatusService.class);

    @Value("${regards.file.catalog.status.service.page.size:500}")
    private int pageSize;

    private final IFileDeletionRequestRepository deletionReqRepo;

    private final IFileStorageRequestAggregationRepository fileStorageRequestAggregationRepository;

    public RequestStatusService(IFileDeletionRequestRepository deletionReqRepo,
                                IFileStorageRequestAggregationRepository fileStorageRequestAggregationRepository) {
        this.deletionReqRepo = deletionReqRepo;
        this.fileStorageRequestAggregationRepository = fileStorageRequestAggregationRepository;
    }

    /**
     * Compute {@link StorageRequestStatus} for new {@link FileStorageRequestAggregation}
     *
     * @param request  request to compute status for
     * @param oDefault default status or empty
     * @return {@link StorageRequestStatus}
     */
    public StorageRequestStatus getNewStatus(FileStorageRequestAggregation request,
                                             Optional<StorageRequestStatus> oDefault) {
        StorageRequestStatus status = oDefault.orElse(StorageRequestStatus.TO_HANDLE);
        String storage = request.getStorage();
        String checksum = request.getMetaInfo().getChecksum();
        Set<StorageRequestStatus> toDelayStatusList = StorageRequestStatus.RUNNING_STATUS;
        if (request.getStatus() == StorageRequestStatus.TO_HANDLE) {
            toDelayStatusList = StorageRequestStatus.RUNNING_AND_DELAYED_STATUS;
        }
        // Delayed storage request if a deletion requests already exists or another storage request is already running for the same file to store
        if (deletionReqRepo.existsByStorageAndFileReferenceMetaInfoChecksumAndStatusIn(storage,
                                                                                       checksum,
                                                                                       StorageRequestStatus.RUNNING_STATUS)
            || fileStorageRequestAggregationRepository.existsByStorageAndMetaInfoChecksumAndStatusIn(storage,
                                                                                                     checksum,
                                                                                                     toDelayStatusList)) {
            status = StorageRequestStatus.DELAYED;
        } else if (request.getStatus() == StorageRequestStatus.DELAYED && status == StorageRequestStatus.TO_HANDLE) {
            LOGGER.info("Request {}/{} undelayed", request.getMetaInfo().getChecksum(), request.getStorage());
        }
        return status;
    }

    /**
     * Update delayed {@link FileStorageRequestAggregation}s that can be handled.
     */
    @MultitenantTransactional
    public void checkDelayedStorageRequests() {
        int nbUpdated = 0;
        for (FileStorageRequestAggregation delayedRequest : fileStorageRequestAggregationRepository.findByStatus(
            StorageRequestStatus.DELAYED,
            PageRequest.of(0, pageSize))) {
            // Check new status for the delayed request
            if (getNewStatus(delayedRequest, Optional.empty()) == StorageRequestStatus.TO_HANDLE) {
                delayedRequest.setStatus(StorageRequestStatus.TO_HANDLE);
                nbUpdated++;
            }
        }
        if (nbUpdated > 0) {
            LOGGER.debug("[STORAGE REQUEST] {} delayed requests can be handle now.", nbUpdated);
        }
    }

}
