/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.configuration;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SimpleRoutingConnectionFactory;

/**
 *
 * Extends {@link SimpleRoutingConnectionFactory} to expose {@link ConnectionFactory} management
 *
 * @author Marc Sordi
 *
 */
public class MultitenantSimpleRoutingConnectionFactory extends SimpleRoutingConnectionFactory {

    @Override
    public void addTargetConnectionFactory(Object pKey, ConnectionFactory pConnectionFactory) {
        super.addTargetConnectionFactory(pKey, pConnectionFactory);
    }

    @Override
    public ConnectionFactory removeTargetConnectionFactory(Object pKey) {
        return super.removeTargetConnectionFactory(pKey);
    }
}
