/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author svissier
 *
 */
@JsonIgnoreProperties
public class RabbitVhost {

    /**
     * name of the vhost represented by this instance
     */
    private String name;

    public RabbitVhost() {
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

}
