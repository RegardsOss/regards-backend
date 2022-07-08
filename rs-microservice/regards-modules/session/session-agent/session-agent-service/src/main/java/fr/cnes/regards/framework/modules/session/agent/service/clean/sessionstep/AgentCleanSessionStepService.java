/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.session.agent.service.clean.sessionstep;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Service to clean old {@link fr.cnes.regards.framework.modules.session.commons.domain.SessionStep}
 * and {@link StepPropertyUpdateRequest }
 *
 * @author Iliana Ghazali
 **/
@MultitenantTransactional
public class AgentCleanSessionStepService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentCleanSessionStepService.class);

    private ISessionStepRepository sessionStepRepo;

    private IStepPropertyUpdateRequestRepository stepPropertyRepo;

    private AgentCleanSessionStepService self;

    @Value("${regards.session.agent.clean.session.step.limit.store:30}")
    private int limitStoreSessionSteps;

    @Value("${regards.session.agent.clean.session.step.page:100}")
    private int pageSize;

    public AgentCleanSessionStepService(ISessionStepRepository sessionStepRepo,
                                        IStepPropertyUpdateRequestRepository stepPropertyRepo,
                                        AgentCleanSessionStepService agentCleanSessionStepService) {
        this.sessionStepRepo = sessionStepRepo;
        this.stepPropertyRepo = stepPropertyRepo;
        this.self = agentCleanSessionStepService;
    }

    public int clean() {
        // Init startClean with the current date minus the limit of SessionStep save configured
        OffsetDateTime startClean = OffsetDateTime.now(ZoneOffset.UTC).minusDays(this.limitStoreSessionSteps);
        LOGGER.debug("Check old session steps before {}", startClean);
        boolean interrupted = Thread.currentThread().isInterrupted();
        int nbSessionStepsDeleted = 0;
        Pageable page = PageRequest.of(0, pageSize, Sort.by("lastUpdateDate"));
        int nbSessionStepsDeletedThisTime = 0;
        do {
            nbSessionStepsDeletedThisTime = self.deleteOnePage(startClean, page);
            nbSessionStepsDeleted += nbSessionStepsDeletedThisTime;
        } while (!interrupted && nbSessionStepsDeletedThisTime != 0);

        // log if thread was interrupted
        if (interrupted) {
            LOGGER.debug("{} thread has been interrupted", this.getClass().getName());
        }
        return nbSessionStepsDeleted;
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteOnePage(OffsetDateTime startClean, Pageable page) {
        int nbDeleted = 0;
        // Get all session steps to delete older than startClean
        Page<SessionStep> sessionStepsToDelete = sessionStepRepo.findByLastUpdateDateBefore(startClean, page);
        // Delete all related StepPropertyUpdateRequests
        for (SessionStep s : sessionStepsToDelete.getContent()) {
            stepPropertyRepo.deleteBySessionStep(s.getStepId(), s.getSource(), s.getSession());
            nbDeleted += sessionStepRepo.deleteAllByStep(s.getStepId(), s.getSource(), s.getSession());
        }
        return nbDeleted;
    }
}