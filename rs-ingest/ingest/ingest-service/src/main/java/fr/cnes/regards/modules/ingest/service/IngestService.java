/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import fr.cnes.regards.framework.oais.dto.ContentInformationDto;
import fr.cnes.regards.framework.oais.dto.OAISDataObjectDto;
import fr.cnes.regards.framework.oais.dto.OAISDataObjectLocationDto;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.ingest.domain.dto.RequestInfoDto;
import fr.cnes.regards.modules.ingest.domain.mapper.IIngestMetadataMapper;
import fr.cnes.regards.modules.ingest.domain.request.IngestErrorType;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.dto.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.conf.IngestConfigurationProperties;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Ingest management service
 *
 * @author Marc Sordi
 * <p>
 * TODO : retry ingestion
 * TODO : retry deletion?
 */
@Service
@MultitenantTransactional
public class IngestService implements IIngestService {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestService.class);

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

    @Autowired
    private SessionNotifier sessionNotifier;

    @Autowired
    private IngestValidationService validationService;

    /**
     * Middleware method extracted for test simulation and also used by operational code.
     * Transform a SIP collection to a SIP flow item collection
     */
    public static Collection<IngestRequestFlowItem> sipToFlow(SIPCollection sips) {
        Collection<IngestRequestFlowItem> items = new ArrayList<>();
        if (sips != null) {
            IngestMetadataDto metadata = sips.getMetadata();
            for (SIPDto sip : sips.getFeatures()) {
                items.add(IngestRequestFlowItem.build(metadata, sip));
            }
        }
        return items;
    }

    /**
     * Validate, save and publish a new request
     *
     * @param item request to manage
     */
    private IngestRequest registerIngestRequest(IngestRequestFlowItem item) {
        IngestMetadata ingestMetadata = metadataMapper.dtoToMetadata(item.getMetadata());
        if (ingestMetadata.getSubmissionDate() == null && item.getMessageProperties() != null) {
            Date timestamp = item.getMessageProperties().getHeader("timestamp");
            if (timestamp != null) {
                ingestMetadata.setSubmissionDate(timestamp.toInstant().atOffset(ZoneOffset.UTC));
            }
        }
        return registerIngestRequest(item.getRequestId(),
                                     item.getSip(),
                                     ingestMetadata,
                                     RequestInfoDto.build(item.getMetadata().getSessionOwner(),
                                                          item.getMetadata().getSession()),
                                     new HashSet<>(),
                                     item.getSip().getId());
    }

    /**
     * Validate, save and publish a new request
     *
     * @param sip             sip to manage
     * @param ingestMetadata  related ingest metadata
     * @param info            synchronous feedback
     * @param grantedRequests collection of granted requests to populate
     */
    private IngestRequest registerIngestRequest(String requestId,
                                                SIPDto sip,
                                                IngestMetadata ingestMetadata,
                                                RequestInfoDto info,
                                                Collection<IngestRequest> grantedRequests,
                                                String sipId) {
        // Validate SIP
        Errors errors = new MapBindingResult(new HashMap<>(), SIPDto.class.getName());
        validator.validate(sip, errors);

        // Validate DescriptiveInformation against given regards model if present
        if (StringUtils.isNotBlank(ingestMetadata.getModel())) {
            errors.addAllErrors(validationService.validate(ingestMetadata.getModel(),
                                                           sip.getProperties().getDescriptiveInformation(),
                                                           SIPDto.class.getName()));
        }
        if (errors.hasErrors()) {
            Set<String> errs = ErrorTranslator.getErrors(errors);
            // Publish DENIED request (do not persist it in DB) / Warning : request id cannot be known
            ingestRequestService.handleRequestDenied(IngestRequest.build(requestId,
                                                                         ingestMetadata,
                                                                         InternalRequestState.ERROR,
                                                                         IngestRequestStep.LOCAL_DENIED,
                                                                         sip,
                                                                         errs,
                                                                         IngestErrorType.GENERATION));
            StringJoiner joiner = new StringJoiner(", ");
            errs.forEach(joiner::add);
            LOGGER.debug("Ingest request ({}) rejected for following reason(s) : {}",
                         requestId == null ? "per REST" : requestId,
                         joiner.toString());
            // Trace denied request
            info.addDeniedRequest(sipId, joiner.toString());

            return null;
        }

        // Check for each feature if storage locations are valide for feature files
        checkSipStorageLocations(sip, ingestMetadata, errors);

        // Save granted ingest request, versioning mode is being handled later
        IngestRequest request = IngestRequest.build(requestId,
                                                    ingestMetadata,
                                                    InternalRequestState.TO_SCHEDULE,
                                                    IngestRequestStep.LOCAL_SCHEDULED,
                                                    sip);
        ingestRequestService.handleRequestGranted(request);
        // Trace granted request
        info.addGrantedRequest(sip.getId(), request.getCorrelationId());
        // Add to granted request collection
        grantedRequests.add(request);
        return request;
    }

    @Override
    public void handleIngestRequests(Collection<IngestRequestFlowItem> items) {
        for (IngestRequestFlowItem item : items) {
            // Validate and transform to request
            IngestRequest ingestRequest = registerIngestRequest(item);
            if (ingestRequest != null) {
                // monitoring
                sessionNotifier.incrementRequestCount(ingestRequest);
            }
        }
    }

    @Override
    public RequestInfoDto handleSIPCollection(SIPCollection sips) throws EntityInvalidException {

        // Check submission limit / If there are more features than configurated bulk max size, reject request!
        if (sips.getFeatures().size() > confProperties.getMaxBulkSize()) {
            throw new EntityInvalidException(String.format(
                "Invalid request due to ingest configuration max bulk size set to %s.",
                confProperties.getMaxBulkSize()));
        }

        // Validate and transform ingest metadata
        IngestMetadata ingestMetadata = getIngestMetadata(sips.getMetadata());

        // Register requests
        Collection<IngestRequest> grantedRequests = new ArrayList<>();
        String source = ingestMetadata.getSessionOwner();
        String session = ingestMetadata.getSession();
        RequestInfoDto info = RequestInfoDto.build(source, session, "SIP Collection ingestion scheduled");

        int count = 1;
        for (SIPDto sip : sips.getFeatures()) {
            String sipId = sip.getId() != null ? sip.getId() : "SIP n°" + count;
            // Validate and transform to request
            registerIngestRequest(null, sip, ingestMetadata, info, grantedRequests, sipId);
            count++;
        }

        // Monitoring
        sessionNotifier.incrementRequestCount(source, session, grantedRequests.size());

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
            LOGGER.debug("SIP collection submission rejected due to invalid ingest metadata : {}",
                         String.join(",", errs));
            // Throw invalid exception
            throw new EntityInvalidException(new ArrayList<>(errs));
        }

        return metadataMapper.dtoToMetadata(dto);
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

    /**
     * Validate given SIP dataobjects to ensure storage location is configured for each needed {@link DataType} to store
     */
    private void checkSipStorageLocations(SIPDto sip, IngestMetadata ingestMetadata, Errors errors) {
        Assert.notNull(errors, "Errors should not be null");
        if (((sip != null) & (sip.getProperties() != null))
            && (sip.getProperties().getContentInformations() != null)
            && (ingestMetadata != null)) {
            Set<DataType> handleTypes = Sets.newHashSet();
            ingestMetadata.getStorages().stream().map(StorageMetadata::getTargetTypes).forEach(t -> {
                if (t.isEmpty()) {
                    handleTypes.addAll(Sets.newHashSet(DataType.values()));
                } else {
                    handleTypes.addAll(t);
                }
            });
            for (ContentInformationDto ci : sip.getProperties().getContentInformations()) {
                Double height = ci.getRepresentationInformation().getSyntax().getHeight();
                Double width = ci.getRepresentationInformation().getSyntax().getWidth();
                OAISDataObjectDto dobj = ci.getDataObject();
                DataType regardsDataType = dobj.getRegardsDataType();
                // If file needed to be stored check that the data type is well configured
                if (dobj.getLocations().stream().anyMatch(l -> l.getStorage() == null) && !handleTypes.contains(
                    regardsDataType)) {
                    errors.reject("NOT_HANDLED_STORAGE_DATA_TYPE",
                                  String.format(
                                      "Data type %s to store is not associated to a configured storage location",
                                      regardsDataType.toString()));
                }
                // add check on quicklook or thumbnail to assert that if they are to be referenced, height and width have been set
                if ((regardsDataType == DataType.QUICKLOOK_HD) || (regardsDataType == DataType.QUICKLOOK_MD) || (
                    regardsDataType
                    == DataType.QUICKLOOK_SD) || (regardsDataType == DataType.THUMBNAIL)) {
                    for (OAISDataObjectLocationDto location : dobj.getLocations()) {
                        if (!Strings.isNullOrEmpty(location.getStorage()) && ((height == null) || (width == null))) {
                            errors.reject("REFERENCED_IMAGE_WITHOUT_DIMENSION",
                                          String.format(
                                              "Both height and width must be set for images(%s in SIP: %s) that are being referenced!",
                                              dobj.getFilename(),
                                              sip.getId()));
                        }
                    }
                }
            }
        }
    }
}
