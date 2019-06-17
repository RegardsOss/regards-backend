/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * class regrouping properties about the microservice
 * @author svissier
 */
@Validated
@ConfigurationProperties(prefix = "regards.amqp.microservice")
public class AmqpMicroserviceProperties {

    /**
     * Microservice identifier unique to identify exchanges/queue related to only one type of microservices.<br/>
     * If not specified, fallback to microservice name (i.e. spring.application.name property)
     */
    private String typeIdentifier;

    /**
     * Microservice instance identifier
     */
    private String instanceIdentifier;

    /**
     * @return the microservice type identifier
     */
    public String getTypeIdentifier() {
        return typeIdentifier;
    }

    /**
     * Set the microservice type identifier
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