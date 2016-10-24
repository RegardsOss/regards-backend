/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.service;

import fr.cnes.regards.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.modules.jobs.domain.JobInfo;

/**
 *
 */
public class JobInfoService implements IJobInfoService {

    /**
     * DAO autowired by Spring
     */
    private final IJobInfoRepository jobInfoRepository;

    /**
     *
     * @param pCollectionRepository
     */
    public JobInfoService(final IJobInfoRepository pJobInfoRepository) {
        super();
        jobInfoRepository = pJobInfoRepository;
    }

    @Override
    public JobInfo createJobInfo(final JobInfo pJobInfo) {
        return jobInfoRepository.save(pJobInfo);
    }

    @Override
    public JobInfo updateJobInfo(final JobInfo pJobInfo) {
        return jobInfoRepository.save(pJobInfo);
    }

}
