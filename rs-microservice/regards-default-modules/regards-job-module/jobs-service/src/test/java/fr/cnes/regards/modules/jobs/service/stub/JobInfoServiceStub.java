/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.stub;

import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.jobs.domain.JobInfo;
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

}
