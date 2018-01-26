package fr.cnes.regards.modules.storage.service.job;

import java.util.Map;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class DeleteDataFilesJob extends AbstractStoreFilesJob {

    @Override
    protected void doRun(Map<String, JobParameter> parameterMap) {
        Long confIdToUse = parameterMap.get(AbstractStoreFilesJob.PLUGIN_TO_USE_PARAMETER_NAME).getValue();
        try {
            IDataStorage storagePlugin = pluginService.getPlugin(confIdToUse);
            // now that we have the plugin instance, lets retrieve the aip from the job parameters and ask the plugin to do the deletion
            IWorkingSubset workingSubset = parameterMap.get(WORKING_SUB_SET_PARAMETER_NAME).getValue();
            storagePlugin.safeDelete(workingSubset.getDataFiles(), progressManager);
        } catch (ModuleException e) {
            //throwing new runtime allows us to make the job fail.
            throw new RsRuntimeException(e);
        }
    }

    @Override
    protected void handleNotHandledDataFile(StorageDataFile notHandled) {
        progressManager.deletionFailed(notHandled, NOT_HANDLED_MSG);
    }
}
