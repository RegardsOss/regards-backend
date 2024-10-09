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
package fr.cnes.regards.modules.filecatalog.service.request;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.filecatalog.dao.IFileDeletionRequestRepository;
import fr.cnes.regards.modules.filecatalog.domain.FileReference;
import fr.cnes.regards.modules.filecatalog.domain.request.FileDeletionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Service to handle {@link FileDeletionRequest}s to physically delete files.
 *
 * @author Sébastien Binda
 */
@Service
public class FileDeletionRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDeletionRequestService.class);

    private final IFileDeletionRequestRepository fileDeletionRequestRepository;

    public FileDeletionRequestService(IFileDeletionRequestRepository fileDeletionRequestRepository) {
        this.fileDeletionRequestRepository = fileDeletionRequestRepository;
    }

    public void delete(FileDeletionRequest fileDeletionRequest) {
        if (fileDeletionRequestRepository.existsById(fileDeletionRequest.getId())) {
            fileDeletionRequestRepository.deleteById(fileDeletionRequest.getId());
        } else {
            LOGGER.warn("Unable to delete file deletion request {} cause it does not exists.",
                        fileDeletionRequest.getId());
        }
    }

    /**
     * Search for existing deletion request on the given file references.
     */
    public Set<FileDeletionRequest> search(Set<FileReference> existingOnesWithSameChecksum) {
        // FIXME Neo Storage lot 4;
        return new HashSet<>();
    }

    @MultitenantTransactional(readOnly = true)
    public Long count(String storage, FileRequestStatus status) {
        // FIXME Neo Storage lot 4
        //return fileDeletionRequestRepository.countByStorageAndStatus(storage, status);

        return 0L;
    }

    /**
     * Delete all requests for the given storage identifier
     */
    public void deleteByStorage(String storageLocationId, Optional<FileRequestStatus> status) {
        if (status.isPresent()) {
            fileDeletionRequestRepository.deleteByStorageAndStatus(storageLocationId, status.get());
        } else {
            fileDeletionRequestRepository.deleteByStorage(storageLocationId);
        }
    }

    public boolean isDeletionRunning(String storage) {
        // FIXME Neo Storage lot 4;
        return false;
    }

}
