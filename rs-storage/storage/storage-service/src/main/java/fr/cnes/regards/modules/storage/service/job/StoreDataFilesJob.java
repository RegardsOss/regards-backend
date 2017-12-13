/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.job;

import java.util.Map;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class StoreDataFilesJob extends AbstractStoreFilesJob {

    @Override
    public void doRun(Map<String, JobParameter> parameterMap) {
        storeFile(parameterMap, false);
    }

    @Override
    protected void handleNotHandledDataFile(StorageDataFile notHandled) {
        progressManager.storageFailed(notHandled, NOT_HANDLED_MSG);
    }
}
