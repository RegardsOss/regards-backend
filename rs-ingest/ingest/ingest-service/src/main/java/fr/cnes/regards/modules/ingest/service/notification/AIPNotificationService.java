
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.DisseminationInfo;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.IngestErrorType;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.DeletionRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.dissemination.AipDisseminationRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.service.request.AIPUpdateRequestService;
import fr.cnes.regards.modules.ingest.service.request.RequestService;
import fr.cnes.regards.modules.notifier.client.INotifierClient;
import fr.cnes.regards.modules.notifier.dto.in.SpecificRecipientNotificationRequestEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Notification service for {@link AbstractRequest}
 *
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

    @Autowired
    private AIPUpdateRequestService aipUpdateRequestService;

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
                if (abstractRequest instanceof IngestRequest ingestRequest) {
                    ingestRequest.setState(InternalRequestState.RUNNING);
                    ingestRequest.setStep(IngestRequestStep.LOCAL_TO_BE_NOTIFIED);
                } else if (abstractRequest instanceof OAISDeletionRequest oaisDeletionRequest) {
                    oaisDeletionRequest.setState(InternalRequestState.RUNNING);
                    oaisDeletionRequest.setStep(DeletionRequestStep.LOCAL_TO_BE_NOTIFIED);
                } else if (abstractRequest instanceof AIPUpdateRequest aipUpdateRequest) {
                    aipUpdateRequest.setState(InternalRequestState.RUNNING);
                    aipUpdateRequest.setStep(AIPUpdateRequestStep.LOCAL_TO_BE_NOTIFIED);
                } else if (abstractRequest instanceof AipDisseminationRequest disseminationRequest) {
                    disseminationRequest.setState(InternalRequestState.WAITING_NOTIFIER_DISSEMINATION_RESPONSE);
                }
            }
            abstractRequestRepo.saveAll(requestsToSend);

            // then create notification request events and send them to notifier
            notifierClient.sendNotifications(createNotificationRequestEvents(requestsToSend));
        }
    }

    private List<NotificationRequestEvent> createNotificationRequestEvents(Set<AbstractRequest> requestsToSend) {
        List<NotificationRequestEvent> eventToSend = Lists.newArrayList();
        // for each request, create the associated notification request event
        for (AbstractRequest abstractRequest : requestsToSend) {
            // INGEST REQUESTS
            if (abstractRequest instanceof IngestRequest ingestRequest) {
                ingestRequest.getAips()
                             .forEach((aip) -> eventToSend.add(new NotificationRequestEvent(gson.toJsonTree(aip)
                                                                                                .getAsJsonObject(),
                                                                                            gson.toJsonTree(new AipNotificationRequestMetadata(
                                                                                                    RequestTypeConstant.INGEST_VALUE,
                                                                                                    aip.getSession(),
                                                                                                    aip.getSessionOwner()))
                                                                                                .getAsJsonObject(),
                                                                                            ingestRequest.getCorrelationId(),
                                                                                            abstractRequest.getSessionOwner())));
            }
            // OAIS DELETION REQUESTS
            else if (abstractRequest instanceof OAISDeletionRequest oaisDeletionRequest) {
                // remark : aip content is in payload because it has already been removed from database
                eventToSend.add(new NotificationRequestEvent(gson.toJsonTree(oaisDeletionRequest.getAipToNotify())
                                                                 .getAsJsonObject(),
                                                             gson.toJsonTree(new AipNotificationRequestMetadata(
                                                                     RequestTypeConstant.OAIS_DELETION_VALUE,
                                                                     oaisDeletionRequest.getSession(),
                                                                     oaisDeletionRequest.getSessionOwner()))
                                                                 .getAsJsonObject(),
                                                             oaisDeletionRequest.getCorrelationId(),
                                                             abstractRequest.getSessionOwner()));
            }
            // UPDATE REQUESTS
            else if (abstractRequest instanceof AIPUpdateRequest aipUpdateRequest) {
                eventToSend.add(new NotificationRequestEvent(gson.toJsonTree(aipUpdateRequest.getAip())
                                                                 .getAsJsonObject(),
                                                             gson.toJsonTree(new AipNotificationRequestMetadata(
                                                                 RequestTypeConstant.UPDATE_VALUE,
                                                                 aipUpdateRequest.getSession(),
                                                                 aipUpdateRequest.getSessionOwner())).getAsJsonObject(),
                                                             aipUpdateRequest.getCorrelationId(),
                                                             abstractRequest.getSessionOwner()));
            }
            // DISSEMINATION REQUESTS
            else if (abstractRequest instanceof AipDisseminationRequest disseminationRequest) {
                JsonObject payload = gson.toJsonTree(disseminationRequest.getAip()).getAsJsonObject();
                JsonObject metadata = gson.toJsonTree(new AipNotificationRequestMetadata(RequestTypeConstant.AIP_DISSEMINATION_VALUE,
                                                                                         disseminationRequest.getSession(),
                                                                                         disseminationRequest.getSessionOwner()))
                                          .getAsJsonObject();

                Set<String> recipients = disseminationRequest.getRecipients();
                if (recipients.isEmpty()) {
                    // Notify with rules
                    eventToSend.add(new NotificationRequestEvent(payload,
                                                                 metadata,
                                                                 disseminationRequest.getCorrelationId(),
                                                                 disseminationRequest.getAip().getSessionOwner()));
                } else {
                    // Notify without rules, because notify directly only to selected recipients
                    eventToSend.add(new SpecificRecipientNotificationRequestEvent(payload,
                                                                                  metadata,
                                                                                  disseminationRequest.getCorrelationId(),
                                                                                  disseminationRequest.getAip()
                                                                                                      .getSessionOwner(),
                                                                                  recipients));
                }
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
        // Sort requests by type
        Set<IngestRequest> ingestRequests = new HashSet<>();
        Set<AipDisseminationRequest> disseminationRequests = new HashSet<>();
        Set<AbstractRequest> allOtherRequests = new HashSet<>();
        for (AbstractRequest successRequest : successRequests) {
            if (successRequest instanceof IngestRequest ingestRequest) {
                ingestRequests.add(ingestRequest);
            } else if (successRequest instanceof AipDisseminationRequest aipDisseminationRequest) {
                disseminationRequests.add(aipDisseminationRequest);
            } else {
                allOtherRequests.add(successRequest);
            }
        }

        if (!disseminationRequests.isEmpty()) {
            handleDisseminationNotificationSuccess(disseminationRequests);
        }

        if (!ingestRequests.isEmpty()) {
            handleIngestNotificationSuccess(ingestRequests);
        }

        // Handle other requests types (Deletion and Update requests)
        // no need to publish events like ingest requests as no service needs it for the moment
        if (!allOtherRequests.isEmpty()) {
            allOtherRequests.forEach((request) -> AIPNotificationLogger.notificationSuccess(request.getId(),
                                                                                            request.getProviderId()));
            // Delete successful requests
            requestService.deleteRequests(successRequests);
        }
    }

    private void handleDisseminationNotificationSuccess(Set<AipDisseminationRequest> disseminationRequests) {
        OffsetDateTime now = OffsetDateTime.now();
        Multimap<AIPEntity, AbstractAIPUpdateTask> updateTasksByAIP = ArrayListMultimap.create();
        for (AipDisseminationRequest disseminationRequest : disseminationRequests) {
            List<DisseminationInfo> disseminationInfos = disseminationRequest.getRecipients()
                                                                             .stream()
                                                                             .map(recipient -> new DisseminationInfo(
                                                                                 recipient,
                                                                                 now,
                                                                                 null))
                                                                             .toList();
            AIPUpdateParametersDto aipUpdateDto = AIPUpdateParametersDto.build();
            aipUpdateDto.setUpdateDisseminationInfo(disseminationInfos);

            // for loop, but it is supposed to have only one task created
            for (AbstractAIPUpdateTask updateDisseminationTask : AbstractAIPUpdateTask.build(aipUpdateDto)) {
                updateTasksByAIP.put(disseminationRequest.getAip(), updateDisseminationTask);
            }
        }
        aipUpdateRequestService.create(updateTasksByAIP);
        // delete dissemination requests
        requestService.deleteRequests(Sets.newHashSet(disseminationRequests));
    }

    private void handleIngestNotificationSuccess(Set<IngestRequest> successIngestRequests) {
        List<IngestRequestEvent> ingestRequestEvents = new ArrayList<>();
        String sipId;

        // find sip id for publication
        // ingest requests are reloaded from database because aips are not loaded from successIngestRequests (lazy mode)
        List<IngestRequest> tmpRequests = ingestRequestRepository.findByIdIn(successIngestRequests.stream()
                                                                                                  .map(IngestRequest::getId)
                                                                                                  .collect(Collectors.toSet()));
        for (IngestRequest request : tmpRequests) {
            sipId = request.getAips().get(0).getSip().getSipId();
            ingestRequestEvents.add(IngestRequestEvent.build(request.getCorrelationId(),
                                                             request.getSip().getId(),
                                                             sipId,
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
        String errorMsg = "An error occurred while notifying the request result to notifier. "
                          + "Please check issues reported on notifier.";
        for (AbstractRequest abstractRequest : errorRequests) {
            // INGEST REQUESTS
            if (abstractRequest instanceof IngestRequest ingestRequest) {
                AIPNotificationLogger.notificationError(ingestRequest.getId(),
                                                        ingestRequest.getProviderId(),
                                                        ingestRequest.getErrors());
                // put request state to error and change step
                ingestRequest.setState(InternalRequestState.ERROR);
                ingestRequest.setStep(IngestRequestStep.REMOTE_NOTIFICATION_ERROR);
                ingestRequest.setErrorType(IngestErrorType.NOTIFICATION);
                ingestRequest.addError(errorMsg);
            }
            // OAIS DELETION REQUESTS
            else if (abstractRequest instanceof OAISDeletionRequest oaisDeletionRequest) {
                AIPNotificationLogger.notificationError(oaisDeletionRequest.getId(),
                                                        oaisDeletionRequest.getProviderId(),
                                                        oaisDeletionRequest.getErrors());
                // put request state to error and change step
                oaisDeletionRequest.setState(InternalRequestState.ERROR);
                oaisDeletionRequest.setStep(DeletionRequestStep.REMOTE_NOTIFICATION_ERROR);
                oaisDeletionRequest.setErrorType(IngestErrorType.NOTIFICATION);
                oaisDeletionRequest.addError(errorMsg);
            }
            // UPDATE REQUESTS
            else if (abstractRequest instanceof AIPUpdateRequest aipUpdateRequest) {
                AIPNotificationLogger.notificationError(aipUpdateRequest.getId(),
                                                        aipUpdateRequest.getProviderId(),
                                                        aipUpdateRequest.getErrors());
                // put request state to error and change step
                aipUpdateRequest.setState(InternalRequestState.ERROR);
                aipUpdateRequest.setStep(AIPUpdateRequestStep.REMOTE_NOTIFICATION_ERROR);
                aipUpdateRequest.setErrorType(IngestErrorType.NOTIFICATION);
                aipUpdateRequest.addError(errorMsg);
            }
            // DISSEMINATION REQUEST
            else if (abstractRequest instanceof AipDisseminationRequest aipDisseminationRequest) {
                AIPNotificationLogger.notificationError(aipDisseminationRequest.getId(),
                                                        aipDisseminationRequest.getProviderId(),
                                                        aipDisseminationRequest.getErrors());
                aipDisseminationRequest.setState(InternalRequestState.ERROR);
                aipDisseminationRequest.setErrorType(IngestErrorType.DISSEMINATION);
                aipDisseminationRequest.addError(errorMsg);
            }
        }
        // Save error requests
        abstractRequestRepo.saveAll(errorRequests);
    }
}