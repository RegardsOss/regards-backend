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


package fr.cnes.regards.modules.ingest.service.notification;

import java.util.Set;

import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.notifier.client.NotifierClient;

/**
 * Interface of {@link AIPNotificationService}
 * @author Iliana Ghazali
 */

public interface IAIPNotificationService {

    /**
     * Send requests to {@link NotifierClient}
     * @param requestsToSend abstract requests to send for notification
     */
    void sendRequestsToNotifier(Set<AbstractRequest> requestsToSend);

    /**
     * Handle ingest requests notified successfully
     * @param successRequests abstract requests in success
     */
    void handleNotificationSuccess(Set<AbstractRequest> successRequests);

    /**
     * Handle ingest requests with notification errors
     * @param errorRequests abstract requests in error
     */
    void handleNotificationError(Set<AbstractRequest> errorRequests);

}
