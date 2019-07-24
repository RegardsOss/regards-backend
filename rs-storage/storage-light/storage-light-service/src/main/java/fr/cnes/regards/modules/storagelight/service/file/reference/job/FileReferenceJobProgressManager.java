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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceRequest;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEventState;
import fr.cnes.regards.modules.storagelight.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageProgressManager;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceRequestService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;

/**
 * Implementation of {@link IStorageProgressManager} used by {@link IDataStorage} plugins.<br/>
 * This implementation notify the system thanks to the AMQP publisher.
 *
 * @author Sébastien Binda
 */
public class FileReferenceJobProgressManager implements IStorageProgressManager {

    private static final Logger LOG = LoggerFactory.getLogger(FileReferenceJobProgressManager.class);

    private final IPublisher publisher;

    private final IJob<?> job;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final FileReferenceService fileReferenceService;

    private final FileReferenceRequestService fileRefRequestService;

    private final String tenant;

    public FileReferenceJobProgressManager(FileReferenceService fileReferenceService,
            FileReferenceRequestService fileRefRequestService, IPublisher publisher, IJob<?> job,
            IRuntimeTenantResolver runtimeTenantResolver) {
        this.publisher = publisher;
        this.job = job;
        this.tenant = runtimeTenantResolver.getTenant();
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.fileReferenceService = fileReferenceService;
        this.fileRefRequestService = fileRefRequestService;
    }

    @Override
    public void storageSucceed(FileReferenceRequest fileRefRequest, String storedUrl, Long fileSize) {

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
                FileReference newFileRef = oFileRef.get();
                // Create new event message for new FileReference
                FileReferenceEvent event = new FileReferenceEvent(newFileRef.getMetaInfo().getChecksum(),
                        FileReferenceEventState.STORED, newFileRef.getOwners(),
                        String.format("File %s successfully referenced at %s", newFileRef.getMetaInfo().getFileName(),
                                      newFileRef.getLocation().toString()));
                // hell yeah this is not the usual publish method, but i know what i'm doing so trust me!
                publishWithTenant(event);
            } else {
                // The file is not really referenced so handle reference error by modifying request to be retry later
                fileRefRequest.setOrigin(fileRefRequest.getDestination());
                fileRefRequest.setStatus(FileRequestStatus.ERROR);
                fileRefRequest.setErrorCause(String.format("Unable to save new file reference for file %s",
                                                           fileRefRequest.getDestination().toString()));
                fileRefRequestService.updateFileReferenceRequest(fileRefRequest);
            }
        }
    }

    @Override
    public void storageFailed(FileReferenceRequest fileRefRequest, String cause) {
        LOG.error("[STORAGE ERROR] - Store error for file {} (id={})in {} (checksum: {}). Cause : {}",
                  fileRefRequest.getMetaInfo().getFileName(), fileRefRequest.getId(),
                  fileRefRequest.getDestination().toString(), fileRefRequest.getMetaInfo().getChecksum(), cause);
        job.advanceCompletion();
        fileRefRequest.setOrigin(fileRefRequest.getDestination());
        fileRefRequest.setStatus(FileRequestStatus.ERROR);
        fileRefRequest.setErrorCause(cause);
        fileRefRequestService.updateFileReferenceRequest(fileRefRequest);
        FileReferenceEvent event = new FileReferenceEvent(fileRefRequest.getMetaInfo().getChecksum(),
                FileReferenceEventState.STORE_ERROR, fileRefRequest.getOwners(), cause,
                fileRefRequest.getDestination());
        publishWithTenant(event);
    }

    private void publishWithTenant(FileReferenceEvent event) {
        runtimeTenantResolver.forceTenant(tenant);
        publisher.publish(event);
    }
}
