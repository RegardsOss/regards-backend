/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.event.notifier;

import com.google.gson.JsonObject;
import fr.cnes.regards.framework.amqp.event.*;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * An amqp message for the notification request event in order to notify rs-notifier
 * (see {link fr.cnes.regards.modules.notifier.dto.out.NotifierEvent})
 *
 * @author Stephane Cortine
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class NotificationRequestEvent extends AbstractRequestEvent implements ISubscribable {

    private @NotNull(message = "JSON element is required") JsonObject payload;

    private JsonObject metadata;

    public NotificationRequestEvent(JsonObject payload, JsonObject metadata, String requestId, String requestOwner) {
        this.payload = payload;
        this.metadata = metadata;

        super.setRequestId(requestId);
        super.setRequestOwner(requestOwner);
        super.setRequestDate(OffsetDateTime.now());
    }

    public JsonObject getPayload() {
        return payload;
    }

    public void setPayload(JsonObject payload) {
        this.payload = payload;
    }

    public JsonObject getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonObject metadata) {
        this.metadata = metadata;
    }
}

