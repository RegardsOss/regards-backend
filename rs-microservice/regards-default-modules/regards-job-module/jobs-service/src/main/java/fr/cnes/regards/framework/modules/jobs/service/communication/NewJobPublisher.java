/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.service.communication;

import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.IPublisher;

/**
 * @author Léo Mieulet
 */
@Service
public class NewJobPublisher implements INewJobPublisher {

    /**
     * Allows to publish an event in that queue
     */
    private final IPublisher publisher;

    /**
     * @param pPublisher
     *            Rabbit queue publisher
     */
    public NewJobPublisher(final IPublisher pPublisher) {
        super();
        publisher = pPublisher;
    }

    @Override
    public void sendJob(final long pJobInfoId) {
        publisher.publish(new NewJobEvent(pJobInfoId));
    }
}
