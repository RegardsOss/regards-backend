/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IRestorationProgressManager;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IStorageLocation;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IStorageProgressManager;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileCacheRequestDto;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.service.file.request.FileCacheRequestService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Set;

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

    /**
     * Business identifier of plugin to access file in external cache.
     */
    private String pluginBusinessId;

    public FileCacheJobProgressManager(FileCacheRequestService fileCacheRequestService, IJob<?> job) {
        this.job = job;
        this.fileCacheRequestService = fileCacheRequestService;
    }

    public void setPluginBusinessId(String pluginBusinessId) {
        this.pluginBusinessId = pluginBusinessId;
    }

    @Override
    public void restoreSucceededInternalCache(FileCacheRequestDto fileReqDto, Path restoredFilePath) {
        FileCacheRequest fileCacheRequest = FileCacheRequest.fromDto(fileReqDto);
        FileReference fileRef = fileCacheRequest.getFileReference();
        // Check file exists in cache
        if (Files.exists(restoredFilePath)) {
            try {
                URL cacheFileLocation = new URL("file", null, restoredFilePath.toString());
                // Compute the real file size in internal cache
                long fileSize = restoredFilePath.toFile().length();
                job.advanceCompletion();
                fileCacheRequestService.handleSuccessInternalCache(fileCacheRequest,
                                                                   cacheFileLocation,
                                                                   fileRef.getLazzyOwners(),
                                                                   fileSize,
                                                                   String.format(
                                                                       "File %s (checksum=%s, size=%s) successfully restored from "
                                                                       + "%s to %s (internal cache).",
                                                                       fileRef.getMetaInfo().getFileName(),
                                                                       fileRef.getMetaInfo().getChecksum(),
                                                                       fileSize,
                                                                       fileRef.getLocation().toString(),
                                                                       restoredFilePath));
                handled.add(fileCacheRequest);
            } catch (MalformedURLException e) {
                LOGGER.error(e.getMessage(), e);
                restoreFailed(fileReqDto, e.getMessage());
                try {
                    Files.delete(restoredFilePath);
                } catch (IOException e1) {
                    // Nothing to do, only log error.
                    LOGGER.error(e1.getMessage(), e1);
                }
            }
        } else {
            restoreFailed(fileReqDto,
                          String.format(
                              "Unknown error during file %s restoration in internal cache. Storage location plugin "
                              + "indicates that the file is restored but file does not exists at %s.",
                              fileReqDto.getChecksum(),
                              restoredFilePath));
        }
    }

    @Override
    public void restoreFailed(FileCacheRequestDto fileCacheRequestDto, String cause) {
        job.advanceCompletion();
        FileCacheRequest fileCacheRequest = FileCacheRequest.fromDto(fileCacheRequestDto);
        fileCacheRequestService.handleError(fileCacheRequest, cause);
        handled.add(fileCacheRequest);
    }

    @Override
    public void restoreSucceededExternalCache(FileCacheRequestDto fileCacheRequestDto,
                                              URL restoredFileUrl,
                                              @Nullable Long fileSize,
                                              OffsetDateTime expirationDate) {
        FileCacheRequest fileCacheRequest = FileCacheRequest.fromDto(fileCacheRequestDto);
        if (StringUtils.isBlank(pluginBusinessId)) {
            restoreFailed(fileCacheRequestDto,
                          String.format(
                              "Error during file %s restoration in external cache. Businness identifier of plugin to "
                              + "access file in external cache is undefined.",
                              fileCacheRequestDto.getChecksum()));
            return;
        }
        FileReference fileRef = fileCacheRequest.getFileReference();
        Long newFileSize = fileSize;
        if (newFileSize == null) {
            LOGGER.debug("The plugin returned an empty file size, so use the size indicated in the request");
            newFileSize = fileCacheRequestDto.getFileSize();
        }

        fileCacheRequestService.handleSuccessExternalCache(fileCacheRequest,
                                                           restoredFileUrl,
                                                           fileRef.getLazzyOwners(),
                                                           newFileSize,
                                                           pluginBusinessId,
                                                           expirationDate,
                                                           String.format(
                                                               "File %s (checksum=%s, size=%s) successfully restored from %s to %s"
                                                               + " (external cache).",
                                                               fileRef.getMetaInfo().getFileName(),
                                                               fileRef.getMetaInfo().getChecksum(),
                                                               fileSize,
                                                               fileRef.getLocation().toString(),
                                                               restoredFileUrl));
        job.advanceCompletion();
        handled.add(fileCacheRequest);
    }

    /**
     * Does the given {@link FileCacheRequest} has been handled by the restoration job ?
     */
    public boolean isHandled(FileCacheRequest fileCacheRequest) {
        return handled.contains(fileCacheRequest);
    }
}
