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
package fr.cnes.regards.modules.ingest.service.aip;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdatesCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.aip.SearchFacetsAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntityLight;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.aip.SearchFacetsAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;

/**
 * AIP Service interface. Service to handle business around {@link AIPEntity}s
 * @author Sébastien Binda
 */
public interface IAIPService {

    /**
     * Create and save {@link AIPEntity} from list of {@link AIP}
     * @param sip linked {@link SIPEntity}
     * @param aips list of {@link AIP}
     * @return list of related {@link AIPEntity}
     */
    List<AIPEntity> createAndSave(SIPEntity sip, List<AIP> aips);

    /**
     * Mark all {@link AIPEntity} linked to this {@link SIPEntity#getSipId()} as deleted and send events to remove
     * all files associated to these deleted AIPs
     * @return the request group id sent to storage
     */
    String scheduleAIPEntityDeletion(String sipId);

    /**
     * Save an AIPUpdatesCreatorRequest and try to schedule it in a job
     * @param params the AIPUpdateParametersDto payload
     */
    void registerAIPEntityUpdate(AIPUpdateParametersDto params);

    /**
     * Try to run the AIPUpdate request in a job
     */
    void scheduleAIPEntityUpdate(AIPUpdatesCreatorRequest request);

    /**
     * Remove all {@link AIPEntity} linked to an {@link SIPEntity#getSipId()}
     */
    void processDeletion(String sipId, boolean deleteIrrevocably);

    /**
     * Save AIP
     */
    AIPEntity save(AIPEntity entity);

    /**
     * Download current AIP file related to AIP entity with specified urn
     */
    void downloadAIP(UniformResourceName aipId, HttpServletResponse response) throws ModuleException;

    /**
     * Calculate checksum of an AIP as it will be written when AIP file is downloaded
     * @param aip
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    String calculateChecksum(AIP aip) throws NoSuchAlgorithmException, IOException;

    /**
     * Retrieve all {@link AIPEntity}s matching parameters.
     */
    Page<AIPEntity> search(SearchAIPsParameters filters, Pageable pageable);

    Page<AIPEntityLight> searchLight(SearchAIPsParameters filters, Pageable pageable);

    /**
     * Compute the checksum of the AIP and save it
     * @param aipEntity
     */
    void computeAndSaveChecksum(AIPEntity aipEntity) throws ModuleException;

    /**
     * Retrieve all tags used by a set of AIPS matching provided filters
     * @param filters
     * @return list of tags
     */
    List<String> searchTags(SearchFacetsAIPsParameters filters);

    /**
     * Retrieve all storages used by a set of AIPS matching provided filters
     * @param filters
     * @return list of storage business id
     */
    List<String> searchStorages(SearchFacetsAIPsParameters filters);

    /**
     * Retrieve all storages used by a set of AIPS matching provided filters
     * @param filters
     * @return list of storage business id
     */
    List<String> searchCategories(SearchFacetsAIPsParameters filters);

    /**
     * Search for a {@link AIPEntity} by its ipId
     */
    Optional<AIPEntity> getAip(UniformResourceName aipId);

    /**
     * Retrieve a set of aip using a sip id
     * @param sipId
     * @return
     */
    Set<AIPEntity> getAips(String sipId);

    List<AIPEntity> saveAll(Collection<AIPEntity> updates);

    /**
     * Retrieve {@link AIPEntity}s from given aip ids
     * @param aipIds
     * @return
     */
    Collection<AIPEntity> getAips(Collection<String> aipIds);

}
