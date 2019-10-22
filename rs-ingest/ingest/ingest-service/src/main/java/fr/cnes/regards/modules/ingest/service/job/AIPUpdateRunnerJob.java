/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.ingest.domain.job.AIPEntityUpdateWrapper;
import fr.cnes.regards.modules.ingest.domain.request.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.job.step.IUpdateStep;
import fr.cnes.regards.modules.ingest.service.job.step.UpdateAIPLocation;
import fr.cnes.regards.modules.ingest.service.job.step.UpdateAIPSimpleProperty;
import fr.cnes.regards.modules.ingest.service.job.step.UpdateAIPStorage;
import fr.cnes.regards.modules.storagelight.client.IStorageClient;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * @author Léo Mieulet
 */
public class AIPUpdateRunnerJob extends AbstractJob<Void> {

    public static final String UPDATE_REQUEST_IDS = "UPDATE_REQUEST_IDS";

    private ListMultimap<String, AIPUpdateRequest> requestByAIP;

    @Autowired
    private IAIPService aipRepository;

    @Autowired
    private IStorageClient storageClient;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

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
        List<AIPUpdateRequest> requests = aipUpdateRequestRepository.findAllById(updateRequestIds);
        // Save request by aip to edit
        requestByAIP = ArrayListMultimap.create();
        for (AIPUpdateRequest request : requests) {
            requestByAIP.put(request.getAip().getAipId(), request);
        }
    }

    @Override
    public void run() {
        for (String aipId : requestByAIP.keySet()) {
            // Get the ordered list of task to execute on this AIP
            List<AIPUpdateRequest> updateRequests = getOrderedTaskList(aipId);
            // Run each task
            AIPEntityUpdateWrapper aipWrapper = runUpdates(updateRequests);
            // Did something change in the AIP?
            if (!aipWrapper.isPristine()) {
                // Save the AIP
                aipRepository.save(aipWrapper.getAip());
                // Wrapper also collect events
                if (aipWrapper.hasDeletionRequests()) {
                    // Request files deletion
                    storageClient.delete(aipWrapper.getDeletionRequests());
                }
                // TODO Flag the AIP metadata to update
            }
            // update progress
            advanceCompletion();
        }
        // Keep only ERROR requests
        List<AIPUpdateRequest> requests = requestByAIP.values().stream()
                .filter(request -> request.getState() != InternalRequestStep.ERROR)
                .collect(Collectors.toList());
        aipUpdateRequestRepository.deleteAll(requests);
    }

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
                LOGGER.warn("An error occured while updating aip {}: {}", aip.getAip().getAipId(), e.getMessage());
                // Save error inside requests
                updateRequest.setErrors(Sets.newHashSet(e.getMessage()));
                updateRequest.setState(InternalRequestStep.ERROR);
            }
        }
        return aip;
    }

    private List<AIPUpdateRequest> getOrderedTaskList(String aipId) {
        List<AIPUpdateRequest> aipUpdateRequests = requestByAIP.get(aipId);
        aipUpdateRequests.sort(AIPUpdateRunnerJob::compareUpdateRequests);
        return aipUpdateRequests;
    }

    @Override
    public int getCompletionCount() {
        return requestByAIP.keys().size();
    }

}