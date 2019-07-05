package fr.cnes.regards.modules.storage.service.job;

import java.net.URL;
import java.util.Map;
import java.util.Optional;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class DeleteDataFilesJob extends AbstractStoreFilesJob {

    /**
     * Failure causes message format
     */
    static {
        failureCauses = "Deletion failed due to the following reasons: %s";
    }

    @Override
    protected void doRun(Map<String, JobParameter> parameterMap) {
        Long confIdToUse = parameterMap.get(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME).getValue();
        try {
            IDataStorage<IWorkingSubset> storagePlugin = pluginService.getPlugin(confIdToUse);
            // now that we have the plugin instance, lets retrieve the aip from the job parameters and ask the plugin to do the deletion
            IWorkingSubset workingSubset = parameterMap.get(WORKING_SUB_SET_PARAMETER_NAME).getValue();
            try {
                storagePlugin.safeDelete(workingSubset, progressManager);
            } catch (IllegalStateException e) {
                workingSubset.getDataFiles()
                        .forEach(file -> progressManager.deletionFailed(file, Optional.empty(), e.getMessage()));
                throw new IllegalStateException(
                        String.format("Could not delete data for plugin configuration with label: %s",
                                      pluginService.getPluginConfiguration(confIdToUse).getLabel()),
                        e);
            }
        } catch (ModuleException | PluginUtilsRuntimeException | NotAvailablePluginConfigurationException e) {
            //throwing new runtime allows us to make the job fail.
            throw new RsRuntimeException(e);
        }
    }

    @Override
    protected void handleNotHandledDataFile(StorageDataFile notHandled, Optional<URL> notHandledUrl) {
        progressManager.deletionFailed(notHandled, notHandledUrl, NOT_HANDLED_MSG);
    }
}
