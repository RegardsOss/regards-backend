/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityWithDisseminationRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureUpdateDisseminationRequestRepository;
import fr.cnes.regards.modules.feature.domain.FeatureDisseminationInfo;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.ILightFeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationInfoType;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationRequest;
import fr.cnes.regards.modules.feature.dto.event.in.DisseminationAckEvent;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import fr.cnes.regards.modules.notifier.dto.out.Recipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class FeatureUpdateDisseminationService {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureUpdateDisseminationService.class);

    private static final int PAGE_SIZE = 400;

    @Autowired
    private IFeatureEntityWithDisseminationRepository featureWithDisseminationRepo;

    @Autowired
    private IFeatureUpdateDisseminationRequestRepository featureUpdateDisseminationRequestRepository;

    @Autowired
    private IFeatureEntityRepository featureEntityRepository;

    @Autowired
    private FeatureUpdateDisseminationService self;

    public int handleRequests() {
        Pageable page = PageRequest.of(0, PAGE_SIZE);
        OffsetDateTime startDate = OffsetDateTime.now();
        Page<FeatureUpdateDisseminationRequest> results;
        int totalHandled = 0;
        do {
            // Search requests to process
            results = featureUpdateDisseminationRequestRepository.getFeatureUpdateDisseminationRequestsProcessable(
                    startDate, page);
            if (!results.isEmpty()) {
                self.handleFeatureUpdateDisseminationRequests(results);
            }
            totalHandled += results.getNumberOfElements();
            LOG.info("Handled {} update dissemination request (remaining {}).", results.getNumberOfElements(),
                     results.getTotalElements() - results.getNumberOfElements());
        } while (results.hasNext());
        return totalHandled;
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFeatureUpdateDisseminationRequests(Page<FeatureUpdateDisseminationRequest> results) {
        // Retrieve features related to these events
        List<FeatureEntity> featureEntities = getFeatureEntities(results);

        // Update features recipients
        for (FeatureEntity featureEntity : featureEntities) {
            // Retrieve the request associated to the featureEntity
            List<FeatureUpdateDisseminationRequest> requests = getRequestsByFeatureEntity(featureEntity, results);
            for (FeatureUpdateDisseminationRequest request : requests) {
                switch (request.getUpdateType()) {
                    case ACK:
                        updateFeatureRecipientsWithAckRequest(featureEntity, request);
                        break;
                    case PUT:
                        updateFeatureRecipientsWithPutRequest(featureEntity, request);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported request type " + request.getUpdateType());
                }
            }
            featureEntity.updateDisseminationPending();
        }
        featureWithDisseminationRepo.saveAll(featureEntities);
        featureUpdateDisseminationRequestRepository.deleteInBatch(results);
    }

    private List<FeatureEntity> getFeatureEntities(Page<FeatureUpdateDisseminationRequest> results) {
        Set<FeatureUniformResourceName> urnList = results.stream().map(FeatureUpdateDisseminationRequest::getUrn)
                .collect(Collectors.toSet());
        List<FeatureEntity> featureEntities = featureWithDisseminationRepo.findByUrnIn(urnList);
        return featureEntities;
    }

    private void updateFeatureRecipientsWithPutRequest(FeatureEntity featureEntity,
            FeatureUpdateDisseminationRequest request) {
        // Check if a featureRecipient exists with same recipient label
        Optional<FeatureDisseminationInfo> featureRecipientToUpdate = featureEntity.getDisseminationsInfo().stream()
                .filter(featureRecipient -> featureRecipient.getLabel().equals(request.getRecipientLabel()))
                .findFirst();
        if (featureRecipientToUpdate.isPresent()) {
            // Reset an existing feature recipient as this recipient has been re-notified
            featureRecipientToUpdate.get().setRequestDate(request.getCreationDate());
            featureRecipientToUpdate.get().setAckDateByAckRequired(request.getAckRequired());
        } else {
            // Add new recipient
            featureEntity.getDisseminationsInfo()
                    .add(new FeatureDisseminationInfo(request.getRecipientLabel(), request.getAckRequired()));
        }
    }

    private void updateFeatureRecipientsWithAckRequest(FeatureEntity featureEntity,
            FeatureUpdateDisseminationRequest request) {
        // Check if a featureRecipient exists with same recipient label
        Optional<FeatureDisseminationInfo> featureRecipientToUpdate = featureEntity.getDisseminationsInfo().stream()
                .filter(featureRecipient -> featureRecipient.getLabel().equals(request.getRecipientLabel()))
                .findFirst();
        if (featureRecipientToUpdate.isPresent()) {
            // Update existing feature recipient ack date
            featureRecipientToUpdate.get().setAckDate(request.getCreationDate());
        }
    }

    public void saveAckRequests(List<DisseminationAckEvent> messages) {
        // Retrieve features related to these events
        Collection<FeatureUniformResourceName> urnList = messages.stream().map(DisseminationAckEvent::getUrn)
                .collect(Collectors.toList());

        List<ILightFeatureEntity> lightFeatureEntities = featureEntityRepository.findByUrnIn(urnList);
        List<FeatureUpdateDisseminationRequest> updateAckRequests = new ArrayList<>();

        for (DisseminationAckEvent disseminationAckEvent : messages) {
            // Retrieve the FeatureEntity this event refers to
            Optional<ILightFeatureEntity> lightFeatureEntityOpt = lightFeatureEntities.stream()
                    .filter(urnAndSessionById -> urnAndSessionById.getUrn().equals(disseminationAckEvent.getUrn()))
                    .findFirst();
            if (lightFeatureEntityOpt.isPresent()) {
                // Add an ACK request
                updateAckRequests.add(new FeatureUpdateDisseminationRequest(disseminationAckEvent.getUrn(),
                                                                            disseminationAckEvent.getRecipientLabel(),
                                                                            FeatureUpdateDisseminationInfoType.ACK,
                                                                            Optional.empty()));
            }
        }
        // Save requests
        featureUpdateDisseminationRequestRepository.saveAll(updateAckRequests);
    }

    public void savePutRequests(List<NotifierEvent> notifierEvents,
            Set<AbstractFeatureRequest> associatedFeatureRequest) {
        // Retrieve features related to these events
        Collection<FeatureUniformResourceName> urnList = associatedFeatureRequest.stream()
                .map(AbstractFeatureRequest::getUrn).collect(Collectors.toSet());
        List<ILightFeatureEntity> lightFeatureEntities = featureEntityRepository.findByUrnIn(urnList);
        List<FeatureUpdateDisseminationRequest> putAckRequests = new ArrayList<>();

        // Update features recipients
        for (ILightFeatureEntity featureEntity : lightFeatureEntities) {
            // Retrieve requests associated to the featureEntity
            List<NotifierEvent> associatedNotifierEvents = getNotifierEvents(featureEntity, notifierEvents,
                                                                             associatedFeatureRequest);
            for (NotifierEvent notifierEvent : associatedNotifierEvents) {
                for (Recipient recipient : notifierEvent.getRecipients()) {
                    putAckRequests.add(
                            new FeatureUpdateDisseminationRequest(featureEntity.getUrn(), recipient.getLabel(),
                                                                  FeatureUpdateDisseminationInfoType.PUT,
                                                                  Optional.of(recipient.isAckRequired())));
                }
            }
        }
        // Save requests
        featureUpdateDisseminationRequestRepository.saveAll(putAckRequests);
    }

    private List<NotifierEvent> getNotifierEvents(ILightFeatureEntity featureEntity, List<NotifierEvent> notifierEvents,
            Set<AbstractFeatureRequest> associatedFeatureRequest) {
        List<AbstractFeatureRequest> featureRequests = associatedFeatureRequest.stream()
                .filter(request -> request.getUrn().equals(featureEntity.getUrn())).collect(Collectors.toList());
        return notifierEvents.stream().filter(notifierEvent -> featureRequests.stream()
                        .anyMatch(featureRequest -> featureRequest.getRequestId().equals(notifierEvent.getRequestId())))
                .collect(Collectors.toList());
    }

    private List<FeatureUpdateDisseminationRequest> getRequestsByFeatureEntity(FeatureEntity featureEntity,
            Page<FeatureUpdateDisseminationRequest> results) {
        return results.stream().filter(request -> request.getUrn().equals(featureEntity.getUrn()))
                .collect(Collectors.toList());
    }
}
