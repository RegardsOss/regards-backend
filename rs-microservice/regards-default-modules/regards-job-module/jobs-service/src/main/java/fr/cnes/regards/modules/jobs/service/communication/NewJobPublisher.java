/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 * @author lmieulet
 */
public class NewJobPublisher implements INewJobPublisher {

    /**
     * Allows to publish an event in that queue
     */
    private final IPublisher publisher;

    /**
     * @param pPublisher
     *            Rabit queue publisher
     */
    public NewJobPublisher(final IPublisher pPublisher) {
        super();
        publisher = pPublisher;
    }

    @Override
    public void sendJob(final long pJobInfoId) throws RabbitMQVhostException {
        final AmqpCommunicationMode amqpCommunicationMode = AmqpCommunicationMode.ONE_TO_ONE;
        final AmqpCommunicationTarget amqpCommunicationTarget = AmqpCommunicationTarget.INTERNAL;
        final NewJobEvent event = new NewJobEvent(pJobInfoId);
        publisher.publish(event, amqpCommunicationMode, amqpCommunicationTarget);
    }
}
