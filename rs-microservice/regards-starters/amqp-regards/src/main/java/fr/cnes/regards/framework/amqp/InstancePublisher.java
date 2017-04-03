/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;

/**
 *
 * {@link InstancePublisher} uses a fixed tenant to publish instance events.
 *
 * @author Marc Sordi
 *
 */
public class InstancePublisher extends AbstractPublisher implements IInstancePublisher {

    public InstancePublisher(RabbitTemplate pRabbitTemplate, RegardsAmqpAdmin pRegardsAmqpAdmin,
            IRabbitVirtualHostAdmin pRabbitVirtualHostAdmin) {
        super(pRabbitTemplate, pRegardsAmqpAdmin, pRabbitVirtualHostAdmin);
    }

    @Override
    protected String resolveTenant() {
        return AmqpConstants.AMQP_MANAGER;
    }
}
