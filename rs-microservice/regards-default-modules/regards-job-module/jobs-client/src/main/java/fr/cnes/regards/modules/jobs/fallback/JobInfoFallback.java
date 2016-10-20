/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.fallback;

import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.jobs.client.JobInfoClient;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.Output;

/**
 * Hystrix fallback for Feign {@link JobInfoClient}. This default implementation is executed when the circuit is open or
 * there is an error.<br>
 * To enable this fallback, set the fallback attribute to this class name in {@link JobInfoClient}.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class JobInfoFallback implements JobInfoClient {

    @Override
    public HttpEntity<List<JobInfo>> retrieveJobs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<List<JobInfo>> retrieveJobsByState(final String pState) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<JobInfo> retrieveEmail(final Long pId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<List<Output>> getJobResults(final Long pId) {
        // TODO Auto-generated method stub
        return null;
    }

}
