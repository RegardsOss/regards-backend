/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.event.transactional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.IPollable;

/**
 * A service that poll an event in a transaction enabling acknowledgement feature.
 *
 * @author Marc Sordi
 *
 */
@Service
public class PollableService {

    /**
     * Poller
     */
    private final IPoller poller;

    public PollableService(IPoller pPoller) {
        this.poller = pPoller;
    }

    @Transactional
    public <T extends IPollable> TenantWrapper<T> transactionalPoll(Class<T> pEventType, boolean pCrash) {
        TenantWrapper<T> wrapper = poller.poll(pEventType);
        // Do something : for instance, store in database not to lose event
        if (pCrash) {
            // An error occurs : transaction manager will rollback database and restore AMQP event on server
            throw new UnsupportedOperationException("Poll fails!");
        } else {
            return wrapper;
        }
    }
}
