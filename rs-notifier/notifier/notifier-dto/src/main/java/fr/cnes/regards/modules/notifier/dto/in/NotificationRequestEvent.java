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
package fr.cnes.regards.modules.notifier.dto.in;

import javax.validation.constraints.NotNull;

import java.time.OffsetDateTime;

import com.google.gson.JsonElement;
import fr.cnes.regards.framework.amqp.event.AbstractRequestEvent;
import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * An event contain a JSON element plus an action
 * @author Kevin Marchois
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class NotificationRequestEvent extends AbstractRequestEvent implements ISubscribable {

    @NotNull(message = "JSON element is required")
    private JsonElement payload;

    private JsonElement metadata;

    public NotificationRequestEvent(JsonElement payload, JsonElement metadata, String requestId, String requestOwner) {
        this.payload = payload;
        this.metadata = metadata;
        super.setRequestId(requestId);
        super.setRequestOwner(requestOwner);
        super.setRequestDate(OffsetDateTime.now());
    }

    public JsonElement getPayload() {
        return payload;
    }

    public void setPayload(JsonElement payload) {
        this.payload = payload;
    }

    public JsonElement getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonElement metadata) {
        this.metadata = metadata;
    }
}
