/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 *
 * {@link Subscriber} uses {@link ITenantResolver} to resolve tenants in multitenant context. On listener will be
 * created for each tenant.
 *
 * @author svissier
 * @author Marc Sordi
 *
 */
public class Subscriber extends AbstractSubscriber implements ISubscriber {

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(Subscriber.class);

    /**
     * provider of projects allowing us to listen to any necessary RabbitMQ Vhost
     */
    private final ITenantResolver tenantResolver;

    public Subscriber(IRabbitVirtualHostAdmin pVirtualHostAdmin, RegardsAmqpAdmin pRegardsAmqpAdmin,
            Jackson2JsonMessageConverter pJackson2JsonMessageConverter, ITenantResolver pTenantResolver) {
        super(pVirtualHostAdmin, pRegardsAmqpAdmin, pJackson2JsonMessageConverter);
        tenantResolver = pTenantResolver;
    }

    @Override
    protected Set<String> resolveTenants() {
        return tenantResolver.getAllTenants();
    }

    @Override
    public void addTenant(String pTenant) {
        addTenantListeners(pTenant);
    }

    @Override
    public void removeTenant(String pTenant) {
        removeTenantListeners(pTenant);
    }

}
