/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp.domain;

/**
 * @author svissier
 *
 */

public class TestReceiver {

    private TestEvent message_;

    public TestReceiver() {

    }

    public void receive(TestEvent pMessage) {
        message_ = pMessage;
    }

    public TestEvent getMessage() {
        return message_;
    }
}
