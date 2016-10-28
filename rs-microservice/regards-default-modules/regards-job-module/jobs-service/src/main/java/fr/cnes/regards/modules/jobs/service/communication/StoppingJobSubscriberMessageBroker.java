/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;

/**
 *
 */
public class StoppingJobSubscriberMessageBroker implements IHandler<StoppingJobEvent> {

    @Override
    public void handle(final TenantWrapper<StoppingJobEvent> pT) {
        // TODO Auto-generated method stub

    }

}
