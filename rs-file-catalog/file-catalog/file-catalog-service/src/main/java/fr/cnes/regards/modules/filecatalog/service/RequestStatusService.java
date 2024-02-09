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
import fr.cnes.regards.modules.filecatalog.dao.IFileDeletionRequestRepository;
import fr.cnes.regards.modules.filecatalog.dao.IFileStorageRequestAggregationRepository;
import fr.cnes.regards.modules.filecatalog.domain.request.FileStorageRequestAggregation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * @author Thibaud Michaudel
 **/
@Service
@MultitenantTransactional
public class RequestStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestStatusService.class);

    private final IFileDeletionRequestRepository deletionReqRepo;

    private final IFileStorageRequestAggregationRepository fileStorageRequestAggregationRepository;

    public RequestStatusService(IFileDeletionRequestRepository deletionReqRepo,
                                IFileStorageRequestAggregationRepository fileStorageRequestAggregationRepository) {
        this.deletionReqRepo = deletionReqRepo;
        this.fileStorageRequestAggregationRepository = fileStorageRequestAggregationRepository;
    }

    /**
     * Compute {@link FileRequestStatus} for new {@link FileStorageRequestAggregation}
     *
     * @param request  request to compute status for
     * @param oDefault default status or empty
     * @return {@link FileRequestStatus}
     */
    public FileRequestStatus getNewStatus(FileStorageRequestAggregation request, Optional<FileRequestStatus> oDefault) {
        FileRequestStatus status = oDefault.orElse(FileRequestStatus.TO_DO);
        String storage = request.getStorage();
        String checksum = request.getMetaInfo().getChecksum();
        Set<FileRequestStatus> toDelayStatusList = FileRequestStatus.RUNNING_STATUS;
        if (request.getStatus() == FileRequestStatus.TO_DO) {
            toDelayStatusList = FileRequestStatus.RUNNING_AND_DELAYED_STATUS;
        }
        // Delayed storage request if a deletion requests already exists
        if (deletionReqRepo.existsByStorageAndFileReferenceMetaInfoChecksumAndStatusIn(storage,
                                                                                       checksum,
                                                                                       FileRequestStatus.RUNNING_STATUS)) {
            status = FileRequestStatus.DELAYED;
        }
        // Delay storage request if an other storage request is already running for the same file to store
        else if (fileStorageRequestAggregationRepository.existsByStorageAndMetaInfoChecksumAndStatusIn(storage,
                                                                                                       checksum,
                                                                                                       toDelayStatusList)) {
            status = FileRequestStatus.DELAYED;
        } else if (request.getStatus() == FileRequestStatus.DELAYED && status == FileRequestStatus.TO_DO) {
            LOGGER.info("Request {}/{} undelayed", request.getMetaInfo().getChecksum(), request.getStorage());
        }
        return status;
    }

}
