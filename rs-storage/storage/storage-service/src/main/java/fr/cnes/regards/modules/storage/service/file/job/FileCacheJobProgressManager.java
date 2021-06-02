/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.file.job;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.domain.plugin.IRestorationProgressManager;
import fr.cnes.regards.modules.storage.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storage.domain.plugin.IStorageProgressManager;
import fr.cnes.regards.modules.storage.service.file.request.FileCacheRequestService;

/**
 * Implementation of {@link IStorageProgressManager} used by {@link IStorageLocation} plugins.<br>
 * This implementation notify the system thanks to the AMQP publisher.<br>
 * This manager is used by storage plugins to inform {@link FileCacheRequestJob}s progression.
 *
 * @author Sébastien Binda
 */
public class FileCacheJobProgressManager implements IRestorationProgressManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCacheJobProgressManager.class);

    private final IJob<?> job;

    private final Set<FileCacheRequest> handled = Sets.newHashSet();

    private final FileCacheRequestService fileCacheRequestService;

    public FileCacheJobProgressManager(FileCacheRequestService fileCacheRequestService, IJob<?> job) {
        super();
        this.job = job;
        this.fileCacheRequestService = fileCacheRequestService;
    }

    @Override
    public void restoreSucceed(FileCacheRequest fileReq, Path resoredFilePath) {
        FileReference fileRef = fileReq.getFileReference();
        // Check file exists in cache
        if (Files.exists(resoredFilePath)) {
            try {
                URL cacheFileLocation = new URL("file", null, resoredFilePath.toString());
                String successMessage = String
                        .format("File %s (checksum=%s, size=%s) successfully restored from %s to %s.",
                                fileRef.getMetaInfo().getFileName(), fileRef.getMetaInfo().getChecksum(),
                                resoredFilePath.toFile().length(), fileRef.getLocation().toString(), resoredFilePath);
                job.advanceCompletion();
                fileCacheRequestService.handleSuccess(fileReq, cacheFileLocation, fileRef.getLazzyOwners(),
                                                      resoredFilePath.toFile().length(), successMessage);
                handled.add(fileReq);
            } catch (MalformedURLException e) {
                LOGGER.error(e.getMessage(), e);
                restoreFailed(fileReq, e.getMessage());
                try {
                    Files.delete(resoredFilePath);
                } catch (IOException e1) {
                    // Nothing to do, only log error.
                    LOGGER.error(e1.getMessage(), e1);
                }
            }
        } else {
            String errorCause = String
                    .format("Unknown error during file %s restoration in cache. Storage location plugin indicates that the file is restored but file does not exists at %s.",
                            fileReq.getChecksum(), resoredFilePath);
            restoreFailed(fileReq, errorCause);
        }
    }

    @Override
    public void restoreFailed(FileCacheRequest fileReq, String cause) {
        job.advanceCompletion();
        fileCacheRequestService.handleError(fileReq, cause);
        handled.add(fileReq);
    }

    /**
     * Does the given {@link FileCacheRequest} has been handled by the restoration job ?
     */
    public boolean isHandled(FileCacheRequest req) {
        return handled.contains(req);
    }
}
