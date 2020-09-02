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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import fr.cnes.regards.modules.feature.dao.IFeatureCopyRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureDeletionRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureUpdateRequestRepository;
import fr.cnes.regards.modules.feature.dao.ILightFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.dao.ILightFeatureUpdateRequestRepository;
import fr.cnes.regards.modules.feature.dao.INotificationRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCopyRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.domain.request.LightFeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.LightFeatureUpdateRequest;
import fr.cnes.regards.modules.feature.domain.request.NotificationRequest;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureManagementAction;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.NotificationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestType;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
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

    public static final String DEBUG_MSG_NOTIFICATION_REQUESTS_IN_MS = "------------->>> {} Notification requests in {} ms";

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

    @Autowired
    private ILightFeatureCreationRequestRepository lightFeatureCreationRequestRepository;

    @Autowired
    private ILightFeatureUpdateRequestRepository lightFeatureUpdateRequestRepository;

    @Autowired
    private IFeatureCopyRequestRepository featureCopyRequestRepository;

    @Autowired
    private IFeatureDeletionRequestRepository featureDeletionRequestRepository;

    @Autowired
    private IFeatureCreationRequestRepository featureCreationRequestRepository;

    @Autowired
    private IFeatureUpdateRequestRepository featureUpdateRequestRepository;

    @PersistenceContext
    private EntityManager em;

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
                                                                FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
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
    public int scheduleRequests() {
        long scheduleStart = System.currentTimeMillis();
        int nbRequestScheduled = 0;
        // job priority is determined as highest priority of all scheduled requests, initialized as low
        int jobPriority = PriorityLevel.LOW.getPriorityLevel();
        // first lets get request to be notified
        Page<LightFeatureCreationRequest> creationRequests = this.lightFeatureCreationRequestRepository.findByStep(
                FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
                PageRequest
                        .of(0, properties.getMaxBulkSize(), Sort.by(Order.asc("priority"), Order.asc("requestDate"))));
        Page<LightFeatureUpdateRequest> updateRequests = this.lightFeatureUpdateRequestRepository
                .findRequestsToSchedule(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
                                        OffsetDateTime.now(),
                                        PageRequest.of(0,
                                                       properties.getMaxBulkSize(),
                                                       Sort.by(Order.asc("priority"), Order.asc("requestDate"))),
                                        OffsetDateTime.now());
        Page<FeatureCopyRequest> copyRequests = this.featureCopyRequestRepository
                .findByStep(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
                            OffsetDateTime.now(),
                            PageRequest.of(0,
                                           properties.getMaxBulkSize(),
                                           Sort.by(Order.asc("priority"), Order.asc("requestDate"))));
        Page<FeatureDeletionRequest> deletionRequests = this.featureDeletionRequestRepository.findByStep(
                FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
                OffsetDateTime.now(),
                PageRequest
                        .of(0, properties.getMaxBulkSize(), Sort.by(Order.asc("priority"), Order.asc("requestDate"))));
        Page<NotificationRequest> notificationRequests = this.notificationRequestRepo
                .findByStep(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
                            OffsetDateTime.now(),
                            PageRequest.of(0,
                                           properties.getMaxBulkSize(),
                                           Sort.by(Order.asc("priority"), Order.asc("requestDate"))));
        // prepare job parameters
        Set<JobParameter> jobParameters = Sets.newHashSet();
        if (!creationRequests.isEmpty()) {
            // jobPriority is low so in worst case we just re-affect the same value
            jobPriority = creationRequests.getContent().get(0).getPriority().getPriorityLevel();
            Set<Long> creationRequestIds = creationRequests.stream().map(LightFeatureCreationRequest::getId)
                    .collect(Collectors.toSet());
            jobParameters.add(new JobParameter(NotificationRequestJob.CREATION_REQUEST_IDS, creationRequestIds));
            nbRequestScheduled += creationRequestIds.size();
            lightFeatureCreationRequestRepository
                    .updateStep(FeatureRequestStep.LOCAL_NOTIFICATION_SCHEDULED, creationRequestIds);
        }
        if (!updateRequests.isEmpty()) {
            int updateRequestsPriority = updateRequests.getContent().get(0).getPriority().getPriorityLevel();
            // jobPriority must be computed here as creation requests priority may be higher
            jobPriority = Math.max(jobPriority, updateRequestsPriority);
            Set<Long> updateRequestIds = updateRequests.stream().map(LightFeatureUpdateRequest::getId)
                    .collect(Collectors.toSet());
            jobParameters.add(new JobParameter(NotificationRequestJob.UPDATE_REQUEST_IDS, updateRequestIds));
            nbRequestScheduled += updateRequestIds.size();
            lightFeatureUpdateRequestRepository
                    .updateStep(FeatureRequestStep.LOCAL_NOTIFICATION_SCHEDULED, updateRequestIds);
        }
        if (!copyRequests.isEmpty()) {
            int copyRequestsPriority = copyRequests.getContent().get(0).getPriority().getPriorityLevel();
            jobPriority = Math.max(jobPriority, copyRequestsPriority);
            Set<Long> copyRequestIds = copyRequests.stream().map(FeatureCopyRequest::getId).collect(Collectors.toSet());
            jobParameters.add(new JobParameter(NotificationRequestJob.COPY_REQUEST_IDS, copyRequestIds));
            nbRequestScheduled += copyRequestIds.size();
            featureCopyRequestRepository.updateStep(FeatureRequestStep.LOCAL_NOTIFICATION_SCHEDULED, copyRequestIds);
        }
        if (!deletionRequests.isEmpty()) {
            int deletionRequestsPriority = deletionRequests.getContent().get(0).getPriority().getPriorityLevel();
            jobPriority = Math.max(jobPriority, deletionRequestsPriority);
            Set<Long> deletionRequestIds = deletionRequests.stream().map(FeatureDeletionRequest::getId)
                    .collect(Collectors.toSet());
            jobParameters.add(new JobParameter(NotificationRequestJob.DELETION_REQUEST_IDS, deletionRequestIds));
            nbRequestScheduled += deletionRequestIds.size();
            featureDeletionRequestRepository
                    .updateStep(FeatureRequestStep.LOCAL_NOTIFICATION_SCHEDULED, deletionRequestIds);
        }
        if (!notificationRequests.isEmpty()) {
            int notificationRequestsPriority = notificationRequests.getContent().get(0).getPriority()
                    .getPriorityLevel();
            jobPriority = Math.max(jobPriority, notificationRequestsPriority);
            Set<Long> notificationRequestIds = notificationRequests.stream().map(NotificationRequest::getId)
                    .collect(Collectors.toSet());
            jobParameters
                    .add(new JobParameter(NotificationRequestJob.NOTIFICATION_REQUEST_IDS, notificationRequestIds));
            nbRequestScheduled += notificationRequestIds.size();
            notificationRequestRepo.updateStep(FeatureRequestStep.LOCAL_NOTIFICATION_SCHEDULED, notificationRequestIds);
        }
        // Shedule job

        if (!jobParameters.isEmpty()) {

            // the job priority will be set according the priority of the first request to schedule
            JobInfo jobInfo = new JobInfo(false,
                                          jobPriority,
                                          jobParameters,
                                          authResolver.getUser(),
                                          NotificationRequestJob.class.getName());
            jobInfoService.createAsQueued(jobInfo);

            LOGGER.debug("------------->>> {} Notification requests scheduled in {} ms",
                         nbRequestScheduled,
                         System.currentTimeMillis() - scheduleStart);
            return nbRequestScheduled;
        }
        return 0;
    }

    public int sendCreationRequestToNotifier(Set<Long> requestIds) {
        long scheduleStart = System.currentTimeMillis();
        List<FeatureCreationRequest> dbRequests = featureCreationRequestRepository.findAllById(requestIds);
        if (!dbRequests.isEmpty()) {
            List<NotificationActionEvent> toSend = new ArrayList<>();
            for (FeatureCreationRequest request : dbRequests) {
                // As we are updating state only thanks to light feature creation request repository for performance issues,
                // we have to detach each of the FeatureCreationRequest entity which is not recognize by hibernate
                // as the same than LightFeatureCreationRequest entity.
                this.em.detach(dbRequests);
                toSend.add(new NotificationActionEvent(gson.toJsonTree(request.getFeature()),
                                                       gson.toJsonTree(new NotificationActionEventMetadata(
                                                               FeatureManagementAction.CREATED)),
                                                       request.getRequestId(),
                                                       request.getRequestOwner()));
            }
            lightFeatureCreationRequestRepository
                    .updateStep(FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED, requestIds);
            effectivelySend(scheduleStart, toSend);
            return requestIds.size();
        }
        return 0;
    }

    private void effectivelySend(long scheduleStart, List<NotificationActionEvent> toSend) {
        notifierClient.sendNotifications(toSend);

        // if there is an event there is a request, moreover each request was retrieved thanks to a Set of id
        // so event.size == request.size
        LOGGER.debug(DEBUG_MSG_NOTIFICATION_REQUESTS_IN_MS,
                     toSend.size(),
                     System.currentTimeMillis() - scheduleStart);
    }

    public int sendDeletionRequestToNotifier(Set<Long> requestIds) {
        long scheduleStart = System.currentTimeMillis();
        List<FeatureDeletionRequest> dbRequests = featureDeletionRequestRepository.findAllById(requestIds);
        if (!dbRequests.isEmpty()) {
            List<NotificationActionEvent> toSend = new ArrayList<>();
            for (FeatureDeletionRequest request : dbRequests) {
                if (request.isAlreadyDeleted()) {
                    toSend.add(new NotificationActionEvent(gson.toJsonTree(request.getToNotify()),
                                                           gson.toJsonTree(new NotificationActionEventMetadata(
                                                                   FeatureManagementAction.ALREADY_DELETED)),
                                                           request.getRequestId(),
                                                           request.getRequestOwner()));
                } else {
                    toSend.add(new NotificationActionEvent(gson.toJsonTree(request.getToNotify()),
                                                           gson.toJsonTree(new NotificationActionEventMetadata(
                                                                   FeatureManagementAction.DELETED)),
                                                           request.getRequestId(),
                                                           request.getRequestOwner()));
                }
            }
            featureDeletionRequestRepository.updateStep(FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED, requestIds);
            effectivelySend(scheduleStart, toSend);
            return requestIds.size();
        }
        return 0;
    }

    public int sendNotifRequestToNotifier(Set<Long> requestIds) {
        long scheduleStart = System.currentTimeMillis();
        Set<Pair<Feature, NotificationRequest>> featureNRequestsToNotify = new HashSet<>();
        List<NotificationRequest> dbRequests = notificationRequestRepo.findAllById(requestIds);
        if (!dbRequests.isEmpty()) {
            for (NotificationRequest request : dbRequests) {
                    featureNRequestsToNotify
                            .add(Pair.of(featureRepo.findByUrn(request.getUrn()).getFeature(), request));
            }
            List<NotificationActionEvent> toSend = new ArrayList<>();
            for (Pair<Feature, NotificationRequest> featureNRequest : featureNRequestsToNotify) {
                NotificationRequest request = featureNRequest.getSecond();
                toSend.add(new NotificationActionEvent(gson.toJsonTree(featureNRequest.getFirst()),
                                                       gson.toJsonTree(new NotificationActionEventMetadata(
                                                               FeatureManagementAction.NOTIFIED)),
                                                       request.getRequestId(),
                                                       request.getRequestOwner()));
            }
            notificationRequestRepo.updateStep(FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED, requestIds);
            effectivelySend(scheduleStart, toSend);
            return requestIds.size();
        }
        return 0;
    }

    public int sendUpdateRequestToNotifier(Set<Long> requestIds) {
        long scheduleStart = System.currentTimeMillis();
        Set<Pair<Feature, FeatureUpdateRequest>> featureNRequestsToNotify = new HashSet<>();
        List<FeatureUpdateRequest> dbRequests = featureUpdateRequestRepository.findAllById(requestIds);
        if (!dbRequests.isEmpty()) {
            for (FeatureUpdateRequest request : dbRequests) {
                    featureNRequestsToNotify
                            .add(Pair.of(featureRepo.findByUrn(request.getUrn()).getFeature(), request));
                    // see why in this#sendCreationRequestToNotifier
                    this.em.detach(request);
            }
            List<NotificationActionEvent> toSend = new ArrayList<>();
            for (Pair<Feature, FeatureUpdateRequest> featureNRequest : featureNRequestsToNotify) {
                FeatureUpdateRequest request = featureNRequest.getSecond();
                toSend.add(new NotificationActionEvent(gson.toJsonTree(featureNRequest.getFirst()),
                                                       gson.toJsonTree(new NotificationActionEventMetadata(
                                                               FeatureManagementAction.UPDATED)),
                                                       request.getRequestId(),
                                                       request.getRequestOwner()));
            }
            lightFeatureUpdateRequestRepository.updateStep(FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED, requestIds);
            effectivelySend(scheduleStart, toSend);
            return requestIds.size();
        }
        return 0;
    }

    public int sendCopyRequestToNotifier(Set<Long> requestIds) {
        long scheduleStart = System.currentTimeMillis();
        Set<Pair<Feature, FeatureCopyRequest>> featureNRequestsToNotify = new HashSet<>();
        List<FeatureCopyRequest> dbRequests = featureCopyRequestRepository.findAllById(requestIds);
        if (!dbRequests.isEmpty()) {
            for (FeatureCopyRequest request : dbRequests) {
                    featureNRequestsToNotify
                            .add(Pair.of(featureRepo.findByUrn(request.getUrn()).getFeature(), request));
            }
            List<NotificationActionEvent> toSend = new ArrayList<>();
            for (Pair<Feature, FeatureCopyRequest> featureNRequest : featureNRequestsToNotify) {
                FeatureCopyRequest request = featureNRequest.getSecond();
                toSend.add(new NotificationActionEvent(gson.toJsonTree(featureNRequest.getFirst()),
                                                       gson.toJsonTree(new NotificationActionEventMetadata(
                                                               FeatureManagementAction.COPY)),
                                                       request.getRequestId(),
                                                       request.getRequestOwner()));
            }
            featureCopyRequestRepository.updateStep(FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED, requestIds);
            effectivelySend(scheduleStart, toSend);
            return requestIds.size();
        }
        return 0;
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
        // Successful requests are deleted now!
        abstractFeatureRequestRepo.deleteInBatch(success);
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
