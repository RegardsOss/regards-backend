/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp.domain;

import fr.cnes.regards.modules.core.amqp.Handler;

/**
 * @author svissier
 *
 */

public class TestReceiver implements Handler<TestEvent> {

    private TestEvent message_;

    public TestReceiver() {

    }

    @Override
    public void handle(TestEvent pMessage) {
        message_ = pMessage;
    }

    public TestEvent getMessage() {
        return message_;
    }
}
