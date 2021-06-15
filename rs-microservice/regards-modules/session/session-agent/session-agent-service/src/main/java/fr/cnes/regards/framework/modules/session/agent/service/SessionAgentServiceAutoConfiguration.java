/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
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
import org.springframework.context.annotation.Profile;

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
    @Profile("!noscheduler")
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
    @Profile("!noscheduler")
    public AgentCleanSnapshotProcessScheduler agentCleanSnapshotProcessScheduler() {
        return new AgentCleanSnapshotProcessScheduler();
    }

    /**
     * Handlers
     */

    @Bean
    @Profile("!nohandler")
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
    @Profile("!noscheduler")
    public AgentSnapshotScheduler agentSnapshotScheduler(){
        return new AgentSnapshotScheduler();
    }
}