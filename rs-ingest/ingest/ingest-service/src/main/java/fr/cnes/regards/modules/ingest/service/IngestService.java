/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.builder.SIPEntityBuilder;
import fr.cnes.regards.modules.ingest.domain.dto.SIPDto;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPSession;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;

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

    public static final String MD5_ALGORITHM = "MD5";

    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private Gson gson;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private ISIPService sipService;

    @Autowired
    private ISIPSessionService sipSessionService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IIngestProcessingChainRepository ingestChainRepository;

    @Autowired
    private Validator validator;

    @Autowired
    private EntityManager em;

    @Override
    public Collection<SIPDto> ingest(SIPCollection sips) throws ModuleException {
        Collection<SIPDto> dtos = new ArrayList<>();

        // Process SIPs
        for (SIP sip : sips.getFeatures()) {
            dtos.add(store(sip, sips.getMetadata(), authResolver.getUser(), false));
        }

        return dtos;
    }

    /**
     * Validate {@link IngestMetadata}
     * @param metadata {@link IngestMetadata}
     * @throws EntityInvalidException if invalid!
     */
    private void validateIngestMetadata(IngestMetadata metadata) throws EntityInvalidException {
        // Check metadata not null
        if (metadata == null) {
            String message = "Ingest metadata is required in SIP submission request.";
            LOGGER.error(message);
            // HTTP 422 in GlobalControllerAdvice
            throw new EntityInvalidException(message);
        }
        // Check metadata processing chain name not null
        if (metadata.getProcessing() == null) {
            String message = "Ingest processing chain name is required in SIP submission request.";
            LOGGER.error(message);
            // HTTP 422 in GlobalControllerAdvice
            throw new EntityInvalidException(message);
        }
        // Check metadata processing chain name exists
        Optional<IngestProcessingChain> ipc = ingestChainRepository.findOneByName(metadata.getProcessing());
        if (!ipc.isPresent()) {
            String message = "Ingest processing chain must exists. Please, configure the chain before SIP submission.";
            LOGGER.error(message);
            // HTTP 422 in GlobalControllerAdvice
            throw new EntityInvalidException(message);
        }

    }

    @Override
    public Collection<SIPDto> ingest(InputStream input) throws ModuleException {

        try (Reader json = new InputStreamReader(input, DEFAULT_CHARSET)) {
            SIPCollection sips = gson.fromJson(json, SIPCollection.class);
            return ingest(sips);
        } catch (JsonIOException | IOException e) {
            LOGGER.error("Cannot read JSON file containing SIP collection", e);
            throw new EntityInvalidException(e.getMessage(), e);
        }
    }

    @Override
    public SIPDto retryIngest(UniformResourceName sipId) throws ModuleException {
        Optional<SIPEntity> oSip = sipRepository.findOneBySipId(sipId.toString());
        if (oSip.isPresent()) {
            SIPEntity sip = oSip.get();
            switch (sip.getState()) {
                case AIP_GEN_ERROR:
                case INVALID:
                case DELETED:
                    sipRepository.updateSIPEntityState(SIPState.CREATED, sip.getId());
                    break;
                case AIP_SUBMITTED:
                case STORE_ERROR:
                case STORED:
                    throw new EntityOperationForbiddenException(sipId.toString(), SIPEntity.class,
                            "SIP ingest process is already successully done");
                case REJECTED:
                    throw new EntityOperationForbiddenException(sipId.toString(), SIPEntity.class,
                            "SIP format is not valid");
                case VALID:
                case QUEUED:
                case CREATED:
                case AIP_CREATED:
                    throw new EntityOperationForbiddenException(sipId.toString(), SIPEntity.class,
                            "SIP ingest is already running");
                default:
                    throw new EntityOperationForbiddenException(sipId.toString(), SIPEntity.class,
                            "SIP is in undefined state for ingest retry");
            }
            return sipRepository.findOne(sip.getId()).toDto();
        } else {
            throw new EntityNotFoundException(sipId.toString(), SIPEntity.class);
        }
    }

    @Override
    public Boolean isRetryable(UniformResourceName sipId) throws EntityNotFoundException {
        Optional<SIPEntity> os = sipRepository.findOneBySipId(sipId.toString());
        if (os.isPresent()) {
            switch (os.get().getState()) {
                case INVALID:
                case AIP_GEN_ERROR:
                    return true;
                default:
                    return false;
            }
        } else {
            throw new EntityNotFoundException(sipId.toString(), SIPEntity.class);
        }
    }

    /**
     * Store a SIP for further processing
     * @param sip {@link SIP} to store
     * @param metadata bulk ingest metadata
     * @return a {@link SIPEntity} ready to be processed saved in database or a rejected one not saved in database
     */
    @Override
    public SIPDto store(SIP sip, IngestMetadata metadata, String owner, boolean publishRejected) {

        LOGGER.info("Handling new SIP {}", sip.getId());
        // Manage version
        Integer version = sipRepository.getNextVersion(sip.getId());

        // Manage session
        SIPSession session = sipSessionService.getSession(metadata.getSession().orElse(null), true);

        SIPEntity entity = SIPEntityBuilder.build(runtimeTenantResolver.getTenant(), session, sip,
                                                  metadata.getProcessing(), owner, version, SIPState.CREATED,
                                                  EntityType.DATA);

        // Validate metadata
        try {
            validateIngestMetadata(metadata);
        } catch (EntityInvalidException e) {
            // Invalid SIP
            entity.setState(SIPState.REJECTED);
            String errorMessage = String.format("Ingest processing chain \"%s\" not found!", metadata.getProcessing());
            LOGGER.warn("SIP rejected because : {}", errorMessage);
            entity.getRejectionCauses().add(errorMessage);
            if (publishRejected) {
                publisher.publish(new SIPEvent(entity));
            }
            return entity.toDto();
        }

        // Validate SIP
        Errors errors = new MapBindingResult(new HashMap<>(), "sip");
        validator.validate(sip, errors);
        if (errors.hasErrors()) {
            // Invalid SIP
            entity.setState(SIPState.REJECTED);
            errors.getAllErrors().forEach(error -> {
                entity.getRejectionCauses().add(error.getDefaultMessage());
                LOGGER.warn("SIP {} error : {}", entity.getProviderId(), error.toString());
            });
            LOGGER.warn("SIP {} rejected because invalid", entity.getProviderId());
            if (publishRejected) {
                publisher.publish(new SIPEvent(entity));
            }
            return entity.toDto();
        }

        try {
            // Compute checksum
            String checksum = SIPEntityBuilder.calculateChecksum(gson, sip, MD5_ALGORITHM);
            entity.setChecksum(checksum);

            // Prevent SIP from being ingested twice
            if (sipRepository.isAlreadyIngested(checksum)) {
                entity.setState(SIPState.REJECTED);
                entity.getRejectionCauses().add("SIP already submitted");
                if (publishRejected) {
                    publisher.publish(new SIPEvent(entity));
                }
                LOGGER.warn("SIP {} rejected cause already submitted", entity.getProviderId());
            } else {
                // Entity is persisted only if all properties properly set
                // And SIP not already stored with a same checksum
                sipService.saveSIPEntity(entity);
                publisher.publish(new SIPEvent(entity));
                LOGGER.debug("SIP {} saved, ready for asynchronous processing", entity.getProviderId());
            }

        } catch (NoSuchAlgorithmException | IOException e) {
            LOGGER.error("Cannot compute checksum for SIP identified by {}", sip.getId());
            LOGGER.error("Exception occurs!", e);
            entity.setState(SIPState.REJECTED);
            entity.getRejectionCauses().add("Not able to generate internal SIP checksum");
            if (publishRejected) {
                publisher.publish(new SIPEvent(entity));
            }
        }

        // Ensure performance by flushing transaction cache
        em.flush();
        em.clear();

        return entity.toDto();
    }
}
