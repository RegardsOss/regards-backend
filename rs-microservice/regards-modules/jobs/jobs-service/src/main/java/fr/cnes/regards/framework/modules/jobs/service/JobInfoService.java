/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.modules.jobs.service;

import com.google.common.collect.ImmutableList;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatusInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.domain.event.StopJobEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class JobInfoService implements IJobInfoService, ApplicationContextAware {

    public static final String ERROR_CREATE_JOB_INFO = "An error occurred while creating the JobInfo...";

    private static final Logger LOGGER = LoggerFactory.getLogger(JobInfoService.class);

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Value("${regards.jobs.succeeded.retention.days:1}")
    private int succeededJobsRetentionDays;

    @Value("${regards.jobs.failed.retention.days:30}")
    private int failedJobsRetentionDays;

    // number of time slots after that we consider a job is dead
    @Value("${regards.jobs.slot.number:3}")
    private int timeSlotNumber;

    @Value("${regards.jobs.toberun.expiration.hours:60}")
    public int toBeRunExpirationMinutes;

    @Autowired
    private IPublisher publisher;

    private IJobInfoService self;

    private ApplicationContext applicationContext;

    /**
     * {@link JobInfo} JPA Repository
     */
    @Autowired
    private IJobInfoRepository jobInfoRepository;

    /**
     * Last {@link OffsetDateTime} when jobs ping has been processed
     */
    private OffsetDateTime lastJobPingDate = null;

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onContextRefreshedEvent(ContextRefreshedEvent event) {
        if (self == null) {
            try {
                self = applicationContext.getBean(IJobInfoService.class);
            } catch (NoSuchBeanDefinitionException e) {
                // in this case there is nothing to do but wait for the next event
                LOGGER.warn(e.getMessage(), e);
            }
        }
    }

    @Override
    public JobInfo findHighestPriorityQueuedJobAndSetAsToBeRun() {
        JobInfo found = jobInfoRepository.findHighestPriorityQueued();
        if (found != null) {
            Hibernate.initialize(found.getParameters());
            found.updateStatus(JobStatus.TO_BE_RUN);
            jobInfoRepository.save(found);
        }
        return found;
    }

    @Override
    public List<JobInfo> retrieveJobs() {
        return ImmutableList.copyOf(jobInfoRepository.findAll());
    }

    @Override
    public List<JobInfo> retrieveJobs(JobStatus state) {
        return jobInfoRepository.findAllByStatusStatus(state);
    }

    @Override
    public JobInfo retrieveJob(UUID id) {
        return jobInfoRepository.findCompleteById(id);
    }

    @Override
    public JobInfo createAsPending(JobInfo jobInfo) {
        if (jobInfo.getId() != null) {
            throw new IllegalArgumentException(ERROR_CREATE_JOB_INFO);
        }
        jobInfo.updateStatus(JobStatus.PENDING);
        return jobInfoRepository.save(jobInfo);
    }

    @Override
    public JobInfo createAsQueued(JobInfo jobInfo) {
        if (jobInfo.getId() != null) {
            throw new IllegalArgumentException(ERROR_CREATE_JOB_INFO);
        }
        jobInfo.updateStatus(JobStatus.QUEUED);
        return jobInfoRepository.save(jobInfo);
    }

    @Override
    public List<JobInfo> createAsQueued(Collection<JobInfo> jobsInfo) {
        return jobsInfo.stream().map(this::createAsQueued).toList();
    }

    @Override
    public JobInfo createPendingTriggerJob(JobInfo jobInfo, OffsetDateTime dateToTriggerJob) {
        if (jobInfo.getId() != null) {
            throw new IllegalArgumentException(ERROR_CREATE_JOB_INFO);
        }
        jobInfo.updateStatus(JobStatus.PENDING);
        jobInfo.setTriggerAfterDate(dateToTriggerJob);
        return jobInfoRepository.save(jobInfo);
    }

    @Override
    public List<JobInfo> updatePendingJobsToBeTriggered(OffsetDateTime startSearching, int maxJobsToRetrieve) {
        Pageable pageToRequest = PageRequest.of(0, maxJobsToRetrieve, Sort.by("status.statusDate"));
        List<JobInfo> jobInfoToBeTriggered = jobInfoRepository.findByStatusStatusAndTriggerAfterDateLessThan(JobStatus.PENDING,
                                                                                                             startSearching,
                                                                                                             pageToRequest);
        jobInfoToBeTriggered.forEach(jobInfo -> jobInfo.updateStatus(JobStatus.QUEUED));
        LOGGER.debug("{} jobs to be triggerred updated from PENDING to QUEUED.", jobInfoToBeTriggered.size());
        return jobInfoToBeTriggered;
    }

    @Override
    public JobInfo enqueueJobForId(UUID jobInfoId) {
        JobInfo jobInfo = retrieveJob(jobInfoId);
        jobInfo.updateStatus(JobStatus.QUEUED);
        return save(jobInfo);
    }

    @Override
    public JobInfo save(final JobInfo jobInfo) {
        if (jobInfo.getId() == null) {
            throw new IllegalArgumentException(ERROR_CREATE_JOB_INFO);
        }
        return jobInfoRepository.save(jobInfo);
    }

    @Override
    public void saveAll(List<JobInfo> jobInfo) {
        jobInfoRepository.saveAll(jobInfo);
    }

    @Override
    public JobInfo lock(JobInfo jobInfo) {
        jobInfo.setLocked(true);
        return save(jobInfo);
    }

    @Override
    public JobInfo unlock(JobInfo jobInfo) {
        jobInfo.setLocked(false);
        return save(jobInfo);
    }

    @Override
    public void stopJob(UUID id) {
        publisher.publish(new StopJobEvent(id));
    }

    @Override
    public int stopJobs(List<JobInfo> jobsInfo) {
        List<StopJobEvent> jobStopEvents = jobsInfo.stream().map(jobInfo -> new StopJobEvent(jobInfo.getId())).toList();
        publisher.publish(jobStopEvents);
        return jobStopEvents.size();
    }

    @Override
    public void updateJobInfosCompletion(Iterable<JobInfo> jobInfos) {
        for (JobInfo jobInfo : jobInfos) {
            JobStatusInfo status = jobInfo.getStatus();
            jobInfoRepository.updateCompletion(status.getPercentCompleted(),
                                               status.getEstimatedCompletion(),
                                               jobInfo.getId(),
                                               OffsetDateTime.now());
        }
    }

    @Override
    public void updateExpirationDate(OffsetDateTime expirationDate, Set<UUID> jobInfoIds) {
        jobInfoRepository.updateExpirationDate(expirationDate, jobInfoIds);
    }

    @Override
    public void updateJobInfosHeartbeat(Collection<UUID> ids) {
        jobInfoRepository.updateHeartbeatDateForIdsIn(OffsetDateTime.now(), ids);
    }

    @Override
    public void cleanOutOfDateJobsOnTenant() {
        // Delete expired jobs
        jobInfoRepository.deleteExpiredJobs();
        // Delete succeeded jobs since configured retention days
        jobInfoRepository.deleteSucceededJobsSince(succeededJobsRetentionDays);
        // Delete failed or aborted jobs since configured retention days
        jobInfoRepository.deleteFailedAndAbortJobsSince(failedJobsRetentionDays);
    }

    @Override
    public void cleanDeadJobs() {
        long deadAfter = JobService.HEARTBEAT_DELAY * timeSlotNumber;
        OffsetDateTime deadLimitDate = OffsetDateTime.now().minus(deadAfter, ChronoUnit.MILLIS);
        // Only clean dead jobs if last jobs ping date is after dead limit date to ensure ping is realy done by
        // associated scheduler
        if (lastJobPingDate != null && lastJobPingDate.isAfter(deadLimitDate)) {
            List<JobInfo> jobs = retrieveJobs(JobStatus.RUNNING);
            List<JobEvent> failEvents = new ArrayList<>();
            for (JobInfo job : jobs) {
                // if last heartbeat date is null it means job has been started but not yet pinged by job engine
                if ((job.getLastHeartbeatDate() != null) && job.getLastHeartbeatDate().isBefore(deadLimitDate)) {
                    job.getStatus()
                       .setStackTrace(String.format(
                           "This jobs has been considered dead because heartbeat has not responded for more than %s ms",
                           deadAfter));
                    job.updateStatus(JobStatus.FAILED);
                    LOGGER.warn("Job {} of type {} does not respond anymore after waiting activity ping for {} ms.",
                                job.getId(),
                                job.getClassName(),
                                deadAfter);
                    failEvents.add(new JobEvent(job.getId(), JobEventType.FAILED, job.getClassName()));
                }
            }
            publisher.publish(failEvents);
            saveAll(jobs);
        }
    }

    @Override
    public Long countByClassAndParameterValueAndStatus(String className,
                                                       String parameterName,
                                                       String parameterValue,
                                                       JobStatus... jobStatuses) {
        return jobInfoRepository.countByClassNameAndParameters_NameAndParameters_ValueAndStatusStatusIn(className,
                                                                                                        parameterName,
                                                                                                        parameterValue,
                                                                                                        jobStatuses);
    }

    @Override
    public Long retrieveJobsCount(String className, JobStatus... statuses) {
        return jobInfoRepository.countByClassNameAndStatusStatusIn(className, statuses);
    }

    @Override
    public Page<JobInfo> retrieveJobs(String className, Pageable page, JobStatus... statuses) {
        return jobInfoRepository.findByClassNameAndStatusStatusIn(className, statuses, page);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void updateLastJobsPingDate() {
        lastJobPingDate = OffsetDateTime.now();
    }

    @Override
    public void requeueOldToBeRunJobs() {
        OffsetDateTime tooOldDate = OffsetDateTime.now().minusMinutes(toBeRunExpirationMinutes);
        List<JobInfo> jobInfos = jobInfoRepository.findAllByStatusStatusAndStatusStatusDateLessThan(JobStatus.TO_BE_RUN,
                                                                                                         tooOldDate,
                                                                                                         Pageable.ofSize(
                                                                                                             100));
        if (!jobInfos.isEmpty()) {
            LOGGER.warn("Requeue {} jobs in TO_BE_RUN status for too long.", jobInfos.size());
            jobInfos.forEach(jobInfo -> jobInfo.updateStatus(JobStatus.QUEUED));
            jobInfoRepository.saveAll(jobInfos);
        }
    }

}
