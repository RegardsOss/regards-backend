package fr.cnes.regards.framework.modules.session.agent.service.clean.sessionstep;

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
        logger.debug("[{}] AgentCleanSessionStepJob starts", jobInfoId);
        long start = System.currentTimeMillis();
        int nbSessionStep = agentCleanSessionStepService.clean();
        logger.debug("[{}] AgentCleanSessionStepJob ends in {} ms. {} session step deleted", jobInfoId,
                     System.currentTimeMillis() - start, nbSessionStep);
    }

}