/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.service;

import java.util.List;

import fr.cnes.regards.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;

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
    public List<JobInfo> retrieveJobInfoList() {
        return jobInfoRepository.findAll();
    }

    @Override
    public List<JobInfo> retrieveJobInfoListByState(final JobStatus pState) {
        return jobInfoRepository.findAllByStatusStatus(pState);
    }

    @Override
    public JobInfo retrieveJobInfoById(final Long pJobInfoId) {
        return jobInfoRepository.findOne(pJobInfoId);
    }

    @Override
    public JobInfo save(final JobInfo pJobInfo) {
        return jobInfoRepository.save(pJobInfo);

    }

}
