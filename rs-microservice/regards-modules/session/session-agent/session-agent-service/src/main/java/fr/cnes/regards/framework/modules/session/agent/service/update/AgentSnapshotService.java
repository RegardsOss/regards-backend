package fr.cnes.regards.framework.modules.session.agent.service.update;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventStateEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.StepState;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to create or update {@link SessionStep} with new {@link StepPropertyUpdateRequest}.
 *
 * @author Iliana Ghazali
 **/

@Service
@RegardsTransactional
public class AgentSnapshotService {

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyRepo;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private AgentSnapshotService self;


    public void generateSessionStepPage() {

    }

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
        String source = snapshotProcess.getSource();
        OffsetDateTime lastUpdated = snapshotProcess.getLastUpdate();
        Set<StepPropertyUpdateRequest> stepPropertyUpdateRequests;

        // Get all events to process
        if (lastUpdated != null) {
            stepPropertyUpdateRequests = this.stepPropertyRepo
                    .findBySourceAndDateBetween(source, lastUpdated, freezeDate);
        } else {
            stepPropertyUpdateRequests = this.stepPropertyRepo.findBySourceAndDateBefore(source, freezeDate);
        }
        //FIXME : add pagination for stepPropertyUpdateRequests

        // loop on every stepPropertyUpdateRequest to create or update stepEvents
        for (StepPropertyUpdateRequest stepPropertyUpdateRequest : stepPropertyUpdateRequests) {
            String session = stepPropertyUpdateRequest.getSession();
            String stepId = stepPropertyUpdateRequest.getStepId();

            // CREATE SESSION if not already in sessionStepsBySession
            if (!sessionStepsBySession.containsKey(session)) {
                sessionStepsBySession.put(session, new HashMap<>());
            }

            // GET OR CREATE SESSION STEP if not already in sessionStepsBySession[session]
            SessionStep sessionStep = sessionStepsBySession.get(session).get(stepId);
            if (sessionStep == null) {
                // if present in the database, initialize sessionStep
                // else create sessionStep
                Optional<SessionStep> sessionStepOpt = this.sessionStepRepo
                        .findBySourceAndSessionAndStepId(source, session, stepId);
                if (sessionStepOpt.isPresent()) {
                    sessionStep = sessionStepOpt.get();
                    sessionStep.getState().setRunning(false); // init running state
                } else {
                    sessionStep = new SessionStep(stepId, stepPropertyUpdateRequest.getSource(), session,
                                                  stepPropertyUpdateRequest.getStepPropertyInfo().getStepType(),
                                                  new StepState(), new SessionStepProperties());
                }
            }

            // UPDATE SESSION STEP WITH STEP EVENT INFO
            updateSessionStepInfo(sessionStep, stepPropertyUpdateRequest);

            // UPDATE OR ADD SESSION STEP in sessionStepsBySession
            sessionStepsBySession.get(session).put(stepId, sessionStep);

            // UPDATE STEP PROPERTY REQUEST WITH ASSOCIATED SESSION STEP
            stepPropertyUpdateRequest.setSessionStep(sessionStep);
        }

        // SAVE SESSION STEPS
        List<SessionStep> sessionStepsUpdated = sessionStepsBySession.entrySet().stream()
                .flatMap(session -> session.getValue().values().stream()).collect(Collectors.toList());
        if(!sessionStepsUpdated.isEmpty()) {
            this.sessionStepRepo.saveAll(sessionStepsUpdated);
            // UPDATE STEP PROPERTY REQUEST
            this.stepPropertyRepo.saveAll(stepPropertyUpdateRequests);

            // PUBLISH NEW SESSION STEPS
            this.publisher.publish(sessionStepsUpdated.stream().map(SessionStepEvent::new).collect(Collectors.toList()));
        }
        return sessionStepsUpdated.size();
    }

    /**
     * Update sessionStep with stepPropertyUpdateRequest
     *
     * @param sessionStep               aggregation of stepPropertyUpdateRequest
     * @param stepPropertyUpdateRequest to update sessionStep
     */
    private void updateSessionStepInfo(SessionStep sessionStep, StepPropertyUpdateRequest stepPropertyUpdateRequest) {
        StepPropertyInfo stepPropertyInfo = stepPropertyUpdateRequest.getStepPropertyInfo();
        // addition of input relative
        if (stepPropertyInfo.isInputRelated()) {
            sessionStep.setInputRelated(sessionStep.getInputRelated() + 1);
        }
        // addition of output relative
        if (stepPropertyInfo.isOutputRelated()) {
            sessionStep.setOutputRelated(sessionStep.getOutputRelated() + 1);
        }
        // set state
        StepPropertyEventStateEnum state = stepPropertyInfo.getState();
        if (state.equals(StepPropertyEventStateEnum.WAITING)) {
            sessionStep.getState().setWaiting(sessionStep.getState().getWaiting() + 1);
        } else if (state.equals(StepPropertyEventStateEnum.ERROR)) {
            sessionStep.getState().setErrors(sessionStep.getState().getErrors() + 1);
        } else if (!sessionStep.getState().isRunning() && state.equals(StepPropertyEventStateEnum.RUNNING)) {
            sessionStep.getState().setRunning(true);
        }

        // update properties
        updateProperties(sessionStep, stepPropertyInfo.getProperty(), stepPropertyInfo.getValue(),
                         stepPropertyUpdateRequest.getType());

        // update lastUpdateDate of SessionStep with the most recent date of stepPropertyUpdateRequest
        if (sessionStep.getLastUpdate() == null || sessionStep.getLastUpdate()
                .isBefore(stepPropertyUpdateRequest.getDate())) {
            sessionStep.setLastUpdate(stepPropertyUpdateRequest.getDate());
        }
    }

    private void updateProperties(SessionStep sessionStep, String property, String value,
            StepPropertyEventTypeEnum type) {
        // find if property is already in sessionStep.properties
        if (!sessionStep.getProperties().containsKey(property)) {
            // If not found, put the new property with its value
            sessionStep.getProperties().put(property, value);
        } else {
            // If found, update the property
            // if eventType is increment, increment the value of the property only if the value is a number
            // else if eventType is decrement, decrement the value of the property only if the value is a number
            // else replace property
            String valueToUpdate = sessionStep.getProperties().get(property);

            if (NumberUtils.isCreatable(valueToUpdate) && NumberUtils.isCreatable(value)) {
                if (type.equals(StepPropertyEventTypeEnum.INC)) {
                    sessionStep.getProperties().put(property, String.valueOf(
                            NumberUtils.toLong(valueToUpdate) + NumberUtils.toLong(value)));
                } else if (type.equals(StepPropertyEventTypeEnum.DEC)) {
                    sessionStep.getProperties().put(property, String.valueOf(
                            NumberUtils.toLong(valueToUpdate) - NumberUtils.toLong(value)));
                }
            } else {
                sessionStep.getProperties().put(property, value);
            }
        }
    }
}