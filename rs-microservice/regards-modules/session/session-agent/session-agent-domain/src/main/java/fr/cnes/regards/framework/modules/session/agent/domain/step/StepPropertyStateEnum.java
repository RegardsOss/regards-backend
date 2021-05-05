package fr.cnes.regards.framework.modules.session.agent.domain.step;

import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;

/**
 * Event states for {@link StepPropertyUpdateRequestEvent}
 *
 * @author Iliana Ghazali
 **/
public enum StepPropertyStateEnum {
    SUCCESS, RUNNING, ERROR, WAITING, INFO;
}
