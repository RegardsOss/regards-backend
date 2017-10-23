/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

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

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final Gson gson;

    private final ISIPRepository sipRepository;

    public IngestService(IRuntimeTenantResolver runtimeTenantResolver, Gson gson, ISIPRepository sipRepository) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.gson = gson;
        this.sipRepository = sipRepository;
    }

    @Override
    public Collection<SIPEntity> ingest(SIPCollection sips) throws ModuleException {

        Collection<SIPEntity> entities = new ArrayList<>();
        IngestMetadata metadata = sips.getMetadata();
        for (SIP sip : sips.getFeatures()) {
            entities.add(store(sip, metadata));
        }
        return entities;
    }

    /**
     * Store a SIP for further processing
     * @param sip {@link SIP} to store
     * @param metadata bulk ingest metadata
     * @return a {@link SIPEntity} ready to be processed saved in database or a rejected one not saved in database
     */
    private SIPEntity store(SIP sip, IngestMetadata metadata) {

        SIPEntity entity = new SIPEntity();
        entity.setSipId(sip.getId());
        entity.setState(SIPState.CREATED);
        entity.setSip(sip);
        entity.setIngestDate(OffsetDateTime.now());
        entity.setProcessing(metadata.getProcessing());
        entity.setSessionId(metadata.getSessionId().orElse(null));

        // Compute internal IP_ID
        UUID uuid = UUID.nameUUIDFromBytes(sip.getId().getBytes());

        // Manage version
        Integer version = sipRepository.getNextVersion(entity.getSipId());
        UniformResourceName urn = new UniformResourceName(OAISIdentifier.SIP, EntityType.DATA,
                runtimeTenantResolver.getTenant(), uuid, version);
        entity.setIpId(urn.toString());
        entity.setVersion(version);

        // Compute checksum
        String jsonSip = gson.toJson(sip);
        InputStream inputStream = new ByteArrayInputStream(jsonSip.getBytes());
        String checksum;
        try {
            checksum = ChecksumUtils.computeHexChecksum(inputStream, MD5_ALGORITHM);
            entity.setChecksum(checksum);

            // Prevent SIP from being ingested twice
            if (sipRepository.isAlreadyIngested(checksum)) {
                entity.setState(SIPState.REJECTED);
                entity.setReasonForRejection("SIP already submitted");
                LOGGER.warn("SIP {} rejected cause already submitted", entity.getSipId());
            } else {
                // Entity is persisted only if all properties properly set
                // And SIP not already stored with a same checksum
                sipRepository.save(entity);
                LOGGER.info("SIP {} saved, ready for asynchronous processing", entity.getSipId());
            }

        } catch (NoSuchAlgorithmException | IOException e) {
            LOGGER.error("Cannot compute checksum for sip identified by {}", sip.getId());
            LOGGER.error("Exception occurs!", e);
            entity.setState(SIPState.REJECTED);
            entity.setReasonForRejection("Not able to generate internal SIP checksum");
        }

        return entity;
    }

    @Override
    public Collection<SIPEntity> getAllVersions(String sipId) {
        return sipRepository.getAllVersions(sipId);
    }
}
