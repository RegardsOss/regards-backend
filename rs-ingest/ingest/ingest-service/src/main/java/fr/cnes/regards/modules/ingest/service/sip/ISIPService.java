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
package fr.cnes.regards.modules.ingest.service.sip;

import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.dto.RejectedSipDto;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;

/**
 * Service to handle access to {@link SIPEntity} entities.
 * @author Sébastien Binda
 */
public interface ISIPService {

    /**
     * Does a version of asked SIP into "after valid" state already exist ? (see {@link SIPState} for accepted states
     */
    boolean validatedVersionExists(String providerId);

    /**
     * Retrieve all {@link SIPEntity}s matching the parameters. SIPs are ordered by {@link SIPEntity#getIngestDate()}
     */
    Page<SIPEntity> search(String providerId, String sessionOwner, String session, OffsetDateTime from,
            List<SIPState> state, String ingestChain, Pageable page);

    /**
     * Retrieve one {@link SIPEntity} for the given sipId
     */
    SIPEntity getSIPEntity(UniformResourceName sipId) throws EntityNotFoundException;

    /**
     * Delete all {@link SIPEntity} for the given provider id
     * @param sipEntity
     * @param removeIrrevocably
     * @return rejected or undeletable {@link SIPEntity}s
     */
    RejectedSipDto deleteSIPEntity(SIPEntity sipEntity, boolean removeIrrevocably);

    /**
     * Save the given {@link SIPEntity} in DAO, update the associated session and publish a change event
     * @param sip {@link SIPEntity} to update
     * @return {@link SIPEntity} updated
     */
    SIPEntity saveSIPEntity(SIPEntity sip);

    /**
     * Compute checksum for current SIP using {@link SIPService#MD5_ALGORITHM}
     */
    String calculateChecksum(SIP sip) throws NoSuchAlgorithmException, IOException;

    /**
     * Check if current checksum already stored
     */
    boolean isAlreadyIngested(String checksum);

    /**
     * Get next version of this SIP
     */
    Integer getNextVersion(SIP sip);
}
