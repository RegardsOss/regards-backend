/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 *
 */
public class NewJobPublisherMessageBroker {

    @Autowired
    private Publisher publisher;

    public void sendJob(final String pProjectName, final long pJobInfoId) throws RabbitMQVhostException {
        final AmqpCommunicationMode amqpCommunicationMode = AmqpCommunicationMode.ONE_TO_ONE;
        final AmqpCommunicationTarget amqpCommunicationTarget = AmqpCommunicationTarget.INTERNAL;
        final NewJobEvent event = new NewJobEvent(pJobInfoId);
        publisher.publish(event, amqpCommunicationMode, amqpCommunicationTarget);
    }
}
