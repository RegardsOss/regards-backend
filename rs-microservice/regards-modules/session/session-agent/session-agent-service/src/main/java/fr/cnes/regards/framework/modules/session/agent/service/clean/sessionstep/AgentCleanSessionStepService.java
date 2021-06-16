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
package fr.cnes.regards.framework.modules.session.agent.service.clean.sessionstep;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Service to clean old {@link fr.cnes.regards.framework.modules.session.commons.domain.SessionStep}
 * and {@link StepPropertyUpdateRequest }
 *
 * @author Iliana Ghazali
 **/
@MultitenantTransactional
public class AgentCleanSessionStepService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentCleanSessionStepService.class);

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyRepo;

    @Value("${regards.session.agent.clean.session.step.limit.store:30}")
    private int limitStoreSessionSteps;

    @Value("${regards.session.agent.clean.session.step.page:1000}")
    private int pageSize;

    public int clean() {
        // Init startClean with the current date minus the limit of SessionStep save configured
        OffsetDateTime startClean = OffsetDateTime.now().minusDays(this.limitStoreSessionSteps);
        LOGGER.debug("Check old session steps before {}", startClean);
        boolean interrupted = Thread.currentThread().isInterrupted();
        int nbSessionStepsDeleted = 0;
        Pageable page = PageRequest.of(0, pageSize, Sort.by("lastUpdateDate"));
        Page<SessionStep> sessionStepsToDelete;
        do {
            // Get all session steps to delete older than startClean
            sessionStepsToDelete = sessionStepRepo.findByLastUpdateDateBefore(startClean, page);
            // Delete all related StepPropertyUpdateRequests
            this.stepPropertyRepo
                    .deleteInBatch(stepPropertyRepo.findBySessionStepIn(sessionStepsToDelete.getContent()));
            // Delete SessionSteps
            this.sessionStepRepo.deleteInBatch(sessionStepsToDelete);
            nbSessionStepsDeleted += sessionStepsToDelete.getNumberOfElements();
        } while (!interrupted && sessionStepsToDelete.hasNext());

        // log if thread was interrupted
        if (interrupted) {
            LOGGER.debug("{} thread has been interrupted", this.getClass().getName());
        }
        return nbSessionStepsDeleted;
    }
}