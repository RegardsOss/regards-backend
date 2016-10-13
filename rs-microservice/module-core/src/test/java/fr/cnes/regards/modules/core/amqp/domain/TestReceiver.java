/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp.domain;

import fr.cnes.regards.modules.core.amqp.utils.IHandler;
import fr.cnes.regards.modules.core.amqp.utils.TenantWrapper;

/**
 * @author svissier
 *
 */

public class TestReceiver implements IHandler<TestEvent> {

    private TestEvent message_;

    public TestReceiver() {

    }

    @Override
    public void handle(TenantWrapper<TestEvent> pMessage) {
        message_ = pMessage.getContent();
    }

    public TestEvent getMessage() {
        return message_;
    }
}
