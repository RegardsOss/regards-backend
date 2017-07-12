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
package fr.cnes.regards.framework.amqp.domain;

/**
 * @author svissier
 *
 */
public class RabbitMqVhostPermission {

    /**
     * syntax allowing to give all rights
     */
    private static final String ALL_RIGHTS = ".*";

    /**
     * fields required by RabbitMQ Http API, represents rights to configure the virtual host
     */
    private final String configure;

    /**
     * fields required by RabbitMQ Http API, represents rights to write into the virtual host
     */
    private final String write;

    /**
     * fields required by RabbitMQ Http API, represents rights to read from the virtual host
     */
    private final String read;

    public RabbitMqVhostPermission() {
        configure = ALL_RIGHTS;
        write = ALL_RIGHTS;
        read = ALL_RIGHTS;
    }

    public String getConfigure() {
        return configure;
    }

    public String getWrite() {
        return write;
    }

    public String getRead() {
        return read;
    }

}
