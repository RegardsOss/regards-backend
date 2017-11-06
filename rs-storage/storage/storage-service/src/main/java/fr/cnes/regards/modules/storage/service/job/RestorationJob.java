/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.job;

import java.nio.file.Path;
import java.util.Map;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.datastorage.INearlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.IWorkingSubset;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class RestorationJob extends AbstractStoreFilesJob {

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
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected void doRun(Map<String, JobParameter> parameterMap) {
        // lets instantiate the plugin to use
        PluginConfiguration confToUse = parameterMap.get(PLUGIN_TO_USE_PARAMETER_NAME).getValue();
        Path destination = parameterMap.get(DESTINATION_PATH_PARAMETER_NAME).getValue();
        try {
            INearlineDataStorage<IWorkingSubset> storagePlugin = pluginService.getPlugin(confToUse.getId());
            // now that we have the plugin instance, lets retrieve the aip from the job parameters and ask the plugin to
            // do the storage
            IWorkingSubset workingSubset = parameterMap.get(WORKING_SUB_SET_PARAMETER_NAME).getValue();
            // before storage on file system, lets update the DataFiles by setting which data storage is used to storeAndCreate
            // them.
            for (DataFile data : workingSubset.getDataFiles()) {
                data.setDataStorageUsed(confToUse);
            }
            logger.debug("Plugin {} - Running restoration for {}files", storagePlugin.getClass().getName(),
                         workingSubset.getDataFiles().size());
            storagePlugin.retrieve(workingSubset, destination, progressManager);
        } catch (ModuleException e) {
            // throwing new runtime allows us to make the job fail.
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void handleNotHandledDataFile(DataFile notHandled) {
        progressManager.restoreFailed(notHandled, NOT_HANDLED_MSG);
    }

}