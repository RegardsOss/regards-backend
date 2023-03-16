/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.ingest.domain.sip.ISipIdAndVersion;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SearchSIPsParameters;
import fr.cnes.regards.modules.ingest.service.request.IngestRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Service to handle access to {@link SIPEntity} entities.
 *
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
     */
    void processDeletion(String sipId, boolean deleteIrrevocably);

    /**
     * Update last flag for specified entity
     */
    SIPEntity updateLastFlag(SIPEntity sip, boolean last);

    /**
     * Update State for specified entity
     */
    void updateState(SIPEntity sip, SIPState state);

    /**
     * Update last flag for specified entity
     */
    void updateLastFlag(ISipIdAndVersion partialSip, boolean last);

    /**
     * Update the last update date of the {@link SIPEntity} and save it in DAO,
     *
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

    /**
     * Retrieve partial SIP avoiding mutating SIP state that may be mutated on other thread.<br/>
     * <p>
     * With very fast processing and when 2 versions of a SIP are handled simultaneously :
     * <ol>
     *  <li>SIP V1 & AIP V1 send a STORAGE request to STORAGE after {@link IngestRequestService#handleIngestJobSucceed(fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest, SIPEntity, java.util.List)}</li>
     *  <li>SIP V2 & AIP V2 is working in transaction in {@link IngestRequestService#handleIngestJobSucceed(fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest, SIPEntity, java.util.List)} to handle versioning loading last SIP V1</li>
     *  <li>SIP V1 & AIP V1 handle STORAGE response and SIP V1 state mutates to STORED</li>
     *  <li>SIP V2 & AIP V2 & SIPV1 are mutated on transaction commit, SIP V1 is restored unexpectedly to INGESTED before STORAGE request is sent to STORAGE</li>
     *  <li>SIP V2 & AIP V2 handle STORAGE response and mutate to STORED</li>
     * </ol>
     *  <br/><br/>
     *  As a result :
     *  <ul>
     *   <li>SIP V1 = INGESTED</li>
     *   <li>SIP V2 = STORED</li>
     *   <li>AIP V1 = STORED</li>
     *   <li>AIP V2 = STORED</li>
     *  </ul>
     *  <br/>
     *  So to avoid this behavior, we only load partial content of the SIP and we will update
     *  only required non-concurrent properties specifically.
     */
    ISipIdAndVersion getLatestSip(String providerId);

    /**
     * Delete the column rawsip of all SIP when these two conditions are met :
     * <li> condition 1 : lastUpdate date is between lowerDate (excluded) and upperDate (included)</li>
     * <li> condition 2 : state is STORED or DELETED</li>
     *
     * @return the number of sip updated
     */
    int cleanOldRawSip(OffsetDateTime lowerDate, OffsetDateTime upperDate);
}
