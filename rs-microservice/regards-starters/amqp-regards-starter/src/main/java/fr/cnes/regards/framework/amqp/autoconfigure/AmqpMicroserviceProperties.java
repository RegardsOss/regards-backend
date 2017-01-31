/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.autoconfigure;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * class regrouping properties about the microservice
 *
 * @author svissier
 *
 */
@ConfigurationProperties(prefix = "regards.amqp.microservice")
public class AmqpMicroserviceProperties {

    /**
     * type identifier unique to identify exchanges/queue related to only one type of microservices
     */
    @NotNull
    private String typeIdentifier;

    public String getTypeIdentifier() {
        return typeIdentifier;
    }

    public void setTypeIdentifier(String pTypeIdentifier) {
        typeIdentifier = pTypeIdentifier;
    }
}