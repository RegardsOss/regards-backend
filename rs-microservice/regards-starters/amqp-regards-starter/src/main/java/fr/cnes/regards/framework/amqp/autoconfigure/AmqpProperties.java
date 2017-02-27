/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.autoconfigure;

import org.springframework.boot.autoconfigure.amqp.RabbitProperties;

/**
 * class regrouping all sources of properties used for our amqp client
 *
 * @author svissier
 *
 */
public class AmqpProperties {

    /**
     * properties taken directly from spring
     */
    private final RabbitProperties rabbitProperties;

    /**
     * properties related to the microservice
     */
    private final AmqpMicroserviceProperties amqpMicroserviceProperties;

    /**
     * properties related to the broker
     */
    private final AmqpManagementProperties amqpManagementProperties;

    /**
     * @param pRabbitProperties
     *            spring properties
     * @param pAmqpManagmentProperties
     *            management properties
     * @param pAmqpMicroserviceProperties
     *            microservice properties
     */
    public AmqpProperties(RabbitProperties pRabbitProperties, AmqpManagementProperties pAmqpManagmentProperties,
            AmqpMicroserviceProperties pAmqpMicroserviceProperties) {
        rabbitProperties = pRabbitProperties;
        amqpManagementProperties = pAmqpManagmentProperties;
        amqpMicroserviceProperties = pAmqpMicroserviceProperties;
    }

    public String getRabbitmqPassword() {
        return rabbitProperties.getPassword();
    }

    public String getRabbitmqUserName() {
        return rabbitProperties.getUsername();
    }

    public String getRabbitmqAddresses() {
        return rabbitProperties.determineAddresses();
    }

    public String getTypeIdentifier() {
        return amqpMicroserviceProperties.getTypeIdentifier();
    }

    public String getInstanceIdentifier() {
        return amqpMicroserviceProperties.getInstanceIdentifier();
    }

    public String getAmqpManagementHost() {
        return amqpManagementProperties.getHost();
    }

    public Integer getAmqpManagementPort() {
        return amqpManagementProperties.getPort();
    }

}
