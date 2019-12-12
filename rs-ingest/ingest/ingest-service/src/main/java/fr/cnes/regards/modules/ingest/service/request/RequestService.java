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
package fr.cnes.regards.modules.ingest.service.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.ingest.dao.AbstractRequestSpecifications;
import fr.cnes.regards.modules.ingest.dao.IAIPStoreMetaDataRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.mapper.IRequestMapper;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdatesCreatorRequest;
import fr.cnes.regards.modules.ingest.dto.request.RequestDto;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import fr.cnes.regards.modules.ingest.service.job.AIPUpdatesCreatorJob;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import fr.cnes.regards.modules.ingest.service.job.OAISDeletionsCreatorJob;
import fr.cnes.regards.modules.ingest.service.job.RequestDeletionJob;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import fr.cnes.regards.modules.storage.client.RequestInfo;

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

    /**
     * Key used by macro request to store the blocking state result of that request type
     */
    private static final String GLOBAL_REQUEST_SESSION = "____GLOBAL_REQUEST_SESSION____";

    @Autowired
    @Lazy
    private IIngestRequestService ingestRequestService;

    @Autowired
    private IAIPStoreMetaDataRequestService aipSaveMetaDataService;

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IAIPStoreMetaDataRepository aipStoreMetaDataRepository;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private IRequestMapper requestMapper;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Override
    public void handleRemoteStoreError(Set<RequestInfo> requestInfos) {
        List<AbstractRequest> requests = findRequestsByGroupIdIn(requestInfos.stream().map(RequestInfo::getGroupId)
                .collect(Collectors.toList()));
        for (RequestInfo ri : requestInfos) {
            for (AbstractRequest request : requests) {
                if (request.getRemoteStepGroupIds().contains(ri.getGroupId())) {
                    if (request instanceof IngestRequest) {
                        ingestRequestService.handleRemoteStoreError((IngestRequest) request, ri);
                    } else if (request instanceof AIPStoreMetaDataRequest) {
                        aipSaveMetaDataService.handleError((AIPStoreMetaDataRequest) request, ri);
                    }
                }
            }
        }
    }

    @Override
    public void handleRemoteStoreSuccess(Set<RequestInfo> requestInfos) {
        List<AbstractRequest> requests = findRequestsByGroupIdIn(requestInfos.stream().map(RequestInfo::getGroupId)
                .collect(Collectors.toList()));
        for (RequestInfo ri : requestInfos) {
            for (AbstractRequest request : requests) {
                if (request.getRemoteStepGroupIds().contains(ri.getGroupId())) {
                    if (request instanceof IngestRequest) {
                        ingestRequestService.handleRemoteStoreSuccess((IngestRequest) (request), ri);
                    } else if (request instanceof AIPStoreMetaDataRequest) {
                        aipSaveMetaDataService.handleSuccess((AIPStoreMetaDataRequest) request, ri);
                    }
                }
            }
        }
    }

    @Override
    public void handleRemoteRequestGranted(Set<RequestInfo> requests) {
        // Do not track at the moment : the ongoing request could send a success too quickly
        // and could cause unnecessary concurrent access to thehandleRemoteRequestGranted database!
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
        requests.forEach(sessionNotifier::ingestRequestErrorDeleted);
        ingestRequestRepository.deleteAll(requests);

        List<AIPStoreMetaDataRequest> storeMetaRequests = aipStoreMetaDataRepository.findAllByAipIdIn(aipIds);
        storeMetaRequests.forEach(sessionNotifier::aipStoreMetaRequestErrorDeleted);
        aipStoreMetaDataRepository.deleteAll(storeMetaRequests);

        List<AIPUpdateRequest> updateRequests = aipUpdateRequestRepository.findAllByAipIdIn(aipIds);
        aipUpdateRequestRepository.deleteAll(updateRequests);
    }

    @Override
    public void relaunchRequests(SearchRequestsParameters filters) {
        //TODO
    }

    @Override
    public void scheduleRequests(List<AbstractRequest> requests) {
        // Store request state (can be scheduled right now ?) by session
        Table<String, String, InternalRequestState> stateBySession = HashBasedTable.create();

        for (AbstractRequest request : requests) {
            // Ignore BLOCKED requests
            if (request.getState() != InternalRequestState.BLOCKED) {
                String sessionOwner = Optional.ofNullable(request.getSessionOwner()).orElse(GLOBAL_REQUEST_SESSION);
                String session = Optional.ofNullable(request.getSession()).orElse(GLOBAL_REQUEST_SESSION);

                // Compute the session state if not already available
                if (!stateBySession.contains(sessionOwner, session)) {
                    // Check if the request can be processed right now
                    request = scheduleRequest(request);

                    // Store if request for this session can be executed right now
                    stateBySession.put(sessionOwner, session, request.getState());
                } else {
                    InternalRequestState state = stateBySession.get(sessionOwner, session);
                    request.setState(state);
                    abstractRequestRepository.save(request);
                }
            } else {
                // Just save the request as BLOCKED
                abstractRequestRepository.save(request);
            }

        }
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

    @Override
    public void scheduleJob(AbstractRequest request) {
        request.setState(InternalRequestState.RUNNING);

        Set<JobParameter> jobParameters = Sets.newHashSet();
        JobInfo jobInfo;

        if (request instanceof OAISDeletionCreatorRequest) {
            // Schedule OAIS Deletion job
            jobParameters.add(new JobParameter(OAISDeletionsCreatorJob.REQUEST_ID, request.getId()));
            jobInfo = new JobInfo(false, IngestJobPriority.SESSION_DELETION_JOB_PRIORITY.getPriority(), jobParameters,
                    authResolver.getUser(), OAISDeletionsCreatorJob.class.getName());
            // Lock job to avoid automatic deletion. The job must be unlock when the link to the request is removed.
        } else if (request instanceof AIPUpdatesCreatorRequest) {
            // Schedule Updates Creator job
            jobParameters.add(new JobParameter(AIPUpdatesCreatorJob.REQUEST_ID, request.getId()));
            jobInfo = new JobInfo(false, IngestJobPriority.UPDATE_AIP_SCAN_JOB_PRIORITY.getPriority(), jobParameters,
                    authResolver.getUser(), AIPUpdatesCreatorJob.class.getName());
        } else {
            throw new IllegalArgumentException(
                    String.format("You should not use this method for requests having [%s] type", request.getDtype()));
        }

        jobInfo.setLocked(true);
        jobInfoService.createAsQueued(jobInfo);
        request.setJobInfo(jobInfo);
        // save request (same transaction)
        abstractRequestRepository.save(request);
    }

    @Override
    public void unblockRequests(RequestTypeEnum requestType) {
        // Build search filters
        SearchRequestsParameters searchFilters = SearchRequestsParameters.build().withRequestType(requestType)
                .withState(InternalRequestState.BLOCKED);
        // Retrieve PENDING requests
        Page<AbstractRequest> pageRequests = findRequests(searchFilters, PageRequest.of(0, 500));

        // Store request state (can be scheduled right now ?) by session
        Table<String, String, InternalRequestState> stateBySession = HashBasedTable.create();

        List<AbstractRequest> requests = pageRequests.getContent();
        for (AbstractRequest request : requests) {
            String sessionOwner = Optional.ofNullable(request.getSessionOwner()).orElse(GLOBAL_REQUEST_SESSION);
            String session = Optional.ofNullable(request.getSession()).orElse(GLOBAL_REQUEST_SESSION);

            // Compute the session state if not already available
            if (!stateBySession.contains(sessionOwner, session)) {
                // Check if the request can be processed right now
                boolean shouldDelayRequest = shouldDelayRequest(request);
                InternalRequestState state = shouldDelayRequest ? InternalRequestState.BLOCKED
                        : InternalRequestState.CREATED;
                // Store if request for this session can be executed right now
                stateBySession.put(sessionOwner, session, state);
            }
            InternalRequestState state = stateBySession.get(sessionOwner, session);
            request.setState(state);
            abstractRequestRepository.save(request);
        }

        // For macro job, create a job
        if ((requestType == RequestTypeEnum.AIP_UPDATES_CREATOR) || (requestType == RequestTypeEnum.OAIS_DELETION)) {
            for (AbstractRequest request : requests) {
                if (request.getState() == InternalRequestState.CREATED) {
                    scheduleJob(request);
                }
            }
        }
    }

    @Override
    public void cleanRequestJob(AbstractRequest request) {
        if ((request instanceof OAISDeletionCreatorRequest) || (request instanceof AIPUpdatesCreatorRequest)) {
            // Unlock job to allow automatic deletion
            if ((request.getJobInfo() != null) && request.getJobInfo().isLocked()) {
                JobInfo jobInfoToUnlock = request.getJobInfo();
                jobInfoToUnlock.setLocked(false);
                jobInfoService.save(jobInfoToUnlock);
            }
            abstractRequestRepository.delete(request);
            return;
        }
        throw new IllegalArgumentException(
                String.format("You should not use this method for requests having [%s] type", request.getDtype()));
    }

    @Override
    public void registerRequestDeletion(SearchRequestsParameters filters) {
        Set<JobParameter> jobParameters = Sets.newHashSet(new JobParameter(RequestDeletionJob.CRITERIA, filters));
        // Schedule OAIS Deletion job
        JobInfo jobInfo = new JobInfo(false, IngestJobPriority.REQUEST_DELETION_JOB_PRIORITY.getPriority(),
                jobParameters, authResolver.getUser(), RequestDeletionJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
    }

    @Override
    public void registerRequestRetry(SearchRequestsParameters filters) {

    }

    @Override
    public void deleteRequest(AbstractRequest request) {
        if ((request instanceof OAISDeletionCreatorRequest) || (request instanceof AIPUpdatesCreatorRequest)) {
            cleanRequestJob(request);
        } else {
            abstractRequestRepository.delete(request);
        }
    }

    private boolean shouldDelayRequest(AbstractRequest request) {
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
                spec = AbstractRequestSpecifications.searchRequestBlockingOAISDeletion(sessionOwnerOp, sessionOp);
                break;
            case RequestTypeConstant.STORE_METADATA_VALUE:
                spec = AbstractRequestSpecifications.searchRequestBlockingStoreMeta(sessionOwnerOp, sessionOp);
                break;
            case RequestTypeConstant.UPDATE_VALUE:
                spec = AbstractRequestSpecifications.searchRequestBlockingUpdate(sessionOwnerOp, sessionOp);
                break;
            case RequestTypeConstant.INGEST_VALUE:
                // Ingest cannot be blocked, don't call this method to save that kind of request
            default:
                throw new IllegalArgumentException(String
                        .format("You should not use this method for requests having [%s] type", request.getDtype()));
        }
        return abstractRequestRepository.exists(spec);
    }

}
