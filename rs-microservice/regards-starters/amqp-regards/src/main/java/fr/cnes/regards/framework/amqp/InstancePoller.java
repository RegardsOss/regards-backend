/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;

/**
 * {@link InstancePoller} uses a fixed tenant to poll instance events.
 *
 * @author Marc Sordi
 *
 */
public class InstancePoller extends AbstractPoller implements IInstancePoller {

    public InstancePoller(IRabbitVirtualHostAdmin pVirtualHostAdmin, RabbitTemplate pRabbitTemplate,
            RegardsAmqpAdmin pRegardsAmqpAdmin) {
        super(pVirtualHostAdmin, pRabbitTemplate, pRegardsAmqpAdmin);
    }

    @Override
    protected String resolveTenant() {
        return AmqpConstants.AMQP_MANAGER;
    }

}
