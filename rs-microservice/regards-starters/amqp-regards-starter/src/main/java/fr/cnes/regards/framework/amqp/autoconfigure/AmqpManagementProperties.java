/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * class regrouping the properties about managment of the broker
 *
 * @author svissier
 *
 */
@ConfigurationProperties(prefix = "regards.amqp.management")
public class AmqpManagementProperties {

    /**
     * value from the configuration file representing the host of the manager of the broker
     */
    private String host = "localhost";

    /**
     * value from the configuration file representing the port on which the manager of the broker is listening
     */
    private Integer port = 15672;

    public String getHost() {
        return host;
    }

    public void setHost(String pAmqpManagementHost) {
        host = pAmqpManagementHost;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer pAmqpManagementPort) {
        port = pAmqpManagementPort;
    }
}