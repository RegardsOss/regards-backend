/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.domain;

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

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    @Override
    public boolean equals(Object pOther) {
        return (pOther instanceof RabbitVhost) && ((RabbitVhost) pOther).name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
