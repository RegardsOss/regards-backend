/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.fallback;

import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.jobs.client.JobInfoClient;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.jobs.domain.Output;

/**
 * Hystrix fallback for Feign {@link JobInfoClient}. This default implementation is executed when the circuit is open or
 * there is an error.<br>
 * To enable this fallback, set the fallback attribute to this class name in {@link JobInfoClient}.
 *
 */
@Component
public class JobInfoFallback implements JobInfoClient {

    @Override
    public HttpEntity<List<Resource<JobInfo>>> retrieveJobs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<List<Resource<JobInfo>>> retrieveJobsByState(final JobStatus pState) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Resource<JobInfo>> retrieveJobInfo(final Long pJobInfoId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Resource<JobInfo>> stopJob(final Long pJobInfoId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<List<Output>> getJobResults(final Long pJobInfoId) {
        // TODO Auto-generated method stub
        return null;
    }

}
