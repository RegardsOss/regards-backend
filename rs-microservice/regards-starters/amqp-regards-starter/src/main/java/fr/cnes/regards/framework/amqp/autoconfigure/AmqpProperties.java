/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.autoconfigure;

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
    private final SpringRabbitMQProperties springRabbitMQProperties;

    /**
     * properties related to the microservice
     */
    private final AmqpMicroserviceProperties amqpMicroserviceProperties;

    /**
     * properties related to the broker
     */
    private final AmqpManagementProperties amqpManagementProperties;

    /**
     * @param pSpringRabbitMQProperties
     *            spring properties
     * @param pAmqpManagmentProperties
     *            management properties
     * @param pAmqpMicroserviceProperties
     *            microservice properties
     */
    public AmqpProperties(SpringRabbitMQProperties pSpringRabbitMQProperties,
            AmqpManagementProperties pAmqpManagmentProperties, AmqpMicroserviceProperties pAmqpMicroserviceProperties) {
        springRabbitMQProperties = pSpringRabbitMQProperties;
        amqpManagementProperties = pAmqpManagmentProperties;
        amqpMicroserviceProperties = pAmqpMicroserviceProperties;
    }

    public String getRabbitmqPassword() {
        return springRabbitMQProperties.getPassword();
    }

    public String getRabbitmqUserName() {
        return springRabbitMQProperties.getUsername();
    }

    public String getRabbitmqAddresses() {
        return springRabbitMQProperties.getAddresses();
    }

    public String getTypeIdentifier() {
        return amqpMicroserviceProperties.getTypeIdentifier();
    }

    public String getAmqpManagementHost() {
        return amqpManagementProperties.getHost();
    }

    public Integer getAmqpManagementPort() {
        return amqpManagementProperties.getPort();
    }

}
