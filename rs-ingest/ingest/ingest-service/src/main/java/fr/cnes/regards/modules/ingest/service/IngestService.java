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
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.dto.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.domain.dto.IngestRequestInfoDto;
import fr.cnes.regards.modules.ingest.domain.dto.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.domain.entity.request.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.entity.request.IngestRequestState;
import fr.cnes.regards.modules.ingest.domain.event.IngestRequestEvent;
import fr.cnes.regards.modules.ingest.domain.event.IngestRequestType;
import fr.cnes.regards.modules.ingest.domain.mapper.IIngestMetadataMapper;

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
    private IIngestMetadataMapper metadataMapper;

    @Autowired
    private Validator validator;

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

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
            publisher.publish(IngestRequestEvent.build(item.getRequestId(),
                                                       item.getSip() != null ? item.getSip().getId() : null, null,
                                                       IngestRequestState.DENIED, IngestRequestType.INGEST, errs));
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
                                                    IngestRequestState.GRANTED, item.getSip());
        ingestRequestRepository.save(request);

        publisher.publish(IngestRequestEvent.build(item.getRequestId(),
                                                   item.getSip() != null ? item.getSip().getId() : null, null,
                                                   request.getState(), IngestRequestType.INGEST, null));
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
    public IngestRequestInfoDto redirectToDataflow(SIPCollection sips) {
        String requestId = sips.getRequestId();
        IngestMetadataDto metadata = sips.getMetadata();
        for (SIP sip : sips.getFeatures()) {
            publisher.publish(IngestRequestFlowItem.build(requestId, metadata, sip));
        }
        return IngestRequestInfoDto.build(requestId, "SIP Collection ingestion request redirected to dataflow");
    }

    @Override
    public IngestRequestInfoDto redirectToDataflow(InputStream input) throws ModuleException {
        try (Reader json = new InputStreamReader(input, DEFAULT_CHARSET)) {
            SIPCollection sips = gson.fromJson(json, SIPCollection.class);
            return redirectToDataflow(sips);
        } catch (JsonIOException | IOException e) {
            LOGGER.error("Cannot read JSON file containing SIP collection", e);
            throw new EntityInvalidException(e.getMessage(), e);
        }
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
