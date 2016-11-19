/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest.mock;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
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
public class AmqpMockPublisher implements IPublisher {

    @Override
    public <T> void publish(final T pEvt, final AmqpCommunicationMode pAmqpCommunicationMode,
            final AmqpCommunicationTarget pAmqpCommunicationTarget) throws RabbitMQVhostException {
        // Publisher Mock
    }

    @Override
    public <T> void publish(final T pEvt, final AmqpCommunicationMode pAmqpCommunicationMode,
            final AmqpCommunicationTarget pAmqpCommunicationTarget, final int pPriority) throws RabbitMQVhostException {
        // Publisher Mock
    }

    @Override
    public <T> void publish(final String pTenant, final T pEvt, final AmqpCommunicationMode pAmqpCommunicationMode,
            final AmqpCommunicationTarget pAmqpCommunicationTarget) throws RabbitMQVhostException {
        // Publisher Mock
    }

    @Override
    public <T> void publish(final String pTenant, final T pEvt, final AmqpCommunicationMode pAmqpCommunicationMode,
            final AmqpCommunicationTarget pAmqpCommunicationTarget, final int pPriority) throws RabbitMQVhostException {
        // Publisher Mock
    }

}
