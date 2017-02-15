/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.event;

/**
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
public class PollableException extends Exception {

    public PollableException(String pMessage) {
        super(pMessage);
    }
}
