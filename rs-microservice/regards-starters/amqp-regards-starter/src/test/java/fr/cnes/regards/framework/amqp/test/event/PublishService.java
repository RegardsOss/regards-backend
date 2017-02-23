/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.event;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.amqp.IPublisher;

/**
 * A service that publish an event in a transaction to test tenant binding.
 *
 * @author Marc Sordi
 *
 */
@Service
public class PublishService {

    /**
     * Poller
     */
    private final IPublisher publisher;

    public PublishService(IPublisher pPublisher) {
        this.publisher = pPublisher;
    }

    @Transactional
    public void doSomethingInTransaction() {
        // ... do something

        // Init event
        PollOneAllEvent event = new PollOneAllEvent();
        String message = "Published in transaction!";
        event.setMessage(message);
        publisher.publish(event);

        // ... do something
    }
}
