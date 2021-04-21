package fr.cnes.regards.framework.modules.session.agent.domain.events.update;

/**
 * Event types for {@link StepPropertyUpdateRequestEvent}
 *
 * @author Iliana Ghazali
 **/
public enum StepPropertyEventTypeEnum {

    /**
     * Increment the value
     */
    INC,
    /**
     * Decrement the value
     */
    DEC,
    /**
     * Set the value
     */
    VALUE
}
