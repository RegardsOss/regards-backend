/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.job;

import java.util.Map;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;

/**
 * This job is executed by JobService while its scheduling is handled by an IAIPService. This means that the job context is prepared by an IAIPService.
 *
 * This job aims to store the metadata of an AIP.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class StoreMetadataFilesJob extends AbstractStoreFilesJob {

    /**
     * @param parameterMap parsed parameters
     */
    @Override
    protected void doStore(Map<String, JobParameter> parameterMap) {
        storeFile(parameterMap, true);
    }
}
