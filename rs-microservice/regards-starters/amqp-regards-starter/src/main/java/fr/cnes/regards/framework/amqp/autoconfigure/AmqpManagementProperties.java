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

import fr.cnes.regards.framework.amqp.configuration.VirtualHostMode;

/**
 * class regrouping the properties about managment of the broker
 * @author svissier
 */
@ConfigurationProperties(prefix = "regards.amqp.management")
public class AmqpManagementProperties {

    /**
     * value from the configuration file representing the host of the manager of the broker
     */
    private String host = "localhost";

    /**
     * value from the configuration file representing the port on which the manager of the broker is listening
     */
    private Integer port = 15672;

    private VirtualHostMode mode = VirtualHostMode.SINGLE;

    /**
     * @return the management host
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the management host
     */
    public void setHost(String pAmqpManagementHost) {
        host = pAmqpManagementHost;
    }

    /**
     * @return the management port
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Set the management port
     */
    public void setPort(Integer pAmqpManagementPort) {
        port = pAmqpManagementPort;
    }

    public VirtualHostMode getMode() {
        return mode;
    }

    public void setMode(VirtualHostMode mode) {
        this.mode = mode;
    }
}