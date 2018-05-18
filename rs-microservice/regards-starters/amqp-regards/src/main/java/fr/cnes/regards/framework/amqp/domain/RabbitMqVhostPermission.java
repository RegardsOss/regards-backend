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

    /**
     * default constructor
     */
    public RabbitMqVhostPermission() {
        configure = ALL_RIGHTS;
        write = ALL_RIGHTS;
        read = ALL_RIGHTS;
    }

    /**
     * @return the right to configure objects onto rabbitmq virtual host
     */
    public String getConfigure() {
        return configure;
    }

    /**
     * @return the right to write into objects into rabbitmq virtual host
     */
    public String getWrite() {
        return write;
    }

    /**
     * @return the right to read from objects into rabbitmq virtual host
     */
    public String getRead() {
        return read;
    }

}
