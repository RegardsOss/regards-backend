/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.stub;

import java.util.List;

import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.jobs.service.service.IJobInfoService;

/**
 *
 */
@Component
public class JobInfoServiceStub implements IJobInfoService {

    @Override
    public JobInfo createJobInfo(final JobInfo pJobInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JobInfo updateJobInfo(final JobInfo pJobInfo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<JobInfo> retrieveJobInfoList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<JobInfo> retrieveJobInfoListByState(final JobStatus pState) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JobInfo retrieveJobInfoById(final Long pJobInfoId) {
        // TODO Auto-generated method stub
        return null;
    }

}
