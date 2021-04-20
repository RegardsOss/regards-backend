package fr.cnes.regards.framework.modules.session.agent.service.update;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * If new {@link fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyUpdateRequest}s were added in the
 * database, launch {@link AgentSnapshotJob}s to update
 * {@link fr.cnes.regards.framework.modules.session.commons.domain.SessionStep}s
 * by source.
 *
 * @author Iliana Ghazali
 **/
@Service
@RegardsTransactional
public class AgentSnapshotJobService {

    @Autowired
    private JobInfoService jobInfoService;

    @Autowired
    private ISnapshotProcessRepository snapshotProcessRepo;

    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyUpdateRequestRepo;

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSnapshotJobService.class);

    public void scheduleJob() {
        long start = System.currentTimeMillis();
        LOGGER.info("[AGENT SNAPSHOT SCHEDULER] Scheduling job at date {}...", OffsetDateTime.now());
        JobInfo jobInfo;

        // Freeze start date to select stepEvents
        OffsetDateTime schedulerStartDate = OffsetDateTime.now();
        List<SnapshotProcess> snapshotProcessesRetrieved = this.snapshotProcessRepo.findAll();
        Set<SnapshotProcess> snapshotProcessesUpdated = new HashSet<>();

        // RETRIEVE SNAPSHOT PROCESSES
        // search on every source, if events were added until schedulerStartDate
        for (SnapshotProcess snapshotProcess : snapshotProcessesRetrieved) {
            // Add snapshot only is there is no current job already processing events
            if (snapshotProcess.getJobId() == null) {
                OffsetDateTime lastUpdated = snapshotProcess.getLastUpdate();
                if ((lastUpdated == null && stepPropertyUpdateRequestRepo
                        .countBySourceAndDateBefore(snapshotProcess.getSource(), schedulerStartDate) >= 1) || (
                        lastUpdated != null && stepPropertyUpdateRequestRepo
                                .countBySourceAndDateBetween(snapshotProcess.getSource(),
                                                             snapshotProcess.getLastUpdate(), schedulerStartDate)
                                >= 1)) {
                    snapshotProcessesUpdated.add(snapshotProcess);
                }
            }
        }

        // IF EVENTS WERE ADDED
        // launch one job per snapshotProcess, ie, one job per source
        if (!snapshotProcessesUpdated.isEmpty()) {
            for (SnapshotProcess snapshotProcessToUpdate : snapshotProcessesUpdated) {
                // create one job per each source
                HashSet<JobParameter> jobParameters = Sets
                        .newHashSet(new JobParameter(AgentSnapshotJob.SNAPSHOT_PROCESS, snapshotProcessToUpdate),
                                    new JobParameter(AgentSnapshotJob.FREEZE_DATE, schedulerStartDate));
                jobInfo = new JobInfo(false, 0, jobParameters, null, AgentSnapshotJob.class.getName());

                // update snapshot process with new job id to indicate there is a current process ongoing
                snapshotProcessToUpdate.setJobId(jobInfo.getId());
                this.snapshotProcessRepo.save(snapshotProcessToUpdate);

                // create job
                jobInfoService.createAsQueued(jobInfo);
                LOGGER.info("[AGENT SNAPSHOT SCHEDULER] AgentSnapshotJob scheduled in {} ms for source {}",
                            System.currentTimeMillis() - start, snapshotProcessToUpdate.getSource());
            }
        } else {
            LOGGER.info("[AGENT SNAPSHOT SCHEDULER] No sessionSteps found to be updated. Handled in {} ms",
                        System.currentTimeMillis() - start);
        }
    }
}


