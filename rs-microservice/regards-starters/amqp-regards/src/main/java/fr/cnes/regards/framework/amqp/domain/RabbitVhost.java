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
