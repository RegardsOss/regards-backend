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
package fr.cnes.regards.framework.modules.session.agent.service.update;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequestInfo;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.StepState;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionStepEvent;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Service to create or update {@link SessionStep} with new {@link StepPropertyUpdateRequest}.
 *
 * @author Iliana Ghazali
 **/

@Service
@MultitenantTransactional
public class AgentSnapshotService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSnapshotService.class);

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyRepo;

    @Autowired
    private ISnapshotProcessRepository snapshotProcessRepo;

    @Autowired
    private IPublisher publisher;

    @Value("${regards.session.agent.step.requests.page.size:1000}")
    private int stepPropertyPageSize;

    private OffsetDateTime lastSnapshotDate;

    /**
     * Create or update {@link SessionStep}s with new {@link StepPropertyUpdateRequest}.
     * Publish {@link SessionStepEvent} containing {@link SessionStep}
     *
     * @param snapshotProcess process to retrieve all step properties by source and lastUpdateDate
     * @param freezeDate      corresponding to schedulerStartDate. Limit date to retrieve step properties
     * @return number of {@link SessionStep}s created
     */
    public int generateSessionStep(SnapshotProcess snapshotProcess, OffsetDateTime freezeDate) {
        Map<String, Map<String, SessionStep>> sessionStepsBySession = new HashMap<>();

        // CREATE SESSION STEPS
        boolean interrupted;
        Pageable pageToRequest = PageRequest.of(0, stepPropertyPageSize, Sort.by("date").and(Sort.by("id")));
        List<StepPropertyUpdateRequest> stepPropertyRequestsProcessed = new ArrayList<>();
        // iterate on all pages of stepPropertyUpdateRequest to create SessionSteps
        do {
            pageToRequest = updateOnePageStepRequests(sessionStepsBySession, stepPropertyRequestsProcessed,
                                                      snapshotProcess, freezeDate, pageToRequest);
            interrupted = Thread.currentThread().isInterrupted();
        } while (pageToRequest != null && !interrupted);

        // SAVE SESSION STEPS ONLY IF PROCESS WAS NOT INTERRUPTED
        int sessionUpdatedSize = 0;
        if (!interrupted) {
            List<SessionStep> sessionStepsUpdated = sessionStepsBySession.entrySet().stream()
                    .flatMap(session -> session.getValue().values().stream()).collect(Collectors.toList());
            if (!sessionStepsUpdated.isEmpty()) {
                sessionUpdatedSize = sessionStepsUpdated.size();
                // save session steps
                this.sessionStepRepo.saveAll(sessionStepsUpdated);
                // save step property requests linked to session steps
                this.stepPropertyRepo.saveAll(stepPropertyRequestsProcessed);
                // update snapshotProcess lastUpdateDate with the most recent stepPropertyRequest
                snapshotProcess.setLastUpdateDate(lastSnapshotDate);
                this.snapshotProcessRepo.save(snapshotProcess);
                // publish session steps events
                this.publisher
                        .publish(sessionStepsUpdated.stream().map(SessionStepEvent::new).collect(Collectors.toList()));
            }
        } else {
            LOGGER.debug("{} thread has been interrupted", this.getClass().getName());
        }

        return sessionUpdatedSize;
    }

    /**
     * Create or update SessionSteps by session and by stepId with new StepPropertyUpdateRequest events
     *
     * @param sessionStepsBySession map containing SessionSteps by session and stepId
     * @param stepPropertyProcessed all the stepProperty processed on this page
     * @param snapshotProcess       information about the stepPropertyRequests to process
     * @param freezeDate            only considered stepPropertyRequests before this date
     * @param pageToRequest         page of stepPropertyRequests requested
     * @return nextPageable if present
     */
    private Pageable updateOnePageStepRequests(Map<String, Map<String, SessionStep>> sessionStepsBySession,
            List<StepPropertyUpdateRequest> stepPropertyProcessed, SnapshotProcess snapshotProcess,
            OffsetDateTime freezeDate, Pageable pageToRequest) {
        String source = snapshotProcess.getSource();
        OffsetDateTime lastUpdated = snapshotProcess.getLastUpdateDate();

        // get step property requests to process
        Page<StepPropertyUpdateRequest> stepPropertyPage;
        if (lastUpdated != null) {
            stepPropertyPage = this.stepPropertyRepo
                    .findBySourceAndDateGreaterThanAndDateLessThanEqual(source, lastUpdated, freezeDate, pageToRequest);
        } else {
            stepPropertyPage = this.stepPropertyRepo.findBySourceAndDateBefore(source, freezeDate, pageToRequest);
        }

        // loop on every stepPropertyUpdateRequest to create or update SessionSteps
        List<StepPropertyUpdateRequest> stepPropertyUpdateRequests = stepPropertyPage.getContent();
        for (StepPropertyUpdateRequest stepPropertyUpdateRequest : stepPropertyUpdateRequests) {
            String session = stepPropertyUpdateRequest.getSession();
            String stepId = stepPropertyUpdateRequest.getStepId();

            // CREATE SESSION if not already in sessionStepsBySession
            sessionStepsBySession.computeIfAbsent(session, steps -> new HashMap<>());

            // GET OR CREATE SESSION STEP if not already in sessionStepsBySession[session]
            SessionStep sessionStep = sessionStepsBySession.get(session).get(stepId);
            if (sessionStep == null) {
                // if present in the database, initialize sessionStep else create sessionStep
                sessionStep = this.sessionStepRepo.findBySourceAndSessionAndStepId(source, session, stepId)
                        .orElse(new SessionStep(stepId, stepPropertyUpdateRequest.getSource(), session,
                                                stepPropertyUpdateRequest.getStepPropertyInfo().getStepType(),
                                                new StepState()));
            }

            // UPDATE SESSION STEP WITH STEP EVENT INFO
            updateSessionStepInfo(sessionStep, stepPropertyUpdateRequest);

            // UPDATE OR ADD SESSION STEP in sessionStepsBySession
            sessionStepsBySession.get(session).put(stepId, sessionStep);

            // UPDATE STEP PROPERTY REQUEST WITH ASSOCIATED SESSION STEP
            stepPropertyUpdateRequest.setSessionStep(sessionStep);

            // UPDATE SNAPSHOT PROCESS LAST UPDATE DATE
            OffsetDateTime stepPropertyDate = stepPropertyUpdateRequest.getDate();
            if (lastSnapshotDate == null || lastSnapshotDate.isBefore(stepPropertyDate)) {
                lastSnapshotDate = stepPropertyDate;
            }
        }
        // add stepPropertyRequests processed to the list of stepProperties processed
        stepPropertyProcessed.addAll(stepPropertyUpdateRequests);

        return stepPropertyPage.hasNext() ? stepPropertyPage.nextPageable() : null;
    }

    /**
     * Update sessionStep with stepPropertyUpdateRequest
     *
     * @param sessionStep               aggregation of stepPropertyUpdateRequest
     * @param stepPropertyUpdateRequest to update sessionStep
     */
    private void updateSessionStepInfo(SessionStep sessionStep, StepPropertyUpdateRequest stepPropertyUpdateRequest) {
        StepPropertyUpdateRequestInfo stepPropertyUpdateRequestInfo = stepPropertyUpdateRequest.getStepPropertyInfo();

        // UPDATE PROPERTIES
        String property = stepPropertyUpdateRequestInfo.getProperty();
        String value = stepPropertyUpdateRequestInfo.getValue();
        StepPropertyEventTypeEnum type = stepPropertyUpdateRequest.getType();

        // if eventType is increment, increment the value of the property only if the value is a number
        // else if eventType is decrement, decrement the value of the property only if the value is a number
        // else set with value from step property event
        // if the property is already present, its value will be replaced by the new one

        String previousValue = sessionStep.getProperties().get(property);
        if (previousValue == null) {
            previousValue = "0";
        }
        if (type.equals(StepPropertyEventTypeEnum.INC) && (NumberUtils.isCreatable(previousValue) && NumberUtils
                .isCreatable(value))) {
            // increment parameters (in/out, state, property)
            calculateDifferences(sessionStep, stepPropertyUpdateRequestInfo, property,
                                 NumberUtils.toLong(previousValue), NumberUtils.toLong(value));

        } else if (type.equals(StepPropertyEventTypeEnum.DEC) && (NumberUtils.isCreatable(previousValue) && NumberUtils
                .isCreatable(value))) {
            // decrement parameters (in/out, state, property)
            calculateDifferences(sessionStep, stepPropertyUpdateRequestInfo, property,
                                 NumberUtils.toLong(previousValue), -NumberUtils.toLong(value));

        } else if (type.equals(StepPropertyEventTypeEnum.VALUE) && NumberUtils.isCreatable(value)) {
            // reset all values to 0 if value is a number
            resetSessionStep(sessionStep);
        } else {
            // set property with value
            sessionStep.getProperties().put(property, value);
        }

        // UPDATE lastUpdateDate of SessionStep with the most recent date of stepPropertyUpdateRequest
        if (sessionStep.getLastUpdateDate() == null || sessionStep.getLastUpdateDate()
                .isBefore(stepPropertyUpdateRequest.getDate())) {
            sessionStep.setLastUpdateDate(stepPropertyUpdateRequest.getDate());
        }
    }

    /**
     * Update properties and input/output related attributes of SessionStep
     *
     * @param sessionStep                   sessionStep to be updated
     * @param stepPropertyUpdateRequestInfo information of the step
     * @param property                      property to modify
     * @param previousValue                 previous value of the corresponding property
     * @param valueNum                      new value to update the corresponding property
     */
    private void calculateDifferences(SessionStep sessionStep,
            StepPropertyUpdateRequestInfo stepPropertyUpdateRequestInfo, String property, long previousValue,
            long valueNum) {
        // forbid negative values
        // set in/out
        if (stepPropertyUpdateRequestInfo.isInputRelated()) {
            sessionStep.setInputRelated(Math.max(sessionStep.getInputRelated() + valueNum, 0L));
        }
        if (stepPropertyUpdateRequestInfo.isOutputRelated()) {
            sessionStep.setOutputRelated(Math.max(sessionStep.getOutputRelated() + valueNum, 0L));
        }
        // Set state
        StepState stepState = sessionStep.getState();
        if (stepPropertyUpdateRequestInfo.getState().equals(StepPropertyStateEnum.WAITING)) {
            stepState.setWaiting(Math.max(stepState.getWaiting() + valueNum, 0L));
        } else if (stepPropertyUpdateRequestInfo.getState().equals(StepPropertyStateEnum.ERROR)) {
            stepState.setErrors(Math.max(stepState.getErrors() + valueNum, 0L));
        } else if (stepPropertyUpdateRequestInfo.getState().equals(StepPropertyStateEnum.RUNNING)) {
            stepState.setRunning(Math.max(stepState.getRunning() + valueNum, 0L));
        }
        // set property
        sessionStep.getProperties().put(property, String.valueOf(Math.max(previousValue + valueNum, 0L)));
    }

    private void resetSessionStep(SessionStep sessionStep) {
        // reset input related/output related
        sessionStep.setInputRelated(0L);
        sessionStep.setOutputRelated(0L);
        // reset state
        sessionStep.setState(new StepState());
        // reset all properties
        sessionStep.getProperties().replaceAll((key, val) -> String.valueOf(0L));
    }
}