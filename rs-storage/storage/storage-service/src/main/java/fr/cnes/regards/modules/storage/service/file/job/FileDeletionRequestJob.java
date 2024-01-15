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
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.fileaccess.plugin.domain.FileDeletionWorkingSubset;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IStorageLocation;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileDeletionRequestDto;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.service.file.request.FileDeletionRequestService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Deletion of file references job. This jobs his scheduled to delete a bundle of file reference,
 * thanks to {@link FileDeletionRequest}s.<br/>
 * The deletion jobs are used to delete files on a specific storage location.
 *
 * @author Sébastien Binda
 */
public class FileDeletionRequestJob extends AbstractJob<Void> {

    /**
     * JOB Parameter key for the storage plugin configuration identifier to use for the deletion.
     */
    public static final String DATA_STORAGE_CONF_BUSINESS_ID = "dscbid";

    /**
     * JOB Parameter key for the Working subset of {@link FileDeletionRequest} to handle for deletion.
     */
    public static final String WORKING_SUB_SET = "wss";

    @Autowired
    private FileDeletionRequestService fileDeletionRequestService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * The job parameters as a map
     */
    protected Map<String, JobParameter> parameters;

    private int nbRequestToHandle = 0;

    @Override
    public void setParameters(Map<String, JobParameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        // Initiate the job progress manager
        FileDeletionJobProgressManager progressManager = new FileDeletionJobProgressManager(fileDeletionRequestService,
                                                                                            storageLocationService,
                                                                                            this);
        // lets instantiate the plugin to use
        String plgBusinessId = parameters.get(DATA_STORAGE_CONF_BUSINESS_ID).getValue();
        FileDeletionWorkingSubset workingSubset = parameters.get(WORKING_SUB_SET).getValue();
        nbRequestToHandle = workingSubset.getFileDeletionRequests().size();
        logger.debug("[DELETION JOB] Running deletion job for {} deletion requests", nbRequestToHandle);
        String errorCause = null;
        try {
            IStorageLocation storagePlugin = pluginService.getPlugin(plgBusinessId);
            if (storagePlugin.allowPhysicalDeletion()) {
                // If deletion is allowed ask plugin for files deletion.
                storagePlugin.delete(workingSubset, progressManager);
            } else {
                // If plugin indicates that the physical deletion is not allowed, only delete file reference in database.
                workingSubset.getFileDeletionRequests().forEach(progressManager::deletionSucceed);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            errorCause = String.format("Deletion job failed cause : %s", e.getMessage());
        } finally {
            // Publish event for all not handled files
            for (FileDeletionRequestDto req : workingSubset.getFileDeletionRequests()) {
                if (!progressManager.isHandled(FileDeletionRequest.fromDto(req))) {
                    progressManager.deletionFailed(req,
                                                   String.format(
                                                       "File %s (checksum: %s) not handled by deletion job. Deletion job failed cause : %s",
                                                       req.getFileReference().getMetaInfo().getFileName(),
                                                       req.getFileReference().getMetaInfo().getChecksum(),
                                                       errorCause));
                }
            }
            if (nbRequestToHandle > 0) {
                logger.info("[DELETION JOB] Deletion job handled in {}ms for {} deletion requests",
                            System.currentTimeMillis() - start,
                            nbRequestToHandle);
            }
        }
    }

    @Override
    public int getCompletionCount() {
        return nbRequestToHandle > 0 ? nbRequestToHandle : super.getCompletionCount();
    }
}
