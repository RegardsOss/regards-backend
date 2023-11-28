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

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.domain.plugin.FileRestorationWorkingSubset;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineStorageLocation;
import fr.cnes.regards.modules.storage.service.file.request.FileCacheRequestService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Storage of file references job. This jobs is scheduled to store a bundle of file reference,
 * thanks to {@link FileStorageRequestAggregation}s.<br/>
 * The storage jobs are used to storage files on a specific storage location.
 *
 * @author Sébastien Binda
 */
public class FileCacheRequestJob extends AbstractJob<Void> {

    /**
     * JOB Parameter key for the storage plugin configuration identifier to use for the restoration.
     */
    public static final String DATA_STORAGE_CONF_BUSINESS_ID = "dscbid";

    /**
     * JOB Parameter key for the Working subset of {@link FileCacheRequest} to handle for storage.
     */
    public static final String WORKING_SUB_SET = "wss";

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private FileCacheRequestService fileCacheRequestService;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    private int nbRequestToHandle = 0;

    private String plgBusinessId;

    private FileRestorationWorkingSubset workingSubset;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        // lets instantiate the plugin to use
        plgBusinessId = parameters.get(DATA_STORAGE_CONF_BUSINESS_ID).getValue();
        workingSubset = parameters.get(WORKING_SUB_SET).getValue();
        nbRequestToHandle = workingSubset.getFileRestorationRequests().size();
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        // Initiate the job progress manager
        FileCacheJobProgressManager progressManager = new FileCacheJobProgressManager(fileCacheRequestService, this);
        logger.debug("[AVAILABILITY JOB] Runing availability job for {} cache requests", nbRequestToHandle);
        INearlineStorageLocation storagePlugin;
        String errorCause = null;
        try {
            storagePlugin = pluginService.getPlugin(plgBusinessId);
            storagePlugin.retrieve(workingSubset, progressManager);
        } catch (Exception e) {
            // throwing new runtime allows us to make the job fail.
            errorCause = String.format("Storage job failed cause : %s", e.getMessage());
            throw new JobRuntimeException(e);
        } finally {
            // Publish event for all not handled files
            for (FileCacheRequest req : workingSubset.getFileRestorationRequests()) {
                if (!progressManager.isHandled(req)) {
                    FileReference fileRef = req.getFileReference();
                    progressManager.restoreFailed(req,
                                                  String.format("File %s (checksum: %s) not handled by storage job. %s",
                                                                fileRef.getMetaInfo().getFileName(),
                                                                fileRef.getMetaInfo().getChecksum(),
                                                                errorCause));
                }
            }
            logger.debug("[AVAILABILITY JOB] Availability job handled in {} ms for {} cache requests",
                         System.currentTimeMillis() - start,
                         nbRequestToHandle);
        }
    }

    @Override
    public int getCompletionCount() {
        return nbRequestToHandle > 0 ? nbRequestToHandle : super.getCompletionCount();
    }
}
