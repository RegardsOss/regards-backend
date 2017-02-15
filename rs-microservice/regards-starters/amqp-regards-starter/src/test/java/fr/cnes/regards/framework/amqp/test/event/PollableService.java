/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.event;

import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.IPollable;

/**
 * TODO
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
    public TenantWrapper<T> pollAndSave(String pTenant, Class<T> pEventType, boolean pCrash) throws PollableException {
        TenantWrapper<T> wrapper = poller.poll(pTenant, pEventType);
        // FIXME store in database not to lose event
        if (pCrash) {
            throw new PollableException("Poll fails!");
        } else {
            return wrapper;
        }
    }
}
