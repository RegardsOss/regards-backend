/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.configuration;

import org.springframework.amqp.core.Message;

public class RabbitVersion {

    private RabbitVersion() {
    }

    public static boolean isVersion1(Message message) {
        String apiVersion = message.getMessageProperties().getHeader(AmqpConstants.REGARDS_API_VERSION_HEADER);
        return (apiVersion == null) || apiVersion.equals(AmqpConstants.REGARDS_VERSION_1_0);
    }

    public static boolean isVersion1_1(Message message) {
        String apiVersion = message.getMessageProperties().getHeader(AmqpConstants.REGARDS_API_VERSION_HEADER);
        return (apiVersion != null) && apiVersion.equals(AmqpConstants.REGARDS_VERSION_1_1);
    }
}
