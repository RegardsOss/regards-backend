package fr.cnes.regards.framework.modules.session.agent.service.clean.sessionstep;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.modules.session.agent.service.update.AgentSnapshotJobService;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to launch {@link AgentCleanSessionStepJob}
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class AgentCleanSessionStepJobService {

    @Autowired
    private JobInfoService jobInfoService;

    @Autowired
    private ISnapshotProcessRepository snapshotProcessRepo;

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentCleanSessionStepJobService.class);


    public void scheduleJob() {
        LOGGER.trace("[CLEAN SESSION STEP SCHEDULER] Scheduling job ...");
        long start = System.currentTimeMillis();
        JobInfo jobInfo = new JobInfo(false, 0, null, null, AgentCleanSessionStepJob.class.getName());
        // create job
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.trace("[CLEAN SESSION STEP SCHEDULER] AgentSnapshotJob scheduled in {}", System.currentTimeMillis() - start);
    }
}
