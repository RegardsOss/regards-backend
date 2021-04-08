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
package fr.cnes.regards.modules.featureprovider.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.AbstractRequestEvent;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.feature.client.FeatureClient;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationMetadataEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.FeatureReferenceCollection;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestSearchParameters;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.dto.StorageMetadata;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.FeatureRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.featureprovider.dao.FeatureExtractionRequestSpecification;
import fr.cnes.regards.modules.featureprovider.dao.IFeatureExtractionRequestRepository;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequest;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequestEvent;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionResponseEvent;
import fr.cnes.regards.modules.featureprovider.domain.plugin.IFeatureFactoryPlugin;
import fr.cnes.regards.modules.featureprovider.service.conf.FeatureProviderConfigurationProperties;

/**
 * Feature reference service management
 *
 * @author Kevin Marchois
 */
@Service
@MultitenantTransactional
public class FeatureExtractionService implements IFeatureExtractionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureExtractionService.class);

    private static final String PREFIX = "[MONITORING] ";

    private static final String PARAM_PREFIX = "";

    private static final String PARAM = " | %s";

    private static final String PX2 = PARAM_PREFIX + PARAM + PARAM;

    private static final String PX3 = PX2 + PARAM;

    private static final String REFERENCE_DENIED_FORMAT = PREFIX + "Feature EXTRACTION DENIED" + PX3;

    private static final String REFERENCE_GRANTED_FORMAT = PREFIX + "Feature EXTRACTION GRANTED" + PX2;

    private static final String REFERENCE_ERROR_FORMAT = PREFIX + "Feature EXTRACTION ERROR" + PX3;

    @Autowired
    private IFeatureExtractionRequestRepository featureExtractionRequestRepo;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private Validator validator;

    @Autowired
    private FeatureProviderConfigurationProperties properties;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private FeatureClient featureClient;

    @Override
    public void validateRequest(AbstractRequestEvent event, Errors errors) {
        if (!event.hasRequestId()) {
            errors.reject("missing.request.id.header", "Missing request id header");
        }
        if (!event.hasRequestDate()) {
            errors.reject("missing.request.date.header", "Missing request date header");
        }
        if (!event.hasRequestOwner()) {
            errors.reject("missing.request.owner.header", "Missing request owner header");
        }
    }

    @Override
    public boolean denyMessage(Message message, String errorMessage) {

        String requestId = AbstractRequestEvent.getRequestId(message.getMessageProperties());
        if (requestId == null) {
            return false;
        }

        String requestOwner = AbstractRequestEvent.getRequestOwner(message.getMessageProperties());
        // Monitoring log
        LOGGER.error(String.format(REFERENCE_DENIED_FORMAT, requestOwner, requestId, errorMessage));
        // Publish DENIED request
        publisher.publish(new FeatureExtractionResponseEvent(requestId, requestOwner, RequestState.DENIED,
                Sets.newHashSet(errorMessage)));
        return true;
    }

    @Override
    public RequestInfo<String> registerRequests(List<FeatureExtractionRequestEvent> events) {

        long registrationStart = System.currentTimeMillis();

        List<FeatureExtractionRequest> grantedRequests = new ArrayList<>();
        RequestInfo<String> requestInfo = new RequestInfo<>();
        Set<String> existingRequestIds = this.featureExtractionRequestRepo.findRequestId();

        events.forEach(item -> prepareFeatureReferenceRequest(item, grantedRequests, requestInfo, existingRequestIds));
        LOGGER.trace("------------->>> {} creation requests prepared in {} ms", grantedRequests.size(),
                     System.currentTimeMillis() - registrationStart);

        // Save a list of validated FeatureCreationRequest from a list of FeatureCreationRequestEvent
        featureExtractionRequestRepo.saveAll(grantedRequests);
        LOGGER.trace("------------->>> {} creation requests registered in {} ms", grantedRequests.size(),
                     System.currentTimeMillis() - registrationStart);

        return requestInfo;
    }

    /**
     * @param item {@link FeatureExtractionRequestEvent} to verify
     * @param grantedRequests validated {@link FeatureExtractionRequestEvent}
     * @param requestInfo received request info
     * @param existingRequestIds list of existing request in database
     */
    private void prepareFeatureReferenceRequest(FeatureExtractionRequestEvent item,
            List<FeatureExtractionRequest> grantedRequests, RequestInfo<String> requestInfo,
            Set<String> existingRequestIds) {
        // Validate event
        Errors errors = new MapBindingResult(new HashMap<>(), FeatureExtractionRequestEvent.class.getName());
        validator.validate(item, errors);
        validateRequest(item, errors);

        if (existingRequestIds.contains(item.getRequestId())
                || grantedRequests.stream().anyMatch(request -> request.getRequestId().equals(item.getRequestId()))) {
            errors.rejectValue("requestId", "request.requestId.exists.error.message", "Request id already exists");
        }

        if (errors.hasErrors()) {
            LOGGER.debug("Error during founded FeatureReferenceRequestEvent validation {}",
                         ErrorTranslator.getErrors(errors));
            requestInfo.addDeniedRequest(item.getRequestId(), ErrorTranslator.getErrors(errors));
            // Monitoring log
            LOGGER.error(String.format(REFERENCE_DENIED_FORMAT, item.getRequestOwner(), item.getRequestId(),
                                       ErrorTranslator.getErrors(errors)));
            // Publish DENIED request (do not persist it in DB)
            publisher.publish(new FeatureExtractionResponseEvent(item.getRequestId(), item.getRequestOwner(),
                    RequestState.DENIED, ErrorTranslator.getErrors(errors)));
            return;
        }
        // Monitoring log
        LOGGER.trace(String.format(REFERENCE_GRANTED_FORMAT, item.getRequestOwner(), item.getRequestId()));
        // Publish GRANTED request
        publisher.publish(new FeatureExtractionResponseEvent(item.getRequestId(), item.getRequestOwner(),
                RequestState.GRANTED, new HashSet<>()));

        // Add to granted request collection
        FeatureCreationMetadataEntity metadata = FeatureCreationMetadataEntity
                .build(item.getMetadata().getSessionOwner(), item.getMetadata().getSession(),
                       item.getMetadata().getStorages(), item.getMetadata().isOverride());
        grantedRequests.add(FeatureExtractionRequest
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

        List<FeatureExtractionRequest> requestsToSchedule = this.featureExtractionRequestRepo
                .findByStep(FeatureRequestStep.LOCAL_DELAYED, OffsetDateTime.now(), PageRequest
                        .of(0, properties.getMaxBulkSize(), Sort.by(Order.asc("priority"), Order.asc("requestDate"))));
        Set<Long> requestIds = requestsToSchedule.stream().map(FeatureExtractionRequest::getId)
                .collect(Collectors.toSet());
        if (!requestsToSchedule.isEmpty()) {

            featureExtractionRequestRepo.updateStep(FeatureRequestStep.LOCAL_SCHEDULED, requestIds);

            jobParameters.add(new JobParameter(FeatureExtractionCreationJob.IDS_PARAMETER, requestIds));

            // the job priority will be set according the priority of the first request to schedule
            JobInfo jobInfo = new JobInfo(false, requestsToSchedule.get(0).getPriority().getPriorityLevel(),
                    jobParameters, authResolver.getUser(), FeatureExtractionCreationJob.class.getName());
            jobInfoService.createAsQueued(jobInfo);

            LOGGER.trace("------------->>> {} reference requests scheduled in {} ms", requestsToSchedule.size(),
                         System.currentTimeMillis() - scheduleStart);

            return requestIds.size();
        }

        return 0;
    }

    @Override
    public void processRequests(List<FeatureExtractionRequest> requests) {

        long processStart = System.currentTimeMillis();

        int successCreationRequestGenerationCount = 0;
        List<FeatureCreationRequestEvent> creationRequestsToRegister = new ArrayList<>();

        for (FeatureExtractionRequest request : requests) {
            try {
                creationRequestsToRegister.add(initFeatureCreationRequest(request));
                successCreationRequestGenerationCount++;
                request.setStep(FeatureRequestStep.REMOTE_CREATION_REQUESTED);
            } catch (NotAvailablePluginConfigurationException | ModuleException e) {
                Set<String> errors = Sets.newHashSet(e.getMessage());
                // Monitoring log
                LOGGER.error(String.format(REFERENCE_ERROR_FORMAT, request.getRequestOwner(), request.getRequestId(),
                                           errors));
                // Publish ERROR request
                request.setState(RequestState.ERROR);
                publisher.publish(new FeatureExtractionResponseEvent(request.getRequestId(), request.getRequestOwner(),
                        RequestState.ERROR, errors));
            }
        }

        this.featureExtractionRequestRepo.saveAll(requests);
        if (!creationRequestsToRegister.isEmpty()) {
            this.featureClient.createFeatures(creationRequestsToRegister);
        }
        // feature creation has been asked to feature module lets handled granted and denied with a listener

        LOGGER.trace("------------->>> {} creation request published in {} ms", successCreationRequestGenerationCount,
                     System.currentTimeMillis() - processStart);
    }

    private <T> FeatureCreationRequestEvent initFeatureCreationRequest(FeatureExtractionRequest request)
            throws NotAvailablePluginConfigurationException, ModuleException {

        Optional<T> plugin;
        try {
            plugin = this.pluginService.getOptionalPlugin(request.getFactory());
        } catch (PluginUtilsRuntimeException e) {
            // Catch unexpected plugin initialization error
            throw new ModuleException(e.getMessage());
        }

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
            return FeatureCreationRequestEvent
                    .build(request.getRequestOwner(), request.getRequestId(),
                           FeatureCreationSessionMetadata.build(metadata.getSessionOwner(), metadata.getSession(),
                                                                request.getPriority(), metadata.isOverride(), array),
                           feature);
        } catch (ModuleException e) {
            // Error should be logged before so only debug level is set.
            LOGGER.debug("Generation issue", e);
            throw new ModuleException(String.format("Error generating feature for request %s : %s",
                                                    request.getRequestId(), e.getMessage()),
                    e);
        }

    }

    @Override
    public RequestInfo<String> registerRequests(@Valid FeatureReferenceCollection collection) {
        // Build events to reuse event registration code
        List<FeatureExtractionRequestEvent> toTreat = new ArrayList<>();
        for (JsonObject parameters : collection.getParameters()) {
            toTreat.add(FeatureExtractionRequestEvent.build(authResolver.getUser(), collection.getMetadata(),
                                                            parameters, OffsetDateTime.now().minusSeconds(1),
                                                            collection.getFactory()));
        }
        return registerRequests(toTreat);
    }

    @Override
    public void handleDenied(List<FeatureRequestEvent> denied) {
        if (!denied.isEmpty()) {
            Map<String, FeatureRequestEvent> deniedRequestPerRequestId = denied.stream()
                    .collect(Collectors.toMap(FeatureRequestEvent::getRequestId, Function.identity()));
            // Filter requests associated to an existing FeatureExtractionResponseEvent
            Set<String> extractRequestIds = featureExtractionRequestRepo
                    .findByRequestIdIn(deniedRequestPerRequestId.keySet());

            if (!extractRequestIds.isEmpty()) {
                List<FeatureExtractionResponseEvent> events = new ArrayList<>();
                // For each, send an extraction error event
                for (String extractRequestId : extractRequestIds) {
                    FeatureRequestEvent extractRequest = deniedRequestPerRequestId.get(extractRequestId);
                    events.add(new FeatureExtractionResponseEvent(extractRequestId, extractRequest.getRequestOwner(),
                            RequestState.ERROR, extractRequest.getErrors()));
                }
                publisher.publish(events);
                // Update FeatureExtractionResponseEvent with error state
                featureExtractionRequestRepo.updateStepByRequestIdIn(FeatureRequestStep.REMOTE_CREATION_ERROR,
                                                                     extractRequestIds);
                featureExtractionRequestRepo.updateState(RequestState.ERROR, extractRequestIds);
            }
        }

    }

    @Override
    public void handleGranted(List<FeatureRequestEvent> granted) {
        if (!granted.isEmpty()) {
            Map<String, FeatureRequestEvent> grantedRequestPerRequestId = granted.stream()
                    .collect(Collectors.toMap(FeatureRequestEvent::getRequestId, Function.identity()));
            // Filter requests associated to an existing FeatureExtractionResponseEvent
            Set<String> extractRequestIds = featureExtractionRequestRepo
                    .findByRequestIdIn(grantedRequestPerRequestId.keySet());

            if (!extractRequestIds.isEmpty()) {
                List<FeatureExtractionResponseEvent> events = new ArrayList<>();
                // For each, send an extraction success event
                for (String extractRequestId : extractRequestIds) {
                    events.add(new FeatureExtractionResponseEvent(extractRequestId,
                            grantedRequestPerRequestId.get(extractRequestId).getRequestOwner(), RequestState.SUCCESS,
                            new HashSet<>()));
                }
                publisher.publish(events);
                // Delete all success FeatureExtractionResponseEvent
                featureExtractionRequestRepo.deleteAllByRequestIdIn(extractRequestIds);
            }
        }
    }

    @Override
    public void handleProcessingUnexpectedException(List<FeatureExtractionRequest> featureExtractionRequests) {
        List<FeatureExtractionResponseEvent> errorResponses = new ArrayList<>(featureExtractionRequests.size());
        Set<String> errorMessages = ImmutableSet
                .of("Unforeseen issue occurred during this request processing. Please contact administrator or look at the logs");
        for (FeatureExtractionRequest request : featureExtractionRequests) {
            request.setState(RequestState.ERROR);
            errorResponses.add(new FeatureExtractionResponseEvent(request.getRequestId(), request.getRequestOwner(),
                    RequestState.ERROR, errorMessages));
        }
        featureExtractionRequestRepo.saveAll(featureExtractionRequests);
        publisher.publish(errorResponses);
    }

    @Override
    public RequestsPage<FeatureRequestDTO> findRequests(FeatureRequestSearchParameters searchParameters,
            Pageable page) {
        Page<FeatureExtractionRequest> requests = featureExtractionRequestRepo
                .findAll(FeatureExtractionRequestSpecification.searchAllByFilters(searchParameters, page), page);
        Page<FeatureRequestDTO> results = requests.map(f -> FeatureExtractionRequest.toDTO(f));
        return new RequestsPage<>(results.getContent(), getInfo(searchParameters), results.getPageable(),
                results.getTotalElements());
    }

    private RequestsInfo getInfo(FeatureRequestSearchParameters searchParameters) {
        if ((searchParameters.getState() != null) && (searchParameters.getState() != RequestState.ERROR)) {
            return RequestsInfo.build(0L);
        } else {
            searchParameters.withState(RequestState.ERROR);
            return RequestsInfo.build(featureExtractionRequestRepo.count(FeatureExtractionRequestSpecification
                    .searchAllByFilters(searchParameters, PageRequest.of(0, 1))));
        }
    }
}
