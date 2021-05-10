package fr.cnes.regards.framework.modules.session.agent.service;

import fr.cnes.regards.framework.modules.session.agent.service.clean.sessionstep.AgentCleanSessionStepJobService;
import fr.cnes.regards.framework.modules.session.agent.service.clean.sessionstep.AgentCleanSessionStepScheduler;
import fr.cnes.regards.framework.modules.session.agent.service.clean.sessionstep.AgentCleanSessionStepService;
import fr.cnes.regards.framework.modules.session.agent.service.clean.snapshotprocess.AgentCleanSnapshotProcessJobService;
import fr.cnes.regards.framework.modules.session.agent.service.clean.snapshotprocess.AgentCleanSnapshotProcessScheduler;
import fr.cnes.regards.framework.modules.session.agent.service.clean.snapshotprocess.AgentCleanSnapshotProcessService;
import fr.cnes.regards.framework.modules.session.agent.service.handlers.SessionAgentEventHandler;
import fr.cnes.regards.framework.modules.session.agent.service.handlers.SessionAgentHandlerService;
import fr.cnes.regards.framework.modules.session.agent.service.update.AgentSnapshotJobService;
import fr.cnes.regards.framework.modules.session.agent.service.update.AgentSnapshotScheduler;
import fr.cnes.regards.framework.modules.session.agent.service.update.AgentSnapshotService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Iliana Ghazali
 **/

@Configuration
public class SessionAgentServiceAutoConfiguration {

    /**
     * Clean
     */
    @Bean
    public AgentCleanSessionStepJobService agentCleanSessionStepJobService() {
        return new AgentCleanSessionStepJobService();
    }

    @Bean
    public AgentCleanSessionStepService agentCleanSessionStepService() {
        return new AgentCleanSessionStepService();
    }

    @Bean
    public AgentCleanSessionStepScheduler agentCleanSessionStepScheduler() {
        return new AgentCleanSessionStepScheduler();
    }

    @Bean
    public AgentCleanSnapshotProcessJobService agentCleanSnapshotProcessJobService() {
        return new AgentCleanSnapshotProcessJobService();
    }

    @Bean
    public AgentCleanSnapshotProcessService agentCleanSnapshotProcessService() {
        return new AgentCleanSnapshotProcessService();
    }

    @Bean
    public AgentCleanSnapshotProcessScheduler agentCleanSnapshotProcessScheduler() {
        return new AgentCleanSnapshotProcessScheduler();
    }

    /**
     * Handlers
     */

    @Bean
    public SessionAgentEventHandler sessionAgentHandler() {
        return new SessionAgentEventHandler();
    }

    @Bean
    public SessionAgentHandlerService sessionAgentHandlerService() {
        return new SessionAgentHandlerService();
    }

    /**
     * Update
     */

    @Bean
    public AgentSnapshotJobService agentSnapshotJobService() {
        return new AgentSnapshotJobService();
    }

    @Bean
    public AgentSnapshotService agentSnapshotService(){
        return new AgentSnapshotService();
    }

    @Bean
    public AgentSnapshotScheduler agentSnapshotScheduler(){
        return new AgentSnapshotScheduler();
    }
}