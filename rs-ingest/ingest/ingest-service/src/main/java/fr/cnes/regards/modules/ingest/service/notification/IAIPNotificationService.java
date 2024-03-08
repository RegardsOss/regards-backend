/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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


package fr.cnes.regards.modules.ingest.service.notification;

import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.notifier.client.NotifierClient;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;

import java.util.Map;
import java.util.Set;

/**
 * Interface of {@link AIPNotificationService}
 *
 * @author Iliana Ghazali
 */

public interface IAIPNotificationService {

    /**
     * Send requests to {@link NotifierClient}
     *
     * @param requestsToSend abstract requests to send for notification
     */
    void sendRequestsToNotifier(Set<AbstractRequest> requestsToSend);

    /**
     * Handle requests notified successfully
     *
     * @param successRequests abstract requests in success with their coresponding event
     */
    void handleNotificationSuccess(Map<AbstractRequest, NotifierEvent> successRequests);

    /**
     * Handle requests with notification errors
     *
     * @param errorRequests abstract requests in error
     */
    void handleNotificationError(Set<AbstractRequest> errorRequests);

}
