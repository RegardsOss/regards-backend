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
package fr.cnes.regards.modules.feature.service.request;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.agent.client.ISessionAgentClient;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityWithDisseminationRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureUpdateDisseminationRequestRepository;
import fr.cnes.regards.modules.feature.domain.FeatureDisseminationInfo;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.ILightFeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureDisseminationInfos;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationInfoType;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationRequest;
import fr.cnes.regards.modules.feature.domain.request.dissemination.SessionFeatureDisseminationInfos;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.in.DisseminationAckEvent;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionProperty;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import fr.cnes.regards.modules.notifier.dto.out.Recipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Léo Mieulet
 */
@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class FeatureUpdateDisseminationService {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureUpdateDisseminationService.class);

    /**
     * The name of the property gathering all metadata about this processing step
     */
    private static final String DISSEMINATION_SESSION_STEP = "fem_dissemination";

    private static final int PAGE_SIZE = 400;

    private IFeatureEntityWithDisseminationRepository featureWithDisseminationRepository;

    private IFeatureUpdateDisseminationRequestRepository featureUpdateDisseminationRequestRepository;

    private IFeatureEntityRepository featureEntityRepository;

    private IAbstractFeatureRequestRepository<AbstractFeatureRequest> abstractFeatureRequestRepository;

    private FeatureUpdateDisseminationService self;

    private ISessionAgentClient sessionNotificationClient;

    public FeatureUpdateDisseminationService(IFeatureEntityWithDisseminationRepository featureWithDisseminationRepository,
                                             IFeatureUpdateDisseminationRequestRepository featureUpdateDisseminationRequestRepository,
                                             IFeatureEntityRepository featureEntityRepository,
                                             FeatureUpdateDisseminationService featureUpdateDisseminationService,
                                             ISessionAgentClient sessionNotificationClient,
                                             IAbstractFeatureRequestRepository<AbstractFeatureRequest> abstractFeatureRequestRepository) {
        this.featureWithDisseminationRepository = featureWithDisseminationRepository;
        this.featureUpdateDisseminationRequestRepository = featureUpdateDisseminationRequestRepository;
        this.featureEntityRepository = featureEntityRepository;
        this.self = featureUpdateDisseminationService;
        this.sessionNotificationClient = sessionNotificationClient;
        this.abstractFeatureRequestRepository = abstractFeatureRequestRepository;
    }

    public int handleRequests() {
        Pageable page = PageRequest.of(0, PAGE_SIZE);
        OffsetDateTime startDate = OffsetDateTime.now();
        Page<FeatureUpdateDisseminationRequest> results;
        int totalHandled = 0;
        do {
            // Search requests to process
            results = featureUpdateDisseminationRequestRepository.getFeatureUpdateDisseminationRequestsProcessable(
                startDate,
                page);
            if (!results.isEmpty()) {
                self.handleFeatureUpdateDisseminationRequests(results);
            }
            totalHandled += results.getNumberOfElements();
            if (results.getNumberOfElements() > 0) {
                LOG.info("Handled {} update dissemination request (remaining {}).",
                         results.getNumberOfElements(),
                         results.getTotalElements() - results.getNumberOfElements());
            }
        } while (results.hasNext());
        return totalHandled;
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFeatureUpdateDisseminationRequests(Page<FeatureUpdateDisseminationRequest> results) {
        SessionFeatureDisseminationInfos sessionInfos = new SessionFeatureDisseminationInfos();

        // Retrieve features related to these events
        List<FeatureEntity> featureEntities = getFeatureEntities(results);
        // Update features recipients
        for (FeatureEntity featureEntity : featureEntities) {
            // Retrieve the request associated to the featureEntity
            List<FeatureUpdateDisseminationRequest> requests = getRequestsByFeatureEntity(featureEntity, results);
            for (FeatureUpdateDisseminationRequest request : requests) {
                switch (request.getUpdateType()) {
                    case ACK:
                        updateFeatureRecipientsWithAckRequest(featureEntity, request, sessionInfos);
                        break;
                    case PUT:
                        updateFeatureRecipientsWithPutRequest(featureEntity, request, sessionInfos);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported request type " + request.getUpdateType());
                }
            }
            featureEntity.updateDisseminationPending();
        }
        notifySessions(sessionInfos);

        featureWithDisseminationRepository.saveAll(featureEntities);
        featureUpdateDisseminationRequestRepository.deleteAllInBatch(results);
    }

    private List<FeatureEntity> getFeatureEntities(Page<FeatureUpdateDisseminationRequest> results) {
        Set<FeatureUniformResourceName> urnList = results.stream()
                                                         .map(FeatureUpdateDisseminationRequest::getUrn)
                                                         .collect(Collectors.toSet());
        return featureWithDisseminationRepository.findByUrnIn(urnList);
    }

    private SessionFeatureDisseminationInfos updateFeatureRecipientsWithPutRequest(FeatureEntity featureEntity,
                                                                                   FeatureUpdateDisseminationRequest request,
                                                                                   SessionFeatureDisseminationInfos sessionInfos) {
        // Check if a featureRecipient exists with same recipient label
        Optional<FeatureDisseminationInfo> featureRecipientToUpdate = featureEntity.getDisseminationsInfo()
                                                                                   .stream()
                                                                                   .filter(featureRecipient -> featureRecipient.getLabel()
                                                                                                                               .equals(
                                                                                                                                   request.getRecipientLabel()))
                                                                                   .findFirst();
        if (featureRecipientToUpdate.isPresent()) {
            // Reset an existing feature recipient as this recipient has been re-notified
            featureRecipientToUpdate.get().setRequestDate(request.getCreationDate());

            featureRecipientToUpdate.get().setAckDateForNewDissiminationRequest(request);

            handleBlockingDissemination(featureRecipientToUpdate.get(), featureEntity);
        } else {
            // Add new recipient
            FeatureDisseminationInfo featureDisseminationInfo = new FeatureDisseminationInfo(request.getRecipientLabel(),
                                                                                             request.getAckRequired());
            featureDisseminationInfo.setBlocking(request.isBlockingRequired());

            featureEntity.getDisseminationsInfo().add(featureDisseminationInfo);
        }
        sessionInfos.addRequest(featureEntity, request);
        return sessionInfos;
    }

    private SessionFeatureDisseminationInfos updateFeatureRecipientsWithAckRequest(FeatureEntity featureEntity,
                                                                                   FeatureUpdateDisseminationRequest request,
                                                                                   SessionFeatureDisseminationInfos sessionInfos) {
        // Check if a featureRecipient exists with same recipient label
        Optional<FeatureDisseminationInfo> featureRecipientToUpdate = featureEntity.getDisseminationsInfo()
                                                                                   .stream()
                                                                                   .filter(featureRecipient -> featureRecipient.getLabel()
                                                                                                                               .equals(
                                                                                                                                   request.getRecipientLabel()))
                                                                                   .findFirst();
        if (featureRecipientToUpdate.isPresent()) {
            // Update existing feature recipient ack date
            featureRecipientToUpdate.get().setAckDate(request.getCreationDate());

            handleBlockingDissemination(featureRecipientToUpdate.get(), featureEntity);

            sessionInfos.addRequest(featureEntity, request);
        }
        return sessionInfos;
    }

    /**
     * Switch all feature requests in blocked step ({@link FeatureRequestStep.WAITING_BLOCKING_DISSEMINATION}) to
     * unblocked step ({@link FeatureRequestStep.LOCAL_DELAYED})
     */
    private void handleBlockingDissemination(FeatureDisseminationInfo featureDisseminationInfo,
                                             FeatureEntity featureEntity) {
        List<AbstractFeatureRequest> featureRequests = abstractFeatureRequestRepository.findAllByUrnAndStep(
            featureEntity.getUrn(),
            FeatureRequestStep.WAITING_BLOCKING_DISSEMINATION);
        if (featureDisseminationInfo.isBlocking()) {
            abstractFeatureRequestRepository.updateStep(FeatureRequestStep.LOCAL_DELAYED,
                                                        featureRequests.stream()
                                                                       .map(request -> request.getId())
                                                                       .collect(Collectors.toSet()));
        }
    }

    public void saveAckRequests(List<DisseminationAckEvent> disseminationAckEvts) {
        // Retrieve features related to these events
        Collection<FeatureUniformResourceName> urnList = disseminationAckEvts.stream()
                                                                             .map(f -> FeatureUniformResourceName.fromString(
                                                                                 f.getUrn()))
                                                                             .collect(Collectors.toList());
        List<ILightFeatureEntity> lightFeatureEntities = featureEntityRepository.findLightByUrnIn(urnList);

        List<FeatureUpdateDisseminationRequest> updateAckRequests = new ArrayList<>();

        for (DisseminationAckEvent disseminationAckEvent : disseminationAckEvts) {
            // Retrieve the FeatureEntity this event refers to
            Optional<ILightFeatureEntity> lightFeatureEntityOpt = lightFeatureEntities.stream()
                                                                                      .filter(urnAndSessionById -> urnAndSessionById.getUrn()
                                                                                                                                    .toString()
                                                                                                                                    .equals(
                                                                                                                                        disseminationAckEvent.getUrn()))
                                                                                      .findFirst();
            if (lightFeatureEntityOpt.isPresent()) {
                // Add an ACK request
                updateAckRequests.add(new FeatureUpdateDisseminationRequest(FeatureUniformResourceName.fromString(
                    disseminationAckEvent.getUrn()),
                                                                            disseminationAckEvent.getRecipientLabel(),
                                                                            FeatureUpdateDisseminationInfoType.ACK));
            }
        }
        // Save requests in database
        featureUpdateDisseminationRequestRepository.saveAll(updateAckRequests);
    }

    public void savePutRequests(List<NotifierEvent> notifierEvents,
                                Set<AbstractFeatureRequest> associatedFeatureRequest) {
        // Retrieve features related to these events
        Collection<FeatureUniformResourceName> urnList = associatedFeatureRequest.stream()
                                                                                 .map(AbstractFeatureRequest::getUrn)
                                                                                 .collect(Collectors.toSet());
        List<ILightFeatureEntity> lightFeatureEntities = featureEntityRepository.findLightByUrnIn(urnList);

        List<FeatureUpdateDisseminationRequest> putAckRequests = new ArrayList<>();

        // Update features recipients
        for (ILightFeatureEntity featureEntity : lightFeatureEntities) {
            // Retrieve requests associated to the featureEntity
            List<NotifierEvent> associatedNotifierEvents = getNotifierEvents(featureEntity,
                                                                             notifierEvents,
                                                                             associatedFeatureRequest);
            for (NotifierEvent notifierEvent : associatedNotifierEvents) {
                for (Recipient recipient : notifierEvent.getRecipients()) {
                    putAckRequests.add(new FeatureUpdateDisseminationRequest(featureEntity.getUrn(),
                                                                             recipient.getLabel(),
                                                                             FeatureUpdateDisseminationInfoType.PUT,
                                                                             recipient.isAckRequired(),
                                                                             recipient.isBlockingRequired()));
                }
            }
        }
        // Save requests in database
        featureUpdateDisseminationRequestRepository.saveAll(putAckRequests);
    }

    private List<NotifierEvent> getNotifierEvents(ILightFeatureEntity featureEntity,
                                                  List<NotifierEvent> notifierEvents,
                                                  Set<AbstractFeatureRequest> associatedFeatureRequest) {
        List<AbstractFeatureRequest> featureRequests = associatedFeatureRequest.stream()
                                                                               .filter(request -> request.getUrn()
                                                                                                         .equals(
                                                                                                             featureEntity.getUrn()))
                                                                               .toList();
        return notifierEvents.stream()
                             .filter(notifierEvent -> featureRequests.stream()
                                                                     .anyMatch(featureRequest -> featureRequest.getRequestId()
                                                                                                               .equals(
                                                                                                                   notifierEvent.getRequestId())))
                             .toList();
    }

    private List<FeatureUpdateDisseminationRequest> getRequestsByFeatureEntity(FeatureEntity featureEntity,
                                                                               Page<FeatureUpdateDisseminationRequest> results) {
        return results.stream()
                      .filter(request -> request.getUrn().equals(featureEntity.getUrn()))
                      .collect(Collectors.toList());
    }

    private void notifySessions(SessionFeatureDisseminationInfos sessionInfos) {
        sessionInfos.keySet().forEach(key -> {

            String source = key.getLeft();
            String session = key.getMiddle();
            String recipientLabel = key.getRight();
            FeatureDisseminationInfos featureDisseminationInfos = sessionInfos.get(key);

            Set<FeatureUpdateDisseminationRequest> featureUpdateDisseminationRequestPUT = featureDisseminationInfos.get(
                FeatureUpdateDisseminationInfoType.PUT);
            Set<FeatureUpdateDisseminationRequest> featureUpdateDisseminationRequestACK = featureDisseminationInfos.get(
                FeatureUpdateDisseminationInfoType.ACK);
            Set<FeatureUpdateDisseminationRequest> putRequestNoAckRequired = featureUpdateDisseminationRequestPUT.stream()
                                                                                                                 .filter(
                                                                                                                     request -> !request.getAckRequired())
                                                                                                                 .collect(
                                                                                                                     Collectors.toSet());

            // Pending requests are only ACKRequired requests
            int nbPending = featureUpdateDisseminationRequestPUT.size() - putRequestNoAckRequired.size();
            // Done requests are NoACKRequired requests + done requests
            int nbDone = featureUpdateDisseminationRequestACK.size() + putRequestNoAckRequired.size();
            // Need to remove all requests from pending when they go to DONE
            int nbPendingToRemove = featureUpdateDisseminationRequestACK.size();

            if (nbPending > 0) {
                FeatureSessionProperty property = FeatureSessionProperty.RUNNING_DISSEMINATION_PRODUCTS;
                StepProperty stepProperty = getStepProperty(source, session, property, recipientLabel, nbPending);
                sessionNotificationClient.increment(stepProperty);
            }
            if (nbDone > 0) {
                FeatureSessionProperty property = FeatureSessionProperty.DISSEMINATED_PRODUCTS;
                StepProperty stepProperty = getStepProperty(source, session, property, recipientLabel, nbDone);
                sessionNotificationClient.increment(stepProperty);
            }
            if (nbPendingToRemove > 0) {
                FeatureSessionProperty property = FeatureSessionProperty.RUNNING_DISSEMINATION_PRODUCTS;
                StepProperty stepProperty = getStepProperty(source,
                                                            session,
                                                            property,
                                                            recipientLabel,
                                                            nbPendingToRemove);
                sessionNotificationClient.decrement(stepProperty);
            }
        });
    }

    private StepProperty getStepProperty(String source,
                                         String session,
                                         FeatureSessionProperty property,
                                         String recipientLabel,
                                         int nbProducts) {
        return new StepProperty(DISSEMINATION_SESSION_STEP,
                                source,
                                session,
                                new StepPropertyInfo(StepTypeEnum.DISSEMINATION,
                                                     property.getState(),
                                                     getSessionPropertyName(property, recipientLabel),
                                                     String.valueOf(nbProducts),
                                                     property.isInputRelated(),
                                                     property.isOutputRelated()));
    }

    public String getSessionPropertyName(FeatureSessionProperty property, String recipientLabel) {
        return String.format(property.getName(), recipientLabel);
    }
}
