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
package fr.cnes.regards.modules.ingest.service.aip;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntityLight;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.VersioningMode;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.AbstractSearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPLightParameters;
import fr.cnes.regards.modules.ingest.dto.aip.SearchFacetsAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * AIP Service interface. Service to handle business around {@link AIPEntity}s
 *
 * @author Sébastien Binda
 */
public interface IAIPService {

    /**
     * Create and save {@link AIPEntity} from list of {@link AIP}
     *
     * @param sip  linked {@link SIPEntity}
     * @param aips list of {@link AIP}
     * @return list of related {@link AIPEntity}
     */
    List<AIPEntity> createAndSave(SIPEntity sip, List<AIP> aips);

    /**
     * Save an AIPUpdatesCreatorRequest and try to schedule it in a job
     *
     * @param params the AIPUpdateParametersDto payload
     */
    void registerUpdatesCreator(AIPUpdateParametersDto params);

    /**
     * Update last flag for specified entity
     */
    AIPEntity updateLastFlag(AIPEntity sip, boolean last);

    /**
     * Save AIP
     */
    AIPEntity save(AIPEntity entity);

    /**
     * Download current AIP file related to AIP entity with specified urn
     */
    void downloadAIP(OaisUniformResourceName aipId, HttpServletResponse response) throws ModuleException;

    /**
     * Calculate checksum of an AIP as it will be written when AIP file is downloaded
     *
     * @param aip
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    String calculateChecksum(AIP aip) throws NoSuchAlgorithmException, IOException;

    /**
     * Retrieve all {@link AIPEntity}s matching filters.
     *
     * @param filters
     * @param pageable
     * @return page of AIPs
     */
    Page<AIPEntity> findByFilters(AbstractSearchAIPsParameters<?> filters, Pageable pageable);

    /**
     * Retrieve all {@link AIPEntityLight}s matching filters.
     *
     * @param filters
     * @param pageable
     * @return page of light AIPs
     */
    Page<AIPEntityLight> findLightByFilters(SearchAIPLightParameters filters, Pageable pageable);

    /**
     * Retrieve all tags used by a set of AIPS matching provided filters
     *
     * @param filters
     * @return list of tags
     */
    List<String> findTags(SearchFacetsAIPsParameters filters);

    /**
     * Retrieve all storages used by a set of AIPS matching provided filters
     *
     * @param filters
     * @return list of storage business id
     */
    List<String> findStorages(SearchFacetsAIPsParameters filters);

    /**
     * Retrieve all storages used by a set of AIPS matching provided filters
     *
     * @param filters
     * @return list of storage business id
     */
    List<String> findCategories(SearchFacetsAIPsParameters filters);

    /**
     * Search for a {@link AIPEntity} by its ipId
     */
    Optional<AIPEntity> getAip(OaisUniformResourceName aipId);

    /**
     * Retrieve a set of aip using a sip id
     *
     * @param sipId
     */
    Set<AIPEntity> findBySipId(String sipId);

    List<AIPEntity> saveAll(Collection<AIPEntity> updates);

    /**
     * Retrieve {@link AIPEntity}s from given aip ids
     *
     * @param aipIds
     */
    Collection<AIPEntity> findByAipIds(Collection<String> aipIds);

    Set<AIPEntity> findLastByProviderIds(Collection<String> providerIds);

    void handleVersioning(AIPEntity aipEntity,
                          VersioningMode versioningMode,
                          Map<String, AIPEntity> currentLatestPerProviderId);
}
