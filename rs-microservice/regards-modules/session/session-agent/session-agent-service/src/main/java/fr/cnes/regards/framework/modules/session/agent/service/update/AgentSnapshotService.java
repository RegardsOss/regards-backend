package fr.cnes.regards.framework.modules.session.agent.service.update;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventStateEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
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

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyRepo;

    @Autowired
    private IPublisher publisher;

    @Value("${regards.session.agent.step.requests.page.size:1000}")
    private int stepPropertyPageSize;

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
        Pageable pageToRequest = PageRequest.of(0, stepPropertyPageSize, Sort.by(Sort.Order.asc("id")));
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
                // publish session steps events
                this.publisher
                        .publish(sessionStepsUpdated.stream().map(SessionStepEvent::new).collect(Collectors.toList()));
            }
        } else {
            Thread.currentThread().interrupt();
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
                    .findBySourceAndDateBetween(source, lastUpdated, freezeDate, pageToRequest);
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
                // if present in the database, initialize sessionStep
                // else create sessionStep
                sessionStep = this.sessionStepRepo.findBySourceAndSessionAndStepId(source, session, stepId)
                        .orElse(new SessionStep(stepId, stepPropertyUpdateRequest.getSource(), session,
                                                stepPropertyUpdateRequest.getStepPropertyInfo().getStepType(),
                                                new StepState(), new SessionStepProperties()));
            }

            // UPDATE SESSION STEP WITH STEP EVENT INFO
            updateSessionStepInfo(sessionStep, stepPropertyUpdateRequest);

            // UPDATE OR ADD SESSION STEP in sessionStepsBySession
            sessionStepsBySession.get(session).put(stepId, sessionStep);

            // UPDATE STEP PROPERTY REQUEST WITH ASSOCIATED SESSION STEP
            stepPropertyUpdateRequest.setSessionStep(sessionStep);
        }

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
        StepPropertyInfo stepPropertyInfo = stepPropertyUpdateRequest.getStepPropertyInfo();

        // UPDATE PROPERTIES
        String property = stepPropertyInfo.getProperty();
        String value = stepPropertyInfo.getValue();
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
            calculateDifferences(sessionStep, stepPropertyInfo, property, NumberUtils.toLong(previousValue),
                                 NumberUtils.toLong(value));

        } else if (type.equals(StepPropertyEventTypeEnum.DEC) && (NumberUtils.isCreatable(previousValue) && NumberUtils
                .isCreatable(value))) {
            // decrement parameters (in/out, state, property)
            calculateDifferences(sessionStep, stepPropertyInfo, property, NumberUtils.toLong(previousValue),
                                 -NumberUtils.toLong(value));

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
     * @param sessionStep      sessionStep to be updated
     * @param stepPropertyInfo information of the step
     * @param property         property to modify
     * @param previousValue    previous value of the corresponding property
     * @param valueNum         new value to update the corresponding property
     */
    private void calculateDifferences(SessionStep sessionStep, StepPropertyInfo stepPropertyInfo, String property,
            long previousValue, long valueNum) {

        // set in/out
        if (stepPropertyInfo.isInputRelated()) {
            sessionStep.setInputRelated(sessionStep.getInputRelated() + valueNum);
        }
        if (stepPropertyInfo.isOutputRelated()) {
            sessionStep.setOutputRelated(sessionStep.getOutputRelated() + valueNum);
        }
        // Set state
        StepState stepState = sessionStep.getState();
        if (stepPropertyInfo.getState().equals(StepPropertyEventStateEnum.WAITING)) {
            stepState.setWaiting(stepState.getWaiting() + valueNum);
        } else if (stepPropertyInfo.getState().equals(StepPropertyEventStateEnum.ERROR)) {
            stepState.setErrors(stepState.getErrors() + valueNum);
        } else if (stepPropertyInfo.getState().equals(StepPropertyEventStateEnum.RUNNING)) {
            stepState.setRunning(stepState.getRunning() + valueNum);
        }
        sessionStep.getProperties().put(property, String.valueOf(previousValue + valueNum));
    }
}