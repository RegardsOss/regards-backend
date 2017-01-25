/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.IPublisher;

/**
 * @author Léo Mieulet
 */
@Service
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

    @Override
    public void send(final Long pJobInfoId) {
        publisher.publish(new StoppingJobEvent(pJobInfoId));
    }

}
