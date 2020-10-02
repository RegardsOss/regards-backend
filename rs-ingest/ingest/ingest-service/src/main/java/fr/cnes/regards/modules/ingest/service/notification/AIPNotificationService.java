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

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.DeletionRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequestStep;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.notifier.client.INotifierClient;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;

/**
 * Notification service for {@link AbstractRequest}
 * @author Iliana Ghazali
 */

@Service
@MultitenantTransactional
public class AIPNotificationService implements IAIPNotificationService {

    @Autowired
    private INotifierClient notifierClient;

    @Autowired
    private Gson gson;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepo;

    @Value("${spring.application.name}")
    private String microserviceName;

    @Override
    public void sendRequestsToNotifier(Set<AbstractRequest> requestsToSend) {
        for (AbstractRequest abstractRequest : requestsToSend) {
            if (abstractRequest instanceof IngestRequest) {
                IngestRequest ingestRequest = (IngestRequest) abstractRequest;
                ingestRequest.setState(InternalRequestState.RUNNING);
                ingestRequest.setStep(IngestRequestStep.LOCAL_TO_BE_NOTIFIED);
            } else if (abstractRequest instanceof OAISDeletionRequest) {
                OAISDeletionRequest oaisDeletionRequest = (OAISDeletionRequest) abstractRequest;
                oaisDeletionRequest.setStep(DeletionRequestStep.LOCAL_TO_BE_NOTIFIED);
            } else if (abstractRequest instanceof AIPUpdateRequest) {
                AIPUpdateRequest aipUpdateRequest = (AIPUpdateRequest) abstractRequest;
                aipUpdateRequest.setState(InternalRequestState.RUNNING);
                aipUpdateRequest.setStep(AIPUpdateRequestStep.LOCAL_TO_BE_NOTIFIED);
            }
        }
        abstractRequestRepo.saveAll(requestsToSend);
        if (!requestsToSend.isEmpty()) {
            notifierClient.sendNotifications(createNotificationActionEvent(requestsToSend));
        }
    }

    private List<NotificationRequestEvent> createNotificationActionEvent(Set<AbstractRequest> requestsToSend) {
        List<NotificationRequestEvent> eventToSend = Lists.newArrayList();
        for (AbstractRequest abstractRequest : requestsToSend) {
            if (abstractRequest instanceof IngestRequest) {
                IngestRequest ingestRequest = (IngestRequest) abstractRequest;
                eventToSend.add(new NotificationRequestEvent(gson.toJsonTree(ingestRequest.getAips()), gson.toJsonTree(
                        new NotificationActionEventMetadata(RequestTypeConstant.INGEST_VALUE)),
                                                             ingestRequest.getId().toString(), this.microserviceName));
            } else if (abstractRequest instanceof OAISDeletionRequest) {
                OAISDeletionRequest oaisDeletionRequest = (OAISDeletionRequest) abstractRequest;
                eventToSend.add(new NotificationRequestEvent(gson.toJsonTree(oaisDeletionRequest.getAip()),
                                                             gson.toJsonTree(new NotificationActionEventMetadata(
                                                                     RequestTypeConstant.OAIS_DELETION_VALUE)),
                                                             oaisDeletionRequest.getId().toString(),
                                                             this.microserviceName));
            } else if (abstractRequest instanceof AIPUpdateRequest) {
                AIPUpdateRequest aipUpdateRequest = (AIPUpdateRequest) abstractRequest;
                eventToSend.add(new NotificationRequestEvent(gson.toJsonTree(aipUpdateRequest.getAip()),
                                                             gson.toJsonTree(new NotificationActionEventMetadata(
                                                                     RequestTypeConstant.UPDATE_VALUE)),
                                                             aipUpdateRequest.getId().toString(),
                                                             this.microserviceName));
            }
        }
        return eventToSend;
    }

    @Override
    public void handleNotificationSuccess(Set<AbstractRequest> successRequests) {
        // Handle oais deletion and update request types
        if (!successRequests.isEmpty()) {
            for (AbstractRequest abstractRequest : successRequests) {
                if (abstractRequest instanceof OAISDeletionRequest) {
                    OAISDeletionRequest oaisDeletionRequest = (OAISDeletionRequest) abstractRequest;
                    AIPNotificationLogger
                            .notificationSuccess(oaisDeletionRequest.getId(), oaisDeletionRequest.getProviderId());
                    // no need to publish an event as no service needs it for the moment

                } else if (abstractRequest instanceof AIPUpdateRequest) {
                    AIPUpdateRequest aipUpdateRequest = (AIPUpdateRequest) abstractRequest;
                    AIPNotificationLogger
                            .notificationSuccess(aipUpdateRequest.getId(), aipUpdateRequest.getProviderId());
                    // no need to publish an event as no service needs it for the moment
                }
            }
            // Delete successful requests
            abstractRequestRepo.deleteInBatch(successRequests);
        }
    }

    @Override
    public void handleNotificationError(Set<AbstractRequest> errorRequests) {
        for (AbstractRequest abstractRequest : errorRequests) {
            if (abstractRequest instanceof IngestRequest) {
                IngestRequest ingestRequest = (IngestRequest) abstractRequest;
                AIPNotificationLogger.notificationError(ingestRequest.getId(), ingestRequest.getProviderId(),
                                                        ingestRequest.getErrors());
                // put request state to error and change step
                ingestRequest.setState(InternalRequestState.ERROR);
                ingestRequest.setStep(IngestRequestStep.REMOTE_NOTIFICATION_ERROR);

            } else if (abstractRequest instanceof OAISDeletionRequest) {
                OAISDeletionRequest oaisDeletionRequest = (OAISDeletionRequest) abstractRequest;
                AIPNotificationLogger
                        .notificationError(oaisDeletionRequest.getId(), oaisDeletionRequest.getProviderId(),
                                           oaisDeletionRequest.getErrors());
                // put request state to error and change step
                oaisDeletionRequest.setState(InternalRequestState.ERROR);
                oaisDeletionRequest.setStep(DeletionRequestStep.LOCAL_NOTIFICATION_ERROR);

            } else if (abstractRequest instanceof AIPUpdateRequest) {
                AIPUpdateRequest aipUpdateRequest = (AIPUpdateRequest) abstractRequest;
                AIPNotificationLogger.notificationError(aipUpdateRequest.getId(), aipUpdateRequest.getProviderId(),
                                                        aipUpdateRequest.getErrors());
                // put request state to error and change step
                aipUpdateRequest.setState(InternalRequestState.ERROR);
                aipUpdateRequest.setStep(AIPUpdateRequestStep.LOCAL_NOTIFICATION_ERROR);
            }
        }
        // Save error requests
        abstractRequestRepo.saveAll(errorRequests);
    }

    public static class NotificationActionEventMetadata {

        private String action;

        public NotificationActionEventMetadata(String action) {
            this.action = action;
        }

        public String getAction() {
            return action;
        }

    }
}