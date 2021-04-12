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
package fr.cnes.regards.modules.ingest.service.job;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.job.AIPEntityUpdateWrapper;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.domain.settings.AIPNotificationSettings;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.job.step.IUpdateStep;
import fr.cnes.regards.modules.ingest.service.job.step.UpdateAIPLocation;
import fr.cnes.regards.modules.ingest.service.job.step.UpdateAIPSimpleProperty;
import fr.cnes.regards.modules.ingest.service.job.step.UpdateAIPStorage;
import fr.cnes.regards.modules.ingest.service.notification.AIPNotificationService;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import fr.cnes.regards.modules.ingest.service.settings.AIPNotificationSettingsService;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;

/**
 * @author Léo Mieulet
 */
public class AIPUpdateRunnerJob extends AbstractJob<Void> {

    public static final String UPDATE_REQUEST_IDS = "UPDATE_REQUEST_IDS";

    private List<AIPUpdateRequest> requests;

    private int completionCount;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IStorageClient storageClient;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private IRequestService requestService;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private AIPNotificationService aipNotificationService;

    @Autowired
    private AIPNotificationSettingsService aipNotificationSettingsService;

    private static int compareUpdateRequests(AIPUpdateRequest r1, AIPUpdateRequest r2) {
        // sort by type of task
        int sortValue = r1.getUpdateTask().getType().getOrder(r2.getUpdateTask().getType());
        // if that's the same task type, order by creation date
        if (sortValue == 0) {
            sortValue = r1.getCreationDate().compareTo(r2.getCreationDate());
        }
        return sortValue;
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        // Retrieve param
        Type type = new TypeToken<List<Long>>() {

        }.getType();
        List<Long> updateRequestIds = getValue(parameters, UPDATE_REQUEST_IDS, type);
        // Retrieve list of update requests to handle
        this.requests = aipUpdateRequestRepository.findAllById(updateRequestIds);

    }

    @Override
    public void run() {
        // INIT
        int nbRequestsToHandle = this.requests.size(); // nb of requests to handle (retry requests + updates)
        logger.debug("[AIP UPDATE JOB] Running job for {} AIPUpdateRequest(s) requests", nbRequestsToHandle);
        long start = System.currentTimeMillis();

        // UPDATE RETRY
        // filter out requests with notification step (in case of retry)
        Set<AbstractRequest> notificationRetryRequests;
        notificationRetryRequests = requests.stream()
                .filter(req -> req.getStep() == AIPUpdateRequestStep.REMOTE_NOTIFICATION_ERROR)
                .collect(Collectors.toSet());
        if (!notificationRetryRequests.isEmpty()) {
            // remove notifications from requests to process and send them again
            this.requests.removeAll(notificationRetryRequests);
            aipNotificationService.sendRequestsToNotifier(notificationRetryRequests);
        }

        // UPDATE AIPs
        if (!this.requests.isEmpty()) {
            // save request by aip to edit
            ListMultimap<String, AIPUpdateRequest> requestByAIP = ArrayListMultimap.create();
            for (AIPUpdateRequest request : this.requests) {
                requestByAIP.put(request.getAip().getAipId(), request);
            }
            this.completionCount = requestByAIP.keySet().size();
            // run process of update
            updateAIPs(requestByAIP);
        }

        logger.debug("[AIP UPDATE JOB] Job handled for {} AIPUpdateRequest(s) requests in {}ms", nbRequestsToHandle,
                     System.currentTimeMillis() - start);
    }

