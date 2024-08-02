/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
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
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * @author Iliana Ghazali
 **/

@AutoConfiguration
public class SessionAgentServiceAutoConfiguration {

    /**
     * Clean
     */
    @Bean
    public AgentCleanSessionStepJobService agentCleanSessionStepJobService() {
        return new AgentCleanSessionStepJobService();
    }

    @Bean
    @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
    public AgentCleanSessionStepService agentCleanSessionStepService(ISessionStepRepository sessionStepRepo,
                                                                     IStepPropertyUpdateRequestRepository stepPropertyRepo,
                                                                     AgentCleanSessionStepService agentCleanSessionStepService) {
        return new AgentCleanSessionStepService(sessionStepRepo, stepPropertyRepo, agentCleanSessionStepService);
    }

    @Bean
    @Profile("!noscheduler")
    public AgentCleanSessionStepScheduler agentCleanSessionStepScheduler() {
        return new AgentCleanSessionStepScheduler();
    }

    @Bean
    public AgentCleanSnapshotProcessJobService agentCleanSnapshotProcessJobService(JobInfoService jobInfoService,
                                                                                   ISnapshotProcessRepository snapshotProcessRepo) {
        return new AgentCleanSnapshotProcessJobService(jobInfoService, snapshotProcessRepo);
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
    @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
    public AgentSnapshotJobService agentSnapshotJobService(IStepPropertyUpdateRequestRepository stepPropertyUpdateRequestRepo,
                                                           ISnapshotProcessRepository snapshotStepRepo,
                                                           JobInfoService jobInfoService,
                                                           AgentSnapshotJobService self,
                                                           @Value("${regards.session.agent.snapshot.page.size:1000}")
                                                           int snapshotPropertyPageSize) {
        return new AgentSnapshotJobService(jobInfoService,
                                           snapshotStepRepo,
                                           self,
                                           stepPropertyUpdateRequestRepo,
                                           snapshotPropertyPageSize);
    }

    @Bean
    @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
    public AgentSnapshotService agentSnapshotService(ISessionStepRepository sessionStepRepo,
                                                     IStepPropertyUpdateRequestRepository stepPropertyRepo,
                                                     ISnapshotProcessRepository snapshotProcessRepo,
                                                     IPublisher publisher,
                                                     AgentSnapshotService service,
                                                     @Value("${regards.session.agent.step.requests.page.size:1000}")
                                                     int stepPropertyPageSize) {
        return new AgentSnapshotService(sessionStepRepo,
                                        stepPropertyRepo,
                                        snapshotProcessRepo,
                                        publisher,
                                        service,
                                        stepPropertyPageSize);
    }

    @Bean
    @Profile("!noscheduler")
    public AgentSnapshotScheduler agentSnapshotScheduler() {
        return new AgentSnapshotScheduler();
    }
}