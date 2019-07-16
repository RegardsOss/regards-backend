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
package fr.cnes.regards.modules.storagelight.service.jobs;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storagelight.domain.plugin.FileDeletionWorkingSubset;
import fr.cnes.regards.modules.storagelight.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storagelight.service.FileDeletionRequestService;
import fr.cnes.regards.modules.storagelight.service.FileReferenceService;

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
    protected IPublisher publisher;

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
                fileDeletionRequestService, publisher, this, runtimeTenantResolver);
        // lets instantiate the plugin to use
        Long confIdToUse = parameters.get(DATA_STORAGE_CONF_ID).getValue();
        FileDeletionWorkingSubset workingSubset = parameters.get(WORKING_SUB_SET).getValue();
        try {
            IDataStorage storagePlugin = pluginService.getPlugin(confIdToUse);
            storagePlugin.delete(workingSubset, progressManager);
        } catch (ModuleException | PluginUtilsRuntimeException | NotAvailablePluginConfigurationException e) {
            // throwing new runtime allows us to make the job fail.
            throw new JobRuntimeException(e);
        }
    }
}
