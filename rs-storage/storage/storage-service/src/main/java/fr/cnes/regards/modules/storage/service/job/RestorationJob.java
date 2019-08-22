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
package fr.cnes.regards.modules.storage.service.job;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;

/**
 * Restoration job implementation. It allows to restore DataFiles
 * @author Sylvain VISSIERE-GUERINET
 */
public class RestorationJob extends AbstractStoreFilesJob {

    /**
     * Failure causes message format
     */
    static {
        failureCauses = "Restoration failed due to the following reasons: %s";
    }

    /**
     * Job parameter name of destination path
     */
    public static final String DESTINATION_PATH_PARAMETER_NAME = "destination";

    @Override
    protected void checkParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        super.checkParameters(parameters);
        // lets see if destination has been given or not
        JobParameter destinationPath = parameters.get(DESTINATION_PATH_PARAMETER_NAME);
        if ((destinationPath == null) || !(destinationPath.getValue() instanceof Path)) {
            JobParameterMissingException e = new JobParameterMissingException(
                    String.format(PARAMETER_MISSING, this.getClass().getName(), Path.class.getName(),
                                  DESTINATION_PATH_PARAMETER_NAME));
            LOGGER.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected void doRun(Map<String, JobParameter> parameterMap) {
        // lets instantiate the plugin to use
        Long confIdToUse = parameterMap.get(PLUGIN_TO_USE_PARAMETER_NAME).getValue();
        Path destination = parameterMap.get(DESTINATION_PATH_PARAMETER_NAME).getValue();
        try {
            if (!Files.exists(destination)) {
                Files.createDirectories(destination);
            }
            INearlineDataStorage<IWorkingSubset> storagePlugin = pluginService.getPlugin(confIdToUse);
            // now that we have the plugin instance, lets retrieve the aip from the job parameters and ask the plugin to
            // do the storage
            IWorkingSubset workingSubset = parameterMap.get(WORKING_SUB_SET_PARAMETER_NAME).getValue();
            LOGGER.debug("Plugin {} - Running restoration for {}files", storagePlugin.getClass().getName(),
                         workingSubset.getDataFiles().size());
            storagePlugin.retrieve(workingSubset, destination, progressManager);
        } catch (ModuleException | PluginUtilsRuntimeException | IOException
                | NotAvailablePluginConfigurationException e) {
            // throwing new runtime allows us to make the job fail.
            throw new RsRuntimeException(e);
        }
    }

    @Override
    protected void handleNotHandledDataFile(StorageDataFile notHandled, Optional<URL> notHandledUrl) {
        progressManager.restoreFailed(notHandled, notHandledUrl, NOT_HANDLED_MSG);
    }

}