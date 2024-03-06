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
package fr.cnes.regards.framework.amqp.batch.dto;

import fr.cnes.regards.framework.amqp.event.ISubscribable;

/**
 * Wrapper that contains an AMQP event to respond on a specific exchange. Use static methods to build responses.
 *
 * @author Iliana Ghazali
 **/
public final class ResponseMessage<R extends ISubscribable> {

    private final R responsePayload;

    private ResponseMessage() {
        this.responsePayload = null;
    }

    private ResponseMessage(R responsePayload) {
        this.responsePayload = responsePayload;
    }

    public static <R extends ISubscribable> ResponseMessage<R> buildResponse(R payload) {
        return new ResponseMessage<>(payload);
    }

    public static <R extends ISubscribable> ResponseMessage<R> buildEmptyResponse() {
        return new ResponseMessage<>();
    }

    public boolean hasPayload() {
        return this.responsePayload != null;
    }

    public R getResponsePayload() {
        return responsePayload;
    }
}
