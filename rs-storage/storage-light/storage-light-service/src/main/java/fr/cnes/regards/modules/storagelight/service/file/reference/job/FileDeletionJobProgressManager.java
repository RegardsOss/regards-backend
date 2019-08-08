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

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.plugin.IDeletionProgressManager;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileDeletionRequestService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.FileRefEventPublisher;

/**
 * Progress manager class to handle {@link FileDeletionRequestJob} advancement.
 * This progress manager should be used by all storage plugin to inform a deletion success or a deletion error.
 *
 * @author Sébastien Binda
 */
public class FileDeletionJobProgressManager implements IDeletionProgressManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDeletionJobProgressManager.class);

    private final FileRefEventPublisher publisher;

    private final IJob<?> job;

    private final FileReferenceService fileReferenceService;

    private final FileDeletionRequestService fileDeletionRequestService;

    private final Set<FileDeletionRequest> handled = Sets.newHashSet();

    public FileDeletionJobProgressManager(FileReferenceService fileReferenceService,
            FileDeletionRequestService fileDeletionRequestService, FileRefEventPublisher publisher, IJob<?> job) {
        super();
        this.publisher = publisher;
        this.job = job;
        this.fileReferenceService = fileReferenceService;
        this.fileDeletionRequestService = fileDeletionRequestService;
    }

    @Override
    public void deletionFailed(FileDeletionRequest fileDeletionRequest, String cause) {
        FileReference fileRef = fileDeletionRequest.getFileReference();
        LOGGER.error("[DELETION ERROR] - Deletion error for file {} from {} (checksum: {}). Cause : {}",
                     fileRef.getMetaInfo().getFileName(), fileRef.getLocation().toString(),
                     fileRef.getMetaInfo().getChecksum(), cause);
        job.advanceCompletion();
        if (!fileDeletionRequest.isForceDelete()) {
            fileDeletionRequest.setStatus(FileRequestStatus.ERROR);
            fileDeletionRequest.setErrorCause(cause);
            fileDeletionRequestService.updateFileDeletionRequest(fileDeletionRequest);
            publisher.deletionError(fileRef, cause, fileDeletionRequest.getRequestId());
        } else {
            // Delete file deletion request
            fileDeletionRequestService.delete(fileDeletionRequest);
            // Delete file reference
            fileReferenceService.delete(fileRef, fileDeletionRequest.getRequestId());
            // NOTE : The file reference event is published by the fileReferenceService
            LOGGER.warn(String
                    .format("File %s from %s (checksum: %s) has been removed by force from referenced files. (File may still exists on storage).",
                            fileRef.getMetaInfo().getFileName(), fileRef.getLocation().toString(),
                            fileRef.getMetaInfo().getChecksum()));
        }
        handled.add(fileDeletionRequest);
    }

    @Override
    public void deletionSucceed(FileDeletionRequest fileDeletionRequest) {
        FileReference fileRef = fileDeletionRequest.getFileReference();
        String successMessage = String.format("File %s successfully deteled from %s (checksum: %s)",
                                              fileRef.getMetaInfo().getFileName(), fileRef.getLocation().toString(),
                                              fileRef.getMetaInfo().getChecksum());
        LOGGER.info("[DELETION SUCCESS] - {}", successMessage);
        job.advanceCompletion();
        // Delete file deletion request
        fileDeletionRequestService.delete(fileDeletionRequest);
        fileReferenceService.delete(fileRef, fileDeletionRequest.getRequestId());
        // NOTE : the FileReferenceEvent is published by the fileReferenceService when the file is completely deleted
        handled.add(fileDeletionRequest);
    }

    public boolean isHandled(FileDeletionRequest req) {
        return this.handled.contains(req);
    }

}
