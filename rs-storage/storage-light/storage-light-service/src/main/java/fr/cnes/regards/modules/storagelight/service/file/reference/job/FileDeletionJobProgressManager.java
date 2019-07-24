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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEventState;
import fr.cnes.regards.modules.storagelight.domain.plugin.IDeletionProgressManager;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileDeletionRequestService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;

/**
 * @author sbinda
 *
 */
public class FileDeletionJobProgressManager implements IDeletionProgressManager {

    private static final Logger LOG = LoggerFactory.getLogger(FileDeletionJobProgressManager.class);

    private final IPublisher publisher;

    private final IJob<?> job;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final FileReferenceService fileReferenceService;

    private final FileDeletionRequestService fileDeletionRequestService;

    private final String tenant;

    public FileDeletionJobProgressManager(FileReferenceService fileReferenceService,
            FileDeletionRequestService fileDeletionRequestService, IPublisher publisher, IJob<?> job,
            IRuntimeTenantResolver runtimeTenantResolver) {
        super();
        this.publisher = publisher;
        this.job = job;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.fileReferenceService = fileReferenceService;
        this.fileDeletionRequestService = fileDeletionRequestService;
        this.tenant = runtimeTenantResolver.getTenant();
    }

    @Override
    public void deletionFailed(FileDeletionRequest fileDeletionRequest, String cause) {
        FileReference fileRef = fileDeletionRequest.getFileReference();
        LOG.error("[DELETION ERROR] - Deletion error for file {} from {} (checksum: {}). Cause : {}",
                  fileRef.getMetaInfo().getFileName(), fileRef.getLocation().toString(),
                  fileRef.getMetaInfo().getChecksum(), cause);
        job.advanceCompletion();
        fileDeletionRequest.setStatus(FileRequestStatus.ERROR);
        fileDeletionRequest.setErrorCause(cause);
        fileDeletionRequestService.updateFileDeletionRequest(fileDeletionRequest);
        FileReferenceEvent event = new FileReferenceEvent(fileRef.getMetaInfo().getChecksum(),
                FileReferenceEventState.DELETION_ERROR, null, cause, fileRef.getLocation());
        publishWithTenant(event);
    }

    @Override
    public void deletionSucceed(FileDeletionRequest fileDeletionRequest) {
        FileReference fileRef = fileDeletionRequest.getFileReference();
        String successMessage = String.format("File %s successfully deteled from %s (checksum: %s)",
                                              fileRef.getMetaInfo().getFileName(), fileRef.getLocation().toString(),
                                              fileRef.getMetaInfo().getChecksum());
        LOG.info("[DELETION SUCCESS] - {}", successMessage);
        job.advanceCompletion();
        // Delete file deletion request
        fileDeletionRequestService.deleteFileDeletionRequest(fileDeletionRequest);
        fileReferenceService.deleteFileReference(fileRef);
        FileReferenceEvent event = new FileReferenceEvent(fileRef.getMetaInfo().getChecksum(),
                FileReferenceEventState.DELETED, null, successMessage, fileRef.getLocation());
        publishWithTenant(event);
    }

    private void publishWithTenant(FileReferenceEvent event) {
        runtimeTenantResolver.forceTenant(tenant);
        publisher.publish(event);
    }

}
