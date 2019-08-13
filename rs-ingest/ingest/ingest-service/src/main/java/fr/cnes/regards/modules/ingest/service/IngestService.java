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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.dao.IDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.dto.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.domain.dto.RequestInfoDto;
import fr.cnes.regards.modules.ingest.domain.dto.RequestType;
import fr.cnes.regards.modules.ingest.domain.dto.flow.DeletionRequestFlowItem;
import fr.cnes.regards.modules.ingest.domain.dto.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.request.DeletionRequest;
import fr.cnes.regards.modules.ingest.domain.entity.request.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.entity.request.RequestState;
import fr.cnes.regards.modules.ingest.domain.mapper.IIngestMetadataMapper;
import fr.cnes.regards.modules.ingest.service.request.IngestRequestPublisher;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;

/**
 * Ingest management service
 *
 * @author Marc Sordi
 *
 */
@Service
@MultitenantTransactional
public class IngestService implements IIngestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestService.class);

    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    @Autowired
    private Gson gson;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IngestRequestPublisher requestPublisher;

    @Autowired
    private IIngestMetadataMapper metadataMapper;

    @Autowired
    private Validator validator;

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IDeletionRequestRepository deletionRequestRepository;

    @Autowired
    private ISIPService sipService;

    @Override
    public void registerIngestRequests(Collection<IngestRequestFlowItem> items) {
        items.forEach(i -> registerIngestRequest(i));
    }

    private void registerIngestRequest(IngestRequestFlowItem item) {

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
            return;
        }

        // Save granted ingest request
        IngestRequest request = IngestRequest.build(item.getRequestId(),
                                                    metadataMapper.dtoToMetadata(item.getMetadata()),
                                                    RequestState.GRANTED, item.getSip());
        ingestRequestRepository.save(request);
        requestPublisher.publishIngestRequest(request);
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
        RequestInfoDto info = RequestInfoDto
                .build(RequestType.INGEST, "SIP Collection ingestion request redirected to dataflow");
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
    public void registerDeletionRequests(Collection<DeletionRequestFlowItem> items) {
        items.forEach(i -> registerDeletionRequest(i));
    }

    private void registerDeletionRequest(DeletionRequestFlowItem item) {

        // Validate all elements of the flow item
        Errors errors = new MapBindingResult(new HashMap<>(), DeletionRequestFlowItem.class.getName());
        validator.validate(item, errors);
        if (errors.hasErrors()) {
            Set<String> errs = buildErrors(errors);
            requestPublisher.publishDeletionRequest(DeletionRequest
                    .build(item.getRequestId(), RequestState.DENIED, item.getSipId(), errs));
            if (LOGGER.isDebugEnabled()) {
                StringJoiner joiner = new StringJoiner(", ");
                errs.forEach(err -> joiner.add(err));
                LOGGER.debug("Deletion request {} rejected for following reason(s) : {}", item.getRequestId(),
                             joiner.toString());
            }
            // Do not save denied request
            return;
        }

        // Save granted deletion request
        DeletionRequest request = DeletionRequest.build(item.getRequestId(), RequestState.GRANTED,
                                                        item.getSipId());
        deletionRequestRepository.save(request);
        requestPublisher.publishDeletionRequest(request);
    }

    @Override
    public RequestInfoDto deleteByProviderId(String providerId) {
        Collection<SIPEntity> entities = sipService.findAllByProviderId(providerId);
        RequestInfoDto info = RequestInfoDto
                .build(RequestType.DELETION, "SIP deletion by provider id request redirected to dataflow");
        for (SIPEntity entity : entities) {
            DeletionRequestFlowItem item = DeletionRequestFlowItem.build(entity.getSipId());
            info.addRequestMapping(entity.getSipId(), item.getRequestId());
            publisher.publish(item);
        }
        return info;
    }

    @Override
    public RequestInfoDto deleteBySipId(String sipId) {
        RequestInfoDto info = RequestInfoDto.build(RequestType.DELETION,
                                                               "SIP deletion by sip id request redirected to dataflow");
        DeletionRequestFlowItem item = DeletionRequestFlowItem.build(sipId);
        info.addRequestMapping(sipId, item.getRequestId());
        publisher.publish(item);
        return info;
    }

    // FIXME retry with request!
    //    @Override
    //    public SIPDto retryIngest(UniformResourceName sipId) throws ModuleException {
    //        Optional<SIPEntity> oSip = sipRepository.findOneBySipId(sipId.toString());
    //        if (oSip.isPresent()) {
    //            SIPEntity sip = oSip.get();
    //            switch (sip.getState()) {
    //                case ERROR:
    //                    // Notify the SIP status changes
    //                    sipService.notifySipChangedState(sip.getIngestMetadata(), sip.getState(), SIPState.CREATED);
    //                    sipRepository.updateSIPEntityState(SIPState.CREATED, sip.getId());
    //                    break;
    //                case INGESTED:
    //                    throw new EntityOperationForbiddenException(sipId.toString(), SIPEntity.class,
    //                            "SIP ingest process is already successully done");
    //                case REJECTED:
    //                    throw new EntityOperationForbiddenException(sipId.toString(), SIPEntity.class,
    //                            "SIP format is not valid");
    //                case CREATED:
    //                case QUEUED:
    //                case TO_BE_DELETED:
    //                case DELETED:
    //                    throw new EntityOperationForbiddenException(sipId.toString(), SIPEntity.class,
    //                            "SIP ingest is already running");
    //                default:
    //                    throw new EntityOperationForbiddenException(sipId.toString(), SIPEntity.class,
    //                            "SIP is in undefined state for ingest retry");
    //            }
    //            return sip.toDto();
    //        } else {
    //            throw new EntityNotFoundException(sipId.toString(), SIPEntity.class);
    //        }
    //    }
    //
    //    @Override
    //    public Boolean isRetryable(UniformResourceName sipId) throws EntityNotFoundException {
    //        Optional<SIPEntity> os = sipRepository.findOneBySipId(sipId.toString());
    //        if (os.isPresent()) {
    //            switch (os.get().getState()) {
    //                case ERROR:
    //                    return true;
    //                default:
    //                    return false;
    //            }
    //        } else {
    //            throw new EntityNotFoundException(sipId.toString(), SIPEntity.class);
    //        }
    //    }
}
