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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storagelight.domain.plugin.IRestorationProgressManager;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageProgressManager;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileCacheRequestService;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.FileRefEventPublisher;

/**
 * Implementation of {@link IStorageProgressManager} used by {@link IStorageLocation} plugins.<br/>
 * This implementation notify the system thanks to the AMQP publisher.
 *
 * @author Sébastien Binda
 */
public class FileCacheJobProgressManager implements IRestorationProgressManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCacheJobProgressManager.class);

    private final FileRefEventPublisher publisher;

    private final IJob<?> job;

    private final Set<FileCacheRequest> handled = Sets.newHashSet();

    private final FileCacheRequestService fileCacheRequestService;

    public FileCacheJobProgressManager(FileCacheRequestService fileCacheRequestService, FileRefEventPublisher publisher,
            IJob<?> job) {
        super();
        this.publisher = publisher;
        this.job = job;
        this.fileCacheRequestService = fileCacheRequestService;
    }

    @Override
    public void restoreSucceed(FileCacheRequest fileReq) {
        FileReference fileRef = fileReq.getFileReference();
        // Check file exists in cache
        Path cacheFilePath = Paths.get(fileReq.getDestinationPath());
        if (Files.exists(cacheFilePath)) {
            try {
                URL cacheFileLocation = new URL("file", null, cacheFilePath.toString());
                String successMessage = String.format("File %s (checksum %s) successfully restored from %s to %s.",
                                                      fileRef.getMetaInfo().getFileName(),
                                                      fileRef.getMetaInfo().getChecksum(),
                                                      fileRef.getLocation().toString(), fileReq.getDestinationPath());
                LOGGER.debug("[RESTORATION SUCCESS] - {}", successMessage);
                job.advanceCompletion();
                fileCacheRequestService.handleSuccess(fileReq, cacheFileLocation, cacheFilePath.toFile().length());
                publisher.available(fileRef.getMetaInfo().getChecksum(), successMessage, fileReq.getRequestId());
                handled.add(fileReq);
            } catch (MalformedURLException e) {
                LOGGER.error(e.getMessage(), e);
                restoreFailed(fileReq, e.getMessage());
                try {
                    Files.delete(cacheFilePath);
                } catch (IOException e1) {
                    // Nothing to do, only log error.
                    LOGGER.error(e1.getMessage(), e1);
                }
            }
        } else {
            String errorCause = String
                    .format("Unknown error during file %s restoration in cache. Storage location plugin indicates that the file is restored but file does not exists at %s.",
                            fileReq.getChecksum(), fileReq.getDestinationPath());
            restoreFailed(fileReq, errorCause);
        }
    }

    @Override
    public void restoreFailed(FileCacheRequest fileReq, String cause) {
        FileReference fileRef = fileReq.getFileReference();
        LOGGER.error("[RESTORATION ERROR] - Restoration error for file {} from {} (checksum: {}). Cause : {}",
                     fileRef.getMetaInfo().getFileName(), fileRef.getLocation().toString(),
                     fileRef.getMetaInfo().getChecksum(), cause);
        job.advanceCompletion();
        fileCacheRequestService.handleError(fileReq, cause);
        publisher.notAvailable(fileRef.getMetaInfo().getChecksum(), cause, fileReq.getRequestId());
        handled.add(fileReq);
    }

    /**
     * Does the given {@link FileCacheRequest} has been handled by the restoration job ?
     */
    public boolean isHandled(FileCacheRequest req) {
        return handled.contains(req);
    }
}
