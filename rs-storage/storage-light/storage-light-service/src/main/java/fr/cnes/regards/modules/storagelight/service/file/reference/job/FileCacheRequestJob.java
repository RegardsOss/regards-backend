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

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.plugin.FileRestorationWorkingSubset;
import fr.cnes.regards.modules.storagelight.domain.plugin.INearlineStorageLocation;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileCacheRequestService;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.FileReferenceEventPublisher;

/**
 * Storage of file references job. This jobs is scheduled to store a bundle of file reference,
 * thanks to {@link FileStorageRequest}s.<br/>
 * The storage jobs are used to storage files on a specific storage location.
 *
 * @author Sébastien Binda
 *
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
    private FileReferenceEventPublisher publisher;

    @Autowired
    private FileCacheRequestService fileCacheRequestService;

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
        FileCacheJobProgressManager progressManager = new FileCacheJobProgressManager(fileCacheRequestService,
                publisher, this);
        // lets instantiate the plugin to use
        String plgBusinessId = parameters.get(DATA_STORAGE_CONF_BUSINESS_ID).getValue();
        FileRestorationWorkingSubset workingSubset = parameters.get(WORKING_SUB_SET).getValue();
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
                                                                fileRef.getMetaInfo().getChecksum(), errorCause));
                }
            }
        }
    }
}
