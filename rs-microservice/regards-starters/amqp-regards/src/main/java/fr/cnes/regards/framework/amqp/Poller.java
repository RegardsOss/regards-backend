/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * {@link Poller} uses {@link IRuntimeTenantResolver} to resolve current thread tenant to poll an event in the
 * multitenant context.
 *
 * @author svissier
 * @author Marc Sordi
 *
 */
public class Poller extends AbstractPoller implements IPoller {

    /**
     * Resolve thread tenant
     */
    private final IRuntimeTenantResolver threadTenantResolver;

    public Poller(IRabbitVirtualHostAdmin pVirtualHostAdmin, RabbitTemplate pRabbitTemplate,
            RegardsAmqpAdmin pRegardsAmqpAdmin, IRuntimeTenantResolver pThreadTenantResolver) {
        super(pVirtualHostAdmin, pRabbitTemplate, pRegardsAmqpAdmin);
        this.threadTenantResolver = pThreadTenantResolver;
    }

    @Override
    protected String resolveTenant() {
        return threadTenantResolver.getTenant();
    }

}