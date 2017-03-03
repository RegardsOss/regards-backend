/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.event.transactional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.IPollable;

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
    public <T extends IPollable> void transactionalPublish(T pEvent, boolean pCrash) {
        publisher.publish(pEvent);
        // Do something : for instance, store in database
        if (pCrash) {
            // An error occurs : transaction manager will rollback database and restore AMQP event on server
            throw new UnsupportedOperationException("Publish fails!");
        }
    }
}
