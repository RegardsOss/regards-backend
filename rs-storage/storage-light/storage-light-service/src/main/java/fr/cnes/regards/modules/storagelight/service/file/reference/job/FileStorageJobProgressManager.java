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

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageProgressManager;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileStorageRequestService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.FileRefEventPublisher;

/**
 * Progress manager class to handle {@link FileStorageRequestJob} advancement.
 * This progress manager should be used by all storage plugin to inform a storage success or a storage error.
 *
 * @author Sébastien Binda
 */
public class FileStorageJobProgressManager implements IStorageProgressManager {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageJobProgressManager.class);

    private final FileRefEventPublisher publisher;

    private final IJob<?> job;

    private final FileReferenceService fileReferenceService;

    private final FileStorageRequestService fileRefRequestService;

    private final Set<FileStorageRequest> handledRequest = Sets.newHashSet();

    public FileStorageJobProgressManager(FileReferenceService fileReferenceService,
            FileStorageRequestService fileRefRequestService, FileRefEventPublisher publisher, IJob<?> job) {
        this.publisher = publisher;
        this.job = job;
        this.fileReferenceService = fileReferenceService;
        this.fileRefRequestService = fileRefRequestService;
    }

    @Override
    public void storageSucceed(FileStorageRequest fileRefRequest, String storedUrl, Long fileSize) {

        if (storedUrl == null) {
            this.storageFailed(fileRefRequest, String
                    .format("File {} has been successully stored, nevertheless plugin <%> does not provide the new file location",
                            fileRefRequest.getDestination().getStorage(), fileRefRequest.getMetaInfo().getFileName()));
        } else {
            FileLocation newLocation = new FileLocation(fileRefRequest.getDestination().getStorage(), storedUrl);
            LOG.info("[STORAGE SUCCESS] - Store success for file {} (id={})in {} (checksum: {}).",
                     fileRefRequest.getMetaInfo().getFileName(), fileRefRequest.getId(), newLocation,
                     fileRefRequest.getMetaInfo().getChecksum());
            job.advanceCompletion();
            // Create FileReference resulting of the success of FileReferenceRequest
            Optional<FileReference> oFileRef = fileReferenceService.addFileReference(fileRefRequest.getOwners(),
                                                                                     fileRefRequest.getMetaInfo(),
                                                                                     newLocation, newLocation);
            if (oFileRef.isPresent()) {
                // Delete the FileRefRequest as it has been handled
                fileRefRequestService.deleteFileReferenceRequest(fileRefRequest);
            } else {
                String errorCause = String.format("Unable to save new file reference for file %s",
                                                  fileRefRequest.getDestination().toString());
                // The file is not really referenced so handle reference error by modifying request to be retry later
                fileRefRequest.setOrigin(fileRefRequest.getDestination());
                fileRefRequest.setStatus(FileRequestStatus.ERROR);
                fileRefRequest.setErrorCause(errorCause);
                fileRefRequestService.update(fileRefRequest);
                publisher.publishFileRefStoreError(fileRefRequest.getMetaInfo().getChecksum(),
                                                   fileRefRequest.getOwners(), fileRefRequest.getDestination(),
                                                   errorCause);
            }
            handledRequest.add(fileRefRequest);
        }
    }

    @Override
    public void storageFailed(FileStorageRequest fileRefRequest, String cause) {
        LOG.error("[STORAGE ERROR] - Store error for file {} (id={})in {} (checksum: {}). Cause : {}",
                  fileRefRequest.getMetaInfo().getFileName(), fileRefRequest.getId(),
                  fileRefRequest.getDestination().toString(), fileRefRequest.getMetaInfo().getChecksum(), cause);
        job.advanceCompletion();
        fileRefRequest.setOrigin(fileRefRequest.getDestination());
        fileRefRequest.setStatus(FileRequestStatus.ERROR);
        fileRefRequest.setErrorCause(cause);
        fileRefRequestService.update(fileRefRequest);
        publisher.publishFileRefStoreError(fileRefRequest.getMetaInfo().getChecksum(), fileRefRequest.getOwners(),
                                           fileRefRequest.getDestination(), cause);
        handledRequest.add(fileRefRequest);
    }

    public boolean isHandled(FileStorageRequest req) {
        return this.handledRequest.contains(req);
    }
}
