/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.event;

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
public class PollableService<T extends IPollable> {

    /**
     * Poller
     */
    private final IPoller poller;

    public PollableService(IPoller pPoller) {
        this.poller = pPoller;
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantWrapper<T> pollAndSave(Class<T> pEventType, boolean pCrash) throws PollableException {
        TenantWrapper<T> wrapper = poller.poll(pEventType);
        // Do something : for instance, store in database not to lose event
        if (pCrash) {
            // An error occurs : transaction manager will rollback database and restore AMQP event on server
            throw new PollableException("Poll fails!");
        } else {
            return wrapper;
        }
    }
}
