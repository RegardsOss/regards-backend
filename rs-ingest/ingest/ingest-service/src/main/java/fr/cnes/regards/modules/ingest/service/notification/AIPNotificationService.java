
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.DeletionRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequestStep;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;
import fr.cnes.regards.modules.ingest.service.request.RequestService;
import fr.cnes.regards.modules.notifier.client.INotifierClient;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private RequestService requestService;

    @Autowired
    private IPublisher publisher;

    @Value("${spring.application.name}")
    private String microserviceName;

    // ---------------------------
    // HANDLE NOTIFICATION SENDING
    // ---------------------------

    @Override
    public void sendRequestsToNotifier(Set<AbstractRequest> requestsToSend) {
        if (!requestsToSend.isEmpty()) {
            // first update step and states of requests
            for (AbstractRequest abstractRequest : requestsToSend) {
                if (abstractRequest instanceof IngestRequest) {
                    IngestRequest ingestRequest = (IngestRequest) abstractRequest;
                    ingestRequest.setState(InternalRequestState.RUNNING);
                    ingestRequest.setStep(IngestRequestStep.LOCAL_TO_BE_NOTIFIED);
                } else if (abstractRequest instanceof OAISDeletionRequest) {
                    OAISDeletionRequest oaisDeletionRequest = (OAISDeletionRequest) abstractRequest;
                    oaisDeletionRequest.setState(InternalRequestState.RUNNING);
                    oaisDeletionRequest.setStep(DeletionRequestStep.LOCAL_TO_BE_NOTIFIED);
                } else if (abstractRequest instanceof AIPUpdateRequest) {
                    AIPUpdateRequest aipUpdateRequest = (AIPUpdateRequest) abstractRequest;
                    aipUpdateRequest.setState(InternalRequestState.RUNNING);
                    aipUpdateRequest.setStep(AIPUpdateRequestStep.LOCAL_TO_BE_NOTIFIED);
                }
            }
            abstractRequestRepo.saveAll(requestsToSend);

            // then create notification request events and send them to notifier
            notifierClient.sendNotifications(createNotificationRequestEvent(requestsToSend));
        }
    }

    private List<NotificationRequestEvent> createNotificationRequestEvent(Set<AbstractRequest> requestsToSend) {
        List<NotificationRequestEvent> eventToSend = Lists.newArrayList();
        // for each request, create the associated notification request event
        for (AbstractRequest abstractRequest : requestsToSend) {
            // INGEST REQUESTS
            if (abstractRequest instanceof IngestRequest) {
                IngestRequest ingestRequest = (IngestRequest) abstractRequest;
                ingestRequest.getAips().forEach((aip) -> eventToSend.add(
                        new NotificationRequestEvent(
                            gson.toJsonTree(aip).getAsJsonObject(),
                            gson.toJsonTree(new NotificationActionEventMetadata(RequestTypeConstant.INGEST_VALUE)),
                            ingestRequest.getId().toString(), this.microserviceName)));
            }
            // OAIS DELETION REQUESTS
            else if (abstractRequest instanceof OAISDeletionRequest) {
                OAISDeletionRequest oaisDeletionRequest = (OAISDeletionRequest) abstractRequest;
                // remark : aip content is in payload because it has already been removed from database
                eventToSend.add(
                        new NotificationRequestEvent(gson.toJsonTree(oaisDeletionRequest.getAipToNotify()).getAsJsonObject(),
                            gson.toJsonTree(new NotificationActionEventMetadata(RequestTypeConstant.OAIS_DELETION_VALUE)),
                            oaisDeletionRequest.getId().toString(), this.microserviceName));
            }
            // UPDATE REQUESTS
            else if (abstractRequest instanceof AIPUpdateRequest) {
                AIPUpdateRequest aipUpdateRequest = (AIPUpdateRequest) abstractRequest;
                eventToSend.add(
                        new NotificationRequestEvent(gson.toJsonTree(aipUpdateRequest.getAip()).getAsJsonObject(),
                            gson.toJsonTree(new NotificationActionEventMetadata(RequestTypeConstant.UPDATE_VALUE)),
                            aipUpdateRequest.getId().toString(), this.microserviceName));
            }
        }
        return eventToSend;
    }

    // ------------------------------
    // HANDLE NOTIFICATION FEEDBACK
    // ------------------------------

    @Override
    public void handleNotificationSuccess(Set<AbstractRequest> successRequests) {
        // Handle Ingest success
        // filter out ingest requests because their processing is specific
        Set<IngestRequest> ingestRequests = successRequests.stream().filter(IngestRequest.class::isInstance)
                .map(IngestRequest.class::cast).collect(Collectors.toSet());
        if (!ingestRequests.isEmpty()) {
            successRequests.removeAll(ingestRequests);
            handleIngestNotificationSuccess(ingestRequests);
        }

        // Handle Deletion and Update requests
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
                    requestService.deleteRequest(abstractRequest);
                }
            }
            // Delete successful requests
            requestService.deleteRequests(successRequests);
        }
    }

    private void handleIngestNotificationSuccess(Set<IngestRequest> successIngestRequests) {
        List<IngestRequestEvent> ingestRequestEvents = new ArrayList<>();
        String sipId;

        // find sip id for publication
        // ingest requests are reloaded from database because aips are not loaded from successIngestRequests (lazy mode)
        List<IngestRequest> tmpRequests = ingestRequestRepository
                .findByIdIn(successIngestRequests.stream().map(IngestRequest::getId).collect(Collectors.toSet()));
        for (IngestRequest request : tmpRequests) {
            sipId = request.getAips().get(0).getSip().getSipId();
            ingestRequestEvents.add(IngestRequestEvent.build(request.getRequestId(), request.getSip().getId(), sipId,
                                                             RequestState.SUCCESS));
            AIPNotificationLogger.notificationSuccess(request.getId(), request.getProviderId());
        }

        // publish success
        publisher.publish(ingestRequestEvents);

        // delete ingest requests
        requestService.deleteRequests(Sets.newHashSet(successIngestRequests));

    }

    @Override
    public void handleNotificationError(Set<AbstractRequest> errorRequests) {
        // for each type of request set the change the state and the step of the request to ERROR
        for (AbstractRequest abstractRequest : errorRequests) {
            // INGEST REQUESTS
            if (abstractRequest instanceof IngestRequest) {
                IngestRequest ingestRequest = (IngestRequest) abstractRequest;
                AIPNotificationLogger.notificationError(ingestRequest.getId(), ingestRequest.getProviderId(),
                                                        ingestRequest.getErrors());
                // put request state to error and change step
                ingestRequest.setState(InternalRequestState.ERROR);
                ingestRequest.setStep(IngestRequestStep.REMOTE_NOTIFICATION_ERROR);

            }
            // OAIS DELETION REQUESTS
            else if (abstractRequest instanceof OAISDeletionRequest) {
                OAISDeletionRequest oaisDeletionRequest = (OAISDeletionRequest) abstractRequest;
                AIPNotificationLogger
                        .notificationError(oaisDeletionRequest.getId(), oaisDeletionRequest.getProviderId(),
                                           oaisDeletionRequest.getErrors());
                // put request state to error and change step
                oaisDeletionRequest.setState(InternalRequestState.ERROR);
                oaisDeletionRequest.setStep(DeletionRequestStep.REMOTE_NOTIFICATION_ERROR);

            }
            // UPDATE REQUESTS
            else if (abstractRequest instanceof AIPUpdateRequest) {
                AIPUpdateRequest aipUpdateRequest = (AIPUpdateRequest) abstractRequest;
                AIPNotificationLogger.notificationError(aipUpdateRequest.getId(), aipUpdateRequest.getProviderId(),
                                                        aipUpdateRequest.getErrors());
                // put request state to error and change step
                aipUpdateRequest.setState(InternalRequestState.ERROR);
                aipUpdateRequest.setStep(AIPUpdateRequestStep.REMOTE_NOTIFICATION_ERROR);
            }
        }
        // Save error requests
        abstractRequestRepo.saveAll(errorRequests);
    }

    // class used to format RequestTypeConstant in gson
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