    /**
     * Update AIPs
     */
    private void updateAIPs(ListMultimap<String, AIPUpdateRequest> requestByAIP) {
        List<AIPEntity> updates = new ArrayList<>();
        Set<AbstractRequest> requestsToNotify = Sets.newHashSet();
        long numberOfDeletionRequest = 0L;

        // See if notifications are required
        boolean isToNotify = aipNotificationSettingsService.isActiveNotification();

        for (String aipId : requestByAIP.keySet()) {
            // Get the ordered list of task to execute on this AIP
            List<AIPUpdateRequest> updateRequests = getOrderedTaskList(aipId, requestByAIP);
            if (Thread.currentThread().isInterrupted()) {
                updateRequests.forEach(ur -> ur.setState(InternalRequestState.ABORTED));
            } else {
                // Run each task
                AIPEntityUpdateWrapper aipWrapper = runUpdates(updateRequests);
                // Did something change in the AIP?
                if (!aipWrapper.isPristine()) {
                    // Save the AIP through the service
                    updates.add(aipWrapper.getAip());
                    // if notifications are required
                    if (isToNotify) {
                        // add request to list of requests with aip successfully modified
                        requestsToNotify.addAll(updateRequests);
                    }
                    // Wrapper also collect events
                    if (aipWrapper.hasDeletionRequests()) {
                        // Request files deletion
                        Collection<FileDeletionRequestDTO> deletionRequests = aipWrapper.getDeletionRequests();
                        logger.trace("[AIP {}] Run {} deletion requests on storage.", aipWrapper.getAip().getAipId(),
                                     deletionRequests.size());
                        numberOfDeletionRequest += deletionRequests.size();
                        storageClient.delete(deletionRequests);
                    }
                }
            }
            // update progress
            advanceCompletion();
        }
        // this use of Thread.interrupted is really wanted. we need to clear the interrupted flag so hibernate
        // transaction can be realized to update requests states.
        boolean interrupted = Thread.interrupted();

        logger.info(this.getClass().getSimpleName() + ": {} file deletion requested.", numberOfDeletionRequest);

        // Keep only ERROR requests
        List<AIPUpdateRequest> succeedRequestsToDelete = requestByAIP.values().stream()
                .filter(request -> (request.getState() != InternalRequestState.ERROR)
                        && (request.getState() != InternalRequestState.ABORTED))
                .collect(Collectors.toList());

        // If notifications are active, send them to notifier
        // remark : only requests corresponding to modified aip are notified
        if (isToNotify && !requestsToNotify.isEmpty()) {
            succeedRequestsToDelete.removeAll(requestsToNotify);
            aipNotificationService.sendRequestsToNotifier(requestsToNotify);
        }

        // Delete update requests successfully processed (except requests to notify if parameter is active)
        if (!succeedRequestsToDelete.isEmpty()) {
            requestService.deleteRequests(Sets.newHashSet(succeedRequestsToDelete));
        }

        // Save ERROR requests
        List<AIPUpdateRequest> errorRequests = requestByAIP.values().stream()
                .filter(request -> (request.getState() == InternalRequestState.ERROR)
                        || (request.getState() == InternalRequestState.ABORTED))
                .collect(Collectors.toList());
        aipUpdateRequestRepository.saveAll(errorRequests);

        // Save AIPs
        aipService.saveAll(updates);
        // if thread has been interrupted, do not forget to set the flag back!
        if (interrupted) {
            Thread.currentThread().interrupt();
        }

    }

    /**
     * Run update tasks
     */
    private AIPEntityUpdateWrapper runUpdates(List<AIPUpdateRequest> updateRequests) {
        // Initializing update steps

        // Update AIP files bean
        IUpdateStep updateAIPFile = new UpdateAIPLocation();
        beanFactory.autowireBean(updateAIPFile);

        // Update simple AIP properties bean
        IUpdateStep updateAIPSimpleProperty = new UpdateAIPSimpleProperty();
        beanFactory.autowireBean(updateAIPSimpleProperty);

        // Update AIP storages bean
        IUpdateStep updateAIPStorage = new UpdateAIPStorage();
        beanFactory.autowireBean(updateAIPStorage);

        // END initializing update steps

        AIPEntityUpdateWrapper aip = AIPEntityUpdateWrapper.build(updateRequests.get(0).getAip());
        for (AIPUpdateRequest updateRequest : updateRequests) {
            AbstractAIPUpdateTask updateTask = updateRequest.getUpdateTask();

            if ((updateTask != null) && (updateTask.getType() != null)) {
                try {
                    switch (updateTask.getType()) {
                        case ADD_FILE_LOCATION:
                        case REMOVE_FILE_LOCATION:
                            aip = updateAIPFile.run(aip, updateTask);
                            break;
                        case ADD_TAG:
                        case REMOVE_TAG:
                        case ADD_CATEGORY:
                        case REMOVE_CATEGORY:
                            aip = updateAIPSimpleProperty.run(aip, updateTask);
                            break;
                        case REMOVE_STORAGE:
                            aip = updateAIPStorage.run(aip, updateTask);
                            break;
                    }
                } catch (ModuleException e) {
                    logger.warn("An error occured while updating aip {}: {}", aip.getAip().getAipId(), e.getMessage());
                    // Save error inside requests
                    updateRequest.addError(e.getMessage());
                    updateRequest.setState(InternalRequestState.ERROR);
                }
            } else {
                logger.warn("Update task for aip {} is not valid", aip.getAip().getAipId());
                // Save error inside requests
                updateRequest.addError("Update task is not valid");
                updateRequest.setState(InternalRequestState.ERROR);
            }
        }
        return aip;
    }

    private List<AIPUpdateRequest> getOrderedTaskList(String aipId,
            ListMultimap<String, AIPUpdateRequest> requestByAIP) {
        List<AIPUpdateRequest> aipUpdateRequests = requestByAIP.get(aipId);
        aipUpdateRequests.sort(AIPUpdateRunnerJob::compareUpdateRequests);
        return aipUpdateRequests;
    }

    @Override
    public int getCompletionCount() {
        return this.completionCount;
    }

}