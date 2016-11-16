/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.domain;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;

/**
 * @author svissier
 *
 */

public class TestReceiver implements IHandler<TestEvent> {

    /**
     * message recovered from the broker
     */
    private TestEvent message;

    public TestReceiver() {

    }

    @Override
    public void handle(TenantWrapper<TestEvent> pMessage) {
        message = pMessage.getContent();
    }

    public TestEvent getMessage() {
        return message;
    }
}
