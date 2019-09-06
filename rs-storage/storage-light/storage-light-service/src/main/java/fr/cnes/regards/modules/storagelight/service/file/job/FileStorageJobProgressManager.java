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
package fr.cnes.regards.modules.storagelight.service.file.job;

import java.net.URL;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageProgressManager;
import fr.cnes.regards.modules.storagelight.service.file.request.FileReferenceRequestService;
import fr.cnes.regards.modules.storagelight.service.file.request.FileStorageRequestService;

/**
 * Progress manager class to handle {@link FileStorageRequestJob} advancement.
 * This progress manager should be used by all storage plugin to inform a storage success or a storage error.
 *
 * @author Sébastien Binda
 */
public class FileStorageJobProgressManager implements IStorageProgressManager {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageJobProgressManager.class);

    private final IJob<?> job;

    private final FileReferenceRequestService fileRefReqService;

    private final FileStorageRequestService storageRequestService;

    private final Set<FileStorageRequest> handledRequest = Sets.newHashSet();

    public FileStorageJobProgressManager(FileReferenceRequestService fileRefReqService,
            FileStorageRequestService storageRequestService, IJob<?> job) {
        this.job = job;
        this.fileRefReqService = fileRefReqService;
        this.storageRequestService = storageRequestService;
    }

    @Override
    public void storageSucceed(FileStorageRequest request, URL storedUrl, Long fileSize) {
        if (storedUrl == null) {
            this.storageFailed(request, String
                    .format("File {} has been successully stored, nevertheless plugin <%> does not provide the new file location",
                            request.getStorage(), request.getMetaInfo().getFileName()));
        } else {
            FileLocation newLocation = new FileLocation(request.getStorage(), storedUrl.toString());
            String message = String.format("Store success for file %s (id=%s)in %s (checksum: %s).",
                                           request.getMetaInfo().getFileName(), request.getId(), newLocation,
                                           request.getMetaInfo().getChecksum());
            LOG.debug("[STORE SUCCESS {}] - {}.", request.getMetaInfo().getChecksum(), message);
            job.advanceCompletion();
            request.getMetaInfo().setFileSize(fileSize);
            for (String owner : request.getOwners()) {
                try {
                    FileReference fileRef = fileRefReqService.reference(owner, request.getMetaInfo(), newLocation,
                                                                        request.getGroupIds());
                    storageRequestService.handleSuccess(request, fileRef, message);
                } catch (ModuleException e) {
                    String errorCause = String.format("Unable to save new file reference for file %s",
                                                      request.getStorage());
                    storageRequestService.handleError(request, errorCause);

                }
            }

            handledRequest.add(request);
        }
    }

    @Override
    public void storageFailed(FileStorageRequest request, String cause) {
        LOG.error("[STORE ERROR {}] - Store error for file {} (id={})in {}. Cause : {}",
                  request.getMetaInfo().getFileName(), request.getId(), request.getStorage(),
                  request.getMetaInfo().getChecksum(), cause);
        job.advanceCompletion();
        storageRequestService.handleError(request, cause);
        handledRequest.add(request);
    }

    public boolean isHandled(FileStorageRequest req) {
        return this.handledRequest.contains(req);
    }
}
