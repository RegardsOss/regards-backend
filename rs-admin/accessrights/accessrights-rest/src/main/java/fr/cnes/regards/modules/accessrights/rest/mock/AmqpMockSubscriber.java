/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest.mock;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 *
 * Class AmqpMockPublisher
 *
 * Mock to handle amqp messages
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Component
public class AmqpMockSubscriber implements ISubscriber {

    @Override
    public <T> void subscribeTo(final Class<T> pEvt, final IHandler<T> pReceiver,
            final AmqpCommunicationMode pAmqpCommunicationMode, final AmqpCommunicationTarget pAmqpCommunicationTarget)
            throws RabbitMQVhostException {
        // Subsriber Mock
    }

}
