package fr.cnes.regards.modules.storage.service.job;

import java.util.Map;
import java.util.Set;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.IWorkingSubset;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class UpdateDataFilesJob extends AbstractStoreFilesJob {

    public static final String OLD_DATA_FILES_PARAMETER_NAME = "old_data_files";

    @Override
    protected void doRun(Map<String, JobParameter> parameterMap) {
        // lets instantiate the plugin to use
        PluginConfiguration confToUse = parameterMap.get(PLUGIN_TO_USE_PARAMETER_NAME).getValue();
        try {
            IDataStorage storagePlugin = pluginService.getPlugin(confToUse.getId());
            // now that we have the plugin instance, lets retrieve the aip from the job parameters
            IWorkingSubset workingSubset = parameterMap.get(WORKING_SUB_SET_PARAMETER_NAME).getValue();
            // to do updates, first we delete the old one ...
            Set<DataFile> oldDataFiles = parameterMap.get(OLD_DATA_FILES_PARAMETER_NAME).getValue();
            storagePlugin.delete(oldDataFiles, progressManager);
            // ... then we store the new ones
            storagePlugin.store(workingSubset, true, progressManager);
        } catch (ModuleException e) {
            //throwing new runtime allows us to make the job fail.
            throw new RuntimeException(e);
        }
    }
}
