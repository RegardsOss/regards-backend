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
package fr.cnes.regards.modules.workermanager.service.requests;

import com.google.common.collect.*;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.workermanager.dao.IRequestRepository;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerManagerSettings;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.events.EventHeadersHelper;
import fr.cnes.regards.modules.workermanager.dto.events.RawMessageBuilder;
import fr.cnes.regards.modules.workermanager.dto.events.in.RequestEvent;
import fr.cnes.regards.modules.workermanager.dto.events.in.WorkerResponseEvent;
import fr.cnes.regards.modules.workermanager.dto.events.out.ResponseEvent;
import fr.cnes.regards.modules.workermanager.dto.events.out.ResponseStatus;
import fr.cnes.regards.modules.workermanager.dto.events.out.WorkerRequestEvent;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestDTO;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestInfo;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.cache.WorkerCacheService;
import fr.cnes.regards.modules.workermanager.service.config.settings.WorkerManagerSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to handle : <ul>
 * <li>{@link RequestEvent} received from external components to create new requests.</li>
 * <li>{@link WorkerResponseEvent} received from workers to inform about request status.</li>
 * <li>{@link WorkerRequestEvent} received from workers request DLQ in case of unhandled errors on worker.</li>
 * </ul>
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class RequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestService.class);

    private static final String GRANTED_MESSAGE = "Your request has been successfully registered.";

    private static final String DELAYED_MESSAGE = "Your request has been delayed as no worker is currently matching content type <%s>.";

    private static final String INVALID_MESSAGE = "Your request is not valid as its body has been invalidated by the matching worker <%s>.";

    private static final String SUCCESS_MESSAGE = "Your request has been successfully handled by the <%s> worker.";

    private static final String ERROR_MESSAGE = "Error during processing your request in <%s> worker : Cause : %s.";

    private static final String MISSING_HEADER_MESSAGE = "<%s> property is missing from request message headers";

    private static final String SKIPP_CONTENT_TYPE_MESSAGE = "Content type <%s> is configured to be automatically skipped on tenant <%s>";

    private static final String REQUEST_ID_ALREADY_EXISTS_MESSAGE = "Request is denied cause the given requestId <%s> already exists";

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IRequestRepository requestRepository;

    @Autowired
    private WorkerManagerSettingsService settingsService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private WorkerCacheService workerCacheService;

    @Value("${worker.request.queue.name.template:regards.worker.%s.request}")
    private String WORKER_REQUEST_QUEUE_NAME_TEMPLATE;

    @Value("${worker.response.queue.name:regards.worker.manager.response}")
    private String WORKER_RESPONSE_QUEUE_NAME;

    @Value("${worker.request.dlq.name:regards.worker.manager.request.errors}")
    private String WORKER_REQUEST_DLQ_NAME;

    @Value("${worker.routing.key:#}")
    private String ROUTING_KEY;

    /**
     * Get worker exchange name by his workerType
     *
     * @param workerType
     * @return exchange name
     */
    public String getExchangeName(String workerType) {
        return String.format(WORKER_REQUEST_QUEUE_NAME_TEMPLATE, workerType.toLowerCase());
    }

    /**
     * Return name of the AMQP queue to listen for worker responses.
     *
     * @return
     */
    public String getWorkerResponseQueueName() {
        return WORKER_RESPONSE_QUEUE_NAME;
    }

    /**
     * Return name of the AMQP queue to listen for worker request dlq.
     *
     * @return
     */
    public String getWorkerRequestDlqName() {
        return WORKER_REQUEST_DLQ_NAME;
    }

    /**
     * Retrieve one {@link RequestDTO} by his requestId
     *
     * @param requestId
     * @return RequestDTO
     */
    public Optional<RequestDTO> get(String requestId) {
        Optional<RequestDTO> dto = Optional.empty();
        Optional<Request> request = requestRepository.findOneByRequestId(requestId);
        if (request.isPresent()) {
            dto = Optional.of(request.get().toDTO());
        }
        return dto;
    }

    /**
     * Register {@link Request}s by reading given {@link Message}.<br/>
     * Save valid ones and reject invalid ones.
     *
     * @param events {@link Message}
     * @return {@link Request}
     */
    public RequestInfo registerRequests(List<Message> events) {
        RequestInfo requestInfo = new RequestInfo();

        // Omit request not valid
        Collection<Message> validEvents = this.omitInvalidMessages(events, requestInfo);

        // Transform events to Requests
        Collection<Request> requests = getRequestsFromEvents(validEvents);

        // Handle requests
        return this.handleRequests(requests, requestInfo);
    }

    /**
     * Using the WorkerCache and {@link Request} infos, try to send {@link WorkerRequestEvent}s to workers, <br/>
     * update {@link Request}s state and notify the owner of {@link Request}s about advancement.
     *
     * @param requests    request to send and update
     * @param requestInfo various information about what's going on
     * @return {@link RequestInfo}s various information about what's going on
     */
    public RequestInfo handleRequests(Collection<Request> requests, RequestInfo requestInfo) {
        // Create a multimap to publish bulk events by worker type
        Multimap<String, Request> toDispatchRequests = ArrayListMultimap.create();
        // Check and update request depending on whether they can be dispatched (using Worker cache)
        for (Request request : requests) {
            Optional<String> workerTypeOpt = workerCacheService.getWorkerTypeByContentType(request.getContentType());
            if (workerTypeOpt.isPresent()) {
                // Matching a worker alive
                request.setStatus(RequestStatus.DISPATCHED);
                request.setDispatchedWorkerType(workerTypeOpt.get());
                toDispatchRequests.put(workerTypeOpt.get(), request);
                requestInfo.getDispatchedRequests().add(request.toDTO());
            } else {
                // No worker alive
                request.setStatus(RequestStatus.NO_WORKER_AVAILABLE);
                requestInfo.getDelayedRequests().add(request.toDTO());
            }
        }
        // Save status update
        requestRepository.saveAll(requests);

        // Publish requests to corresponding workers
        for (String workerType : toDispatchRequests.keySet()) {
            Collection<Request> requestsByWorkerType = toDispatchRequests.get(workerType);
            List<Message> events = requestsByWorkerType.stream()
                    .map(request -> RawMessageBuilder.build(runtimeTenantResolver.getTenant(), request.getContentType(),
                                                            request.getSource(), request.getSession(),
                                                            request.getRequestId(), request.getContent()))
                    .collect(Collectors.toList());

            // Send events to the worker queue
            String exchangeName = getExchangeName(workerType);
            LOGGER.debug("Sending {} messages to worker {}", events.size(), workerType);
            publisher.broadcastAll(exchangeName, Optional.of(exchangeName), Optional.of(ROUTING_KEY),
                                   Optional.of(WorkerRequestEvent.DLQ_ROOTING_KEY), 0, events, Maps.newHashMap());
        }
        // Notify owner of the request
        notifyStatus(requests);
        return requestInfo;
    }

    /**
     * Handle events received from workers to inform about a request status changed
     *
     * @param events {@link WorkerResponseEvent} to handle
     * @return RequestInfo containing information about requests updated
     */
    public RequestInfo handleWorkersResponses(Collection<WorkerResponseEvent> events) {
        RequestInfo requestInfo = new RequestInfo();
        // Retrieve requests matching worker responses
        List<Request> requests = requestRepository.findByRequestIdIn(
                events.stream().map(e -> e.getRequestIdHeader()).collect(Collectors.toList()));
        // For each worker response update matching request status
        events.forEach(e -> {
            Optional<Request> oRequest = requests.stream().filter(r -> e.getRequestIdHeader().equals(r.getRequestId()))
                    .findFirst();
            if (oRequest.isPresent()) {
                Request request = oRequest.get();
                switch (e.getStatus()) {
                    case RUNNING:
                        request.setStatus(RequestStatus.RUNNING);
                        break;
                    case INVALID_CONTENT:
                        request.setStatus(RequestStatus.INVALID_CONTENT);
                        request.setError(String.join(",", e.getMessages()));
                        break;
                    case SUCCESS:
                        request.setStatus(RequestStatus.SUCCESS);
                        break;
                    case ERROR:
                        request.setStatus(RequestStatus.ERROR);
                        request.setError(String.join(",", e.getMessages()));
                        break;
                }
            } else {
                LOGGER.warn("Request id {} from worker {} does not match ay known request on manager.",
                            e.getRequestIdHeader(), e.getRequestIdHeader());
            }
        });
        // Save updated requests and notify clients
        requestRepository.saveAll(requests);
        notifyStatus(requests);

        requestInfo.setRunningRequests(
                requests.stream().filter(r -> r.getStatus() == RequestStatus.RUNNING).map(Request::toDTO)
                        .collect(Collectors.toList()));
        requestInfo.setSuccessRequests(
                requests.stream().filter(r -> r.getStatus() == RequestStatus.SUCCESS).map(Request::toDTO)
                        .collect(Collectors.toList()));
        requestInfo.setErrorRequests(requests.stream().filter(r -> r.getStatus() == RequestStatus.ERROR
                || r.getStatus() == RequestStatus.INVALID_CONTENT).map(Request::toDTO).collect(Collectors.toList()));

        // Delete succeeded requests. Success requests do not need to be persisted
        requests.stream().filter(r -> r.getStatus().equals(RequestStatus.SUCCESS)).forEach(requestRepository::delete);
        return requestInfo;
    }

    /**
     * Update requests to error status with the associated error string.<br/>
     * The WorkerRequestEvent provided are read from the WorkerRequestEvent DLQ in case of unhandled errors on workers.
     *
     * @param requestEvents {@link WorkerRequestEvent}s
     */
    public void handleRequestErrors(List<WorkerRequestEvent> requestEvents) {
        // Dispatch events in a Pair of RequestId / error
        List<Pair<String, String>> requestErrors = requestEvents.stream()
                .map(m -> Pair.of((String) m.getMessageProperties().getHeader(EventHeadersHelper.REQUEST_ID_HEADER ),
                                  (String) m.getMessageProperties()
                                          .getHeader(EventHeadersHelper.DLQ_ERROR_STACKTRACE_HEADER )))
                .collect(Collectors.toList());
        // Retrieve existing requests from database
        List<Request> requests = requestRepository.findByRequestIdIn(
                requestErrors.stream().map(r -> r.getFirst()).collect(Collectors.toList()));
        // For each request update it with error status and error cause.
        for (Pair<String, String> requestError : requestErrors) {
            String requestId = requestError.getFirst();
            String error = Optional.ofNullable(requestError.getSecond()).orElse("Unknown error from worker");
            requests.stream().filter(r -> r.getRequestId().equals(requestId)).findFirst().ifPresent(request -> {
                switch (request.getStatus()) {
                    case DISPATCHED:
                    case NO_WORKER_AVAILABLE:
                    case RUNNING:
                    case INVALID_CONTENT:
                    case SUCCESS:
                    case ERROR:
                        LOGGER.error("Request error detected from workers dlq for request {} : {}", error);
                        request.setStatus(RequestStatus.ERROR);
                        request.setError(error);
                        break;
                    case TO_DISPATCH:
                    default:
                        LOGGER.error(
                                "Request error detected from workers dlq for request {} but request is in {} status. Error is skipped.",
                                request.getStatus());
                        LOGGER.error("Skipped error is : {}", error);
                        break;
                }
            });
        }
    }

    /**
     * Converts events {@link Message}s to {@link Request}s
     *
     * @param validEvents events to convert
     * @return Requests
     */
    private Collection<Request> getRequestsFromEvents(Collection<Message> validEvents) {
        return validEvents.stream().map(event -> new Request(event, RequestStatus.TO_DISPATCH))
                .collect(Collectors.toList());
    }

    /**
     * Validate all given {@link RequestEvent}s and return only valid ones<br/>
     * Notify with a {@link Message} for all invalid events and return valid ones.<br/>
     * Validity of a {@link Message} is calculated by checking if :<ul>
     * <li>all headers are defined.</li>
     * <li>content_type is not configured to be automatically skipped.</li>
     * <li>request id provided does not exists already</li>
     * </ul>
     *
     * @param events      {@link Message}s to check validity
     * @param requestInfo {@link RequestInfo} RequestInfo information about current requests. New skipped events are added in this object.
     * @return valid {@link Message}s
     */
    private Collection<Message> omitInvalidMessages(List<Message> events, RequestInfo requestInfo) {
        if (events == null || events.isEmpty()) {
            return new ArrayList<>();
        } else {
            // Event requestId must be unique, check if any existing Request has one these events requestId
            List<String> existingIds = requestRepository.findRequestIdByRequestIdIn(
                    events.stream().map(EventHeadersHelper::getRequestIdHeader).filter(Optional::isPresent).map(Optional::get)
                            .collect(Collectors.toList()));

            // Retrieve list of content types configured to be automatically skipped
            List<String> contentTypesToSkip = settingsService.getValue(WorkerManagerSettings.SKIP_CONTENT_TYPES_NAME);
            return events.stream()
                    .filter(event -> this.omitInvalidMessages(event, contentTypesToSkip, existingIds, requestInfo))
                    .collect(Collectors.toList());
        }
    }

    /**
     * Validate a {@link Message} to check headers
     *
     * @param event              {@link Message} to validate
     * @param contentTypesToSkip types to be automatically skipped
     * @param existingIds        requestIds existing in database
     * @param requestInfo        RequestInfo information about current requests. New skipped events are added in this object.
     * @return True if event is valid.
     */
    private boolean omitInvalidMessages(Message event, List<String> contentTypesToSkip, List<String> existingIds,
            RequestInfo requestInfo) {
        List<String> errors = Lists.newArrayList();
        // Check owner is not empty
        if (!EventHeadersHelper.getOwnerHeader(event).isPresent()) {
            errors.add(String.format(MISSING_HEADER_MESSAGE, EventHeadersHelper.OWNER_HEADER ));
        }
        // Check content type is not empty and do not match a content type to skipp
        if (!EventHeadersHelper.getContentTypeHeader(event).isPresent()) {
            errors.add(String.format(MISSING_HEADER_MESSAGE, EventHeadersHelper.CONTENT_TYPE_HEADER ));
        } else if (contentTypesToSkip.contains(EventHeadersHelper.getContentTypeHeader(event).get())) {
            errors.add(String.format(SKIPP_CONTENT_TYPE_MESSAGE, EventHeadersHelper.getContentTypeHeader(event),
                                     runtimeTenantResolver.getTenant()));
        }
        // Check session is not empty
        if (!EventHeadersHelper.getSessionHeader(event).isPresent()) {
            errors.add(String.format(MISSING_HEADER_MESSAGE, EventHeadersHelper.SESSION_HEADER ));
        }
        // Check requestId does not exist already.
        if (EventHeadersHelper.getRequestIdHeader(event).isPresent()) {
            if (existingIds.contains(EventHeadersHelper.getRequestIdHeader(event).get())) {
                errors.add(String.format(REQUEST_ID_ALREADY_EXISTS_MESSAGE, EventHeadersHelper.getRequestIdHeader(event)));
            } else {
                existingIds.add(EventHeadersHelper.getRequestIdHeader(event).get());
            }
        }
        if (!errors.isEmpty()) {
            LOGGER.warn("Skipped request {}. Causes : {}", EventHeadersHelper.getRequestIdHeader(event).orElse("undefined"),
                        String.join(",", errors));
            publisher.publish(ResponseEvent.build(ResponseStatus.SKIPPED).withMessages(errors));
            requestInfo.getSkippedEvents().add(event);
            return false;
        }
        return true;
    }

    /**
     * Send {@link ResponseEvent}s for each given request to inform of the status of their request
     *
     * @param requests {@link Request}s
     */
    private void notifyStatus(Collection<Request> requests) {
        publisher.publish(
                requests.stream().map(this::generateResponseFromRequest).filter(Optional::isPresent).map(Optional::get)
                        .collect(Collectors.toList()), 0);
    }

    /**
     * Generates a {@link ResponseEvent} associated to the current status of the given {@link Request}
     *
     * @param request {@link Request}
     * @return
     */
    private Optional<ResponseEvent> generateResponseFromRequest(Request request) {
        ResponseEvent event = null;
        switch (request.getStatus()) {
            case RUNNING:
                // Do not inform clients for worker running process
                break;
            case DISPATCHED:
                event = ResponseEvent.build(ResponseStatus.GRANTED).withMessage(GRANTED_MESSAGE);
                break;
            case NO_WORKER_AVAILABLE:
                event = ResponseEvent.build(ResponseStatus.DELAYED)
                        .withMessage(String.format(DELAYED_MESSAGE, request.getContentType()));
                break;
            case INVALID_CONTENT:
                event = ResponseEvent.build(ResponseStatus.INVALID_CONTENT)
                        .withMessage(String.format(INVALID_MESSAGE, request.getDispatchedWorkerType()));
                break;
            case SUCCESS:
                event = ResponseEvent.build(ResponseStatus.SUCCESS)
                        .withMessage(String.format(SUCCESS_MESSAGE, request.getDispatchedWorkerType()));
                break;
            case ERROR:
                event = ResponseEvent.build(ResponseStatus.ERROR).withMessage(
                        String.format(ERROR_MESSAGE, request.getDispatchedWorkerType(), request.getError()));
                break;
            default:
                throw new RuntimeException(String.format("Invalid request status %s", request.getStatus().toString()));
        }
        if (event != null) {
            event.setHeader(EventHeadersHelper.REQUEST_ID_HEADER , request.getRequestId());
            event.setHeader(EventHeadersHelper.OWNER_HEADER , request.getSource());
            event.setHeader(EventHeadersHelper.SESSION_HEADER , request.getSession());
            event.setHeader(EventHeadersHelper.WORKER_ID , request.getDispatchedWorkerType());
        }
        return Optional.ofNullable(event);
    }
}
