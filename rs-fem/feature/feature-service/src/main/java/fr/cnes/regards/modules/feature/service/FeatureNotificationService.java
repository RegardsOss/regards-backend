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
package fr.cnes.regards.modules.feature.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.modules.feature.dao.IAbstractFeatureRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureNotificationRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureNotificationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureNotificationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.feature.service.logger.FeatureLogger;
import fr.cnes.regards.modules.notifier.client.INotifierClient;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;

/**
 * Service for prepare {@link NotificationRequestEvent} from {@link FeatureNotificationRequestEvent}
 * @author Kevin Marchois
 *
 */
@Service
@MultitenantTransactional
public class FeatureNotificationService extends AbstractFeatureService implements IFeatureNotificationService {

    public static final String DEBUG_MSG_NOTIFICATION_REQUESTS_IN_MS = "------------->>> {} Notification requests in {} ms";

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

    @Override
    public int registerRequests(List<FeatureNotificationRequestEvent> events) {
        long registrationStart = System.currentTimeMillis();

        List<FeatureNotificationRequest> notificationsRequest = new ArrayList<>();
        Set<String> existingRequestIds = this.featureNotificationRequestRepository.findRequestId();

        events.forEach(item -> prepareNotificationRequest(item, notificationsRequest, existingRequestIds));
        LOGGER.trace("------------->>> {} Notification requests prepared in {} ms", notificationsRequest.size(),
                     System.currentTimeMillis() - registrationStart);

        // Save a list of validated FeatureDeletionRequest from a list of
        featureNotificationRequestRepository.saveAll(notificationsRequest);
        LOGGER.debug("------------->>> {} Notification requests registered in {} ms", notificationsRequest.size(),
                     System.currentTimeMillis() - registrationStart);
        return notificationsRequest.size();
    }

    /**
     * Prepare {@link FeatureNotificationRequest} from {@link FeatureNotificationRequestEvent} to register in database
     * @param item {@link FeatureNotificationRequestEvent} source
     * @param notificationsRequest list of {@link FeatureNotificationRequest} granted
     * @param existingRequestIds list of existing request in database
     */
    private void prepareNotificationRequest(FeatureNotificationRequestEvent item,
            List<FeatureNotificationRequest> notificationsRequest, Set<String> existingRequestIds) {
        // Validate event
        Errors errors = new MapBindingResult(new HashMap<>(), FeatureDeletionRequest.class.getName());
        validator.validate(item, errors);
        validateRequest(item, errors);

        if (existingRequestIds.contains(item.getRequestId()) || notificationsRequest.stream()
                .anyMatch(request -> request.getRequestId().equals(item.getRequestId()))) {
            errors.rejectValue("requestId", "request.requestId.exists.error.message", "Request id already exists");
        }

        if (errors.hasErrors()) {
            // Monitoring log
            FeatureLogger.notificationDenied(item.getRequestOwner(), item.getRequestId(), item.getUrn(),
                                             ErrorTranslator.getErrors(errors));
            // Publish DENIED request
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.NOTIFICATION, item.getRequestId(),
                                                        item.getRequestOwner(), null, item.getUrn(),
                                                        RequestState.DENIED, ErrorTranslator.getErrors(errors)));
            return;
        }

        FeatureNotificationRequest request = FeatureNotificationRequest
                .build(item.getRequestId(), item.getRequestOwner(), item.getRequestDate(),
                       FeatureRequestStep.LOCAL_TO_BE_NOTIFIED, item.getPriority(), item.getUrn(),
                       RequestState.GRANTED);
        // Monitoring log
        FeatureLogger.notificationGranted(item.getRequestOwner(), item.getRequestId(), item.getUrn());
        // Publish GRANTED request
        publisher.publish(FeatureRequestEvent.build(FeatureRequestType.NOTIFICATION, item.getRequestId(),
                                                    item.getRequestOwner(), null, item.getUrn(), RequestState.GRANTED));
        notificationsRequest.add(request);

        // Add new request id to existing ones
        existingRequestIds.add(request.getRequestId());
    }

    @Override
    public int sendToNotifier() {
        long sendingStart = System.currentTimeMillis();
        List<AbstractFeatureRequest> requestsToSend = abstractFeatureRequestRepo
                .findByStepAndRequestDateLessThanEqual(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED, OffsetDateTime.now(),
                                                       PageRequest.of(0, properties.getMaxBulkSize(), Sort
                                                               .by(Order.asc("priority"), Order.asc("requestDate"))))
                .getContent();
        if (!requestsToSend.isEmpty()) {
            List<NotificationRequestEvent> eventToSend = requestsToSend.stream()
                    .map(r -> r.accept(new CreateNotificationRequestEventVisitor(gson, featureRepo)))
                    .collect(Collectors.toList());
            effectivelySend(sendingStart, eventToSend);
            abstractFeatureRequestRepo
                    .updateStep(FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED,
                                requestsToSend.stream().map(AbstractFeatureRequest::getId).collect(Collectors.toSet()));
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
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.NOTIFICATION, request.getRequestId(),
                                                        request.getRequestOwner(), null, request.getUrn(),
                                                        RequestState.SUCCESS));
        }
        // Successful requests are deleted now!
        abstractFeatureRequestRepo.deleteInBatch(success);
    }

    @Override
    public void handleNotificationError(Set<AbstractFeatureRequest> error) {
        for (AbstractFeatureRequest request : error) {
            FeatureLogger.notificationError(request.getRequestOwner(), request.getRequestId(), request.getUrn());
            // Publish request success
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.NOTIFICATION, request.getRequestId(),
                                                        request.getRequestOwner(), null, request.getUrn(),
                                                        RequestState.ERROR));
        }
        Set<Long> ids = error.stream().map(AbstractFeatureRequest::getId).collect(Collectors.toSet());
        abstractFeatureRequestRepo.updateStep(FeatureRequestStep.REMOTE_NOTIFICATION_ERROR, ids);
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
    public Page<FeatureNotificationRequest> findRequests(Pageable page) {
        return featureNotificationRequestRepository.findAll(page);
    }
}
