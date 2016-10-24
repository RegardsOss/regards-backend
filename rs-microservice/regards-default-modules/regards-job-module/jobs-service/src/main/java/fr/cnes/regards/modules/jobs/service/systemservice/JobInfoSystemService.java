/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.systemservice;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.modules.jobs.domain.JobInfo;

/**
 *
 */
public class JobInfoSystemService implements IJobInfoSystemService {

    /**
     * DAO autowired by Spring
     */
    @Autowired
    private final IJobInfoRepository jobInfoRepository;

    /**
     * @param pJobInfoRepository
     */
    public JobInfoSystemService(final IJobInfoRepository pJobInfoRepository) {
        super();
        jobInfoRepository = pJobInfoRepository;
    }

    @Override
    public JobInfo findJobInfo(final String tenantId, final Long jobInfoId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JobInfo updateJobInfo(final String tenantId, final JobInfo pJobInfo) {
        // TODO Auto-generated method stub
        return null;
    }

}
