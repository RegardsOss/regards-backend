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
package fr.cnes.regards.modules.notifier.dto.in;

import com.google.gson.JsonObject;
import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * A notification request event with the list of recipients in order to notify rs-notifier
 * (see {link fr.cnes.regards.modules.notifier.dto.out.NotifierEvent})
 *
 * @author Stephane Cortine
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class SpecificRecipientNotificationRequestEvent extends NotificationRequestEvent {

    /**
     * List of business identifiers for each recipient
     */
    @NotNull
    @NotEmpty
    private Set<String> recipients;

    public SpecificRecipientNotificationRequestEvent(JsonObject payload,
                                                     JsonObject metadata,
                                                     String requestId,
                                                     String requestOwner,
                                                     Set<String> recipients) {
        super(payload, metadata, requestId, requestOwner);
        setRecipients(recipients);
    }

    public Set<String> getRecipients() {
        return recipients;
    }

    public final void setRecipients(Set<String> recipients) {
        this.recipients = recipients;
    }

}
