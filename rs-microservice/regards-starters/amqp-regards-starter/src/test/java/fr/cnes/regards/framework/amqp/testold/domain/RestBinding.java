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
package fr.cnes.regards.framework.amqp.testold.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author svissier
 *
 */
@JsonIgnoreProperties
public class RestBinding {

    private String vhost;

    private String destination;

    private String source;

    private String routing_key;

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String pVhost) {
        vhost = pVhost;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String pDestination) {
        destination = pDestination;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String pSource) {
        source = pSource;
    }

    public String getRouting_key() {
        return routing_key;
    }

    public void setRouting_key(String pRouting_key) {
        routing_key = pRouting_key;
    }

}
