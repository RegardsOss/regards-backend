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
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to create or update {@link SessionStep} with new {@link StepPropertyUpdateRequest}.
 *
 * @author Iliana Ghazali
 **/
@MultitenantTransactional
public class AgentSnapshotService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSnapshotService.class);

    private final ISessionStepRepository sessionStepRepo;

    private final IStepPropertyUpdateRequestRepository stepPropertyRepo;

    private final ISnapshotProcessRepository snapshotProcessRepo;

    private final AgentSnapshotService self;

    private final IPublisher publisher;

    private final int stepPropertyPageSize;

    public AgentSnapshotService(ISessionStepRepository sessionStepRepo,
                                IStepPropertyUpdateRequestRepository stepPropertyRepo,
                                ISnapshotProcessRepository snapshotProcessRepo,
                                IPublisher publisher,
                                AgentSnapshotService self,
                                int stepPropertyPageSize) {
        this.sessionStepRepo = sessionStepRepo;
        this.stepPropertyRepo = stepPropertyRepo;
        this.snapshotProcessRepo = snapshotProcessRepo;
        this.publisher = publisher;
        this.stepPropertyPageSize = stepPropertyPageSize;
        this.self = self;

    }

    /**
     * Create or update {@link SessionStep}s with new {@link StepPropertyUpdateRequest}.
     * Publish {@link SessionStepEvent} containing {@link SessionStep}
     *
     * @param snapshotProcess process to retrieve all step properties by source and lastUpdateDate
     * @param freezeDate      corresponding to schedulerStartDate. Limit date to retrieve step properties
     * @return number of {@link SessionStep}s created
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void generateSessionStep(SnapshotProcess snapshotProcess, OffsetDateTime freezeDate) {
        /**
         * NOTE : Method is annotated with Propagation.NOT_SUPPORTED to avoid use a new db connection.
         * The db connection is created for the update method called under.
         * If propagation is set do REQUIRED, then this method will need two connections one for this transaction, and
         * one for the updateOnePageStepRequests transaction with is in propagation.REQUIRED_NEW.
         */
        OffsetDateTime startDate = snapshotProcess.getLastUpdateDate();

        // CREATE SESSION STEPS
        boolean interrupted;
        Pageable pageToRequest = PageRequest.of(0,
                                                stepPropertyPageSize,
                                                Sort.by("registrationDate").and(Sort.by("id")));
        // iterate on all pages of stepPropertyUpdateRequest to create SessionSteps
        do {
            pageToRequest = self.updateOnePageStepRequests(snapshotProcess, startDate, freezeDate, pageToRequest);
            interrupted = Thread.currentThread().isInterrupted();
        } while (pageToRequest != null && !interrupted);
    }

    private void saveSessionSteps(Map<String, Map<String, SessionStep>> sessionStepsBySession) {
        Set<SessionStep> sessionStepsUpdated = sessionStepsBySession.entrySet()
                                                                    .stream()
                                                                    .flatMap(session -> session.getValue()
                                                                                               .values()
                                                                                               .stream())
                                                                    .collect(Collectors.toSet());
        if (!sessionStepsUpdated.isEmpty()) {
            // save session steps
            this.sessionStepRepo.saveAll(sessionStepsUpdated);
            // publish session steps events
            List<SessionStepEvent> sessionStepEvents = sessionStepsUpdated.stream()
                                                                          .map(sessionStep -> new SessionStepEvent(
                                                                              Hibernate.unproxy(sessionStep,
                                                                                                SessionStep.class)))
                                                                          .toList();
            this.publisher.publish(sessionStepEvents);
        }
    }

    /**
     * Create or update SessionSteps by session and by stepId with new StepPropertyUpdateRequest events
     *
     * @param snapshotProcess information about the stepPropertyRequests to process
     * @param startDate       only consider stepPropertyRequests after this date
     * @param endDate         only considered stepPropertyRequests before this date
     * @param pageToRequest   page of stepPropertyRequests requested
     * @return nextPageable if present
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pageable updateOnePageStepRequests(SnapshotProcess previousSnapshotProcess,
                                              OffsetDateTime startDate,
                                              OffsetDateTime endDate,
                                              Pageable pageToRequest) {
        Map<String, Map<String, SessionStep>> sessionStepsBySession = new HashMap<>();
        String source = previousSnapshotProcess.getSource();
        Page<StepPropertyUpdateRequest> stepPropertyPage = getStepPropertiesByPage(startDate,
                                                                                   endDate,
                                                                                   pageToRequest,
                                                                                   source);
        // Update snapshot process that should have been updated in previous page
        SnapshotProcess snapshotProcess = snapshotProcessRepo.findBySource(source).orElse(previousSnapshotProcess);

        // loop on every stepPropertyUpdateRequest to create or update SessionSteps
        List<StepPropertyUpdateRequest> stepPropertyUpdateRequests = stepPropertyPage.getContent();
        boolean interrupted = false;
        for (StepPropertyUpdateRequest stepPropertyUpdateRequest : stepPropertyUpdateRequests) {
            interrupted = Thread.currentThread().isInterrupted();
            if (interrupted) {
                break;
            }
            String session = stepPropertyUpdateRequest.getSession();
            String stepId = stepPropertyUpdateRequest.getStepId();

            // CREATE SESSION if not already in sessionStepsBySession
            sessionStepsBySession.computeIfAbsent(session, steps -> new HashMap<>());

            // GET OR CREATE SESSION STEP if not already in sessionStepsBySession[session]
            SessionStep sessionStep = sessionStepsBySession.get(session).get(stepId);
            if (sessionStep == null) {
                // if present in the database, initialize sessionStep else create sessionStep
                sessionStep = this.sessionStepRepo.findBySourceAndSessionAndStepId(source, session, stepId)
                                                  .orElse(new SessionStep(stepId,
                                                                          stepPropertyUpdateRequest.getSource(),
                                                                          session,
                                                                          stepPropertyUpdateRequest.getStepPropertyInfo()
                                                                                                   .getStepType(),
                                                                          new StepState()));
            }

            // UPDATE SESSION STEP WITH STEP EVENT INFO
            updateSessionStepInfo(sessionStep, stepPropertyUpdateRequest);

            // UPDATE OR ADD SESSION STEP in sessionStepsBySession
            sessionStepsBySession.get(session).put(stepId, sessionStep);

            // UPDATE STEP PROPERTY REQUEST WITH ASSOCIATED SESSION STEP
            stepPropertyUpdateRequest.setSessionStep(sessionStep);
        }
        if (!stepPropertyUpdateRequests.isEmpty()) {
            snapshotProcess.setLastUpdateDate(stepPropertyUpdateRequests.stream()
                                                                        .max(Comparator.comparing(
                                                                            StepPropertyUpdateRequest::getRegistrationDate))
                                                                        .get()
                                                                        .getRegistrationDate());
        }
        if (!interrupted) {
            // add stepPropertyRequests processed to the list of stepProperties processed
            stepPropertyRepo.saveAll(stepPropertyUpdateRequests);
            this.snapshotProcessRepo.save(snapshotProcess);
            saveSessionSteps(sessionStepsBySession);
        } else {
            LOGGER.debug("{} thread has been interrupted", this.getClass().getName());
        }

        return stepPropertyPage.hasNext() ? stepPropertyPage.nextPageable() : null;
    }

    private Page<StepPropertyUpdateRequest> getStepPropertiesByPage(OffsetDateTime startDate,
                                                                    OffsetDateTime endDate,
                                                                    Pageable pageToRequest,
                                                                    String source) {

        // get step property requests to process
        Page<StepPropertyUpdateRequest> stepPropertyPage;
        if (startDate != null) {
            stepPropertyPage = this.stepPropertyRepo.findBySourceAndRegistrationDateGreaterThanAndRegistrationDateLessThan(
                source,
                startDate,
                endDate,
                pageToRequest);
        } else {
            stepPropertyPage = this.stepPropertyRepo.findBySourceAndRegistrationDateBefore(source,
                                                                                           endDate,
                                                                                           pageToRequest);
        }
        return stepPropertyPage;
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
        if (type.equals(StepPropertyEventTypeEnum.INC) && (NumberUtils.isCreatable(previousValue)
                                                           && NumberUtils.isCreatable(value))) {
            // increment parameters (in/out, state, property)
            computePreviousStepDifferences(sessionStep,
                                           stepPropertyUpdateRequestInfo,
                                           property,
                                           NumberUtils.toLong(previousValue),
                                           NumberUtils.toLong(value));

        } else if (type.equals(StepPropertyEventTypeEnum.DEC) && (NumberUtils.isCreatable(previousValue)
                                                                  && NumberUtils.isCreatable(value))) {
            // decrement parameters (in/out, state, property)
            computePreviousStepDifferences(sessionStep,
                                           stepPropertyUpdateRequestInfo,
                                           property,
                                           NumberUtils.toLong(previousValue),
                                           -NumberUtils.toLong(value));

        } else if (type.equals(StepPropertyEventTypeEnum.VALUE) && NumberUtils.isCreatable(value)) {
            // reset all values to 0 if value is a number
            resetSessionStep(sessionStep);
        } else {
            // set property with value
            sessionStep.getProperties().put(property, value);
        }

        // UPDATE lastUpdateDate of SessionStep with the most recent date of stepPropertyUpdateRequest
        if (sessionStep.getLastUpdateDate() == null || sessionStep.getLastUpdateDate()
                                                                  .isBefore(stepPropertyUpdateRequest.getCreationDate())) {
            sessionStep.setLastUpdateDate(stepPropertyUpdateRequest.getCreationDate());
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
    private void computePreviousStepDifferences(SessionStep sessionStep,
                                                StepPropertyUpdateRequestInfo stepPropertyUpdateRequestInfo,
                                                String property,
                                                long previousValue,
                                                long valueNum) {
        // set in/out
        if (stepPropertyUpdateRequestInfo.isInputRelated()) {
            sessionStep.setInputRelated(sessionStep.getInputRelated() + valueNum);
        }
        if (stepPropertyUpdateRequestInfo.isOutputRelated()) {
            sessionStep.setOutputRelated(sessionStep.getOutputRelated() + valueNum);
        }
        // Set state
        StepState stepState = sessionStep.getState();
        if (stepPropertyUpdateRequestInfo.getState().equals(StepPropertyStateEnum.WAITING)) {
            stepState.setWaiting(stepState.getWaiting() + valueNum);
        } else if (stepPropertyUpdateRequestInfo.getState().equals(StepPropertyStateEnum.ERROR)) {
            stepState.setErrors(stepState.getErrors() + valueNum);
        } else if (stepPropertyUpdateRequestInfo.getState().equals(StepPropertyStateEnum.RUNNING)) {
            stepState.setRunning(stepState.getRunning() + valueNum);
        }
        // set property
        sessionStep.getProperties().put(property, String.valueOf(previousValue + valueNum));
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