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
package fr.cnes.regards.framework.modules.session.manager.service.handlers;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionStepEvent;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service for {@link SessionManagerHandler}. It handles new amqp events received and saves new {@link SessionStep}
 * in the database.
 *
 * @author Iliana Ghazali
 **/
@MultitenantTransactional
public class SessionManagerHandlerService {

    /**
     * Repository to save events received
     */
    @Autowired
    private ISessionStepRepository sessionStepRepo;

    /**
     * Repository to save snapshot processes
     */
    @Autowired
    private ISnapshotProcessRepository snapshotRepo;

    /**
     * Events handler by {@link SessionManagerHandler}
     * Save new {@link SessionStep}s from {@link SessionStepEvent}s
     *
     * @param events {@link SessionStepEvent}s
     */
    public void createSessionSteps(List<SessionStepEvent> events) {
        List<SessionStep> sessionSteps = new ArrayList<>();
        Set<String> sourcesToBeUpdated = new HashSet<>();

        // create stepPropertyUpdateRequest with all stepPropertyUpdateRequestEvent received
        // create the list of sources impacted by these events and create snapshot processes if not existing
        for (SessionStepEvent sessionStepEvent : events) {
            SessionStep sessionStep = sessionStepEvent.getSessionStep();
            sessionStep.setRegistrationDate(OffsetDateTime.now());
            sessionSteps.add(sessionStep);
            sourcesToBeUpdated.add(sessionStep.getSource());
        }

        // get the list of snapshot processes from the database
        Set<SnapshotProcess> snapshotProcessesRetrieved = snapshotRepo.findBySourceIn(sourcesToBeUpdated);
        Set<SnapshotProcess> snapshotProcessesToBeCreated = new HashSet<>();
        // loop on every source impacted and create snapshot process if not existing
        for (String source : sourcesToBeUpdated) {
            if (snapshotProcessesRetrieved.stream().noneMatch(s -> s.getSource().equals(source))) {
                snapshotProcessesToBeCreated.add(new SnapshotProcess(source, null, null));
            }
        }

        // save changes
        this.sessionStepRepo.saveAll(sessionSteps);
        this.snapshotRepo.saveAll(snapshotProcessesToBeCreated);
    }
}