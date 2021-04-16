package fr.cnes.regards.framework.modules.session.agent.service.clean;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Iliana Ghazali
 **/
public class AgentCleanJob extends AbstractJob<Void> {

    @Autowired
    private AgentCleanService agentCleanService;

    @Override
    public void run() {
        logger.debug("[{}] AgentCleanJob starts for source {}", jobInfoId);
        long start = System.currentTimeMillis();
        int nbSessionStep = agentCleanService.clean();
        logger.debug("[{}] AgentCleanJob ends in {} ms. {} session step deleted", jobInfoId, System.currentTimeMillis() - start, nbSessionStep);
    }

}
