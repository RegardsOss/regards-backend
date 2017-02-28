/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.event;

/**
 * @author Marc Sordi
 *
 */
public abstract class AbstractEvent {

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String pMessage) {
        message = pMessage;
    }
}
