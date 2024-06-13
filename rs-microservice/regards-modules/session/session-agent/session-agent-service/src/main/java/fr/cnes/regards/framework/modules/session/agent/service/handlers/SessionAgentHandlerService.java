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
package fr.cnes.regards.framework.modules.session.agent.service.handlers;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequestInfo;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for {@link SessionAgentEventHandler}. It handles new amqp events received and saves new
 * {@link StepPropertyUpdateRequest}s in the database. It creates {@link SnapshotProcess}es related to the
 * source if they do not exist
 *
 * @author Iliana Ghazali
 **/
@MultitenantTransactional
@Service
public class SessionAgentHandlerService {

    @Value("${regards.session.step.merge-similar-event:true}")
    private boolean groupByStepPropertyUpdateRequestEvt;

    /**
     * Repository to save events received
     */
    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyUpdateRequestRepository;

    /**
     * Repository to save snapshot processes
     */
    @Autowired
    private ISnapshotProcessRepository snapshotProcessRepository;

    /**
     * Events handled by {@link SessionAgentEventHandler}
     * Save new {@link StepPropertyUpdateRequest} from {@link StepPropertyUpdateRequestEvent}
     * Initialize new {@link SnapshotProcess}es to process step properties later.
     *
     * @param events {@link StepPropertyUpdateRequestEvent}s
     */
    public Set<String> createStepRequests(List<StepPropertyUpdateRequestEvent> events) {
        List<StepPropertyUpdateRequest> stepPropertiesToSave = new ArrayList<>();
        Set<String> sourcesToBeUpdated = new HashSet<>();
        // Create stepPropertyUpdateRequest with all stepPropertyUpdateRequestEvent received
        // Create the list of sources impacted by these events and create snapshot processes if not existing
        for (StepPropertyUpdateRequestEvent evt : events) {
            StepProperty step = evt.getStepProperty();
            String source = step.getSource();
            StepPropertyInfo stepInfo = evt.getStepProperty().getStepPropertyInfo();
            stepPropertiesToSave.add(new StepPropertyUpdateRequest(step.getStepId(),
                                                                   source,
                                                                   step.getSession(),
                                                                   evt.getDate(),
                                                                   evt.getType(),
                                                                   new StepPropertyUpdateRequestInfo(stepInfo.getStepType(),
                                                                                                     stepInfo.getState(),
                                                                                                     stepInfo.getProperty(),
                                                                                                     stepInfo.getValue(),
                                                                                                     stepInfo.isInputRelated(),
                                                                                                     stepInfo.isOutputRelated())));
            sourcesToBeUpdated.add(source);
        }
        if (groupByStepPropertyUpdateRequestEvt) {
            stepPropertyUpdateRequestRepository.saveAll(createNewListStepPropertyUpdateReqs(stepPropertiesToSave));
        } else {
            stepPropertyUpdateRequestRepository.saveAll(stepPropertiesToSave);
        }
        return sourcesToBeUpdated;
    }

    public void createMissingSnapshotProcesses(Set<String> sources) {
        Set<SnapshotProcess> snapshotProcessesRetrieved = snapshotProcessRepository.findBySourceIn(sources);
        Set<SnapshotProcess> snapshotProcessesToBeCreated = new HashSet<>();
        // Loop on every source impacted and create snapshot process if not existing
        for (String source : sources) {
            if (snapshotProcessesRetrieved.stream().noneMatch(s -> s.getSource().equals(source))) {
                snapshotProcessesToBeCreated.add(new SnapshotProcess(source, null, null));
            }
        }
        this.snapshotProcessRepository.saveAll(snapshotProcessesToBeCreated);
    }

    /**
     * Group the list of {@link StepPropertyUpdateRequest} by {@link StepPropertyUpdateRequest#getClassifier()} in order
     * to compute the sum of their values {@link StepPropertyUpdateRequestInfo#value}
     */
    private List<StepPropertyUpdateRequest> createNewListStepPropertyUpdateReqs(List<StepPropertyUpdateRequest> stepPropertyUpdateReqs) {
        return stepPropertyUpdateReqs.stream()
                                     .collect(Collectors.groupingBy(StepPropertyUpdateRequest::getClassifier))
                                     .entrySet()
                                     .stream()
                                     .map(e -> e.getValue()
                                                .stream()
                                                .reduce((s1, s2) -> createNewStepPropertyUpdateReq(s1, s2)))
                                     .map(s -> s.get())
                                     .toList();

    }

    /**
     * Create the new {@link StepPropertyUpdateRequest} in order to sum the value {@link StepPropertyUpdateRequestInfo#value} of 2 identical
     * {@link StepPropertyUpdateRequest}s.
     */
    private StepPropertyUpdateRequest createNewStepPropertyUpdateReq(StepPropertyUpdateRequest s1,
                                                                     StepPropertyUpdateRequest s2) {
        return new StepPropertyUpdateRequest(s1.getStepId(),
                                             s1.getSource(),
                                             s1.getSession(),
                                             s1.getCreationDate(),
                                             s1.getType(),
                                             new StepPropertyUpdateRequestInfo(s1.getStepPropertyInfo().getStepType(),
                                                                               s1.getStepPropertyInfo().getState(),
                                                                               s1.getStepPropertyInfo().getProperty(),
                                                                               // Sum values of 2 steps
                                                                               Integer.toString(Integer.parseInt(s1.getStepPropertyInfo()
                                                                                                                   .getValue())
                                                                                                + Integer.parseInt(s2.getStepPropertyInfo()
                                                                                                                     .getValue())),
                                                                               s1.getStepPropertyInfo()
                                                                                 .isInputRelated(),
                                                                               s1.getStepPropertyInfo()
                                                                                 .isOutputRelated()));
    }

}