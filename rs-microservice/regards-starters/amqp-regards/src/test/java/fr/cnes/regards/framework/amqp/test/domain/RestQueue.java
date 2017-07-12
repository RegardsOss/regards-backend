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
package fr.cnes.regards.framework.amqp.test.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * helper class for test purpose, represent a rabbitmq queue retrieved from the rest api
 *
 * @author svissier
 *
 */
@JsonIgnoreProperties
public class RestQueue {

    /**
     * name of the queue
     */
    private String name;

    /**
     * virtual host on which queue is defined
     */
    private String vhost;

    /**
     * either the queue is durable
     */
    private boolean durable;

    /**
     * either the queue is exclusive
     */
    private boolean exclusive;

    /**
     * either the queue is auto deleting
     */
    private boolean autoDelete;

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String pVhost) {
        vhost = pVhost;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean pDurable) {
        durable = pDurable;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean pExclusive) {
        exclusive = pExclusive;
    }

    // CHECKSTYLE:OFF
    public boolean isAuto_delete() {
        return autoDelete;
    }

    public void setAuto_delete(boolean pAuto_delete) {
        autoDelete = pAuto_delete;
    }
    // CHECKTYLE:ON
}
