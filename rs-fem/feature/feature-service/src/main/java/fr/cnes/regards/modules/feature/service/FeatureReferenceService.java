/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.feature.dao.IFeatureReferenceRequestRepository;
import fr.cnes.regards.modules.feature.domain.plugin.IFeatureFactoryPlugin;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationMetadataEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureReferenceRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.FeatureReferenceCollection;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureReferenceRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import fr.cnes.regards.modules.feature.service.job.FeatureCreationJob;
import fr.cnes.regards.modules.feature.service.job.FeatureReferenceCreationJob;
import fr.cnes.regards.modules.feature.service.logger.FeatureLogger;

/**
 * Feature reference service management
 *
 * @author Kevin Marchois
 */
@Service
@MultitenantTransactional
public class FeatureReferenceService extends AbstractFeatureService implements IFeatureReferenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureReferenceService.class);

    @Autowired
    private IFeatureReferenceRequestRepository featureReferenceRequestRepo;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private Validator validator;

    @Autowired
    private FeatureConfigurationProperties properties;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IFeatureCreationService featureCreationService;

    @Override
    public RequestInfo<String> registerRequests(List<FeatureReferenceRequestEvent> events) {

        long registrationStart = System.currentTimeMillis();

        List<FeatureReferenceRequest> grantedRequests = new ArrayList<>();
        RequestInfo<String> requestInfo = new RequestInfo<>();
        Set<String> existingRequestIds = this.featureReferenceRequestRepo.findRequestId();

        events.forEach(item -> prepareFeatureReferenceRequest(item, grantedRequests, requestInfo, existingRequestIds));
        LOGGER.trace("------------->>> {} creation requests prepared in {} ms", grantedRequests.size(),
                     System.currentTimeMillis() - registrationStart);

        // Save a list of validated FeatureCreationRequest from a list of FeatureCreationRequestEvent
        featureReferenceRequestRepo.saveAll(grantedRequests);
        LOGGER.trace("------------->>> {} creation requests registered in {} ms", grantedRequests.size(),
                     System.currentTimeMillis() - registrationStart);

        return requestInfo;
    }

    /**
     * @param item {@link FeatureReferenceRequestEvent} to verify
     * @param grantedRequests validated {@link FeatureReferenceRequestEvent}
     * @param requestInfo
     * @param existingRequestIds list of existing request in database
     */
    private void prepareFeatureReferenceRequest(FeatureReferenceRequestEvent item,
            List<FeatureReferenceRequest> grantedRequests, RequestInfo<String> requestInfo,
            Set<String> existingRequestIds) {
        // Validate event
        Errors errors = new MapBindingResult(new HashMap<>(), FeatureReferenceRequestEvent.class.getName());
        validator.validate(item, errors);
        validateRequest(item, errors);

        if (existingRequestIds.contains(item.getRequestId())
                || grantedRequests.stream().anyMatch(request -> request.getRequestId().equals(item.getRequestId()))) {
            errors.rejectValue("requestId", "request.requestId.exists.error.message", "Request id already exists");
        }

        if (errors.hasErrors()) {
            LOGGER.debug("Error during founded FeatureReferenceRequestEvent validation {}", errors.toString());
            requestInfo.addDeniedRequest(item.getRequestId(), ErrorTranslator.getErrors(errors));
            // Monitoring log
            FeatureLogger.referenceDenied(item.getRequestOwner(), item.getRequestId(),
                                          ErrorTranslator.getErrors(errors));
            // Publish DENIED request (do not persist it in DB)
            publisher.publish(FeatureRequestEvent.build(item.getRequestId(), item.getRequestOwner(), null, null,
                                                        RequestState.DENIED, ErrorTranslator.getErrors(errors)));
            return;
        }
        // Monitoring log
        FeatureLogger.referenceGranted(item.getRequestOwner(), item.getRequestId());
        // Publish GRANTED request
        publisher.publish(FeatureRequestEvent.build(item.getRequestId(), item.getRequestOwner(), null, null,
                                                    RequestState.GRANTED, null));

        // Add to granted request collection
        FeatureCreationMetadataEntity metadata = FeatureCreationMetadataEntity
                .build(item.getMetadata().getSessionOwner(), item.getMetadata().getSession(),
                       item.getMetadata().getStorages(), item.getMetadata().isOverride());
        grantedRequests.add(FeatureReferenceRequest
                .build(item.getRequestId(), item.getRequestOwner(), item.getRequestDate(), RequestState.GRANTED,
                       metadata, FeatureRequestStep.LOCAL_DELAYED, item.getMetadata().getPriority(),
                       item.getParameters(), item.getFactory()));
        requestInfo.addGrantedRequest(item.getRequestId(), RequestState.GRANTED.toString());
    }

    @Override
    public int scheduleRequests() {
        long scheduleStart = System.currentTimeMillis();

        // Shedule job
        Set<JobParameter> jobParameters = Sets.newHashSet();
        Set<Long> requestIds = new HashSet<>();

        List<FeatureReferenceRequest> requestsToSchedule = this.featureReferenceRequestRepo
                .findByStep(FeatureRequestStep.LOCAL_DELAYED, OffsetDateTime.now(), PageRequest
                        .of(0, properties.getMaxBulkSize(), Sort.by(Order.asc("priority"), Order.asc("requestDate"))));
        requestIds.addAll(requestsToSchedule.stream().map(request -> request.getId()).collect(Collectors.toList()));
        if (!requestsToSchedule.isEmpty()) {

            featureReferenceRequestRepo.updateStep(FeatureRequestStep.LOCAL_SCHEDULED, requestIds);

            jobParameters.add(new JobParameter(FeatureCreationJob.IDS_PARAMETER, requestIds));

            // the job priority will be set according the priority of the first request to schedule
            JobInfo jobInfo = new JobInfo(false, requestsToSchedule.get(0).getPriority().getPriorityLevel(),
                    jobParameters, authResolver.getUser(), FeatureReferenceCreationJob.class.getName());
            jobInfoService.createAsQueued(jobInfo);

            LOGGER.trace("------------->>> {} reference requests scheduled in {} ms", requestsToSchedule.size(),
                         System.currentTimeMillis() - scheduleStart);

            return requestIds.size();
        }

        return 0;
    }

    @Override
    public void processRequests(List<FeatureReferenceRequest> requests) {

        long processStart = System.currentTimeMillis();

        Set<FeatureReferenceRequest> successCreationRequestGeneration = new HashSet<>();
        List<FeatureCreationRequestEvent> creationRequestsToRegister = new ArrayList<>();

        for (FeatureReferenceRequest request : requests) {
            try {
                FeatureCreationRequestEvent fcre = initFeatureCreationRequest(request);
                creationRequestsToRegister.add(fcre);
                successCreationRequestGeneration.add(request);
            } catch (NotAvailablePluginConfigurationException | ModuleException e) {
                Set<String> errors = Sets.newHashSet(e.getMessage());
                // Monitoring log
                FeatureLogger.referenceError(request.getRequestOwner(), request.getRequestId(), errors);
                // Publish ERROR request
                request.setState(RequestState.ERROR);
                publisher.publish(FeatureRequestEvent.build(request.getRequestId(), request.getRequestOwner(), null,
                                                            null, RequestState.ERROR, errors));
            }
        }

        this.featureReferenceRequestRepo.saveAll(requests);
        this.featureCreationService.registerRequests(creationRequestsToRegister);
        // Successful requests are deleted now!
        this.featureReferenceRequestRepo.deleteInBatch(successCreationRequestGeneration);

        LOGGER.trace("------------->>> {} creation request published in {} ms", successCreationRequestGeneration.size(),
                     System.currentTimeMillis() - processStart);
    }

    private <T> FeatureCreationRequestEvent initFeatureCreationRequest(FeatureReferenceRequest request)
            throws NotAvailablePluginConfigurationException, ModuleException {

        Optional<T> plugin = this.pluginService.getOptionalPlugin(request.getFactory());
        if (!plugin.isPresent()) {
            String errorMessage = String.format("Unknown plugin for configuration %s", request.getFactory());
            LOGGER.error(errorMessage);
            throw new ModuleException(errorMessage);
        }

        if (!IFeatureFactoryPlugin.class.isAssignableFrom(plugin.get().getClass())) {
            String errorMessage = String.format("Bad plugin type for configuration %s. %s must implement %s.",
                                                request.getFactory(), plugin.getClass().getName(),
                                                IFeatureFactoryPlugin.class.getName());
            LOGGER.error(errorMessage);
            throw new ModuleException(errorMessage);
        }

        IFeatureFactoryPlugin factory = (IFeatureFactoryPlugin) plugin.get();

        Feature feature;
        try {
            // Extract feature
            feature = factory.generateFeature(request.getParameters());
            feature.withHistory(request.getRequestOwner());
            FeatureCreationMetadataEntity metadata = request.getMetadata();
            StorageMetadata[] array = new StorageMetadata[metadata.getStorages().size()];
            array = metadata.getStorages().toArray(array);
            return FeatureCreationRequestEvent.build(request.getRequestOwner(), request.getRequestId(),
                                                     FeatureCreationSessionMetadata
                                                             .build(metadata.getSessionOwner(), metadata.getSession(),
                                                                    request.getPriority(), false, array),
                                                     feature);
        } catch (ModuleException e) {
            throw new ModuleException(String.format("Error generating feature for request %s", request.getRequestId()),
                    e);
        }

    }

    @Override
    public RequestInfo<String> registerRequests(@Valid FeatureReferenceCollection collection) {
        // Build events to reuse event registration code
        List<FeatureReferenceRequestEvent> toTreat = new ArrayList<>();
        for (JsonObject parameters : collection.getParameters()) {
            toTreat.add(FeatureReferenceRequestEvent.build(authResolver.getUser(), collection.getMetadata(), parameters,
                                                           OffsetDateTime.now().minusSeconds(1),
                                                           collection.getFactory()));
        }
        return registerRequests(toTreat);
    }

}
