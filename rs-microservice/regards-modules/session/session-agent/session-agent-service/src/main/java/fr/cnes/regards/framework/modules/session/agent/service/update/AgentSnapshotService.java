package fr.cnes.regards.framework.modules.session.agent.service.update;

import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventStateEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.sessioncommons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.sessioncommons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.sessioncommons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.sessioncommons.domain.SessionStepProperties;
import fr.cnes.regards.framework.modules.session.sessioncommons.domain.StepState;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

    public int generateSessionStep(String source, List<StepPropertyUpdateRequest> stepPropertyUpdateRequests) {
        Map<String, Map<String, SessionStep>> sessionStepsBySession = new HashMap<>();
        int nbSessionSteps = 0;

        // init session with data received
        for (StepPropertyUpdateRequest stepPropertyUpdateRequest : stepPropertyUpdateRequests) {
            // GET OR CREATE SESSION if not already in sessionStepsBySession
            String session = stepPropertyUpdateRequest.getSession();
            String stepId = stepPropertyUpdateRequest.getStepId();

            if (!sessionStepsBySession.containsKey(session)) {
                sessionStepsBySession.put(session, new HashMap<>());
                // init eventually with sessionSteps from the database some are already linked to this session
                initSessionStepsBySession(source, session, sessionStepsBySession);
            }

            // GET OR CREATE SESSION STEP if not already in sessionStepsBySession[session]
            SessionStep sessionStep = sessionStepsBySession.get(session).get(stepId);
            if (sessionStep == null) {
                sessionStep = new SessionStep(stepId, stepPropertyUpdateRequest.getSource(), session, stepPropertyUpdateRequest.getStepType(),
                                              new StepState(), new SessionStepProperties());
            }
            // UPDATE SESSION STEP WITH STEP EVENT INFO
            updateSessionStepInfo(sessionStep, stepPropertyUpdateRequest);

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

    private void initSessionStepsBySession(String source, String session, Map<String, Map<String, SessionStep>> sessionStepsBySession) {
        // loop on all the session steps retrieved from the database and init sessionStepsBySession map with
        // their values
        Set<SessionStep> sessionStepsRetrieved = this.sessionStepRepo.findSessionStepBySourceAndSession(source, session);
        if (!sessionStepsRetrieved.isEmpty()) {
            for (SessionStep sessionStep : sessionStepsRetrieved) {
                sessionStepsBySession.get(session).put(sessionStep.getStepId(), sessionStep);
            }
        }
    }

    private void updateSessionStepInfo(SessionStep sessionStep, StepPropertyUpdateRequest stepPropertyUpdateRequest) {
        // addition of input relative
        if (stepPropertyUpdateRequest.isInput_related()) {
            sessionStep.setIn(sessionStep.getIn() + 1);
        }
        // addition of output relative
        if (stepPropertyUpdateRequest.isInput_related()) {
            sessionStep.setIn(sessionStep.getOut() + 1);
        }
        // set state
        StepPropertyEventStateEnum state = stepPropertyUpdateRequest.getState();
        if (state.equals(StepPropertyEventStateEnum.WAITING)) {
            sessionStep.getState().setErrors(sessionStep.getState().getWaiting() + 1);
        } else if (state.equals(StepPropertyEventStateEnum.ERROR)) {
            sessionStep.getState().setErrors(sessionStep.getState().getErrors() + 1);

        } else if (!sessionStep.getState().isRunning() && stepPropertyUpdateRequest.getState()
                .equals(StepPropertyEventStateEnum.RUNNING)) {
            sessionStep.getState().setRunning(true);
        }
        // update properties
        String property = stepPropertyUpdateRequest.getProperty();
        String value = stepPropertyUpdateRequest.getValue();
        StepPropertyEventTypeEnum type = stepPropertyUpdateRequest.getType();

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

        // update lastUpdateDate of SessionStep with the most recent date of stepPropertyUpdateRequest
        if(sessionStep.getLastUpdate() == null || sessionStep.getLastUpdate().isBefore(stepPropertyUpdateRequest.getDate())) {
            sessionStep.setLastUpdate(stepPropertyUpdateRequest.getDate());
        }
    }
}
