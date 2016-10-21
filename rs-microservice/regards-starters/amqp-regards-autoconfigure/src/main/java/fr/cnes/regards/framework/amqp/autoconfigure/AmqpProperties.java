/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.autoconfigure;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author svissier
 *
 */
@ConfigurationProperties
public class AmqpProperties {

    private SpringRabbitMQProperties springRabbitMQProperties;

    private AmqpMicroserviceProperties amqpMicroserviceProperties;

    private AmqpManagmentProperties amqpManagmentProperties;

    public String getRabbitmqPassword() {
        return springRabbitMQProperties.getRabbitmqPassword();
    }

    public String getRabbitmqUserName() {
        return springRabbitMQProperties.getRabbitmqUserName();
    }

    public String getRabbitmqAddresses() {
        return springRabbitMQProperties.getRabbitAddresses();
    }

    public String getTypeIdentifier() {
        return amqpMicroserviceProperties.getTypeIdentifier();
    }

    public String getInstanceIdentifier() {
        return amqpMicroserviceProperties.getInstanceIdentifier();
    }

    public String getAmqpManagementHost() {
        return amqpManagmentProperties.getHost();
    }

    public Integer getAmqpManagementPort() {
        return amqpManagmentProperties.getPort();
    }

    @ConfigurationProperties(prefix = "regards.amqp.managmeent")
    private static class AmqpManagmentProperties {

        /**
         * value from the configuration file representing the host of the manager of the broker
         */
        private String host;

        /**
         * value from the configuration file representing the port on which the manager of the broker is listening
         */
        private Integer port;

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

    @ConfigurationProperties(prefix = "regards.amqp.microservice")
    private static class AmqpMicroserviceProperties {

        /**
         * type identifier unique to identify exchanges/queue related to only one type of microservices
         */
        @NotNull
        private String typeIdentifier;

        /**
         * instance identifier unique to identify exchanges/queue related to only one instance of a microservice
         */
        private String instanceIdentifier;

        public String getTypeIdentifier() {
            return typeIdentifier;
        }

        public void setTypeIdentifier(String pTypeIdentifier) {
            typeIdentifier = pTypeIdentifier;
        }

        public String getInstanceIdentifier() {
            return instanceIdentifier;
        }

        public void setInstanceIdentifier(String pInstanceIdentifier) {
            instanceIdentifier = pInstanceIdentifier;
        }
    }

    @ConfigurationProperties(prefix = "spring.rabbitmq")
    private static class SpringRabbitMQProperties {

        /**
         * addresses configured to
         */
        private String rabbitAddresses;

        /**
         * username used to connect to the broker and it's manager
         */
        private String rabbitmqUserName;

        /**
         * password used to connect to the broker and it's manager
         */
        private String rabbitmqPassword;

        public String getRabbitAddresses() {
            return rabbitAddresses;
        }

        public void setRabbitAddresses(String pRabbitAddresses) {
            rabbitAddresses = pRabbitAddresses;
        }

        public String getRabbitmqUserName() {
            return rabbitmqUserName;
        }

        public void setRabbitmqUserName(String pRabbitmqUserName) {
            rabbitmqUserName = pRabbitmqUserName;
        }

        public String getRabbitmqPassword() {
            return rabbitmqPassword;
        }

        public void setRabbitmqPassword(String pRabbitmqPassword) {
            rabbitmqPassword = pRabbitmqPassword;
        }
    }

}
