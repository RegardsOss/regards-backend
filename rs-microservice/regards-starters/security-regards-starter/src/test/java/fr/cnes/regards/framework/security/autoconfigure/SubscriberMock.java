/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 *
 * Class SubscriberMock
 *
 * Test class to mock AMQP Subscriber
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class SubscriberMock implements ISubscriber {

    @Override
    public <T> void subscribeTo(final Class<T> pEvt, final IHandler<T> pReceiver,
            final AmqpCommunicationMode pAmqpCommunicationMode, final AmqpCommunicationTarget pAmqpCommunicationTarget)
            throws RabbitMQVhostException {
        // Nothing to do.
    }

}
