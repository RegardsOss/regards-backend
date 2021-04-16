package fr.cnes.regards.framework.modules.session.agent.service.update;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.sessioncommons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.sessioncommons.domain.SnapshotProcess;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @PersistenceContext
    private EntityManager entityManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSnapshotJobService.class);

    public void scheduleJob() {
        long start = System.currentTimeMillis();
        LOGGER.trace("[AGENT SNAPSHOT SCHEDULER] Scheduling job at date {}...", start);
        JobInfo jobInfo = null;

        // Freeze start date to select stepEvents
        OffsetDateTime schedulerStartDate = OffsetDateTime.now();

        // Calculate the list of jobs to launch
        Map<String, List<StepPropertyUpdateRequest>> stepsPropertiesToHandle = getStepPropertiesToHandle(
                schedulerStartDate);

        if (!stepsPropertiesToHandle.isEmpty()) {
            for (Map.Entry<String, List<StepPropertyUpdateRequest>> entry : stepsPropertiesToHandle.entrySet()) {
                String source = entry.getKey();

                // create one job per each source of step events
                HashSet<JobParameter> jobParameters = Sets.newHashSet(new JobParameter(AgentSnapshotJob.SOURCE, source),
                                                                      new JobParameter(AgentSnapshotJob.STEP_EVENTS,
                                                                                       entry.getValue()));
                jobInfo = new JobInfo(false, 0, jobParameters, null, AgentSnapshotJob.class.getName());

                // update snapshot process with new job id
                Optional<SnapshotProcess> snapshotProcessOpt = this.snapshotProcessRepo.findBySource(source);
                if (snapshotProcessOpt.isPresent()) {
                    SnapshotProcess snapshotProcess = snapshotProcessOpt.get();
                    snapshotProcess.setJobId(jobInfo.getId());
                    this.snapshotProcessRepo.save(snapshotProcess);
                }
                // create job
                jobInfoService.createAsQueued(jobInfo);
                LOGGER.trace("[AGENT SNAPSHOT SCHEDULER] AgentSnapshotJob scheduled in {}",
                             System.currentTimeMillis() - start);
            }
        }

        // Get all stepPropertyUpdateEventRequest between lastUpdate and schedulerStartDate

    }

    public Map<String, List<StepPropertyUpdateRequest>> getStepPropertiesToHandle(OffsetDateTime endDate) {
        String query = "SELECT step.* FROM t_step_property_update_request step LEFT OUTER JOIN t_snapshot_process "
                + "process on step.source = process.source WHERE process.job_id IS NULL AND (process.last_update IS "
                + "NULL OR (step.date >= process.last_update AND step.date <= :endDate)) ORDER by step.source;";
        @SuppressWarnings("unchecked") List<StepPropertyUpdateRequest> stepsPropertiesToHandle = entityManager
                .createNativeQuery(query).setParameter("endDate", endDate).getResultList();
        Map<String, List<StepPropertyUpdateRequest>> stepsPropertiesBySession = new HashMap<>();
        for (StepPropertyUpdateRequest stepProperty : stepsPropertiesToHandle) {
            String session = stepProperty.getSession();
            if (!stepsPropertiesBySession.containsKey(session)) {
                stepsPropertiesBySession.put(session, new ArrayList<>());
            } else {
                stepsPropertiesBySession.get(session).add(stepProperty);
            }

        }
        return stepsPropertiesBySession;
    }
}

