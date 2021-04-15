package fr.cnes.regards.framework.modules.session.agent.service.jobs;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.modules.session.agent.service.events.AgentSnapshotListenerService;
import fr.cnes.regards.framework.modules.session.sessioncommons.dao.ISnapshotProcessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Iliana Ghazali
 **/
@Service
@RegardsTransactional
public class AgentCleanJobService {

    @Autowired
    private JobInfoService jobInfoService;

    @Autowired
    private ISnapshotProcessRepository snapshotProcessRepo;

    @Autowired
    private AgentSnapshotListenerService agentSnapshotListenerService;

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSnapshotJobService.class);


    public void scheduleJob() {
        LOGGER.trace("[AGENT SNAPSHOT SCHEDULER] Scheduling job ...");
        long start = System.currentTimeMillis();
        JobInfo jobInfo = null;
        jobInfo = new JobInfo(false, 0, null, null, AgentCleanJob.class.getName());
        // create job
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.trace("[AGENT SNAPSHOT SCHEDULER] AgentSnapshotJob scheduled in {}", System.currentTimeMillis() - start);
    }
}
