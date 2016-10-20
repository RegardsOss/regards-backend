/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.connection;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SimpleRoutingConnectionFactory;

/**
 * @author svissier
 *
 */
public class RegardsSimpleRoutingConnectionFactory extends SimpleRoutingConnectionFactory {

    @Override
    public void addTargetConnectionFactory(Object pKey, ConnectionFactory pConnectionFactory) {
        super.addTargetConnectionFactory(pKey, pConnectionFactory);
    }
}
