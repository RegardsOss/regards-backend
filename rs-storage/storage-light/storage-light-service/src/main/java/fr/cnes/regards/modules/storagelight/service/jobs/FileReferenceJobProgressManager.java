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
package fr.cnes.regards.modules.storagelight.service.jobs;

import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.FileReferenceRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceRequest;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEventState;
import fr.cnes.regards.modules.storagelight.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storagelight.domain.plugin.IProgressManager;
import fr.cnes.regards.modules.storagelight.service.FileReferenceService;

/**
 * Implementation of {@link IProgressManager} used by {@link IDataStorage} plugins.<br/>
 * This implementation notify the system thanks to the AMQP publisher.
 *
 * @author Sébastien Binda
 */
public class FileReferenceJobProgressManager implements IProgressManager {

    private static final Logger LOG = LoggerFactory.getLogger(FileReferenceJobProgressManager.class);

    private final IPublisher publisher;

    private final IJob<?> job;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final FileReferenceService fileReferenceService;

    private final String tenant;

    public FileReferenceJobProgressManager(FileReferenceService fileReferenceService, IPublisher publisher, IJob<?> job,
            IRuntimeTenantResolver runtimeTenantResolver) {
        this.publisher = publisher;
        this.job = job;
        this.tenant = runtimeTenantResolver.getTenant();
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.fileReferenceService = fileReferenceService;
    }

    @Override
    public void storageSucceed(FileReferenceRequest fileRefRequest, Long fileSize) {
        LOG.info("[STORAGE SUCCESS] - Store success for file {} (id={})in {} (checksum: {}).",
                 fileRefRequest.getMetaInfo().getFileName(), fileRefRequest.getId(),
                 fileRefRequest.getDestination().toString(), fileRefRequest.getMetaInfo().getChecksum());
        job.advanceCompletion();
        // Create FileReference resulting of the success of FileReferenceRequest
        Optional<FileReference> oFileRef = fileReferenceService
                .createFileReference(fileRefRequest.getOwners(), fileRefRequest.getMetaInfo(),
                                     fileRefRequest.getDestination(), fileRefRequest.getDestination());
        if (oFileRef.isPresent()) {
            // Delete the FileRefRequest as it has been handled
            fileReferenceService.deleteFileReferenceRequest(fileRefRequest);
            FileReference newFileRef = oFileRef.get();
            // Create new event message for new FileReference
            FileReferenceEvent event = new FileReferenceEvent(newFileRef.getMetaInfo().getChecksum(),
                    FileReferenceEventState.STORED,
                    String.format("File %s successfully referenced at %s", newFileRef.getMetaInfo().getFileName(),
                                  newFileRef.getLocation().toString()));
            // hell yeah this is not the usual publish method, but i know what i'm doing so trust me!
            publishWithTenant(event);
        } else {
            // The file is not really referenced so handle reference error by modifying request to be retry later
            fileRefRequest.setOrigin(fileRefRequest.getDestination());
            fileRefRequest.setStatus(FileReferenceRequestStatus.STORE_ERROR);
            fileRefRequest.setErrorCause(String.format("Unable to save new file reference for file %s",
                                                       fileRefRequest.getDestination().toString()));
            fileReferenceService.updateFileReferenceRequest(fileRefRequest);
        }
    }

    @Override
    public void storageFailed(FileReferenceRequest fileRefRequest, String cause) {
        LOG.error("[STORAGE ERROR] - Store error for file {} (id={})in {} (checksum: {}). Cause : {}",
                  fileRefRequest.getMetaInfo().getFileName(), fileRefRequest.getId(),
                  fileRefRequest.getDestination().toString(), fileRefRequest.getMetaInfo().getChecksum(), cause);
        job.advanceCompletion();
        fileRefRequest.setOrigin(fileRefRequest.getDestination());
        fileRefRequest.setStatus(FileReferenceRequestStatus.STORE_ERROR);
        fileRefRequest.setErrorCause(cause);
        fileReferenceService.updateFileReferenceRequest(fileRefRequest);
        FileReferenceEvent event = new FileReferenceEvent(fileRefRequest.getMetaInfo().getChecksum(),
                FileReferenceEventState.STORE_ERROR, cause, fileRefRequest.getDestination());
        publishWithTenant(event);
    }

    @Override
    public void deletionFailed(FileReference fileRef, String cause) {
        LOG.error("[DELETION ERROR] - Deletion error for file {} from {} (checksum: {}). Cause : {}",
                  fileRef.getMetaInfo().getFileName(), fileRef.getLocation().toString(),
                  fileRef.getMetaInfo().getChecksum(), cause);
        job.advanceCompletion();
        FileReferenceEvent event = new FileReferenceEvent(fileRef.getMetaInfo().getChecksum(),
                FileReferenceEventState.DELETION_ERROR, cause, fileRef.getLocation());
        publishWithTenant(event);
    }

    @Override
    public void deletionSucceed(FileReference fileRef) {
        String successMessage = String.format("File %s successfully deteled from %s (checksum: %s)",
                                              fileRef.getMetaInfo().getFileName(), fileRef.getLocation().toString(),
                                              fileRef.getMetaInfo().getChecksum());
        LOG.info("[DELETION SUCCESS] - {}", successMessage);
        job.advanceCompletion();
        FileReferenceEvent event = new FileReferenceEvent(fileRef.getMetaInfo().getChecksum(),
                FileReferenceEventState.DELETED, successMessage, fileRef.getLocation());
        publishWithTenant(event);
    }

    @Override
    public void restoreSucceed(FileReference fileRef, Path restoredFilePath) {
        String successMessage = String.format("File %s successfully restored from %s to %s (checksum : %s).",
                                              fileRef.getMetaInfo().getFileName(), fileRef.getLocation().toString(),
                                              restoredFilePath.toString(), fileRef.getMetaInfo().getChecksum());
        LOG.debug("[RESTORATION SUCCESS] - {}", successMessage);
        job.advanceCompletion();
        FileReferenceEvent event = new FileReferenceEvent(fileRef.getMetaInfo().getChecksum(),
                FileReferenceEventState.RESTORED, successMessage,
                new FileLocation(null, "file:///" + restoredFilePath.toString()));
        publishWithTenant(event);
    }

    @Override
    public void restoreFailed(FileReference fileRef, String cause) {
        LOG.error("[RESTORATION ERROR] - Restoration error for file {} from {} (checksum: {}). Cause : {}",
                  fileRef.getMetaInfo().getFileName(), fileRef.getLocation().toString(),
                  fileRef.getMetaInfo().getChecksum(), cause);
        job.advanceCompletion();
        FileReferenceEvent event = new FileReferenceEvent(fileRef.getMetaInfo().getChecksum(),
                FileReferenceEventState.RESTORATION_ERROR, cause);
        publishWithTenant(event);
    }

    private void publishWithTenant(FileReferenceEvent event) {
        runtimeTenantResolver.forceTenant(tenant);
        publisher.publish(event);
    }
}
