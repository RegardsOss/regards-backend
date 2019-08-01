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

import java.awt.Dimension;
import java.io.IOException;
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
import fr.cnes.regards.modules.storagelight.domain.database.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.plugin.FileStorageWorkingSubset;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileStorageRequestService;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.FileRefEventPublisher;

/**
 * Storage of file references job. This jobs is scheduled to store a bundle of file reference,
 * thanks to {@link FileStorageRequest}s.<br/>
 * The storage jobs are used to storage files on a specific storage location.
 *
 * @author Sébastien Binda
 *
 */
public class FileStorageRequestJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageRequestJob.class);

    /**
     * JOB Parameter key for the storage plugin configuration identifier to use for the storage.
     */
    public static final String DATA_STORAGE_CONF_ID = "dscId";

    /**
     * JOB Parameter key for the Working subset of {@link FileStorageRequest} to handle for storage.
     */
    public static final String WORKING_SUB_SET = "wss";

    @Autowired
    private FileReferenceService fileReferenceService;

    @Autowired
    private FileStorageRequestService fileRefRequestService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private FileRefEventPublisher publisher;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * The job parameters as a map
     */
    protected Map<String, JobParameter> parameters;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        this.parameters = parameters;
    }

    @Override
    public void run() {
        // Initiate the job progress manager
        FileStorageJobProgressManager progressManager = new FileStorageJobProgressManager(fileReferenceService,
                fileRefRequestService, publisher, this);
        // lets instantiate the plugin to use
        Long confIdToUse = parameters.get(DATA_STORAGE_CONF_ID).getValue();
        FileStorageWorkingSubset workingSubset = parameters.get(WORKING_SUB_SET).getValue();
        workingSubset.getFileReferenceRequests().forEach(this::calculateImageDimension);
        IStorageLocation storagePlugin;
        String errorCause = null;
        try {
            storagePlugin = pluginService.getPlugin(confIdToUse);
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
        }
    }

    private void calculateImageDimension(FileStorageRequest fileRefRequest) {
        try {
            if (((fileRefRequest.getMetaInfo().getHeight() == null)
                    || (fileRefRequest.getMetaInfo().getWidth() == null))
                    && fileRefRequest.getMetaInfo().getMimeType().isCompatibleWith(MediaType.valueOf("image/*"))) {
                URL localUrl = new URL(fileRefRequest.getOrigin().getUrl());
                if (localUrl.getProtocol().equals("file")) {
                    Path filePath = Paths.get(localUrl.getPath());
                    if (Files.isReadable(filePath)) {
                        Dimension dimension = CommonFileUtils.getImageDimension(filePath.toFile());
                        fileRefRequest.getMetaInfo().setHeight(((Number) dimension.getHeight()).intValue());
                        fileRefRequest.getMetaInfo().setWidth(((Number) dimension.getWidth()).intValue());
                    } else {
                        LOGGER.warn("Error calculating image file height/width. Cause : File %s is not accessible.",
                                    fileRefRequest.getOrigin().toString());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn(String.format("Error calculating image file height/width. Cause : %s", e.getMessage()), e);
        }
    }

}
