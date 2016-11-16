/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * class regrouping properties from spring amqp used intop our client
 * 
 * @author svissier
 *
 */
@ConfigurationProperties(prefix = "spring.rabbitmq")
public class SpringRabbitMQProperties {

    /**
     * addresses configured to
     */
    private String addresses;

    /**
     * username used to connect to the broker and it's manager
     */
    private String username;

    /**
     * password used to connect to the broker and it's manager
     */
    private String password;

    public String getAddresses() {
        return addresses;
    }

    public void setAddresses(String pAddresses) {
        addresses = pAddresses;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String pUsername) {
        username = pUsername;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String pPassword) {
        password = pPassword;
    }
}