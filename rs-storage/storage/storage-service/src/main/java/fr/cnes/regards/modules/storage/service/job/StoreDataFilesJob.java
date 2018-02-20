/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.job;

import java.io.IOException;
import java.util.Map;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobWorkspaceException;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IWorkingSubset;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class StoreDataFilesJob extends AbstractStoreFilesJob {

    @Override
    protected void handleWorkspaceException(IOException e) throws JobWorkspaceException {
        Long storageConfId = parameters.get(PLUGIN_TO_USE_PARAMETER_NAME).getValue();
        StorageJobProgressManager progressManager = new StorageJobProgressManager(publisher, this, storageConfId);
        IWorkingSubset workingSubset = parameters.get(WORKING_SUB_SET_PARAMETER_NAME).getValue();
        workingSubset.getDataFiles().forEach(file -> progressManager.storageFailed(file, e.toString()));
        super.handleWorkspaceException(e);
    }

    @Override
    public void doRun(Map<String, JobParameter> parameterMap) {
        storeFile(parameterMap, false);
    }

    @Override
    protected void handleNotHandledDataFile(StorageDataFile notHandled) {
        progressManager.storageFailed(notHandled, NOT_HANDLED_MSG);
    }
}
