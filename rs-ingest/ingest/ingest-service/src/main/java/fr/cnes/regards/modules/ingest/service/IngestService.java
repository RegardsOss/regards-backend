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
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.ingest.dao.ISessionDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.dto.RequestInfoDto;
import fr.cnes.regards.modules.ingest.domain.dto.RequestType;
import fr.cnes.regards.modules.ingest.domain.mapper.IIngestMetadataMapper;
import fr.cnes.regards.modules.ingest.domain.mapper.ISessionDeletionRequestMapper;
import fr.cnes.regards.modules.ingest.domain.request.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.SessionDeletionRequest;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionRequestDto;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import fr.cnes.regards.modules.ingest.service.job.SessionDeletionJob;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import fr.cnes.regards.modules.ingest.service.request.IngestRequestPublisher;

/**
 * Ingest management service
 *
 * @author Marc Sordi
 *
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
    private Gson gson;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IngestRequestPublisher requestPublisher;

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
    public Collection<IngestRequest> registerIngestRequests(Collection<IngestRequestFlowItem> items) {
        Collection<IngestRequest> requests = new ArrayList<>();
        for (IngestRequestFlowItem item : items) {
            IngestRequest request = registerIngestRequest(item);
            if (request != null) {
                requests.add(request);
            }
        }
        return requests;
    }

    @Override
    public Collection<IngestRequest> registerAndScheduleIngestRequests(Collection<IngestRequestFlowItem> items) {

        // Register requests
        Collection<IngestRequest> requests = registerIngestRequests(items);

        // Dispatch per chain
        ListMultimap<String, IngestRequest> requestPerChain = ArrayListMultimap.create();
        requests.stream().forEach(r -> requestPerChain.put(r.getMetadata().getIngestChain(), r));

        // Schedule job per chain
        for (String chainName : requestPerChain.keySet()) {
            ingestRequestService.scheduleIngestProcessingJobByChain(chainName, requestPerChain.get(chainName));
        }

        return requests;
    }

    private IngestRequest registerIngestRequest(IngestRequestFlowItem item) {

        // Validate all elements of the flow item
        Errors errors = new MapBindingResult(new HashMap<>(), IngestRequestFlowItem.class.getName());
        validator.validate(item, errors);
        if (errors.hasErrors()) {
            Set<String> errs = buildErrors(errors);
            requestPublisher.publishIngestRequest(IngestRequest.build(item.getRequestId(),
                                                                      metadataMapper.dtoToMetadata(item.getMetadata()),
                                                                      RequestState.DENIED, item.getSip(), errs));
            if (LOGGER.isDebugEnabled()) {
                StringJoiner joiner = new StringJoiner(", ");
                errs.forEach(err -> joiner.add(err));
                LOGGER.debug("Ingest request {} rejected for following reason(s) : {}", item.getRequestId(),
                             joiner.toString());
            }
            // Do not save denied request
            return null;
        }

        // Save granted ingest request
        IngestRequest request = IngestRequest.build(item.getRequestId(),
                                                    metadataMapper.dtoToMetadata(item.getMetadata()),
                                                    RequestState.GRANTED, item.getSip());
        ingestRequestService.save(request);
        requestPublisher.publishIngestRequest(request);
        return request;
    }

    /**
     * Build a set of error string from {@link Errors}
     */
    private Set<String> buildErrors(Errors errors) {
        if (errors.hasErrors()) {
            Set<String> err = new HashSet<>();
            errors.getAllErrors().forEach(error -> {
                if (error instanceof FieldError) {
                    FieldError fieldError = (FieldError) error;
                    err.add(String.format("%s at %s: rejected value [%s].", fieldError.getDefaultMessage(),
                                          fieldError.getField(),
                                          ObjectUtils.nullSafeToString(fieldError.getRejectedValue())));
                } else {
                    err.add(error.getDefaultMessage());
                }
            });
            return err;
        } else {
            throw new IllegalArgumentException("This method must be called only if at least one error exists");
        }
    }

    @Override
    public RequestInfoDto redirectToDataflow(SIPCollection sips) {
        IngestMetadataDto metadata = sips.getMetadata();
        RequestInfoDto info = RequestInfoDto.build(RequestType.INGEST,
                                                   "SIP Collection ingestion request redirected to dataflow");
        for (SIP sip : sips.getFeatures()) {
            IngestRequestFlowItem item = IngestRequestFlowItem.build(metadata, sip);
            info.addRequestMapping(sip.getId(), item.getRequestId());
            publisher.publish(item);
        }
        return info;
    }

    @Override
    public RequestInfoDto redirectToDataflow(InputStream input) throws ModuleException {
        try (Reader json = new InputStreamReader(input, DEFAULT_CHARSET)) {
            SIPCollection sips = gson.fromJson(json, SIPCollection.class);
            return redirectToDataflow(sips);
        } catch (JsonIOException | IOException e) {
            LOGGER.error("Cannot read JSON file containing SIP collection", e);
            throw new EntityInvalidException(e.getMessage(), e);
        }
    }

    @Override
    public SessionDeletionRequestDto registerSessionDeletionRequest(SessionDeletionRequestDto request) {

        SessionDeletionRequest deletionRequest = deletionRequestMapper.dtoToEntity(request);

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
        deletionRequest.setState(RequestState.PENDING);
        deletionRequestRepository.save(deletionRequest);

        return deletionRequestMapper.entityToDto(deletionRequest);
    }
}
