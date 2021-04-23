package fr.cnes.regards.framework.modules.session.agent.service.clean.snapshotprocess;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * See {@link AgentCleanSnapshotProcessService}
 *
 * @author Iliana Ghazali
 **/
public class AgentCleanSnapshotProcessJob extends AbstractJob<Void> {

    @Autowired
    private AgentCleanSnapshotProcessService agentCleanSnapshotProcessService;

    @Override
    public void run() {
        logger.debug("[{}] AgentCleanSnapshotProcessJob starts for source {}", jobInfoId);
        long start = System.currentTimeMillis();
        int nbSnapshotProcessDeleted = agentCleanSnapshotProcessService.clean();
        logger.debug("[{}] AgentCleanSnapshotProcessJob ends in {} ms. {} snapshot process deleted", jobInfoId,
                     System.currentTimeMillis() - start, nbSnapshotProcessDeleted);
    }

}