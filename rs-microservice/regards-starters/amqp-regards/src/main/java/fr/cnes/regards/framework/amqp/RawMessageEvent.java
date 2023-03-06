/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp;

import fr.cnes.regards.framework.amqp.event.IEvent;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 * Extends standard {@link Message} to implements {@link IEvent} for regards-amqp starter.
 * {@link fr.cnes.regards.framework.amqp.IPublisher} needs {@link IEvent}s to publish messages
 *
 * @author Sébastien Binda
 */
public class RawMessageEvent extends Message implements IEvent {

    public RawMessageEvent(byte[] body) {
        super(body);
    }

    public RawMessageEvent(byte[] body, MessageProperties messageProperties) {
        super(body, messageProperties);
    }
}
