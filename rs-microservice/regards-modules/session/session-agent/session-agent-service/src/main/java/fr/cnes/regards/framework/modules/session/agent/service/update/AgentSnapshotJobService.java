package fr.cnes.regards.framework.modules.session.agent.service.update;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * If new {@link StepPropertyUpdateRequest}s were added in the
 * database, launch {@link AgentSnapshotJob}s to update
 * {@link fr.cnes.regards.framework.modules.session.commons.domain.SessionStep}s
 * by source.
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
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
        LOGGER.trace("[AGENT SNAPSHOT SCHEDULER] Scheduling job at date {}...", OffsetDateTime.now());

        // Freeze start date to select stepEvents
        OffsetDateTime schedulerStartDate = OffsetDateTime.now();
        List<SnapshotProcess> snapshotProcessesRetrieved = this.snapshotProcessRepo.findAll();

        // Filter out all snapshot processes currently running or with no step events to update
        Predicate<SnapshotProcess> predicateAlreadyProcessed = process -> (process.getJobId() != null) || (
                (process.getLastUpdateDate() == null && stepPropertyUpdateRequestRepo
                        .countBySourceAndDateBefore(process.getSource(), schedulerStartDate) == 0) || (
                        process.getLastUpdateDate() != null && stepPropertyUpdateRequestRepo
                                .countBySourceAndDateBetween(process.getSource(), process.getLastUpdateDate(),
                                                             schedulerStartDate) == 0));

        snapshotProcessesRetrieved.removeIf(predicateAlreadyProcessed);

        // IF EVENTS WERE ADDED
        // launch one job per snapshotProcess, ie, one job per source
        if (!snapshotProcessesRetrieved.isEmpty()) {
            for (SnapshotProcess snapshotProcessToUpdate : snapshotProcessesRetrieved) {
                // create one job per each source
                HashSet<JobParameter> jobParameters = Sets
                        .newHashSet(new JobParameter(AgentSnapshotJob.SNAPSHOT_PROCESS, snapshotProcessToUpdate),
                                    new JobParameter(AgentSnapshotJob.FREEZE_DATE, schedulerStartDate));
                JobInfo jobInfo = new JobInfo(false, 0, jobParameters, null, AgentSnapshotJob.class.getName());

                // create job
                jobInfo = jobInfoService.createAsQueued(jobInfo);

                // update snapshot process with new job id to indicate there is a current process ongoing
                snapshotProcessToUpdate.setJobId(jobInfo.getId());
                this.snapshotProcessRepo.save(snapshotProcessToUpdate);

                LOGGER.trace("[AGENT SNAPSHOT SCHEDULER] AgentSnapshotJob scheduled in {} ms for source {}",
                            System.currentTimeMillis() - start, snapshotProcessToUpdate.getSource());
            }
        } else {
            LOGGER.trace("[AGENT SNAPSHOT SCHEDULER] No sessionSteps found to be updated. Handled in {} ms",
                        System.currentTimeMillis() - start);
        }
    }
}


