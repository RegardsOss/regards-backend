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
package fr.cnes.regards.modules.feature.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureUpdateRequest;
import fr.cnes.regards.modules.feature.domain.request.AbstractRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCopyRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.domain.request.NotificationRequest;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureManagementAction;
import fr.cnes.regards.modules.feature.dto.event.in.NotificationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.feature.service.job.NotificationRequestJob;
import fr.cnes.regards.modules.feature.service.logger.FeatureLogger;
import fr.cnes.regards.modules.notifier.client.INotifierClient;
import fr.cnes.regards.modules.notifier.dto.in.NotificationActionEvent;

/**
 * Service for prepare {@link NotificationActionEvent} from {@link NotificationRequestEvent}
 * @author Kevin Marchois
 *
 */
@Service
@MultitenantTransactional
public class FeatureNotificationService extends AbstractFeatureService implements IFeatureNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureNotificationService.class);

    @Autowired
    private INotificationRequestRepository notificationRequestRepo;

    @Autowired
    private IFeatureEntityRepository featureRepo;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private Validator validator;

    @Autowired
    private FeatureConfigurationProperties properties;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private Gson gson;

    @Autowired
    private INotifierClient notifierClient;

    @Override
    public int registerRequests(List<NotificationRequestEvent> events) {
        long registrationStart = System.currentTimeMillis();

        List<NotificationRequest> notificationsRequest = new ArrayList<>();
        Set<String> existingRequestIds = this.notificationRequestRepo.findRequestId();

        events.forEach(item -> prepareNotificationRequest(item, notificationsRequest, existingRequestIds));
        LOGGER.trace("------------->>> {} Notification requests prepared in {} ms",
                     notificationsRequest.size(),
                     System.currentTimeMillis() - registrationStart);

        // Save a list of validated FeatureDeletionRequest from a list of
        notificationRequestRepo.saveAll(notificationsRequest);
        LOGGER.debug("------------->>> {} Notification requests registered in {} ms",
                     notificationsRequest.size(),
                     System.currentTimeMillis() - registrationStart);
        return notificationsRequest.size();
    }

    /**
     * Prepare {@link NotificationRequest} from {@link NotificationRequestEvent} to register in database
     * @param item {@link NotificationRequestEvent} source
     * @param notificationsRequest list of {@link NotificationRequest} granted
     * @param existingRequestIds list of existing request in database
     */
    private void prepareNotificationRequest(NotificationRequestEvent item,
            List<NotificationRequest> notificationsRequest, Set<String> existingRequestIds) {
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
            return;
        }

        //FIXME voir avec marc
        NotificationRequest request = NotificationRequest.build(item.getRequestId(),
                                                                item.getRequestOwner(),
                                                                item.getRequestDate(),
                                                                FeatureRequestStep.TO_BE_NOTIFIED,
                                                                item.getPriority(),
                                                                item.getUrn(),
                                                                RequestState.GRANTED);
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
    }

    @Override
    //TODO remove as job is to be removed
    public int scheduleRequests() {
        long scheduleStart = System.currentTimeMillis();

        // Shedule job
        Set<JobParameter> jobParameters = Sets.newHashSet();
        Set<Long> requestIds = new HashSet<>();

        Page<NotificationRequest> dbRequests = this.notificationRequestRepo.findByStep(FeatureRequestStep.LOCAL_DELAYED,
                                                                                       OffsetDateTime.now(),
                                                                                       PageRequest.of(0,
                                                                                                      properties
                                                                                                              .getMaxBulkSize(),
                                                                                                      Sort.by(Order.asc(
                                                                                                              "priority"),
                                                                                                              Order.asc(
                                                                                                                      "requestDate"))));

        if (!dbRequests.isEmpty()) {
            for (NotificationRequest request : dbRequests.getContent()) {
                requestIds.add(request.getId());
            }
            notificationRequestRepo.updateStep(FeatureRequestStep.LOCAL_SCHEDULED, requestIds);

            jobParameters.add(new JobParameter(NotificationRequestJob.IDS_PARAMETER, requestIds));

            // the job priority will be set according the priority of the first request to schedule
            JobInfo jobInfo = new JobInfo(false,
                                          dbRequests.getContent().get(0).getPriority().getPriorityLevel(),
                                          jobParameters,
                                          authResolver.getUser(),
                                          NotificationRequestJob.class.getName());
            jobInfoService.createAsQueued(jobInfo);

            LOGGER.debug("------------->>> {} Notification requests scheduled in {} ms",
                         requestIds.size(),
                         System.currentTimeMillis() - scheduleStart);
            return requestIds.size();
        }
        return 0;
    }

    @Override
    //TODO remove this method and remove notification job & update NotificationRequest processing to use FeatureRequestStep#TO_BE_NOTIFIED
    public void processRequests(List<NotificationRequest> requests) {

        Map<FeatureUniformResourceName, NotificationRequest> notifPerUrn = requests.stream()
                .collect(Collectors.toMap(NotificationRequest::getUrn, Function.identity()));

        List<FeatureEntity> features = this.featureRepo
                .findByUrnIn(requests.stream().map(request -> request.getUrn()).collect(Collectors.toList()));
        List<NotificationActionEvent> notifications = new ArrayList<NotificationActionEvent>();
        for (FeatureEntity entity : features) {
            // Prepare notification
            //FIXME
            //            notifications.add(NotificationActionEvent.build(gson.toJsonTree(entity.getFeature()),
            //                                                            FeatureManagementAction.NOTIFIED.toString()));
            // Monitoring log
            NotificationRequest request = notifPerUrn.get(entity.getUrn());
            //TODO remove delete and deplace logic to check if NotificationActionEvent has been successfully handled or not from above to once µS notifier has responded
            FeatureLogger.notificationSuccess(request.getRequestOwner(), request.getRequestId(), request.getUrn());
            // Publish request success
            publisher.publish(FeatureRequestEvent.build(FeatureRequestType.NOTIFICATION,
                                                        request.getRequestId(),
                                                        request.getRequestOwner(),
                                                        entity.getProviderId(),
                                                        entity.getUrn(),
                                                        RequestState.SUCCESS));
        }
        publisher.publish(notifications);
        this.notificationRequestRepo.deleteAll(requests);
    }

    @Override
    public int sendToNotifier() {
        long scheduleStart = System.currentTimeMillis();
        Set<Long> requestIds = new HashSet<>();
        Map<Feature, Pair<FeatureManagementAction, AbstractRequest>> featureToNotifyWithActionAndRequestId = new HashMap<>();
        Page<AbstractRequest> dbRequests = Page.empty();
        //FIXME
        //        = this.notificationRequestRepo.findByStep(FeatureRequestStep.LOCAL_DELAYED,
        //                                                                                   OffsetDateTime.now(),
        //                                                                                   PageRequest.of(0,
        //                                                                                                  properties
        //                                                                                                          .getMaxBulkSize(),
        //                                                                                                  Sort.by(Order.asc(
        //                                                                                                          "priority"),
        //                                                                                                          Order.asc(
        //                                                                                                                  "requestDate"))));

        if (!dbRequests.isEmpty()) {
            for (AbstractRequest request : dbRequests.getContent()) {
                if (request instanceof AbstractFeatureUpdateRequest) {
                    featureToNotifyWithActionAndRequestId
                            .put(featureRepo.findByUrn(((AbstractFeatureUpdateRequest) request).getUrn()).getFeature(),
                                 Pair.of(FeatureManagementAction.UPDATED, request));
                } else if (request instanceof FeatureDeletionRequest) {
                    FeatureDeletionRequest deletionRequest = (FeatureDeletionRequest) request;
                    if (deletionRequest.isAlreadyDeleted()) {
                        featureToNotifyWithActionAndRequestId.put(deletionRequest.getToNotify(),
                                                                  Pair.of(FeatureManagementAction.ALREADY_DELETED,
                                                                          request));
                    } else {
                        featureToNotifyWithActionAndRequestId
                                .put(deletionRequest.getToNotify(), Pair.of(FeatureManagementAction.DELETED, request));
                    }
                } else if (request instanceof FeatureCreationRequest) {
                    featureToNotifyWithActionAndRequestId.put(((FeatureCreationRequest) request).getFeature(),
                                                              Pair.of(FeatureManagementAction.CREATED, request));
                } else if (request instanceof FeatureCopyRequest) {
                    featureToNotifyWithActionAndRequestId
                            .put(featureRepo.findByUrn(((FeatureCopyRequest) request).getUrn()).getFeature(),
                                 Pair.of(FeatureManagementAction.COPY, request));
                } else if (request instanceof NotificationRequest) {
                    featureToNotifyWithActionAndRequestId
                            .put(featureRepo.findByUrn(((NotificationRequest) request).getUrn()).getFeature(),
                                 Pair.of(FeatureManagementAction.NOTIFIED, request));
                }
                requestIds.add(request.getId());
            }
            notificationRequestRepo.updateStep(FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED, requestIds);
            List<NotificationActionEvent> toSend = new ArrayList<>();
            for (Map.Entry<Feature, Pair<FeatureManagementAction, AbstractRequest>> entry : featureToNotifyWithActionAndRequestId
                    .entrySet()) {
                Feature feature = entry.getKey();
                FeatureManagementAction action = entry.getValue().getFirst();
                AbstractRequest request = entry.getValue().getSecond();
                toSend.add(new NotificationActionEvent(gson.toJsonTree(feature),
                                                       gson.toJsonTree(new NotificationActionEventMetadata(action)),
                                                       request.getRequestId(),
                                                       request.getRequestOwner()));
            }
            notifierClient.sendNotifications(toSend);

            LOGGER.debug("------------->>> {} Notification requests in {} ms",
                         featureToNotifyWithActionAndRequestId.size(),
                         System.currentTimeMillis() - scheduleStart);
            return featureToNotifyWithActionAndRequestId.size();
        }
        return 0;
    }

    @Override
    public void handleNotificationSuccess() {
        // TODO: add notification success log
        FeatureLogger.notificationSuccess(request.getRequestOwner(), request.getRequestId(), request.getUrn());
        // Publish request success
        //TODO: see if all those informations are really needed, for example informations on entity might be difficult to get
        publisher.publish(FeatureRequestEvent.build(FeatureRequestType.NOTIFICATION,
                                                    request.getRequestId(),
                                                    request.getRequestOwner(),
                                                    entity.getProviderId(),
                                                    entity.getUrn(),
                                                    RequestState.SUCCESS));
        // Successful requests are deleted now!
        // FIXME: repo for AbstractRequest
        // featureCreationRequestRepo.deleteInBatch(requestWithoutFilesIds);
    }

    @Override
    public void handleNotificationError() {
        //TODO: add notification error log
        //TODO: handle step and state on request
    }

    private class NotificationActionEventMetadata {

        private String action;

        public NotificationActionEventMetadata(FeatureManagementAction action) {
            this.action = action.toString();
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }
}
