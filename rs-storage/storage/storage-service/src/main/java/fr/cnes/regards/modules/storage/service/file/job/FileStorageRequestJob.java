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

import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.file.CommonFileUtils;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storage.domain.plugin.FileStorageWorkingSubset;
import fr.cnes.regards.modules.storage.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storage.service.file.request.FileStorageRequestService;

/**
 * Storage of file references job. This jobs is scheduled to store a bundle of file reference,
 * thanks to {@link FileStorageRequest}s.<br/>
 * The storage jobs are used to storage files on a specific storage location.
 *
 * @author Sébastien Binda
 *
 */
public class FileStorageRequestJob extends AbstractJob<Void> {

    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(FileStorageRequestJob.class);

    /**
     * JOB Parameter key for the storage plugin configuration identifier to use for the storage.
     */
    public static final String DATA_STORAGE_CONF_BUSINESS_ID = "dscbid";

    /**
     * JOB Parameter key for the Working subset of {@link FileStorageRequest} to handle for storage.
     */
    public static final String WORKING_SUB_SET = "wss";

    @Autowired
    private FileStorageRequestService fileStorageReqService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    private FileStorageWorkingSubset workingSubset;

    private String plgBusinessId;

    private int nbRequestToHandle = 0;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        plgBusinessId = parameters.get(DATA_STORAGE_CONF_BUSINESS_ID).getValue();
        workingSubset = parameters.get(WORKING_SUB_SET).getValue();
        nbRequestToHandle = workingSubset.getFileReferenceRequests().size();
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        // Initiate the job progress manager
        FileStorageJobProgressManager progressManager = new FileStorageJobProgressManager(fileStorageReqService, this);

        nbRequestToHandle = workingSubset.getFileReferenceRequests().size();
        logger.debug("[STORAGE JOB] Runing storage job for {} storage requests", nbRequestToHandle);
        // Calculates if needed image dimensions
        workingSubset.getFileReferenceRequests().forEach(FileStorageRequestJob::calculateImageDimension);

        // lets instantiate the plugin to use
        IStorageLocation storagePlugin;
        String errorCause = null;
        try {
            storagePlugin = pluginService.getPlugin(plgBusinessId);
            storagePlugin.store(workingSubset, progressManager);
        } catch (Exception e) {
            errorCause = String.format("Storage job failed cause : %s", e.getMessage());
            // throwing new runtime allows us to make the job fail.
            throw new JobRuntimeException(e);
        } finally {
            // Publish event for all not handled files
            for (FileStorageRequest req : workingSubset.getFileReferenceRequests()) {
                if (!progressManager.isHandled(req)) {
                    progressManager.storageFailed(req,
                                                  String.format("File %s (checksum: %s) not handled by storage job. %s",
                                                                req.getMetaInfo().getFileName(),
                                                                req.getMetaInfo().getChecksum(), errorCause));
                }
            }
            progressManager.bulkSave();
            logger.info("[STORAGE JOB] storage job handled in {}ms for {} storage requests",
                        System.currentTimeMillis() - start, nbRequestToHandle);
        }
    }

    @Override
    public int getCompletionCount() {
        return nbRequestToHandle > 0 ? nbRequestToHandle : super.getCompletionCount();
    }

    /**
     * Calculate dimensions of the given file to store.<br>
     * This methods do the calculation only if the mimeType of the file is compatible with <image/*> type.
     * @param fileRefRequest to calculate for image dimension
     */
    public static void calculateImageDimension(FileStorageRequest fileRefRequest) {
        try {
            if (((fileRefRequest.getMetaInfo().getHeight() == null)
                    || (fileRefRequest.getMetaInfo().getWidth() == null))
                    && fileRefRequest.getMetaInfo().getMimeType().isCompatibleWith(MediaType.valueOf("image/*"))) {
                URL localUrl = new URL(fileRefRequest.getOriginUrl());
                if (localUrl.getProtocol().equals("file")) {
                    Path filePath = Paths.get(localUrl.toURI().getPath());
                    if (Files.isReadable(filePath)) {
                        Dimension dimension = CommonFileUtils.getImageDimension(filePath.toFile());
                        fileRefRequest.getMetaInfo().setHeight(((Number) dimension.getHeight()).intValue());
                        fileRefRequest.getMetaInfo().setWidth(((Number) dimension.getWidth()).intValue());
                    } else {
                        STATIC_LOGGER
                                .warn("Error calculating image file height/width. Cause : File {} is not accessible.",
                                      fileRefRequest.getOriginUrl());
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            STATIC_LOGGER.warn(String.format("Error calculating image file height/width. Cause : %s", e.getMessage()),
                                e);
        }
    }

}
