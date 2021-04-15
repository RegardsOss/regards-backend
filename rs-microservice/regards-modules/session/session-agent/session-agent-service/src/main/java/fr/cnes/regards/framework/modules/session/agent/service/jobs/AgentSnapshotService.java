package fr.cnes.regards.framework.modules.session.agent.service.jobs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.modules.session.agent.domain.EventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.StepEvent;
import fr.cnes.regards.framework.modules.session.agent.domain.StepEventStateEnum;
import fr.cnes.regards.framework.modules.session.sessioncommons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.sessioncommons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.sessioncommons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.sessioncommons.domain.SessionStepProperties;
import fr.cnes.regards.framework.modules.session.sessioncommons.domain.StepState;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Iliana Ghazali
 **/

@Service
@RegardsTransactional
public class AgentSnapshotService {

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    @Autowired
    private ISnapshotProcessRepository snapshotProcessRepo;

    @Autowired
    private Gson gson;

    public int generateSessionStep(String source, Set<StepEvent> stepEvents) {
        Map<String, Map<String, SessionStep>> sessionStepsBySession = new HashMap<>();
        int nbSessionSteps = 0;

        for (StepEvent stepEvent : stepEvents) {
            // GET OR CREATE SESSION if not already in sessionStepsBySession
            String session = stepEvent.getSession();
            String stepId = stepEvent.getStepId();
            // init session with data received
            initSessionStepsBySession(sessionStepsBySession,
                                      sessionStepRepo.findSessionStepBySourceAndSession(source, session));
            sessionStepsBySession.computeIfAbsent(session, sessionStepsByStepId -> new HashMap<>());

            // GET OR CREATE SESSION STEP if not already in sessionStepsBySession[session]
            SessionStep sessionStep = sessionStepsBySession.get(session).get(stepId);
            if (sessionStep == null) {
                sessionStep = new SessionStep(stepId, stepEvent.getSource(), session, stepEvent.getStepType(),
                                              new StepState(), new SessionStepProperties());
            }
            // UPDATE SESSION STEP WITH STEP EVENT INFO
            updateSessionStepInfo(sessionStep, stepEvent);

            // UPDATE OR ADD SESSION STEP in sessionStepsBySession
            sessionStepsBySession.get(session).put(stepId, sessionStep);
        }

        // SAVE ALL ELEMENTS BY SESSION
        for (Map.Entry<String, Map<String, SessionStep>> sessionStepBySession : sessionStepsBySession.entrySet()) {
            // get all sessionSteps by stepId
            Collection<SessionStep> sessionStepById = sessionStepBySession.getValue().values();
            // update counter
            nbSessionSteps += sessionStepById.size();
            // save sessionSteps
            this.sessionStepRepo.saveAll(sessionStepById);
        }
        return nbSessionSteps;
    }

    private void initSessionStepsBySession(Map<String, Map<String, SessionStep>> sessionStepsBySession,
            Set<SessionStep> sessionStepsRetrieved) {
        // loop on all the session steps retrieved from the database and init sessionStepsBySession map with
        // their values
        for (SessionStep sessionStep : sessionStepsRetrieved) {
            String session = sessionStep.getSession();
            String stepId = sessionStep.getStepId();
            // create session if not already existing in sessionStepsBySession
            if (!sessionStepsBySession.containsKey(session)) {
                sessionStepsBySession.put(session, Maps.newHashMap(ImmutableMap.of(stepId, sessionStep)));
            } else if (!sessionStepsBySession.get(session).containsKey(stepId)) {
                // create stepId if not already existing in the session
                sessionStepsBySession.get(session).put(stepId, sessionStep);
            }
        }
    }

    private void updateSessionStepInfo(SessionStep sessionStep, StepEvent stepEvent) {
        // addition of input relative
        if (stepEvent.isInput_related()) {
            sessionStep.setIn(sessionStep.getIn() + 1);
        }
        // addition of output relative
        if (stepEvent.isInput_related()) {
            sessionStep.setIn(sessionStep.getOut() + 1);
        }
        // set state
        StepEventStateEnum state = stepEvent.getState();
        if (state.equals(StepEventStateEnum.WAITING)) {
            sessionStep.getState().setErrors(sessionStep.getState().getWaiting() + 1);
        } else if (state.equals(StepEventStateEnum.ERROR)) {
            sessionStep.getState().setErrors(sessionStep.getState().getErrors() + 1);

        } else if (!sessionStep.getState().isRunning() && stepEvent.getState().equals(StepEventStateEnum.RUNNING)) {
            sessionStep.getState().setRunning(true);
        }
        // update properties
        String property = stepEvent.getProperty();
        String value = stepEvent.getValue();
        EventTypeEnum type = stepEvent.getEventTypeEnum();

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
                if (type.equals(EventTypeEnum.INC)) {
                    sessionStep.getProperties().put(property, String.valueOf(
                            NumberUtils.toLong(valueToUpdate) + NumberUtils.toLong(value)));
                } else if (type.equals(EventTypeEnum.DEC)) {
                    sessionStep.getProperties().put(property, String.valueOf(
                            NumberUtils.toLong(valueToUpdate) - NumberUtils.toLong(value)));
                }
            } else {
                sessionStep.getProperties().put(property, value);
            }
        }
    }
}
