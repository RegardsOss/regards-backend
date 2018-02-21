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
 * This job is executed by JobService while its scheduling is handled by an IAIPService. This means that the job context is prepared by an IAIPService.
 *
 * This job aims to storeAndCreate the metadata of an AIP.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class StoreMetadataFilesJob extends AbstractStoreFilesJob {

    @Override
    protected void doRun(Map<String, JobParameter> parameterMap) {
        storeFile(parameterMap, true);
    }

    @Override
    protected void handleNotHandledDataFile(StorageDataFile notHandled) {
        progressManager.storageFailed(notHandled, NOT_HANDLED_MSG);
    }

    @Override
    protected void handleWorkspaceException(IOException e) throws JobWorkspaceException {
        StorageJobProgressManager progressManager = new StorageJobProgressManager(publisher, this);
        IWorkingSubset workingSubset = parameters.get(WORKING_SUB_SET_PARAMETER_NAME).getValue();
        workingSubset.getDataFiles().forEach(file -> progressManager.storageFailed(file, e.toString()));
        super.handleWorkspaceException(e);
    }
}
