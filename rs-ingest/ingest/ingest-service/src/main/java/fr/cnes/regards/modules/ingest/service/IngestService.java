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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.modules.ingest.domain.dto.RequestInfoDto;
import fr.cnes.regards.modules.ingest.domain.mapper.IIngestMetadataMapper;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.conf.IngestConfigurationProperties;
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
    private IIngestMetadataMapper metadataMapper;

    @Autowired
    private Validator validator;

    @Autowired
    private IIngestRequestService ingestRequestService;

    /**
     * Validate, save and publish a new request
     * @param item request to manage
     */
    private IngestRequest registerIngestRequest(IngestRequestFlowItem item, InternalRequestState state) {

        // Validate all elements of the flow item
        Errors errors = new MapBindingResult(new HashMap<>(), IngestRequestFlowItem.class.getName());
        validator.validate(item, errors);
        if (errors.hasErrors()) {
            Set<String> errs = ErrorTranslator.getErrors(errors);
            // Publish DENIED request (do not persist it in DB)
            ingestRequestService.handleRequestDenied(IngestRequest
                    .build(item.getRequestId(), metadataMapper.dtoToMetadata(item.getMetadata()),
                           InternalRequestState.ERROR, IngestRequestStep.LOCAL_DENIED, null, errs));
            if (LOGGER.isDebugEnabled()) {
                StringJoiner joiner = new StringJoiner(", ");
                errs.forEach(err -> joiner.add(err));
                LOGGER.debug("Ingest request {} rejected for following reason(s) : {}", item.getRequestId(),
                             joiner.toString());
            }
            return null;
        }

        // Save granted ingest request
        IngestRequest request = IngestRequest.build(item.getRequestId(),
                                                    metadataMapper.dtoToMetadata(item.getMetadata()), state,
                                                    IngestRequestStep.LOCAL_SCHEDULED, item.getSip());
        ingestRequestService.handleRequestGranted(request);
        // return granted request
        return request;
    }

    @Override
    public void handleIngestRequests(Collection<IngestRequestFlowItem> items) {
        // Store requests per chain
        ListMultimap<String, IngestRequest> requestPerChain = ArrayListMultimap.create();
        for (IngestRequestFlowItem item : items) {
            // Validate and transform to request
            IngestRequest ingestRequest = registerIngestRequest(item, InternalRequestState.RUNNING);
            if (ingestRequest != null) {
                requestPerChain.put(ingestRequest.getMetadata().getIngestChain(), ingestRequest);
            }
        }
        // Schedule job per chain
        for (String chainName : requestPerChain.keySet()) {
            ingestRequestService.scheduleIngestProcessingJobByChain(chainName, requestPerChain.get(chainName));
        }
    }

    @Override
    public RequestInfoDto handleSIPCollection(SIPCollection sips) throws EntityInvalidException {

        // Check submission limit / If there are more features than configurated bulk max size, reject request!
        if (sips.getFeatures().size() > confProperties.getMaxBulkSize()) {
            throw new EntityInvalidException(
                    String.format("Invalid request due to ingest configuration max bulk size set to %s.",
                                  confProperties.getMaxBulkSize()));
        }

        // Validate and transform ingest metadata
        IngestMetadata ingestMetadata = getIngestMetadata(sips.getMetadata());

        // Register requests
        Collection<IngestRequest> grantedRequests = new ArrayList<>();
        RequestInfoDto info = RequestInfoDto.build(ingestMetadata.getSessionOwner(), ingestMetadata.getSession(),
                                                   "SIP Collection ingestion scheduled");

        int count = 1;
        for (SIP sip : sips.getFeatures()) {
            String sipId = sip.getId() != null ? sip.getId() : "SIP n°" + count;
            // Validate and transform to request
            registerIngestRequest(sip, ingestMetadata, info, grantedRequests, sipId);
            count++;
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
            Collection<IngestRequest> grantedRequests, String sipId) {
        // Validate SIP
        Errors errors = new MapBindingResult(new HashMap<>(), SIP.class.getName());
        validator.validate(sip, errors);
        if (errors.hasErrors()) {
            Set<String> errs = ErrorTranslator.getErrors(errors);
            // Publish DENIED request (do not persist it in DB) / Warning : request id cannot be known
            ingestRequestService.handleRequestDenied(IngestRequest.build(ingestMetadata, InternalRequestState.ERROR,
                                                                         IngestRequestStep.LOCAL_DENIED, sip, errs));
            StringJoiner joiner = new StringJoiner(", ");
            errs.forEach(err -> joiner.add(err));
            LOGGER.debug("SIP ingestion request rejected for following reason(s) : {}", joiner.toString());
            // Trace denied request
            info.addDeniedRequest(sipId, joiner.toString());

            return;
        }

        // Save granted ingest request
        IngestRequest request = IngestRequest.build(ingestMetadata, InternalRequestState.CREATED,
                                                    IngestRequestStep.LOCAL_SCHEDULED, sip);
        ingestRequestService.handleRequestGranted(request);
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
}
