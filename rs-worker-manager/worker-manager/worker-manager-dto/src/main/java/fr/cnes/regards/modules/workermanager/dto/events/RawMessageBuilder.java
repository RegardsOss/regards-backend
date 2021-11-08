/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workermanager.dto.events;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 * Helper to build a {@link Message} raw to send by broadcast to workers.
 *
 * @author Sébastien Binda
 */
public final class RawMessageBuilder {

    /**
     * Build raw AMQP message to send to workers
     * @param tenant tenant for the request
     * @param contentType content type of the request to dispatch on worker
     * @param source Request owner name
     * @param session Request session name
     * @param requestId Request unique identifier
     * @param payload Request body
     * @return Message
     */
    public static Message build(String tenant, String contentType, String source, String session, String requestId, byte[] payload) {
        MessageProperties properties = new MessageProperties();
        properties.setHeader(EventHeaders.TENANT_HEADER.getName(), tenant);
        properties.setHeader(EventHeaders.CONTENT_TYPE_HEADER.getName(), contentType);
        properties.setHeader(EventHeaders.OWNER_HEADER.getName(), source);
        properties.setHeader(EventHeaders.SESSION_HEADER.getName(), session);
        properties.setHeader(EventHeaders.REQUEST_ID_HEADER.getName(), requestId);
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        return new Message(payload, properties);
    }

}
