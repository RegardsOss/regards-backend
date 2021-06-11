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
package fr.cnes.regards.framework.modules.session.agent.service.handlers;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequestInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for {@link SessionAgentEventHandler}. It handles new amqp events received and saves new
 * {@link StepPropertyUpdateRequest}s in the database. It creates {@link SnapshotProcess}es related to the
 * source if they do not exist
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class SessionAgentHandlerService {

    /**
     * Repository to save events received
     */
    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyRepo;

    /**
     * Repository to save snapshot processes
     */
    @Autowired
    private ISnapshotProcessRepository snapshotRepo;

    /**
     * Events handled by {@link SessionAgentEventHandler}
     * Save new {@link StepPropertyUpdateRequest} from {@link StepPropertyUpdateRequestEvent}
     * Initialize new {@link SnapshotProcess}es to process step properties later.
     *
     * @param events {@link StepPropertyUpdateRequestEvent}s
     */
    public void createStepRequests(List<StepPropertyUpdateRequestEvent> events) {
        Set<StepPropertyUpdateRequest> stepPropertiesToSave = new HashSet<>();
        Set<String> sourcesToBeUpdated = new HashSet<>();
        // create stepPropertyUpdateRequest with all stepPropertyUpdateRequestEvent received
        // create the list of sources impacted by these events and create snapshot processes if not existing
        for (StepPropertyUpdateRequestEvent e : events) {
            StepProperty step = e.getStepProperty();
            String source = step.getSource();
            StepPropertyInfo stepInfo = e.getStepProperty().getStepPropertyInfo();
            stepPropertiesToSave
                    .add(new StepPropertyUpdateRequest(step.getStepId(), source, step.getSession(), e.getDate(),
                                                       e.getType(),
                                                       new StepPropertyUpdateRequestInfo(stepInfo.getStepType(),
                                                                                         stepInfo.getState(),
                                                                                         stepInfo.getProperty(),
                                                                                         stepInfo.getValue(),
                                                                                         stepInfo.isInputRelated(),
                                                                                         stepInfo.isOutputRelated())));
            sourcesToBeUpdated.add(source);
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
        this.stepPropertyRepo.saveAll(stepPropertiesToSave);
        this.snapshotRepo.saveAll(snapshotProcessesToBeCreated);
    }
}