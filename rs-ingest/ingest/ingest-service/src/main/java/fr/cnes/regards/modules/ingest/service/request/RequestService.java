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
package fr.cnes.regards.modules.ingest.service.request;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.ingest.dao.*;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.mapper.IRequestMapper;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.DeletionRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.AIPPostProcessRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdatesCreatorRequest;
import fr.cnes.regards.modules.ingest.dto.request.*;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;
import fr.cnes.regards.modules.ingest.service.job.*;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import fr.cnes.regards.modules.storage.client.IStorageClient;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service to handle all {@link AbstractRequest}s
 *
 * @author Léo Mieulet
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class RequestService implements IRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestService.class);

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private IAIPPostProcessRequestRepository aipPostProcessRequestRepository;

    @Autowired
    private IRequestMapper requestMapper;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Autowired
    private IStorageClient storageClient;

    @Autowired
    private IPublisher publisher;

    @Autowired
    @Lazy
    private IRequestService self;

    @Override
    public void handleRemoteStoreError(AbstractRequest request) {
        LOGGER.warn("Request of type {} cannot be handle for remote storage error", request.getClass().getName());
    }

    @Override
    public void handleRemoteStoreSuccess(AbstractRequest request) {
        LOGGER.warn("Request of type {} cannot be handle for remote storage success", request.getClass().getName());
    }

    @Override
    public void handleRemoteRequestGranted(Set<RequestInfo> requests) {
        // Do not track at the moment : the ongoing request could send a success too quickly
        // and could cause unnecessary concurrent access to the database!
        for (RequestInfo ri : requests) {
            LOGGER.debug("Storage request granted with id \"{}\"", ri.getGroupId());
        }
    }

    @Override
    public List<AbstractRequest> findRequestsByGroupIdIn(List<String> groupIds) {
        return abstractRequestRepository.findAll(AbstractRequestSpecifications.searchAllByRemoteStepGroupId(groupIds));
    }

    @Override
    public Page<AbstractRequest> findRequests(SearchRequestsParameters filters, Pageable pageable) {
        return abstractRequestRepository.findAll(AbstractRequestSpecifications.searchAllByFilters(filters, pageable),
                                                 pageable);
    }

    @Override
    public Page<RequestDto> findRequestDtos(SearchRequestsParameters filters, Pageable pageable) {
        Page<AbstractRequest> requests = findRequests(filters, pageable);

        // Transform AbstractRequests to DTO
        List<RequestDto> dtoList = new ArrayList<>();
        for (AbstractRequest request : requests) {
            dtoList.add(requestMapper.metadataToDto(request));
        }
        return new PageImpl<>(dtoList, pageable, requests.getTotalElements());
    }

    @Override
    public void deleteAllByAip(Set<AIPEntity> aipEntities) {
        // Make the list of all these AIPs id and remove all requests associated
        List<Long> aipIds = aipEntities.stream().map(AIPEntity::getId).collect(Collectors.toList());

        List<IngestRequest> requests = ingestRequestRepository.findAllByAipsIdIn(aipIds);
        requests.forEach(r -> {
            sessionNotifier.ingestRequestErrorDeleted(r);
            // Inform request canceled
            publisher.publish(IngestRequestEvent.build(r.getRequestId(),
                                                       r.getProviderId(),
                                                       null,
                                                       RequestState.DELETED,
                                                       r.getErrors()));
        });
        // Check in order to evict the publishing of empty request
        if (!requests.isEmpty()) {
            // Cancel associated request on storage
            storageClient.cancelRequests(requests.stream()
                                                 .filter(r -> r.getRemoteStepGroupIds() != null)
                                                 .flatMap(r -> r.getRemoteStepGroupIds().stream())
                                                 .collect(Collectors.toSet()));

            ingestRequestRepository.deleteAll(requests);
        }

        aipUpdateRequestRepository.deleteAll(aipUpdateRequestRepository.findAllByAipIdIn(aipIds));
    }

    @Override
    public void scheduleJob(AbstractRequest request) {
        request.setState(InternalRequestState.RUNNING);

        Set<JobParameter> jobParameters = Sets.newHashSet();
        JobInfo jobInfo;

        if (request instanceof OAISDeletionCreatorRequest) {
            // Schedule OAIS Deletion job
            jobParameters.add(new JobParameter(OAISDeletionsCreatorJob.REQUEST_ID, request.getId()));
            jobInfo = new JobInfo(false,
                                  IngestJobPriority.SESSION_DELETION_JOB_PRIORITY,
                                  jobParameters,
                                  authResolver.getUser(),
                                  OAISDeletionsCreatorJob.class.getName());
            // Lock job to avoid automatic deletion. The job must be unlock when the link to the request is removed.
        } else if (request instanceof AIPUpdatesCreatorRequest) {
            // Schedule Updates Creator job
            jobParameters.add(new JobParameter(AIPUpdatesCreatorJob.REQUEST_ID, request.getId()));
            jobInfo = new JobInfo(false,
                                  IngestJobPriority.UPDATE_AIP_SCAN_JOB_PRIORITY,
                                  jobParameters,
                                  authResolver.getUser(),
                                  AIPUpdatesCreatorJob.class.getName());
        } else {
            throw new IllegalArgumentException(String.format(
                "You should not use this method for requests having [%s] type",
                request.getDtype()));
        }

        jobInfo.setLocked(true);
        jobInfo = jobInfoService.createAsQueued(jobInfo);
        request.setJobInfo(jobInfo);
        // save request (same transaction)
        abstractRequestRepository.save(request);
    }

    @Override
    public void scheduleRequestDeletionJob(SearchRequestsParameters filters) {
        Set<JobParameter> jobParameters = Sets.newHashSet(new JobParameter(RequestDeletionJob.CRITERIA_JOB_PARAM_NAME,
                                                                           filters));
        // Schedule request deletion job
        JobInfo jobInfo = new JobInfo(false,
                                      IngestJobPriority.REQUEST_DELETION_JOB_PRIORITY,
                                      jobParameters,
                                      authResolver.getUser(),
                                      RequestDeletionJob.class.getName());
        jobInfo = jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("Schedule {} job with id {}", RequestDeletionJob.class.getName(), jobInfo.getId());
    }

    @Override
    public void scheduleRequestRetryJob(SearchRequestsParameters filters) {
        Set<JobParameter> jobParameters = Sets.newHashSet(new JobParameter(RequestRetryJob.CRITERIA_JOB_PARAM_NAME,
                                                                           filters));
        // Schedule request retry job
        JobInfo jobInfo = new JobInfo(false,
                                      IngestJobPriority.REQUEST_RETRY_JOB_PRIORITY,
                                      jobParameters,
                                      authResolver.getUser(),
                                      RequestRetryJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("Schedule {} job with id {}", RequestRetryJob.class.getName(), jobInfo.getId());
    }

    @Override
    @MultitenantTransactional(propagation = Propagation.NOT_SUPPORTED)
    @Async
    public void abortRequests(String tenant) {
        runtimeTenantResolver.forceTenant(tenant);
        SearchRequestsParameters filters = SearchRequestsParameters.build().withState(InternalRequestState.RUNNING);
        Pageable pageRequest = PageRequest.of(0, 1000, Sort.Direction.ASC, "id");
        Page<AbstractRequest> requestsPage;
        Set<UUID> jobIdsAlreadyStopped = new HashSet<>();
        do {
            requestsPage = self.abortCurrentRequestPage(filters, pageRequest, jobIdsAlreadyStopped);

        } while (requestsPage.hasNext());
        runtimeTenantResolver.clearTenant();
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public Page<AbstractRequest> abortCurrentRequestPage(SearchRequestsParameters filters,
                                                         Pageable pageRequest,
                                                         Set<UUID> jobIdsAlreadyStopped) {
        Page<AbstractRequest> requestsPage;
        requestsPage = findRequests(filters, pageRequest);
        for (AbstractRequest request : requestsPage.getContent()) {
            if (request.getJobInfo() == null) {
                request.setState(InternalRequestState.ABORTED);
            } else {
                UUID jobId = request.getJobInfo().getId();
                if (request.getJobInfo().getStatus().getStatus() != JobStatus.RUNNING) {
                    // set the state here just to be sure we are not forgetting orphan requests that has been taken
                    // into account by some jobs but state just has not been updated for some reason
                    if (request.getState() != InternalRequestState.ERROR) {
                        // ERROR being the only final state of a request, we just do not abort requests in error.
                        // it has to be done here and not on filters to respect what has been asked by API users
                        request.setState(InternalRequestState.ABORTED);
                    }
                }
                if (!jobIdsAlreadyStopped.contains(jobId)) {
                    jobInfoService.stopJob(jobId);
                    jobIdsAlreadyStopped.add(jobId);
                }
            }
        }
        return requestsPage;
    }

    @Override
    public void unblockRequests(RequestTypeEnum requestType) {
        // Build search filters
        SearchRequestsParameters searchFilters = SearchRequestsParameters.build()
                                                                         .withRequestType(requestType)
                                                                         .withState(InternalRequestState.BLOCKED);
        // Retrieve PENDING requests
        Page<AbstractRequest> pageRequests = findRequests(searchFilters, PageRequest.of(0, 500));
        List<AbstractRequest> requests = pageRequests.getContent();

        for (AbstractRequest request : requests) {
            // Rollback the state to TO_SCHEDULE
            request.setState(InternalRequestState.TO_SCHEDULE);
        }
        scheduleRequests(requests);

        // For macro job, create a job
        if (isJobRequest(requestType)) {
            for (AbstractRequest request : requests) {
                if (request.getState() == InternalRequestState.CREATED) {
                    scheduleJob(request);
                }
            }
        }
    }

    /**
     * Notify if necessary the sessionNotifier, then switch the state
     *
     * @param request
     */
    @Override
    public void switchRequestState(AbstractRequest request) {
        // Handle requests tracked by notifications
        if (request instanceof IngestRequest) {
            Optional<IngestRequest> ingReq = ingestRequestRepository.findById(request.getId());
            if (ingReq.isPresent()) {
                sessionNotifier.decrementProductRequestError(ingReq.get());
            }
        } else if (request instanceof AIPPostProcessRequest) {
            Optional<AIPPostProcessRequest> req = aipPostProcessRequestRepository.findById(request.getId());
            if (req.isPresent()) {
                sessionNotifier.decrementPostProcessError(req.get());
                sessionNotifier.incrementPostProcessPending(req.get());
            }
        }
        request.setState(InternalRequestState.TO_SCHEDULE);
        request.clearError();
    }

    @Override
    public void deleteRequests(Collection<AbstractRequest> requests) {
        for (AbstractRequest request : requests) {
            deleteRequest(request);
        }
    }

    @Override
    public void deleteRequest(AbstractRequest request) {
        // Check if the request is linked to a job
        if (isJobRequest(request)) {
            // Unlock job to allow automatic deletion
            if ((request.getJobInfo() != null) && request.getJobInfo().isLocked()) {
                JobInfo jobInfoToUnlock = request.getJobInfo();
                jobInfoToUnlock.setLocked(false);
                jobInfoService.save(jobInfoToUnlock);
            }
        }
        sessionNotifier.requestDeleted(request);
        abstractRequestRepository.delete(request);
    }

    /**
     * Schedule a list of requests and save them into repository
     * Use the history to predict the state of a request having the same type
     * You must call this method only with a list of requests having the same {@link AbstractRequest#getDtype()}
     *
     * @param requests to schedule
     */
    @Override
    public int scheduleRequests(List<AbstractRequest> requests) {
        int nbRequestScheduled = 0;
        int nbRequestBlocked = 0;
        // Store request state (can be scheduled right now ?) by session
        Table<String, String, InternalRequestState> history = HashBasedTable.create();

        for (AbstractRequest request : requests) {
            // Ignore BLOCKED request
            if (request.getState() != InternalRequestState.BLOCKED) {
                // Do not use history if the request is also a jobRequest or session values are missing
                if (!isJobRequest(request) && (request.getSessionOwner() != null) && (request.getSession() != null)) {
                    if (!history.contains(request.getSessionOwner(), request.getSession())) {
                        // Check if the request can be processed right now
                        request = scheduleRequest(request);
                        // Store if request for this session can be executed right now
                        history.put(request.getSessionOwner(), request.getSession(), request.getState());
                    }
                    InternalRequestState state = history.get(request.getSessionOwner(), request.getSession());
                    request.setState(state);
                    abstractRequestRepository.save(request);
                } else {
                    // Schedule the request
                    scheduleRequest(request);
                }
                nbRequestScheduled++;
            } else {
                nbRequestBlocked++;
                abstractRequestRepository.save(request);
            }
        }
        if (nbRequestBlocked > 0) {
            LOGGER.info("{} requests saved in BLOCKED state", nbRequestBlocked);
        }
        return nbRequestScheduled;
    }

    @Override
    public AbstractRequest scheduleRequest(AbstractRequest request) {
        boolean shouldDelayCurrentRequest = shouldDelayRequest(request);
        if (shouldDelayCurrentRequest) {
            // Block the request
            request.setState(InternalRequestState.BLOCKED);
        } else if (request.getState() == InternalRequestState.TO_SCHEDULE) {
            // If the request is accepted but was in TO_SCHEDULE, put it in CREATED
            request.setState(InternalRequestState.CREATED);
        }
        // Save to repo
        return abstractRequestRepository.save(request);
    }

    /**
     * @param request
     * @return true when the concrete {@link AbstractRequest} is run in a job
     * false when the request is processed by bulk
     */
    @Override
    public boolean isJobRequest(AbstractRequest request) {
        return (request instanceof OAISDeletionCreatorRequest) || (request instanceof AIPUpdatesCreatorRequest);
    }

    private boolean isJobRequest(RequestTypeEnum requestType) {
        return Lists.newArrayList(RequestTypeEnum.OAIS_DELETION_CREATOR, RequestTypeEnum.AIP_UPDATES_CREATOR)
                    .contains(requestType);
    }

    /**
     * Try to find some request in a ready state that can prevent the provided {@link AbstractRequest} request
     * to be executed right now
     */
    @Override
    public boolean shouldDelayRequest(AbstractRequest request) {
        Specification<AbstractRequest> spec;
        Optional<String> sessionOwnerOp = Optional.ofNullable(request.getSessionOwner());
        Optional<String> sessionOp = Optional.ofNullable(request.getSession());
        switch (request.getDtype()) {
            case RequestTypeConstant.AIP_UPDATES_CREATOR_VALUE:
                spec = AbstractRequestSpecifications.searchRequestBlockingAipUpdatesCreator(sessionOwnerOp, sessionOp);
                break;
            case RequestTypeConstant.OAIS_DELETION_CREATOR_VALUE:
                spec = AbstractRequestSpecifications.searchRequestBlockingOAISDeletionCreator(sessionOwnerOp,
                                                                                              sessionOp);
                break;
            case RequestTypeConstant.OAIS_DELETION_VALUE:
                if (((OAISDeletionRequest) request).getStep() != DeletionRequestStep.REMOTE_NOTIFICATION_ERROR) {
                    spec = AbstractRequestSpecifications.searchRequestBlockingOAISDeletion(sessionOwnerOp,
                                                                                           sessionOp,
                                                                                           ((OAISDeletionRequest) request).getAip()
                                                                                                                          .getId());
                } else {
                    // In case of notification error, aip has already been deleted so do not delay request.
                    return false;
                }
                break;
            case RequestTypeConstant.UPDATE_VALUE:
                if (((AIPUpdateRequest) request).getStep() != AIPUpdateRequestStep.REMOTE_NOTIFICATION_ERROR) {
                    spec = AbstractRequestSpecifications.searchRequestBlockingUpdate(sessionOwnerOp, sessionOp);
                } else {
                    // In case of notification error, aip has already been updated so do not delay request
                    return false;
                }
                break;
            case RequestTypeConstant.AIP_POST_PROCESS_VALUE:
                spec = AbstractRequestSpecifications.searchRequestBlockingAIPPostProcess(sessionOwnerOp, sessionOp);
                break;
            case RequestTypeConstant.INGEST_VALUE:
                // Ingest cannot be blocked
                return false;
            case RequestTypeConstant.AIP_SAVE_METADATA_VALUE:
                // Save metadata cannot be blocked
                return false;
            default:
                throw new IllegalArgumentException(String.format(
                    "You should not use this method for requests having [%s] type",
                    request.getDtype()));
        }
        return abstractRequestRepository.exists(spec);
    }

    @Override
    public List<AbstractRequest> getRequests(Set<RequestInfo> requestInfos) {
        List<String> groupIds = requestInfos.stream().map(RequestInfo::getGroupId).collect(Collectors.toList());
        List<AbstractRequest> requests = new ArrayList<>();
        // To avoid sql too long request, divide the list of groupIds to search for in subList of 100 groupIds at most.
        final int chunkSize = 100;
        if (groupIds.size() > chunkSize) {
            final AtomicInteger counter = new AtomicInteger();
            groupIds.stream()
                    .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
                    .values()
                    .forEach(list -> {
                        requests.addAll(findRequestsByGroupIdIn(list));
                    });
        } else {
            requests.addAll(findRequestsByGroupIdIn(groupIds));
        }
        return requests;
    }

}
