/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SearchSIPsParameters;

/**
 * Service to handle access to {@link SIPEntity} entities.
 * @author Sébastien Binda
 * @author Léo Mieulet
 */
public interface ISIPService {

    /**
     * Does a version of asked SIP into "after valid" state already exist ? (see {@link SIPState} for accepted states
     */
    boolean validatedVersionExists(String providerId);

    /**
     * Retrieve all {@link SIPEntity}s matching parameters.
     */
    Page<SIPEntity> search(SearchSIPsParameters params, Pageable page);

    /**
     * Retrieve one {@link SIPEntity} for the given sipId
     */
    Optional<SIPEntity> getEntity(String sipId) throws EntityNotFoundException;

    /**
     * Delete the SIPEntity using its {@link SIPEntity#getSipId()}.
     * @param sipId
     * @param deleteIrrevocably
     */
    void processDeletion(String sipId, boolean deleteIrrevocably);

    /**
     * Update last flag for specified entity
     */
    SIPEntity updateLastFlag(SIPEntity sip, boolean last);

    /**
     * Update the last update date of the {@link SIPEntity} and save it in DAO,
     * @param sip {@link SIPEntity} to update
     * @return {@link SIPEntity} updated
     */
    SIPEntity save(SIPEntity sip);

    /**
     * Compute checksum for current SIP using {@link SIPService#MD5_ALGORITHM}
     */
    String calculateChecksum(SIP sip) throws NoSuchAlgorithmException, IOException;

    /**
     * @return true if a {@link SIPEntity} with provided checksum is already stored
     */
    boolean isAlreadyIngested(String checksum);

    /**
     * Get next version of this SIP
     */
    Integer getNextVersion(SIP sip);

    SIPEntity getLatestSip(String providerId);
}
