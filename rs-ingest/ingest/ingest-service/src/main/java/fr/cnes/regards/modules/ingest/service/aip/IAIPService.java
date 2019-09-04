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

import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.dto.RejectedAipDto;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.domain.dto.RejectedAipDto;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.storagelight.domain.dto.FileStorageRequestDTO;

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
     * Build storage request for AIP file itself!
     */
    Collection<FileStorageRequestDTO> buildAIPStorageRequest(AIP aip, List<StorageMetadata> storages)
            throws ModuleException;

    void setAipToStored(UniformResourceName aipId, AIPState state);

    /**
     * Delete the {@link AIPEntity} by his ipId
     */
    Collection<RejectedAipDto> deleteAip(String sipId);

    /**
     * Save AIP
     */
    AIPEntity save(AIPEntity entity);

    /**
     * Download current AIP file related to AIP entity with specified urn
     */
    void downloadAIP(UniformResourceName aipId, HttpServletResponse response) throws ModuleException;

    /**
     * Retrieve AIPs matching provided parameters
     */
    Page<AIPEntity> search(AIPState state, OffsetDateTime from, OffsetDateTime to, List<String> tags, String sessionOwner,
            String session, String providerId, List<String> storages, List<String> categories, Pageable pageable);


    /**
     * Search for a {@link AIPEntity} by its ipId
     */
    Optional<AIPEntity> searchAip(UniformResourceName aipId);
}
