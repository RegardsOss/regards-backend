package fr.cnes.regards.framework.modules.session.agent.domain.step;

import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;

/**
 * Event states for {@link StepPropertyUpdateRequestEvent}. These parameters can modify the state and the
 * input/output values of the {@link fr.cnes.regards.framework.modules.session.commons.domain.SessionStep}
 *
 * @author Iliana Ghazali
 **/
public enum StepPropertyStateEnum {
    /**
     * When the event in success and is related to an input and/or an output
     */
    SUCCESS,
    /**
     * When the event sent has an impact on the running state of the SessionStep
     */
    RUNNING,
    /**
     * When the event sent has an impact on the error state of the SessionStep
     */
    ERROR,
    /**
     * When the event sent has an impact on the waiting state of the SessionStep
     */
    WAITING,
    /**
     * When the event is given for information. It has no impact on the state of the step.
     */
    INFO;
}