/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
     * Microservice identifier unique to identify exchanges/queue related to only one type of microservices
     */
    @NotNull
    private String typeIdentifier;

    /**
     * Microservice instance identifier
     */
    @NotNull
    private String instanceIdentifier;

    /**
     * @return the microservice type identifier
     */
    public String getTypeIdentifier() {
        return typeIdentifier;
    }

    /**
     * Set the microservice type identifier
     * @param pTypeIdentifier
     */
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