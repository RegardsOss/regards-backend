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
package fr.cnes.regards.modules.ingest.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.ingest.dao.ISessionDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.dto.RequestInfoDto;
import fr.cnes.regards.modules.ingest.domain.mapper.IIngestMetadataMapper;
import fr.cnes.regards.modules.ingest.domain.mapper.ISessionDeletionRequestMapper;
import fr.cnes.regards.modules.ingest.domain.request.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.SessionDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionRequestDto;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.conf.IngestConfigurationProperties;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import fr.cnes.regards.modules.ingest.service.job.SessionDeletionJob;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;

/**
 * Ingest management service
 *
 * @author Marc Sordi
 *
 * TODO : retry ingestion
 * TODO : retry deletion?
 */
@Service
@MultitenantTransactional
public class IngestService implements IIngestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestService.class);

    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    @Autowired
    private IngestConfigurationProperties confProperties;

    @Autowired
    private Gson gson;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IIngestMetadataMapper metadataMapper;

    @Autowired
    private ISessionDeletionRequestMapper deletionRequestMapper;

    @Autowired
    private Validator validator;

    @Autowired
    private IIngestRequestService ingestRequestService;

    @Autowired
    private ISessionDeletionRequestRepository deletionRequestRepository;

    @Override
    public Collection<IngestRequest> handleIngestRequests(Collection<IngestRequestFlowItem> items) {

        // Register requests
        Collection<IngestRequest> grantedRequests = new ArrayList<>();
        for (IngestRequestFlowItem item : items) {
            // Validate and transform to request
            registerIngestRequest(item, grantedRequests);
        }

        // Dispatch per chain
        ListMultimap<String, IngestRequest> requestPerChain = ArrayListMultimap.create();
        grantedRequests.stream().forEach(r -> requestPerChain.put(r.getMetadata().getIngestChain(), r));

        // Schedule job per chain
        for (String chainName : requestPerChain.keySet()) {
            ingestRequestService.scheduleIngestProcessingJobByChain(chainName, requestPerChain.get(chainName));
        }

        return grantedRequests;
    }

    /**
     * Validate, save and publish a new request
     * @param item request to manage
     * @param grantedRequests collection of granted requests to populate
     */
    private void registerIngestRequest(IngestRequestFlowItem item, Collection<IngestRequest> grantedRequests) {

        // Validate all elements of the flow item
        Errors errors = new MapBindingResult(new HashMap<>(), IngestRequestFlowItem.class.getName());
        validator.validate(item, errors);
        if (errors.hasErrors()) {
            Set<String> errs = ErrorTranslator.getErrors(errors);
            // Publish DENIED request (do not persist it in DB)
            ingestRequestService.handleDeniedRequest(IngestRequest
                    .build(item.getRequestId(), metadataMapper.dtoToMetadata(item.getMetadata()), RequestState.DENIED,
                           null, errs));
            if (LOGGER.isDebugEnabled()) {
                StringJoiner joiner = new StringJoiner(", ");
                errs.forEach(err -> joiner.add(err));
                LOGGER.debug("Ingest request {} rejected for following reason(s) : {}", item.getRequestId(),
                             joiner.toString());
            }
            return;
        }

        // Save granted ingest request
        IngestRequest request = IngestRequest.build(item.getRequestId(),
                                                    metadataMapper.dtoToMetadata(item.getMetadata()),
                                                    RequestState.GRANTED, item.getSip());
        ingestRequestService.handleGrantedRequest(request);
        // Add to granted request collection
        grantedRequests.add(request);
    }

    //    @Override
    //    public RequestInfoDto redirectToDataflow(SIPCollection sips) {
    //        RequestInfoDto info = RequestInfoDto.build(RequestType.INGEST,
    //                                                   "SIP Collection ingestion request redirected to dataflow");
    //        for (IngestRequestFlowItem item : sipToFlow(sips)) {
    //            info.addRequestMapping(item.getSip().getId(), item.getRequestId());
    //            publisher.publish(item);
    //        }
    //        return info;
    //    }

    @Override
    public RequestInfoDto handleSIPCollection(SIPCollection sips) throws EntityInvalidException {

        // Check submission limit / If there are more features than configurated bulk max size, reject request!
        if (sips.getFeatures().size() > confProperties.getMaxBulkSize()) {
            throw new EntityInvalidException(
                    String.format("Invalid request due to ingest configuration max bulk size set to %s."));
        }

        // Validate and transform ingest metadata
        IngestMetadata ingestMetadata = getIngestMetadata(sips.getMetadata());

        // Register requests
        Collection<IngestRequest> grantedRequests = new ArrayList<>();
        RequestInfoDto info = RequestInfoDto.build("SIP Collection ingestion scheduled");

        for (SIP sip : sips.getFeatures()) {
            // Validate and transform to request
            registerIngestRequest(sip, ingestMetadata, info, grantedRequests);
        }

        ingestRequestService.scheduleIngestProcessingJobByChain(ingestMetadata.getIngestChain(), grantedRequests);

        return info;
    }

    /**
     * Validate and transform ingest metadata
     */
    private IngestMetadata getIngestMetadata(IngestMetadataDto dto) throws EntityInvalidException {
        // Validate metadata
        Errors errors = new MapBindingResult(new HashMap<>(), IngestMetadataDto.class.getName());
        validator.validate(dto, errors);
        if (errors.hasErrors()) {
            Set<String> errs = ErrorTranslator.getErrors(errors);
            if (LOGGER.isDebugEnabled()) {
                StringJoiner joiner = new StringJoiner(", ");
                errs.forEach(err -> joiner.add(err));
                LOGGER.debug("SIP collection submission rejected due to invalid ingest metadata : {}",
                             joiner.toString());
            }
            // Throw invalid exception
            throw new EntityInvalidException(new ArrayList<>(errs));
        }

        return metadataMapper.dtoToMetadata(dto);
    }

    /**
     * Validate, save and publish a new request
     * @param sip sip to manage
     * @param ingestMetadata related ingest metadata
     * @param info synchronous feedback
     * @param grantedRequests collection of granted requests to populate
     */
    private void registerIngestRequest(SIP sip, IngestMetadata ingestMetadata, RequestInfoDto info,
            Collection<IngestRequest> grantedRequests) {

        // Validate SIP
        Errors errors = new MapBindingResult(new HashMap<>(), SIP.class.getName());
        validator.validate(sip, errors);
        if (errors.hasErrors()) {
            Set<String> errs = ErrorTranslator.getErrors(errors);
            // Publish DENIED request (do not persist it in DB) / Warning : request id cannot be known
            ingestRequestService
                    .handleDeniedRequest(IngestRequest.build(ingestMetadata, RequestState.DENIED, sip, errs));

            StringJoiner joiner = new StringJoiner(", ");
            errs.forEach(err -> joiner.add(err));
            LOGGER.debug("SIP ingestion request rejected for following reason(s) : {}", joiner.toString());
            // Trace denied request
            info.addDeniedRequest(sip.getId(), joiner.toString());
            return;
        }

        // Save granted ingest request
        IngestRequest request = IngestRequest.build(ingestMetadata, RequestState.GRANTED, sip);
        ingestRequestService.handleGrantedRequest(request);
        // Trace granted request
        info.addGrantedRequest(sip.getId(), request.getRequestId());
        // Add to granted request collection
        grantedRequests.add(request);
    }

    /**
     * Middleware method extracted for test simulation and also used by operational code.
     * Transform a SIP collection to a SIP flow item collection
     */
    public static Collection<IngestRequestFlowItem> sipToFlow(SIPCollection sips) {
        Collection<IngestRequestFlowItem> items = new ArrayList<>();
        if (sips != null) {
            IngestMetadataDto metadata = sips.getMetadata();
            for (SIP sip : sips.getFeatures()) {
                items.add(IngestRequestFlowItem.build(metadata, sip));
            }
        }
        return items;
    }

    //    @Override
    //    public RequestInfoDto redirectToDataflow(InputStream input) throws ModuleException {
    //        try (Reader json = new InputStreamReader(input, DEFAULT_CHARSET)) {
    //            SIPCollection sips = gson.fromJson(json, SIPCollection.class);
    //            return redirectToDataflow(sips);
    //        } catch (JsonIOException | IOException e) {
    //            LOGGER.error("Cannot read JSON file containing SIP collection", e);
    //            throw new EntityInvalidException(e.getMessage(), e);
    //        }
    //    }

    @Override
    public RequestInfoDto handleSIPCollection(InputStream input) throws ModuleException {
        try (Reader json = new InputStreamReader(input, DEFAULT_CHARSET)) {
            SIPCollection sips = gson.fromJson(json, SIPCollection.class);
            return handleSIPCollection(sips);
        } catch (JsonIOException | IOException e) {
            LOGGER.error("Cannot read JSON file containing SIP collection", e);
            throw new EntityInvalidException(e.getMessage(), e);
        }
    }

    @Override
    public SessionDeletionRequestDto registerSessionDeletionRequest(SessionDeletionRequestDto request) {
        SessionDeletionRequest deletionRequest = deletionRequestMapper.dtoToEntity(request);

        // TODO check if we can accept this request now

        // Save granted deletion request
        deletionRequest.setRequestId(UUID.randomUUID().toString());
        deletionRequest.setState(RequestState.GRANTED);
        deletionRequestRepository.save(deletionRequest);

        // Schedule deletion job
        Set<JobParameter> jobParameters = Sets.newHashSet();
        jobParameters.add(new JobParameter(SessionDeletionJob.ID, deletionRequest.getId()));
        JobInfo jobInfo = new JobInfo(false, IngestJobPriority.SESSION_DELETION_JOB_PRIORITY.getPriority(),
                jobParameters, authResolver.getUser(), SessionDeletionJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);

        // Switch request status (same transaction)
        deletionRequest.setState(RequestState.GRANTED);
        deletionRequestRepository.save(deletionRequest);

        return deletionRequestMapper.entityToDto(deletionRequest);
    }
}
