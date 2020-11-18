/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class JobInfoService implements IJobInfoService, ApplicationContextAware {

    public static final String SOME_FUNNY_MESSAGE = "Please use create method for creating, you dumb...";

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

    @Autowired
    private IPublisher publisher;

    private IJobInfoService self;

    private ApplicationContext applicationContext;

    /**
     * {@link JobInfo} JPA Repository
     */
    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onContextRefreshedEvent(ContextRefreshedEvent event) {
        if (self == null) {
            try {
                self = applicationContext.getBean(IJobInfoService.class);
            } catch (NoSuchBeanDefinitionException e) {
                // in this case there is nothing to do but wait for the next event
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
            throw new IllegalArgumentException(SOME_FUNNY_MESSAGE);
        }
        jobInfo.updateStatus(JobStatus.PENDING);
        return jobInfoRepository.save(jobInfo);
    }

    @Override
    public JobInfo createAsQueued(JobInfo jobInfo) {
        if (jobInfo.getId() != null) {
            throw new IllegalArgumentException(SOME_FUNNY_MESSAGE);
        }
        jobInfo.updateStatus(JobStatus.QUEUED);
        return jobInfoRepository.save(jobInfo);
    }

    @Override
    public JobInfo save(final JobInfo jobInfo) {
        if (jobInfo.getId() == null) {
            throw new IllegalArgumentException(SOME_FUNNY_MESSAGE);
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
    public void updateJobInfosHeartbeat(Collection<UUID> ids) {
        jobInfoRepository.updateHeartbeatDateForIdsIn(OffsetDateTime.now(), ids);
    }

    @Override
    @Scheduled(fixedDelayString = "${regards.jobs.out.of.date.cleaning.rate.ms:3600000}", initialDelay = 0)
    @Transactional(propagation = Propagation.SUPPORTS)
    public void cleanOutOfDateJobs() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            self.cleanOutOfDateJobsOnTenant();
        }
        runtimeTenantResolver.clearTenant();
    }

    @Override
    public void cleanOutOfDateJobsOnTenant() {
        Set<JobInfo> jobs = new HashSet<>();
        // Add expired jobs
        jobs.addAll(jobInfoRepository.findExpiredJobs());
        // Add succeeded jobs since configured retention days
        jobs.addAll(jobInfoRepository.findSucceededJobsSince(succeededJobsRetentionDays));
        // Add failed or aborted jobs since configured retention days
        jobs.addAll(jobInfoRepository.findFailedOrAbortedJobsSince(failedJobsRetentionDays));
        // Remove all these jobs
        jobInfoRepository.deleteAll(jobs);
    }

    @Override
    public void cleanDeadJobs() {
        List<JobInfo> jobs = retrieveJobs(JobStatus.RUNNING);
        List<JobEvent> failEvents = new ArrayList<>();
        long deadAfter = JobService.HEARTBEAT_DELAY * timeSlotNumber;
        OffsetDateTime deadLimitDate = OffsetDateTime.now().minus(deadAfter, ChronoUnit.MILLIS);
        for (JobInfo job : jobs) {
            // if last heartbeat date is null it means job has been started but not yet pinged by job engine
            if (job.getLastHeartbeatDate() != null && job.getLastHeartbeatDate().isBefore(deadLimitDate)) {
                job.updateStatus(JobStatus.FAILED);
                LOGGER.warn("Job {} of type {} does not respond anymore after waiting activity ping for {} ms.",
                            job.getId(),
                            job.getClassName(),
                            deadAfter);
                failEvents.add(new JobEvent(job.getId(), JobEventType.FAILED));
            }
        }
        publisher.publish(failEvents);
        saveAll(jobs);
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
}
