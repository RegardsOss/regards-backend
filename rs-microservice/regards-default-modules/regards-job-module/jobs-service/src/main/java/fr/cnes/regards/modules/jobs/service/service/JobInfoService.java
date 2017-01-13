/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
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
    public JobInfo stopJob(final Long pJobInfoId) throws EntityNotFoundException {
        if (!jobInfoRepository.exists(pJobInfoId)) {
            throw new EntityNotFoundException(pJobInfoId, JobInfo.class);
        }
        final JobInfo jobInfo = jobInfoRepository.findOne(pJobInfoId);
        if (jobInfo != null) {
            try {
                stoppingJobPublisher.send(pJobInfoId);
            } catch (final RabbitMQVhostException e) {
                LOG.error(String.format("Failed to stop the job <%d>, Rabbit MQ error : %s.", jobInfo.getId(),
                                        e.getMessage()),
                          e);
            }
        }
        return jobInfo;
    }

    @Override
    public List<JobInfo> retrieveJobInfoList() {
        Iterable<JobInfo> jobInfos = jobInfoRepository.findAll();
        return (jobInfos != null) ? Lists.newArrayList(jobInfos) : Collections.emptyList();
    }

    @Override
    public List<JobInfo> retrieveJobInfoListByState(final JobStatus pState) {
        return jobInfoRepository.findAllByStatusStatus(pState);
    }

    @Override
    public JobInfo retrieveJobInfoById(final Long pJobInfoId) throws EntityNotFoundException {
        final Optional<JobInfo> jobInfo = Optional.ofNullable(jobInfoRepository.findOne(pJobInfoId));
        return jobInfo.orElseThrow(() -> new EntityNotFoundException(pJobInfoId.toString(), JobInfo.class));
    }

    @Override
    public JobInfo save(final JobInfo pJobInfo) {
        return jobInfoRepository.save(pJobInfo);

    }

}
