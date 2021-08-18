package fr.cnes.regards.modules.crawler.service.session;

import com.google.common.base.Strings;
import fr.cnes.regards.framework.modules.session.agent.client.ISessionAgentClient;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepLight;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Sylvain VISSIERE-GUERINET
 * @author Iliana GHAZALI
 */
@Component
public class SessionNotifier {

    /**
     * The name of the property gathering all metadata about this processing step
     */
    public static final String GLOBAL_SESSION_STEP = "catalog";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionNotifier.class);

    /**
     * Service to notify changes
     */
    @Autowired
    private ISessionAgentClient sessionAgentClient;

    // indexation methods

    public void notifyIndexedSuccess(String sessionOwner, String session, long value) {
        notifyIncrementSession(sessionOwner, session, SessionNotifierPropertyEnum.PROPERTY_AIP_INDEXED, value);
    }

    public void notifyIndexDeletion(String sessionOwner, String session) {
        notifyDecrementSession(sessionOwner, session, SessionNotifierPropertyEnum.PROPERTY_AIP_INDEXED, 1L);
    }

    public void notifyIndexedError(String sessionOwner, String session, long value) {
        notifyIncrementSession(sessionOwner, session, SessionNotifierPropertyEnum.PROPERTY_AIP_INDEXED_ERROR, value);
    }

    public void notifyGlobalIndexDeletion(List<ISessionStepLight> sessionToNotify) {
        notifyValueSession(sessionToNotify, SessionNotifierPropertyEnum.RESET, 0L);
    }
    // ----------- UTILS -----------
    // GENERIC METHODS TO BUILD NOTIFICATIONS

    // INC
    private void notifyIncrementSession(String sessionOwner, String session, SessionNotifierPropertyEnum property,
            long value) {
        if (!Strings.isNullOrEmpty(sessionOwner) && !Strings.isNullOrEmpty(session)) {
            StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, sessionOwner, session,
                                                 new StepPropertyInfo(StepTypeEnum.DISSEMINATION, property.getState(),
                                                                      property.getName(), String.valueOf(value),
                                                                      property.isInputRelated(),
                                                                      property.isOutputRelated()));
            sessionAgentClient.increment(step);
        } else {
            LOGGER.debug(
                    "Session has not been notified of {} features because either sessionOwner({}) or session({}) is null or empty",
                    value, sessionOwner, session);
        }
    }

    // DEC
    private void notifyDecrementSession(String sessionOwner, String session, SessionNotifierPropertyEnum property,
            long value) {
        if (!Strings.isNullOrEmpty(sessionOwner) && !Strings.isNullOrEmpty(session)) {
            StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, sessionOwner, session,
                                                 new StepPropertyInfo(StepTypeEnum.DISSEMINATION, property.getState(),
                                                                      property.getName(), String.valueOf(value),
                                                                      property.isInputRelated(),
                                                                      property.isOutputRelated()));
            sessionAgentClient.decrement(step);
        } else {
            LOGGER.debug("Session has not been notified of {} features because either sessionOwner({}) or session"
                                 + "({}) is null or empty", value, sessionOwner, session);

        }
    }

    // VALUE
    private void notifyValueSession(List<ISessionStepLight> sessionToNotify, SessionNotifierPropertyEnum property,
            long value) {
        // create events
        List<StepProperty> stepPropertyList = new ArrayList<>();
        sessionToNotify.forEach(step -> stepPropertyList
                .add(new StepProperty(GLOBAL_SESSION_STEP, step.getSource(), step.getSession(),
                                      new StepPropertyInfo(StepTypeEnum.DISSEMINATION, property.getState(),
                                                           property.getName(), String.valueOf(value),
                                                           property.isInputRelated(), property.isOutputRelated()))));

        sessionAgentClient.stepValue(stepPropertyList);
    }
}