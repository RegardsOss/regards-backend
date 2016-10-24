/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.stub;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.jobs.service.systemservice.IJobInfoSystemService;

/**
 *
 */
@Service
public class JobInfoSystemServiceStub implements IJobInfoSystemService {

    @Override
    public JobInfo findJobInfo(final String pTenantName, final Long pJobInfoId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JobInfo updateJobInfo(final String pTenantId, final JobInfo pJobInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JobInfo updateJobInfoToDone(final Long pJobInfoId, final JobStatus pJobStatus, final String pTenantName) {
        // TODO Auto-generated method stub
        return null;
    }

}
