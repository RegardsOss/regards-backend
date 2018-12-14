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

import org.springframework.boot.autoconfigure.amqp.RabbitProperties;

/**
 * class regrouping all sources of properties used for our amqp client
 * @author svissier
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
     * @param pRabbitProperties spring properties
     * @param pAmqpManagmentProperties management properties
     * @param pAmqpMicroserviceProperties microservice properties
     */
    public AmqpProperties(RabbitProperties pRabbitProperties, AmqpManagementProperties pAmqpManagmentProperties,
            AmqpMicroserviceProperties pAmqpMicroserviceProperties) {
        rabbitProperties = pRabbitProperties;
        amqpManagementProperties = pAmqpManagmentProperties;
        amqpMicroserviceProperties = pAmqpMicroserviceProperties;
    }

    /**
     * @return the rabbitmq user password
     */
    public String getRabbitmqPassword() {
        return rabbitProperties.getPassword();
    }

    /**
     * @return the rabbitmq user's name
     */
    public String getRabbitmqUserName() {
        return rabbitProperties.getUsername();
    }

    /**
     * @return the rabbitmq adress
     */
    public String getRabbitmqAddresses() {
        return rabbitProperties.determineAddresses();
    }

    /**
     * @return the microservice type identifier
     */
    public String getTypeIdentifier() {
        return amqpMicroserviceProperties.getTypeIdentifier();
    }

    /**
     * @return the microservice instance identifier
     */
    public String getInstanceIdentifier() {
        return amqpMicroserviceProperties.getInstanceIdentifier();
    }

    /**
     * @return the amqp management host
     */
    public String getAmqpManagementHost() {
        return amqpManagementProperties.getHost();
    }

    /**
     * @return the amqp management port
     */
    public Integer getAmqpManagementPort() {
        return amqpManagementProperties.getPort();
    }

}
