/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import java.util.HashSet;
import java.util.Set;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;

/**
 * {@link InstanceSubscriber} uses a fixed tenant to subscribe to instance events.
 *
 * @author Marc Sordi
 *
 */
public class InstanceSubscriber extends AbstractSubscriber implements IInstanceSubscriber {

    public InstanceSubscriber(IRabbitVirtualHostAdmin pVirtualHostAdmin, RegardsAmqpAdmin pRegardsAmqpAdmin,
            Jackson2JsonMessageConverter pJackson2JsonMessageConverter) {
        super(pVirtualHostAdmin, pRegardsAmqpAdmin, pJackson2JsonMessageConverter);
    }

    @Override
    protected Set<String> resolveTenants() {
        // Instance is considered as a single tenant
        Set<String> tenants = new HashSet<>();
        tenants.add(AmqpConstants.AMQP_MANAGER);
        return tenants;
    }
}
