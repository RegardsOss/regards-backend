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
public class StoppingJobPublisher implements IStoppingJobPublisher {

    /**
     * Allows to publish such event
     */
    private final IPublisher publisher;

    /**
     * @param pPublisher
     *            the publisher instance
     */
    public StoppingJobPublisher(final IPublisher pPublisher) {
        publisher = pPublisher;
    }

    /**
     * @param pJobInfoId
     *            the jobInfo id
     */
    @Override
    public void send(final Long pJobInfoId) throws RabbitMQVhostException {
        final AmqpCommunicationMode mode = AmqpCommunicationMode.ONE_TO_MANY;
        final AmqpCommunicationTarget target = AmqpCommunicationTarget.INTERNAL;
        final StoppingJobEvent event = new StoppingJobEvent(pJobInfoId);
        publisher.publish(event, mode, target);
    }

}
