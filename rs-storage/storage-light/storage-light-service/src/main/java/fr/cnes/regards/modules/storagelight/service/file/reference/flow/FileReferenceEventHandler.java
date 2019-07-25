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
package fr.cnes.regards.modules.storagelight.service.file.reference.flow;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceRequest;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileDeletionRequestService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceRequestService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;

/**
 * This handler is used internally by the storage service to update file requests in DELAYED state after event on file references.
 * A fileReferenceRequest DELAYED is restarted after a file deletion request ends.
 *
 * @author Sébastien Binda
 */
@Component
public class FileReferenceEventHandler implements IHandler<FileReferenceEvent> {

    @Autowired
    private FileReferenceRequestService fileReferenceRequestService;

    @Autowired
    private FileDeletionRequestService fileDeletionRequestService;

    @Autowired
    private FileReferenceService fileReferenceService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Override
    public void handle(TenantWrapper<FileReferenceEvent> wrapper) {
        String tenant = wrapper.getTenant();
        runtimeTenantResolver.forceTenant(tenant);
        try {
            switch (wrapper.getContent().getState()) {
                case FULLY_DELETED:
                case DELETION_ERROR:
                    // When a file reference deletion is over, schedule the delayed reference requests if any
                    // Indeed, when a file reference deletion process is running, every file reference request are delayed until
                    // the deletion process is over.
                    this.scheduleDelayedFileRefRequests(wrapper.getContent().getChecksum(),
                                                        wrapper.getContent().getLocation().getStorage());
                    break;
                case DELETED_FOR_OWNER:
                case AVAILABILITY_ERROR:
                case AVAILABLE:
                case STORED:
                case STORE_ERROR:
                default:
                    break;
            }
        } finally {
            runtimeTenantResolver.clearTenant();
        }

    }

    /**
     * After a deletion success, we can schedule the file reference request delayed. Those request was waiting for deletion ends.
     */
    private void scheduleDelayedFileRefRequests(String fileRefChecksum, String fileRefStorage) {
        Optional<FileReferenceRequest> oRequest = fileReferenceRequestService.search(fileRefStorage, fileRefChecksum);
        if (oRequest.isPresent() && (oRequest.get().getStatus() == FileRequestStatus.DELAYED)) {
            // As a storage is scheduled, we can delete the deletion request
            Optional<FileReference> oFileRef = fileReferenceService.search(fileRefStorage, fileRefChecksum);
            if (oFileRef.isPresent()) {
                Optional<FileDeletionRequest> oDeletionRequest = fileDeletionRequestService.search(oFileRef.get());
                if (oDeletionRequest.isPresent()) {
                    fileDeletionRequestService.deleteFileDeletionRequest(oDeletionRequest.get());
                }
            }
            FileReferenceRequest request = oRequest.get();
            request.setStatus(FileRequestStatus.TODO);
            fileReferenceRequestService.updateFileReferenceRequest(request);
        }
    }

}
