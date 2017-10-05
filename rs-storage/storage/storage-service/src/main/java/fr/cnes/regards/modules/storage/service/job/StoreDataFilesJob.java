/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.job;

import java.util.Map;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class StoreDataFilesJob extends AbstractStoreFilesJob {

    @Override
    public void doRun(Map<String, JobParameter> parameterMap) {
        storeFile(parameterMap, false);
    }
}
