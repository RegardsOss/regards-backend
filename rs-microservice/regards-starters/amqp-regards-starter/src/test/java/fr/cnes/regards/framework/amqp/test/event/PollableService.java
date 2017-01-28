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

    /**
     * Crash tester
     */
    private final boolean crash;

    public PollableService(IPoller pPoller, boolean crash) {
        this.poller = pPoller;
        this.crash = crash;
    }

    @Transactional
    public TenantWrapper<T> pollAndSave(String pTenant, Class<T> pEventType) {
        TenantWrapper<T> wrapper = poller.poll(pTenant, pEventType);
        // FIXME store in database not to lose event
        if (crash) {
            throw new UnsupportedOperationException();
        } else {
            return wrapper;
        }
    }
}
