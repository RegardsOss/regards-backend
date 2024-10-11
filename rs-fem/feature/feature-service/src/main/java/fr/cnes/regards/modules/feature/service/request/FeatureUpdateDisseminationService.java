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
import fr.cnes.regards.modules.feature.service.logger.FeatureLogger;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionProperty;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import fr.cnes.regards.modules.notifier.dto.out.Recipient;
import org.hibernate.exception.LockAcquisitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Léo Mieulet
 */
@Component
@EnableRetry
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class FeatureUpdateDisseminationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureUpdateDisseminationService.class);

    /**
     * The name of the property gathering all metadata about this processing step
     */
    private static final String DISSEMINATION_SESSION_STEP = "fem_dissemination";

    private final IFeatureEntityWithDisseminationRepository featureWithDisseminationRepository;

    private final IFeatureUpdateDisseminationRequestRepository featureUpdateDisseminationRequestRepository;

    private final IFeatureEntityRepository featureEntityRepository;

    private final IAbstractFeatureRequestRepository<AbstractFeatureRequest> abstractFeatureRequestRepository;

    private final FeatureUpdateDisseminationService self;

    private final ISessionAgentClient sessionNotificationClient;

    /**
     * Pagination size for handling ACK dissemination info requests
     */
    private final int ackRequestPageSize;

    /**
     * Pagination size for handling PUT dissemination info requests
     */
    private final int putRequestPageSize;

    /**
     * Maximum number of requests to handle in one schedule task.
     */
    private final int maximumNumberOfRequestsToHandle;

    public FeatureUpdateDisseminationService(IFeatureEntityWithDisseminationRepository featureWithDisseminationRepository,
                                             IFeatureUpdateDisseminationRequestRepository featureUpdateDisseminationRequestRepository,
                                             IFeatureEntityRepository featureEntityRepository,
                                             FeatureUpdateDisseminationService featureUpdateDisseminationService,
                                             ISessionAgentClient sessionNotificationClient,
                                             IAbstractFeatureRequestRepository<AbstractFeatureRequest> abstractFeatureRequestRepository,
                                             @Value("${regards.feature.update.dissemination.put.page.size:500}")
                                             int putRequestPageSize,
                                             @Value("${regards.feature.update.dissemination.ack.page.size:100}")
                                             int ackRequestPageSize,
                                             @Value("${regards.feature.update.dissemination.max.requests"
                                                    + ".schedule:5000}") int maximumNumberOfRequestsToHandle) {
        this.featureWithDisseminationRepository = featureWithDisseminationRepository;
        this.featureUpdateDisseminationRequestRepository = featureUpdateDisseminationRequestRepository;
        this.featureEntityRepository = featureEntityRepository;
        this.self = featureUpdateDisseminationService;
        this.sessionNotificationClient = sessionNotificationClient;
        this.abstractFeatureRequestRepository = abstractFeatureRequestRepository;
        this.putRequestPageSize = putRequestPageSize;
        this.ackRequestPageSize = ackRequestPageSize;
        this.maximumNumberOfRequestsToHandle = maximumNumberOfRequestsToHandle;
    }

    /**
     * Handle dissemination requests.
     * First handle PUT requests.
     * Then handle ACK requests.
     * Requests are handled in this order to ensure that ack are handled after the dissemination info is initialized
     * and waiting for the ack during the PUT request if ACK and PUT are handled in the same bash.
     */
    public int handleRequests() {
        OffsetDateTime startDate = OffsetDateTime.now();
        int nbHandledRequests = 0;
        // Pagination size is different for put and ack handling.
        // Put ack needs less database requests so we can handle more requests per page.
        // Add a limit page number to handle to avoid scheduled task to last too long and another thread is run by
        // another instance of the service. (cf lock time of the process in scheduler)
        // IMPORTANT : No limitation on the number of requests to handle is set. We need to handle all PUT requests
        // before handling ACK requests.
        nbHandledRequests = handleRequestsByType(FeatureUpdateDisseminationInfoType.PUT,
                                                 startDate,
                                                 putRequestPageSize,
                                                 null);
        // Ensure all PUT requests have been handled before handle ACK.
        nbHandledRequests += handleRequestsByType(FeatureUpdateDisseminationInfoType.ACK,
                                                  startDate,
                                                  ackRequestPageSize,
                                                  Math.floorDiv(maximumNumberOfRequestsToHandle, ackRequestPageSize));
        return nbHandledRequests;
    }

    /**
     * Handle dissemination requests by type ACK or PUT.
     * As the number of requests can be huge, this method limits the number of requests to handle by setting two
     * parameters, pageSize and pageLimit.
     *
     * @param pageSize  number of requests per page to handle
     * @param pageLimit maximum number of page to handle. If null no limitation, all requests are handled
     */
    private int handleRequestsByType(FeatureUpdateDisseminationInfoType type,
                                     OffsetDateTime startDate,
                                     int pageSize,
                                     @Nullable Integer pageLimit) {
        Pageable page = PageRequest.of(0, pageSize);
        Page<FeatureUpdateDisseminationRequest> results;
        int totalHandled = 0;
        do {
            long start = System.currentTimeMillis();
            // Search requests to process
            results = featureUpdateDisseminationRequestRepository.getFeatureUpdateDisseminationRequestsProcessable(
                startDate,
                type,
                page);
            if (!results.isEmpty()) {
                self.handleFeatureUpdateDisseminationRequests(results);
            }
            totalHandled += results.getNumberOfElements();
            if (results.getNumberOfElements() > 0) {
                LOGGER.info("Handled {} update dissemination request of type {} (remaining {}) in {}ms.",
                            results.getNumberOfElements(),
                            type,
                            results.getTotalElements() - results.getNumberOfElements(),
                            System.currentTimeMillis() - start);
            }
        } while (results.hasNext() && (pageLimit != null && page.getPageNumber() < pageLimit));
        return totalHandled;
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(value = { OptimisticLockingFailureException.class, LockAcquisitionException.class },
               maxAttempts = 10,
               backoff = @Backoff(delay = 1000))
    public void handleFeatureUpdateDisseminationRequests(Page<FeatureUpdateDisseminationRequest> results) {
        SessionFeatureDisseminationInfos sessionInfos = new SessionFeatureDisseminationInfos();

        // Retrieve features related to these events
        List<FeatureEntity> featureEntities = getFeatureEntities(results);
        // Retrieve all blocked requests associated to the update features in one request to avoid one request per
        // request
        List<AbstractFeatureRequest> blockedRequests = abstractFeatureRequestRepository.findAllByUrnInAndStep(
            featureEntities.stream().map(FeatureEntity::getUrn).toList(),
            FeatureRequestStep.WAITING_BLOCKING_DISSEMINATION);
        // Update features recipients
        for (FeatureEntity featureEntity : featureEntities) {
            // Retrieve the request associated to the featureEntity
            List<FeatureUpdateDisseminationRequest> requests = getRequestsByFeatureEntity(featureEntity, results);
            for (FeatureUpdateDisseminationRequest request : requests) {
                switch (request.getUpdateType()) {
                    case ACK:
                        updateFeatureRecipientsWithAckRequest(featureEntity, request, blockedRequests, sessionInfos);
                        break;
                    case PUT:
                        updateFeatureRecipientsWithPutRequest(featureEntity, request, blockedRequests, sessionInfos);
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

    @Recover
    public void recoverOptimisticRetries(Exception e, Page<FeatureUpdateDisseminationRequest> results)
        throws Exception {
        LOGGER.error("[FEATURE UPDATE DISSEMINATION] Too many retries for optimistic lock. Optimistic lock is maybe "
                     + "not the right solution here", e);
        throw e;
    }

    private List<FeatureEntity> getFeatureEntities(Page<FeatureUpdateDisseminationRequest> results) {
        Set<FeatureUniformResourceName> urnList = results.stream()
                                                         .map(FeatureUpdateDisseminationRequest::getUrn)
                                                         .collect(Collectors.toSet());
        return featureWithDisseminationRepository.findByUrnIn(urnList);
    }

    /**
     * Update feature entity with new dissemination recipient
     */
    private void updateFeatureRecipientsWithPutRequest(FeatureEntity featureEntity,
                                                       FeatureUpdateDisseminationRequest request,
                                                       List<AbstractFeatureRequest> blockedRequests,
                                                       SessionFeatureDisseminationInfos sessionInfos) {
        // Check if a featureRecipient exists with same recipient label
        Optional<FeatureDisseminationInfo> featureRecipientToUpdate = featureEntity.getDisseminationsInfo()
                                                                                   .stream()
                                                                                   .filter(featureRecipient -> featureRecipient.getLabel()
                                                                                                                               .equals(
                                                                                                                                   request.getRecipientLabel()))
                                                                                   .findFirst();
        FeatureDisseminationInfo featureDisseminationInfo;
        if (featureRecipientToUpdate.isPresent()) {
            // Reset an existing feature recipient as this recipient has been re-notified
            featureDisseminationInfo = featureRecipientToUpdate.get();
            featureDisseminationInfo.updateRequestDate(request.getCreationDate(), request.getAckRequired());
            handleBlockingDissemination(featureRecipientToUpdate.get(), blockedRequests);
        } else {
            // Add new recipient
            featureDisseminationInfo = new FeatureDisseminationInfo(request);
            featureEntity.getDisseminationsInfo().add(featureDisseminationInfo);
        }
        FeatureLogger.updateDisseminationPut(featureEntity.getProviderId(),
                                             featureEntity.getUrn(),
                                             featureDisseminationInfo.getLabel(),
                                             featureDisseminationInfo.getRequestDate(),
                                             featureDisseminationInfo.getAckDate());
        sessionInfos.addRequest(featureEntity, request);
    }

    /**
     * Update feature entity with dissemination recipient ack.
     * To avoid synchronization issues, if the dissemination recipient to ack is not already associated to the
     * feature we initialize it.
     */
    private void updateFeatureRecipientsWithAckRequest(FeatureEntity featureEntity,
                                                       FeatureUpdateDisseminationRequest request,
                                                       List<AbstractFeatureRequest> blockedRequests,
                                                       SessionFeatureDisseminationInfos sessionInfos) {
        // Check if a featureRecipient exists with same recipient label
        Optional<FeatureDisseminationInfo> featureRecipientToUpdate = featureEntity.getDisseminationsInfo()
                                                                                   .stream()
                                                                                   .filter(featureRecipient -> featureRecipient.getLabel()
                                                                                                                               .equals(
                                                                                                                                   request.getRecipientLabel()))
                                                                                   .findFirst();
        FeatureDisseminationInfo featureDisseminationInfo;
        if (featureRecipientToUpdate.isPresent()) {
            // Update existing feature recipient ack date
            featureDisseminationInfo = featureRecipientToUpdate.get();
            featureDisseminationInfo.updateAckDate(request.getCreationDate());

            handleBlockingDissemination(featureDisseminationInfo, blockedRequests);
        } else {
            // This case can happen with async issue when ack is received before put request
            featureDisseminationInfo = new FeatureDisseminationInfo(request);
            featureEntity.getDisseminationsInfo().add(featureDisseminationInfo);
        }
        FeatureLogger.updateDisseminationAck(featureEntity.getProviderId(),
                                             featureEntity.getUrn(),
                                             featureDisseminationInfo.getLabel(),
                                             featureDisseminationInfo.getRequestDate(),
                                             featureDisseminationInfo.getAckDate());
        sessionInfos.addRequest(featureEntity, request);
    }

    /**
     * Switch all feature requests in blocked step ({@link FeatureRequestStep#WAITING_BLOCKING_DISSEMINATION}) to
     * unblocked step ({@link FeatureRequestStep#LOCAL_DELAYED})
     */
    private void handleBlockingDissemination(FeatureDisseminationInfo featureDisseminationInfo,
                                             List<AbstractFeatureRequest> blockedRequests) {
        if (featureDisseminationInfo.isBlocking()) {
            abstractFeatureRequestRepository.updateStep(FeatureRequestStep.LOCAL_DELAYED,
                                                        blockedRequests.stream()
                                                                       .map(AbstractFeatureRequest::getId)
                                                                       .collect(Collectors.toSet()));
        }
    }

    public void saveAckRequests(List<DisseminationAckEvent> disseminationAckEvts) {

        if (disseminationAckEvts.isEmpty()) {
            return;
        }
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
                                                                            FeatureUpdateDisseminationInfoType.ACK,
                                                                            OffsetDateTime.now()));
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
                                                                             notifierEvent.getNotificationDate(),
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
