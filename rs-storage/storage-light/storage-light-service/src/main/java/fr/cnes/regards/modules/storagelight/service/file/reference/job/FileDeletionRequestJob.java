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
import fr.cnes.regards.modules.storagelight.domain.database.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.plugin.FileDeletionWorkingSubset;
import fr.cnes.regards.modules.storagelight.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileDeletionRequestService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.FileRefEventPublisher;

/**
 * @author Sébastien Binda
 *
 */
public class FileDeletionRequestJob extends AbstractJob<Void> {

    public static final String DATA_STORAGE_CONF_ID = "dscId";

    public static final String WORKING_SUB_SET = "wss";

    @Autowired
    private FileReferenceService fileReferenceService;

    @Autowired
    private FileDeletionRequestService fileDeletionRequestService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    protected FileRefEventPublisher publisher;

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
        FileDeletionJobProgressManager progressManager = new FileDeletionJobProgressManager(fileReferenceService,
                fileDeletionRequestService, publisher, this);
        // lets instantiate the plugin to use
        Long confIdToUse = parameters.get(DATA_STORAGE_CONF_ID).getValue();
        FileDeletionWorkingSubset workingSubset = parameters.get(WORKING_SUB_SET).getValue();
        try {
            IDataStorage storagePlugin = pluginService.getPlugin(confIdToUse);
            storagePlugin.delete(workingSubset, progressManager);
        } catch (Exception e) {
            // Publish event for all not handled files
            for (FileDeletionRequest req : workingSubset.getFileDeletionRequests()) {
                if (!progressManager.isHandled(req)) {
                    progressManager.deletionFailed(req, String
                            .format("File %s (checksum: %s) not handled by deletion job. Deletion job failed cause : %s",
                                    req.getFileReference().getMetaInfo().getFileName(),
                                    req.getFileReference().getMetaInfo().getChecksum(), e.getMessage()));
                }
            }
            // throwing new runtime allows us to make the job fail.
            throw new JobRuntimeException(e);
        }
    }
}
