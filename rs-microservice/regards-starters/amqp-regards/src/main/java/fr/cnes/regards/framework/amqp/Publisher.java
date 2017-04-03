/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 *
 * {@link Publisher} uses {@link IRuntimeTenantResolver} to resolve current thread tenant to publish an event in the
 * multitenant context.
 *
 * @author svissier
 * @author lmieulet
 * @author Marc Sordi
 *
 */
public class Publisher extends AbstractPublisher implements IPublisher {

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(Publisher.class);

    /**
     * Resolve thread tenant
     */
    private final IRuntimeTenantResolver threadTenantResolver;

    public Publisher(IRabbitVirtualHostAdmin pVirtualHostAdmin, RabbitTemplate pRabbitTemplate,
            final RegardsAmqpAdmin pRegardsAmqpAdmin, IRuntimeTenantResolver pThreadTenantResolver) {
        super(pRabbitTemplate, pRegardsAmqpAdmin, pVirtualHostAdmin);
        this.threadTenantResolver = pThreadTenantResolver;
    }

    @Override
    protected String resolveTenant() {
        return threadTenantResolver.getTenant();
    }
}
