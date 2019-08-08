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
package fr.cnes.regards.modules.storagelight.service.file.reference.job;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageProgressManager;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileStorageRequestService;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.FileReferenceEventPublisher;

/**
 * Progress manager class to handle {@link FileStorageRequestJob} advancement.
 * This progress manager should be used by all storage plugin to inform a storage success or a storage error.
 *
 * @author Sébastien Binda
 */
public class FileStorageJobProgressManager implements IStorageProgressManager {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageJobProgressManager.class);

    private final FileReferenceEventPublisher publisher;

    private final IJob<?> job;

    private final FileReferenceService referenceService;

    private final FileStorageRequestService storageRequestService;

    private final Set<FileStorageRequest> handledRequest = Sets.newHashSet();

    public FileStorageJobProgressManager(FileReferenceService referenceService,
            FileStorageRequestService storageRequestService, FileReferenceEventPublisher publisher, IJob<?> job) {
        this.publisher = publisher;
        this.job = job;
        this.referenceService = referenceService;
        this.storageRequestService = storageRequestService;
    }

    @Override
    public void storageSucceed(FileStorageRequest request, String storedUrl, Long fileSize) {
        if (storedUrl == null) {
            this.storageFailed(request, String
                    .format("File {} has been successully stored, nevertheless plugin <%> does not provide the new file location",
                            request.getStorage(), request.getMetaInfo().getFileName()));
        } else {
            FileLocation newLocation = new FileLocation(request.getStorage(), storedUrl);
            LOG.info("[STORAGE SUCCESS] - Store success for file {} (id={})in {} (checksum: {}).",
                     request.getMetaInfo().getFileName(), request.getId(), newLocation,
                     request.getMetaInfo().getChecksum());
            job.advanceCompletion();
            request.getMetaInfo().setFileSize(fileSize);
            for (String owner : request.getOwners()) {
                try {
                    referenceService.referenceFile(owner, request.getMetaInfo(), newLocation, request.getRequestIds());
                    // Delete the FileRefRequest as it has been handled
                    try {
                        storageRequestService.delete(request);
                    } catch (EmptyResultDataAccessException e) {
                        LOG.warn(String.format("Unable to delete storage request with id %s. Cause : %s",
                                               request.getId(), e.getMessage()),
                                 e);
                    }
                } catch (ModuleException e) {
                    String errorCause = String.format("Unable to save new file reference for file %s",
                                                      request.getStorage());
                    // The file is not really referenced so handle reference error by modifying request to be retry later
                    request.setOriginUrl(null);
                    request.setStatus(FileRequestStatus.ERROR);
                    request.setErrorCause(errorCause);
                    storageRequestService.update(request);
                    publisher.storeError(request.getMetaInfo().getChecksum(), request.getOwners(), request.getStorage(),
                                         errorCause, request.getRequestIds());
                }
            }

            handledRequest.add(request);
        }
    }

    @Override
    public void storageFailed(FileStorageRequest request, String cause) {
        LOG.error("[STORAGE ERROR] - Store error for file {} (id={})in {} (checksum: {}). Cause : {}",
                  request.getMetaInfo().getFileName(), request.getId(), request.getStorage(),
                  request.getMetaInfo().getChecksum(), cause);
        job.advanceCompletion();
        request.setStatus(FileRequestStatus.ERROR);
        request.setErrorCause(cause);
        storageRequestService.update(request);
        publisher.storeError(request.getMetaInfo().getChecksum(), request.getOwners(), request.getStorage(), cause,
                             request.getRequestIds());
        handledRequest.add(request);
    }

    public boolean isHandled(FileStorageRequest req) {
        return this.handledRequest.contains(req);
    }
}
