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
package fr.cnes.regards.modules.ltamanager.service.submission.update.ingest.notification;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;

/**
 * @author Thomas GUILLOU
 **/
public class SuccessLtaRequestNotification extends NotificationRequestEvent {

    public final static String NOTIF_ACTION = "LONG_TERM_ARCHIVED";

    public SuccessLtaRequestNotification(JsonObject payload,
                                         JsonObject metadata,
                                         String requestId,
                                         String requestOwner) {
        super(payload, metadata, requestId, requestOwner);
    }

    public static SuccessLtaRequestNotification fromRequest(SubmissionRequest request,
                                                            String currentTenant,
                                                            Gson gson) {
        SuccessLtaRequestNotificationPayload payload = new SuccessLtaRequestNotificationPayload(request.getOriginUrn());
        SuccessLtaRequestNotificationMetadata metadata = new SuccessLtaRequestNotificationMetadata(currentTenant,
                                                                                                   request.getSession());
        return new SuccessLtaRequestNotification(gson.toJsonTree(payload).getAsJsonObject(),
                                                 gson.toJsonTree(metadata).getAsJsonObject(),
                                                 request.getCorrelationId(),
                                                 request.getOwner());
    }
}
