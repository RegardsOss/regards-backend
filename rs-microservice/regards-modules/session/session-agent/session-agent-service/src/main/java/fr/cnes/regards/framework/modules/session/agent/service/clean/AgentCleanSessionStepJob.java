package fr.cnes.regards.framework.modules.session.agent.service.clean;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * See {@link AgentCleanSessionStepService}
 *
 * @author Iliana Ghazali
 **/
public class AgentCleanSessionStepJob extends AbstractJob<Void> {

    @Autowired
    private AgentCleanSessionStepService agentCleanSessionStepService;

    @Override
    public void run() {
        logger.debug("[{}] AgentCleanJob starts for source {}", jobInfoId);
        long start = System.currentTimeMillis();
        int nbSessionStep = agentCleanSessionStepService.clean();
        logger.debug("[{}] AgentCleanJob ends in {} ms. {} session step deleted", jobInfoId,
                     System.currentTimeMillis() - start, nbSessionStep);
    }

}