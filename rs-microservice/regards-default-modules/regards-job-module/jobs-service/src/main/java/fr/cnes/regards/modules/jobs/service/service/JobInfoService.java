/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.jpa.utils.IterableUtils;
import fr.cnes.regards.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.jobs.service.communication.INewJobPublisher;
import fr.cnes.regards.modules.jobs.service.communication.IStoppingJobPublisher;

/**
 * @author Léo Mieulet
 */
@Service
public class JobInfoService implements IJobInfoService {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JobInfoService.class);

    /**
     * {@link JobInfo} JPA Repository
     */
    private final IJobInfoRepository jobInfoRepository;

    /**
     * Allows to publish a new job event
     */
    private final INewJobPublisher newJobPublisher;

    /**
     * Allows to publish a stopping job event
     */
    private final IStoppingJobPublisher stoppingJobPublisher;

    /**
     *
     * @param pJobInfoRepository
     *            JobInfo Repository
     * @param pNewJobPublisher
     *            NewJob Publisher
     * @param pStoppingJobPublisher
     *            StoppingJob Publisher
     */
    public JobInfoService(final IJobInfoRepository pJobInfoRepository, final INewJobPublisher pNewJobPublisher,
            final IStoppingJobPublisher pStoppingJobPublisher) {
        super();
        jobInfoRepository = pJobInfoRepository;
        newJobPublisher = pNewJobPublisher;
        stoppingJobPublisher = pStoppingJobPublisher;
    }

    @Override
    public JobInfo createJobInfo(final JobInfo pJobInfo) {
        final JobInfo jobInfo = jobInfoRepository.save(pJobInfo);
        if (jobInfo != null) {
            try {
                newJobPublisher.sendJob(jobInfo.getId());
            } catch (final RabbitMQVhostException e) {
                jobInfo.getStatus().setJobStatus(JobStatus.FAILED);
                jobInfoRepository.save(jobInfo);
                LOG.error("Failed to submit the new jobInfo to rabbit", e);
            }
        }
        return jobInfo;
    }

    @Override
    public JobInfo stopJob(final Long pJobInfoId) {
        final JobInfo jobInfo = jobInfoRepository.findOne(pJobInfoId);
        if (jobInfo != null) {
            try {
                stoppingJobPublisher.send(pJobInfoId);
            } catch (final RabbitMQVhostException e) {
                LOG.error("Failed to stop the job, Rabbit MQ error:", e);
            }
        }
        return jobInfo;
    }

    @Override
    public List<JobInfo> retrieveJobInfoList() {
        return IterableUtils.toList(jobInfoRepository.findAll());
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
