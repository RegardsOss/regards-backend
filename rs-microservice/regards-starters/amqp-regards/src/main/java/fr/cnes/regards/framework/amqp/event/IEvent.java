/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.event;

/**
 * This interface idenfies an event and its target.
 *
 * @author Marc Sordi
 *
 */
public interface IEvent {

    /**
     * Define the event target type
     *
     * @return {@link Target}
     */
    Target to();

    default String withCustomTarget() {
        throw new UnsupportedOperationException("Custom target not implement yet");
    }
}
