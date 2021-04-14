package fr.cnes.regards.framework.modules.session.agent.service.jobs;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.domain.StepEvent;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.modules.session.agent.service.events.AgentSnapshotListenerService;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
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
    private AgentSnapshotListenerService agentSnapshotListenerService;

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSnapshotJobService.class);

    // source - set of step events
    private Map<String, Set<StepEvent>> eventsBySource;

    private List<SnapshotProcess> snapshotProcessesToUpdate;

    OffsetDateTime schedulerStartDate;

    public void scheduleJob() {
        LOGGER.trace("[AGENT SNAPSHOT SCHEDULER] Scheduling job ...");
        long start = System.currentTimeMillis();
        JobInfo jobInfo = null;

        // Freeze start date to select stepEvents
        this.schedulerStartDate = OffsetDateTime.now();
        // Subscribe to listener
        agentSnapshotListenerService.subscribe(this);

        // Schedule jobs for each source of snapshotProcessesToUpdate
        if (this.eventsBySource != null) {
            for (SnapshotProcess snapshotProcess : snapshotProcessesToUpdate) {
                String source = snapshotProcess.getSource();
                // create one job per each source of step events
                HashSet<JobParameter> jobParameters = Sets
                        .newHashSet(new JobParameter(AgentSnapshotJob.SOURCE, source),
                                    new JobParameter(AgentSnapshotJob.STEP_EVENTS, this.eventsBySource.get(source)));
                jobInfo = new JobInfo(false, 0, jobParameters, null, AgentSnapshotJob.class.getName());
                // update snapshot process with new job id
                snapshotProcess.setJob_id(jobInfo.getId());
                this.snapshotProcessRepo.save(snapshotProcess);
                // create job
                jobInfoService.createAsQueued(jobInfo);
                LOGGER.trace("[AGENT SNAPSHOT SCHEDULER] AgentSnapshotJob scheduled in {}",
                             System.currentTimeMillis() - start);
            }
        }

        agentSnapshotListenerService.unsubscribe(this);
    }

    public void handleStepEvents(List<StepEvent> stepEventList) {
        // GET SNAPSHOT PROCESSES
        List<SnapshotProcess> allSnapshotProcesses = this.snapshotProcessRepo.findAll();
        // HANDLE STEP EVENTS
        for (StepEvent stepEvent : stepEventList) {
            String source = stepEvent.getSource();
            // find if stepEvent.source is already in snapshot process list. Return null if not found.
            SnapshotProcess snapshotProcess = allSnapshotProcesses.stream().filter(process -> process.getSource().equals(source)).findFirst().orElse(null);

            // if not found
            if(snapshotProcess == null) {
                // add event to eventsBySource Map
                if (this.eventsBySource.containsKey(source)) {
                    this.eventsBySource.get(source).add(stepEvent);
                } else {
                    this.eventsBySource.put(source, Sets.newHashSet(stepEvent));
                }
                // add new source to snapshotProcessesToUpdate list
                this.snapshotProcessesToUpdate.add(new SnapshotProcess(source, null, null));
            // if found and a job is not already running
            } else if (snapshotProcess.getJob_id() == null) {
                OffsetDateTime stepEventDate = stepEvent.getDate();
                // if event.date is correct, add event to eventsBySource map
                if (snapshotProcess.getLastUpdate() == null || (stepEventDate.isAfter(snapshotProcess.getLastUpdate()) && stepEventDate.isBefore(this.schedulerStartDate))) {
                    if (this.eventsBySource.containsKey(source)) {
                        this.eventsBySource.get(source).add(stepEvent);
                    } else {
                        this.eventsBySource.put(source, Sets.newHashSet(stepEvent));
                        // add source to snapshotProcessesToUpdate list
                        this.snapshotProcessesToUpdate.add(new SnapshotProcess(source, null, null));
                    }
                }
            }
        }
    }

}