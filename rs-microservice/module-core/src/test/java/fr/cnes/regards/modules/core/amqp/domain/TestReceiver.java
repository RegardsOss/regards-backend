/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp.domain;

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
