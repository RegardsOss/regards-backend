/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.job;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.modules.storage.domain.StorageException;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;
import fr.cnes.regards.modules.storage.domain.plugin.WorkingSubsetWrapper;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class UpdateDataFilesJob extends AbstractStoreFilesJob {

    /**
     * Job Parameter name for the old data files
     */
    public static final String OLD_DATA_FILES_PARAMETER_NAME = "old_data_files";

    @Override
    protected void checkParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        super.checkParameters(parameters);
        // lets see if old data files has been given or not
        JobParameter oldDataFiles;
        if (((oldDataFiles = parameters.get(OLD_DATA_FILES_PARAMETER_NAME)) == null) || !(oldDataFiles
                .getValue() instanceof StorageDataFile[])) {
            JobParameterMissingException e = new JobParameterMissingException(String.format(PARAMETER_MISSING,
                                                                                            this.getClass().getName(),
                                                                                            StorageDataFile[].class
                                                                                                    .getName(),
                                                                                            OLD_DATA_FILES_PARAMETER_NAME));
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void doRun(Map<String, JobParameter> parameterMap) {
        // lets instantiate the plugin to use
        Long confIdToUse = parameterMap.get(PLUGIN_TO_USE_PARAMETER_NAME).getValue();
        try {
            IDataStorage storagePlugin = pluginService.getPlugin(confIdToUse);
            // now that we have the plugin instance, lets retrieve the aip from the job parameters
            IWorkingSubset workingSubset = parameterMap.get(WORKING_SUB_SET_PARAMETER_NAME).getValue();
            // to do updates, first we store the new one ...
            storagePlugin.store(workingSubset, true, progressManager);
            // manual call to after run here to globalize the code.
            afterRun();
            // ... then we delete the old ones, that has been updated:
            // first lets get the all the data files that should have been updated
            Set<StorageDataFile> oldDataFiles = Sets
                    .newHashSet((StorageDataFile[]) parameterMap.get(OLD_DATA_FILES_PARAMETER_NAME).getValue());
            // then lets remove the ones that failed
            oldDataFiles.removeAll(progressManager.getFailedDataFile());
            // now we have the old data files that have been replaced
            WorkingSubsetWrapper<IWorkingSubset> subSetsToDelete = storagePlugin
                    .prepare(oldDataFiles, DataStorageAccessModeEnum.DELETION_MODE);
            for(Map.Entry<StorageDataFile, String> entry : subSetsToDelete.getRejectedDataFiles().entrySet()) {
                progressManager.deletionFailed(entry.getKey(), entry.getValue());
            }
            for (IWorkingSubset toDelete : subSetsToDelete.getWorkingSubSets()) {
                storagePlugin.delete(toDelete, progressManager);
            }
            if (progressManager.isProcessError()) {
                throw new StorageException("Update process failed");
            }
        } catch (ModuleException e) {
            // throwing new runtime allows us to make the job fail.
            throw new JobRuntimeException(e);
        }
    }

    @Override
    protected void handleNotHandledDataFile(StorageDataFile notHandled) {
        progressManager.storageFailed(notHandled, NOT_HANDLED_MSG);
    }

    @Override
    public int getCompletionCount() {
        return ((IWorkingSubset) parameters.get(WORKING_SUB_SET_PARAMETER_NAME).getValue()).getDataFiles().size()
                + ((StorageDataFile[]) parameters.get(OLD_DATA_FILES_PARAMETER_NAME).getValue()).length;
    }
}
