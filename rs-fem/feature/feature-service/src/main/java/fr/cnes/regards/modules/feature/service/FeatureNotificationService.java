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
package fr.cnes.regards.modules.feature.service;

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.modules.feature.dao.FeatureNotificationRequestSpecification;
import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureNotificationRequestRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.ILightFeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.*;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureNotificationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.feature.service.logger.FeatureLogger;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionNotifier;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionProperty;
import fr.cnes.regards.modules.notifier.client.INotifierClient;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service to handle {@link NotificationRequestEvent} that are related to a {@link FeatureNotificationRequestEvent}
 *
 * @author Kevin Marchois
 */
@Service
@MultitenantTransactional
public class FeatureNotificationService extends AbstractFeatureService<FeatureNotificationRequest>
    implements IFeatureNotificationService {

    public static final String DEBUG_MSG_NOTIFICATION_REQUESTS_IN_MS = "------------->>> {} Notification requests sent in {} ms";

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureNotificationService.class);

    @Autowired
    private IFeatureNotificationRequestRepository featureNotificationRequestRepository;

    @Autowired
    private IFeatureEntityRepository featureRepo;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private Validator validator;

    @Autowired
    private FeatureConfigurationProperties properties;

    @Autowired
    private Gson gson;

    @Autowired
    private INotifierClient notifierClient;

    @Autowired
    private IAbstractFeatureRequestRepository<AbstractFeatureRequest> abstractFeatureRequestRepo;

    @Autowired
    private FeatureSessionNotifier featureSessionNotifier;

    @Autowired
    public IFeatureCreationService featureCreationService;

    @Autowired
    public IFeatureDeletionService featureDeletionService;

    @Autowired
    public IFeatureUpdateService featureUpdateService;

    @Override
    public int registerRequests(List<FeatureNotificationRequestEvent> events) {
        long registrationStart = System.currentTimeMillis();

        List<FeatureNotificationRequest> notificationsRequest = new ArrayList<>();
        Set<String> existingRequestIds = this.featureNotificationRequestRepository.findRequestId();

        Set<FeatureUniformResourceName> featureUrns = events.stream()
                                                            .map(FeatureNotificationRequestEvent::getUrn)
                                                            .collect(Collectors.toSet());
        Map<FeatureUniformResourceName, FeatureEntity> featureByUrn = featureRepo.findCompleteByUrnIn(featureUrns)
                                                                                 .stream()
                                                                                 .collect(Collectors.toMap(FeatureEntity::getUrn,
                                                                                                           Function.identity()));

        events.forEach(item -> prepareNotificationRequest(item,
                                                          featureByUrn.get(item.getUrn()),
                                                          notificationsRequest,
                                                          existingRequestIds));
        LOGGER.trace("------------->>> {} Notification requests prepared in {} ms",
                     notificationsRequest.size(),
                     System.currentTimeMillis() - registrationStart);

        // Save a list of validated FeatureDeletionRequest from a list of
        featureNotificationRequestRepository.saveAll(notificationsRequest);
        LOGGER.debug("------------->>> {} Notification requests registered in {} ms",
                     notificationsRequest.size(),
                     System.currentTimeMillis() - registrationStart);
        return notificationsRequest.size();
    }

    /**
     * Prepare {@link FeatureNotificationRequest} from {@link FeatureNotificationRequestEvent} to register in database
     *
     * @param item                 {@link FeatureNotificationRequestEvent} source
     * @param featureToNotify      {@link FeatureEntity} feature to notify
     * @param notificationsRequest list of {@link FeatureNotificationRequest} granted
     * @param existingRequestIds   list of existing request in database
     */
    private void prepareNotificationRequest(FeatureNotificationRequestEvent item,
                                            FeatureEntity featureToNotify,
                                            List<FeatureNotificationRequest> notificationsRequest,
                                            Set<String> existingRequestIds) {
        // Validate event
        Errors errors = new MapBindingResult(new HashMap<>(), FeatureDeletionRequest.class.getName());
        validator.validate(item, errors);
        validateRequest(item, errors);

        if (existingRequestIds.contains(item.getRequestId()) || notificationsRequest.stream()
                                                                                    .anyMatch(request -> request.getRequestId()
                                                                                                                .equals(
                                                                                                                    item.getRequestId()))) {
            errors.rejectValue("requestId", "request.requestId.exists.error.message", "Request id already exists");
        }

        if (featureToNotify == null) {
            errors.rejectValue("urn", "request.urn.feature.does.not.exists", "Feature to notify does not exists");
        }

        if (errors.hasErrors()) {
            // Monitoring log
            FeatureLogger.notificationDenied(item.getRequestOwner(),
                                             item.getRequestId(),
                                             item.getUrn(),
                                             ErrorTranslator.getErrors(errors));
            // Publish DENIED request
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.NOTIFICATION,
                                                        item.getRequestId(),
                                                        item.getRequestOwner(),
                                                        null,
                                                        item.getUrn(),
                                                        RequestState.DENIED,
                                                        ErrorTranslator.getErrors(errors)));
            // Update session properties
            if (featureToNotify != null) {
                featureSessionNotifier.incrementCount(featureToNotify, FeatureSessionProperty.DENIED_NOTIFY_REQUESTS);
            }
        } else {
            FeatureNotificationRequest request = FeatureNotificationRequest.build(item.getRequestId(),
                                                                                  item.getRequestOwner(),
                                                                                  item.getRequestDate(),
                                                                                  FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
                                                                                  item.getPriority(),
                                                                                  item.getUrn(),
                                                                                  RequestState.GRANTED);
            request.setToNotify(featureToNotify.getFeature());
            request.setSessionToNotify(featureToNotify.getSession());
            request.setSourceToNotify(featureToNotify.getSessionOwner());
            // Monitoring log
            FeatureLogger.notificationGranted(item.getRequestOwner(), item.getRequestId(), item.getUrn());
            // Publish GRANTED request
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.NOTIFICATION,
                                                        item.getRequestId(),
                                                        item.getRequestOwner(),
                                                        null,
                                                        item.getUrn(),
                                                        RequestState.GRANTED));
            notificationsRequest.add(request);

            // Add new request id to existing ones
            existingRequestIds.add(request.getRequestId());
            // Update session properties
            featureSessionNotifier.incrementCount(featureToNotify, FeatureSessionProperty.NOTIFY_REQUESTS);
        }
    }

    @Override
    public int sendToNotifier() {
        long sendingStart = System.currentTimeMillis();
        List<AbstractFeatureRequest> requestsToSend = abstractFeatureRequestRepo.findByStepAndRequestDateLessThanEqual(
                                                                                    FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
                                                                                    OffsetDateTime.now(),
                                                                                    PageRequest.of(0, properties.getMaxBulkSize(), Sort.by(Order.asc("priority"), Order.asc("requestDate"))))
                                                                                .getContent();
        Set<AbstractFeatureRequest> visitorErrorRequests = new HashSet<>();
        CreateNotificationRequestEventVisitor visitor = new CreateNotificationRequestEventVisitor(gson,
                                                                                                  featureRepo,
                                                                                                  visitorErrorRequests);
        if (!requestsToSend.isEmpty()) {
            List<NotificationRequestEvent> eventToSend = requestsToSend.stream()
                                                                       .map(r -> r.accept(visitor))
                                                                       .filter(Optional::isPresent)
                                                                       .map(Optional::get)
                                                                       .collect(Collectors.toList());
            effectivelySend(sendingStart, eventToSend);
            // remove visitor error requests from requests to send because they are in error and not sent!
            Set<AbstractFeatureRequest> requestsSent = new HashSet<>(requestsToSend);
            requestsSent.removeAll(visitorErrorRequests);
            abstractFeatureRequestRepo.updateStep(FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED,
                                                  requestsSent.stream()
                                                              .map(AbstractFeatureRequest::getId)
                                                              .collect(Collectors.toSet()));
            // handle notification error for visitor error requests
            handleNotificationError(visitorErrorRequests, FeatureRequestStep.LOCAL_NOTIFICATION_ERROR);

            getSessionInfoByUrn(requestsToSend.stream()
                                              .filter(FeatureNotificationRequest.class::isInstance)
                                              .map(AbstractFeatureRequest::getUrn)
                                              .collect(Collectors.toList())).forEach((urn, entity) -> featureSessionNotifier.incrementCount(
                entity,
                FeatureSessionProperty.RUNNING_NOTIFY_REQUESTS));
        }
        return requestsToSend.size();
    }

    private void effectivelySend(long scheduleStart, List<NotificationRequestEvent> toSend) {
        notifierClient.sendNotifications(toSend);

        // if there is an event there is a request, moreover each request was retrieved thanks to a Set of id
        // so event.size == request.size
        LOGGER.debug(DEBUG_MSG_NOTIFICATION_REQUESTS_IN_MS, toSend.size(), System.currentTimeMillis() - scheduleStart);
    }

    @Override
    public void handleNotificationSuccess(Set<AbstractFeatureRequest> success) {
        for (AbstractFeatureRequest request : success) {
            FeatureLogger.notificationSuccess(request.getRequestOwner(), request.getRequestId(), request.getUrn());
            // Publish request success
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.NOTIFICATION,
                                                        request.getRequestId(),
                                                        request.getRequestOwner(),
                                                        null,
                                                        request.getUrn(),
                                                        RequestState.SUCCESS));
        }
        onSuccess(success);
        // Successful requests are deleted now!
        abstractFeatureRequestRepo.deleteAllInBatch(success);
    }

    @Override
    public void handleNotificationError(Set<AbstractFeatureRequest> error, FeatureRequestStep errorStep) {
        for (AbstractFeatureRequest request : error) {
            FeatureLogger.notificationError(request.getRequestOwner(), request.getRequestId(), request.getUrn());
            // Publish request success
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.NOTIFICATION,
                                                        request.getRequestId(),
                                                        request.getRequestOwner(),
                                                        null,
                                                        request.getUrn(),
                                                        RequestState.ERROR));
        }
        onError(error);
        Set<Long> ids = error.stream().map(AbstractFeatureRequest::getId).collect(Collectors.toSet());
        abstractFeatureRequestRepo.updateStep(errorStep, ids);
        abstractFeatureRequestRepo.updateState(RequestState.ERROR, ids);
    }

    @Override
    public FeatureRequestType getRequestType() {
        return FeatureRequestType.NOTIFICATION;
    }

    @Override
    protected void logRequestDenied(String requestOwner, String requestId, Set<String> errors) {
        FeatureLogger.notificationDenied(requestOwner, requestId, null, errors);
    }

    @Override
    public Page<FeatureNotificationRequest> findRequests(FeatureRequestsSelectionDTO selection, Pageable page) {
        return featureNotificationRequestRepository.findAll(FeatureNotificationRequestSpecification.searchAllByFilters(
            selection,
            page), page);
    }

    @Override
    public RequestsInfo getInfo(FeatureRequestsSelectionDTO selection) {
        if ((selection.getFilters() != null) && ((selection.getFilters().getState() != null) && (selection.getFilters()
                                                                                                          .getState()
                                                                                                 != RequestState.ERROR))) {
            return RequestsInfo.build(0L);
        } else {
            selection.getFilters().withState(RequestState.ERROR);
            return RequestsInfo.build(featureNotificationRequestRepository.count(FeatureNotificationRequestSpecification.searchAllByFilters(
                selection,
                PageRequest.of(0, 1))));
        }
    }

    @Override
    protected IAbstractFeatureRequestRepository<FeatureNotificationRequest> getRequestsRepository() {
        return featureNotificationRequestRepository;
    }

    @Override
    public int scheduleRequests() {
        throw new NotImplementedException("Schedule of notification requests is not implemented.");
    }

    @Override
    protected FeatureNotificationRequest updateForRetry(FeatureNotificationRequest request) {
        // Nothing to do
        return request;
    }

    @Override
    protected void sessionInfoUpdateForRetry(Collection<FeatureNotificationRequest> requests) {
        Map<FeatureUniformResourceName, ILightFeatureEntity> sessionInfoByUrn = getSessionInfoByUrn(requests.stream()
                                                                                                            .map(
                                                                                                                FeatureNotificationRequest::getUrn)
                                                                                                            .collect(
                                                                                                                Collectors.toSet()));
        sessionInfoByUrn.forEach((urn, entity) -> featureSessionNotifier.decrementCount(entity,
                                                                                        FeatureSessionProperty.IN_ERROR_NOTIFY_REQUESTS));
    }

    @Override
    protected void sessionInfoUpdateForDelete(Collection<FeatureNotificationRequest> requests) {
        Map<FeatureUniformResourceName, ILightFeatureEntity> sessionInfoByUrn = getSessionInfoByUrn(requests.stream()
                                                                                                            .map(
                                                                                                                FeatureNotificationRequest::getUrn)
                                                                                                            .collect(
                                                                                                                Collectors.toSet()));
        sessionInfoByUrn.forEach((urn, entity) -> {
            featureSessionNotifier.decrementCount(entity, FeatureSessionProperty.IN_ERROR_NOTIFY_REQUESTS);
            featureSessionNotifier.decrementCount(entity, FeatureSessionProperty.NOTIFY_REQUESTS);
        });
    }

    @Override
    public void doOnSuccess(Collection<FeatureNotificationRequest> requests) {
        Map<FeatureUniformResourceName, ILightFeatureEntity> sessionInfoByUrn = getSessionInfoByUrn(requests.stream()
                                                                                                            .map(
                                                                                                                FeatureNotificationRequest::getUrn)
                                                                                                            .collect(
                                                                                                                Collectors.toSet()));
        sessionInfoByUrn.forEach((urn, entity) -> {
            featureSessionNotifier.decrementCount(entity, FeatureSessionProperty.RUNNING_NOTIFY_REQUESTS);
            featureSessionNotifier.incrementCount(entity, FeatureSessionProperty.NOTIFY_PRODUCTS);
        });
    }

    @Override
    public void doOnTerminated(Collection<FeatureNotificationRequest> requests) {
        // Nothing to do. Termination done in doOnSuccess
    }

    @Override
    public void doOnError(Collection<FeatureNotificationRequest> requests) {
        // Only notify session for requests not already in error state.
        Set<FeatureUniformResourceName> newErrorRequestsFeatureUrn = requests.stream()
                                                                             .filter(r -> r.getState()
                                                                                          != RequestState.ERROR)
                                                                             .map(FeatureNotificationRequest::getUrn)
                                                                             .collect(Collectors.toSet());
        Map<FeatureUniformResourceName, ILightFeatureEntity> sessionInfoByUrn = getSessionInfoByUrn(
            newErrorRequestsFeatureUrn);
        sessionInfoByUrn.forEach((urn, entity) -> {
            featureSessionNotifier.incrementCount(entity, FeatureSessionProperty.IN_ERROR_NOTIFY_REQUESTS);
            featureSessionNotifier.decrementCount(entity, FeatureSessionProperty.RUNNING_NOTIFY_REQUESTS);
        });
    }

    private void onError(Collection<AbstractFeatureRequest> requests) {
        featureCreationService.doOnError(filterRequests(requests, FeatureCreationRequest.class));
        featureDeletionService.doOnError(filterRequests(requests, FeatureDeletionRequest.class));
        featureUpdateService.doOnError(filterRequests(requests, FeatureUpdateRequest.class));
        doOnError(filterRequests(requests, FeatureNotificationRequest.class));
    }

    private void onSuccess(Collection<AbstractFeatureRequest> requests) {
        featureCreationService.doOnTerminated(filterRequests(requests, FeatureCreationRequest.class));
        featureDeletionService.doOnTerminated(filterRequests(requests, FeatureDeletionRequest.class));
        featureUpdateService.doOnTerminated(filterRequests(requests, FeatureUpdateRequest.class));
        doOnSuccess(filterRequests(requests, FeatureNotificationRequest.class));
    }

    private <T extends AbstractFeatureRequest> Collection<T> filterRequests(Collection<AbstractFeatureRequest> requests,
                                                                            Class<T> clazz) {
        return requests.stream().filter(clazz::isInstance).map(clazz::cast).collect(Collectors.toSet());
    }

}
