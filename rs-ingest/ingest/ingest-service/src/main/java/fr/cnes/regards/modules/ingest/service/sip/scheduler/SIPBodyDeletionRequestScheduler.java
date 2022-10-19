/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.sip.scheduler;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.ingest.dao.SipDeletionSchedulerRepository;
import fr.cnes.regards.modules.ingest.domain.scheduler.SipDeletionSchedulerEntity;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import fr.cnes.regards.modules.ingest.service.job.SIPBodyDeletionJob;
import fr.cnes.regards.modules.ingest.service.settings.IngestSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

/**
 * Service to handle {@link SipBodyDeletetionScheduler}s
 *
 * @author tguillou
 */
@Service
@MultitenantTransactional
public class SIPBodyDeletionRequestScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPBodyDeletionRequestScheduler.class);

    private final JobInfoService jobInfoService;

    private IngestSettingsService ingestSettingsService;

    private SipDeletionSchedulerRepository sipDeletionSchedulerRepository;

    public SIPBodyDeletionRequestScheduler(JobInfoService jobInfoService,
                                           IngestSettingsService ingestSettingsService,
                                           SipDeletionSchedulerRepository sipDeletionSchedulerRepository) {
        this.jobInfoService = jobInfoService;
        this.ingestSettingsService = ingestSettingsService;
        this.sipDeletionSchedulerRepository = sipDeletionSchedulerRepository;
    }

    public JobInfo scheduleJob() {
        JobInfo jobInfo = null;
        // disable sip deletion if SIP time to live = -1
        if (ingestSettingsService.getSipBodyTimeToLive() == -1) {
            return null;
        }

        // if there is the same job waiting in thread-pool, do nothing
        Long numberOfJobsNotFinished = jobInfoService.retrieveJobsCount(SIPBodyDeletionJob.class.getName(),
                                                                        JobStatus.PENDING,
                                                                        JobStatus.QUEUED,
                                                                        JobStatus.RUNNING,
                                                                        JobStatus.TO_BE_RUN);
        if (numberOfJobsNotFinished > 0) {
            LOGGER.info("[SIP DELETION SCHEDULER] Cannot start the job : A deletion job is already running");
            return null;
        }

        // Schedule deletion job
        Set<JobParameter> jobParameters = Sets.newHashSet(new JobParameter(SIPBodyDeletionJob.LAST_SCHEDULED_DATE_PARAMETER,
                                                                           getLastScheduledDate()),
                                                          new JobParameter(SIPBodyDeletionJob.CLOSEST_DATE_TO_DELETE_PARAMETER,
                                                                           nowMinusTimeToLive()));
        jobInfo = new JobInfo(false,
                              IngestJobPriority.DELETE_SIP_BODY_JOB_PRIORITY,
                              jobParameters,
                              null,
                              SIPBodyDeletionJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        return jobInfo;
    }

    /**
     * @return the last upper bound date of the job, stored in bdd
     */
    private OffsetDateTime getLastScheduledDate() {
        Optional<SipDeletionSchedulerEntity> schedulerEntityOpt = sipDeletionSchedulerRepository.findFirst();
        return schedulerEntityOpt.isPresent() ?
            schedulerEntityOpt.get().getLastScheduledDate() :
            Instant.EPOCH.atOffset(ZoneOffset.UTC);
    }

    /**
     * @return the most recent date where sip have to be delete.
     */
    private OffsetDateTime nowMinusTimeToLive() {
        // sipBodyTimeToLive is in days, so x24 to have hours
        // if sipBodyTimeToLive is 0, the date returned is now(), that means there is no upper date in the job.
        return OffsetDateTime.now().minusHours(ingestSettingsService.getSipBodyTimeToLive() * 24L);
    }

}
