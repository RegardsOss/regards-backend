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


package fr.cnes.regards.modules.ingest.service.notification;

import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import fr.cnes.regards.modules.notifier.client.INotifierRequestListener;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Listener for aip notification events {@link AIPNotificationService}
 *
 * @author Iliana Ghazali
 */

@Component
public class AIPNotifierListener implements INotifierRequestListener {

    @Autowired
    private IAIPNotificationService notificationService;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepo;

    @Autowired
    private IIngestRequestService ingestRequestService;

    @Value("${spring.application.name}")
    private String microserviceName;

    @Override
    public void onRequestDenied(List<NotifierEvent> denied) {
        handleNotificationIssue(denied);
    }

    @Override
    public void onRequestGranted(List<NotifierEvent> granted) {
        // Nothing to do.
    }

    @Override
    public void onRequestSuccess(List<NotifierEvent> successEvents) {
        // Retrieve requests ids from events
        List<String> requestIds = successEvents.stream().map(NotifierEvent::getRequestId).toList();

        // Handle notification successes
        if (!requestIds.isEmpty()) {
            int nbRequests = requestIds.size();
            // Find corresponding requests and handle them
            Set<AbstractRequest> successRequests = abstractRequestRepo.findAllByCorrelationIdIn(requestIds);

            //Build a map with the request and its corresponding event
            Map<AbstractRequest, NotifierEvent> mapRequestEvent = new HashMap<>();
            for (AbstractRequest successRequest : successRequests) {

                Optional<NotifierEvent> opEvent = successEvents.stream()
                                                               .filter(s -> s.getRequestId()
                                                                             .equals(successRequest.getCorrelationId()))
                                                               .findFirst();
                opEvent.ifPresent(notifierEvent -> mapRequestEvent.put(successRequest, notifierEvent));
            }

            if (!successRequests.isEmpty()) {
                AIPNotificationLogger.notificationEventSuccess(nbRequests);
                notificationService.handleNotificationSuccess(mapRequestEvent);
                AIPNotificationLogger.notificationEventSuccessHandled(successEvents.size());
            }
        }
    }

    @Override
    public void onRequestError(List<NotifierEvent> errorEvents) {
        handleNotificationIssue(errorEvents);
    }

    private void handleNotificationIssue(List<NotifierEvent> events) {
        // Retrieve requests ids from events
        List<String> requestIds = events.stream().map(NotifierEvent::getRequestId).collect(Collectors.toList());

        // Handle notification errors
        if (!requestIds.isEmpty()) {
            int nbRequests = requestIds.size();
            // Find corresponding requests and handle them
            Set<AbstractRequest> errorRequest = abstractRequestRepo.findAllByCorrelationIdIn(requestIds);
            if (!errorRequest.isEmpty()) {
                AIPNotificationLogger.notificationEventError(nbRequests);
                notificationService.handleNotificationError(errorRequest);
                AIPNotificationLogger.notificationEventErrorHandled(nbRequests);
            }
        }
    }
}