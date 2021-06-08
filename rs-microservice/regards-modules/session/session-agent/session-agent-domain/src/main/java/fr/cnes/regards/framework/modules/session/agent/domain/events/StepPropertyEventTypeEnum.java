package fr.cnes.regards.framework.modules.session.agent.domain.events;

/**
 * Event types for {@link StepPropertyUpdateRequestEvent}. They indicate which modification to operate on the
 * properties and state of the {@link fr.cnes.regards.framework.modules.session.commons.domain.SessionStep}
 *
 * @author Iliana Ghazali
 **/
public enum StepPropertyEventTypeEnum {

    /**
     * Increment the values of the SessionStep
     */
    INC,
    /**
     * Decrement the values of the SessionStep
     */
    DEC,
    /**
     * Set the value
     */
    VALUE
}
