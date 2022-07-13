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
package fr.cnes.regards.framework.modules.session.agent.service.update;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * If new {@link StepPropertyUpdateRequest}s were added in the
 * database, launch {@link AgentSnapshotJob}s to update
 * {@link fr.cnes.regards.framework.modules.session.commons.domain.SessionStep}s
 * by source.
 *
 * @author Iliana Ghazali
 **/
public class AgentSnapshotJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSnapshotJobService.class);

    private static final String LOG_HEADER = "[AGENT SNAPSHOT SCHEDULER] >>>";

    public static final int JOB_PRIORITY = 1000;

    private final int snapshotPropertyPageSize;

    private final AgentSnapshotJobService self;

    private final JobInfoService jobInfoService;

    private final ISnapshotProcessRepository snapshotRepo;

    private final IStepPropertyUpdateRequestRepository stepPropertyUpdateRequestRepo;

    public AgentSnapshotJobService(JobInfoService jobInfoService,
                                   ISnapshotProcessRepository snapshotRepo,
                                   AgentSnapshotJobService agentSnapshotJobService,
                                   IStepPropertyUpdateRequestRepository stepPropertyUpdateRequestRepo,
                                   int snapshotPropertyPageSize) {
        this.jobInfoService = jobInfoService;
        this.snapshotRepo = snapshotRepo;
        this.snapshotPropertyPageSize = snapshotPropertyPageSize;
        this.self = agentSnapshotJobService;
        this.stepPropertyUpdateRequestRepo = stepPropertyUpdateRequestRepo;
    }

    public void scheduleJob() {
        OffsetDateTime schedulerStartDate = OffsetDateTime.now();
        LOGGER.debug("{} Scheduling AgentSnapshotJobs at {}...", LOG_HEADER, schedulerStartDate);
        int totalNbJobsScheduled = 0;
        Pageable pageable = PageRequest.of(0, snapshotPropertyPageSize, Sort.by(Sort.Order.asc("source")));
        boolean hasNext = true;

        while (hasNext) {
            Pair<Boolean, Integer> pairHasNextNbJobs = self.handlePageSnapshots(schedulerStartDate, pageable);
            hasNext = pairHasNextNbJobs.getFirst();
            totalNbJobsScheduled += pairHasNextNbJobs.getSecond();
            if(hasNext) {
                pageable = pageable.next();
            }
        }

        LOGGER.debug("{} Scheduled a total of {} AgentSnapshotJobs in {} ms",
                     LOG_HEADER,
                     totalNbJobsScheduled,
                     Duration.between(schedulerStartDate, OffsetDateTime.now()).toMillis());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Pair<Boolean, Integer> handlePageSnapshots(OffsetDateTime schedulerStartDate, Pageable pageable) {
        Page<SnapshotProcess> snapshotPage = this.snapshotRepo.findByJobIdIsNull(pageable);
        // Filter out all snapshot processes with no step events to update
        Predicate<SnapshotProcess> predicateSnapAlreadyProcessed = process -> (process.getLastUpdateDate() == null ?
            stepPropertyUpdateRequestRepo.countBySourceAndRegistrationDateBefore(process.getSource(), schedulerStartDate) == 0 :
            stepPropertyUpdateRequestRepo.countBySourceAndRegistrationDateGreaterThanAndRegistrationDateLessThan(process.getSource(),
                                                                                                                 process.getLastUpdateDate(),
                                                                                                                 schedulerStartDate) == 0);
        Set<SnapshotProcess> snapshotsRetrieved = new HashSet<>(snapshotPage.getContent());
        snapshotsRetrieved.removeIf(predicateSnapAlreadyProcessed);
        // launch one job per snapshotProcess, ie, one job per source
        if (!snapshotsRetrieved.isEmpty()) {
            return Pair.of(snapshotPage.hasNext(), createOneJobPerSnapshot(schedulerStartDate, snapshotsRetrieved));
        } else {
            LOGGER.trace("{} No AgentSnapshotJobs scheduled for page number {}", LOG_HEADER, pageable.getPageNumber());
            return Pair.of(snapshotPage.hasNext(), 0);
        }
    }

    private int createOneJobPerSnapshot(OffsetDateTime schedulerStartDate,
                                        Set<SnapshotProcess> snapshotProcessesRetrieved) {
        int nbJobsScheduled = 0;
        for (SnapshotProcess snapshotProcessToUpdate : snapshotProcessesRetrieved) {
            // create one job per each source
            HashSet<JobParameter> jobParameters = Sets.newHashSet(new JobParameter(AgentSnapshotJob.SNAPSHOT_PROCESS,
                                                                                   snapshotProcessToUpdate),
                                                                  new JobParameter(AgentSnapshotJob.FREEZE_DATE,
                                                                                   schedulerStartDate));
            JobInfo jobInfo = new JobInfo(false, JOB_PRIORITY, jobParameters, null, AgentSnapshotJob.class.getName());

            // create job
            jobInfo = jobInfoService.createAsQueued(jobInfo);

            // update snapshot process with new job id to indicate there is a current process ongoing
            snapshotProcessToUpdate.setJobId(jobInfo.getId());
            this.snapshotRepo.save(snapshotProcessToUpdate);
            nbJobsScheduled++;

            LOGGER.trace("{} AgentSnapshotJob scheduled for source {}",
                         LOG_HEADER,
                         snapshotProcessToUpdate.getSource());
        }
        return nbJobsScheduled;
    }
}
