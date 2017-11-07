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

import java.time.OffsetDateTime;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * Service to handle access to {@link SIPEntity} entities.
 * @author Sébastien Binda
 */
public interface ISIPService {

    /**
     * Retrieve all submitted SIP with same sipId
     * @param sipId SIP ID
     * @return all version of related SIP with specified sipId
     */
    Collection<SIPEntity> getAllVersions(String sipId);

    /**
     * Retrieve all {@link SIPEntity}s matching the parameters. SIPs are ordered by {@link SIPEntity#getIngestDate()}
     * @param sipId
     * @param sessionId
     * @param owner
     * @param from
     * @param state
     * @param page
     * @return
     */
    Page<SIPEntity> getSIPEntities(String sipId, String sessionId, String owner, OffsetDateTime from, SIPState state,
            Pageable page);

    /**
     * Retrieve one {@link SIPEntity} for the given ipId
     * @param ipId
     * @return
     * @throws EntityNotFoundException
     */
    SIPEntity getSIPEntity(String ipId) throws ModuleException;

    /**
     * Delete one {@link SIPEntity} for the given ipId
     * @param ipId
     * @throws EntityNotFoundException
     */
    void deleteSIPEntity(String ipId) throws ModuleException;

    /**
     * Delete all {@link SIPEntity} for the given sipId
     * @param sipId
     * @throws ModuleException
     */
    void deleteSIPEntities(String sipId) throws ModuleException;

    /**
     * Check if the SIP with the given ipId is deletable
     * @param ipId
     * @return
     */
    Boolean isDeletable(String ipId) throws EntityNotFoundException;

    /**
     * Check if the SIP with the given ipId is available for new ingestion submission
     * @param ipId
     * @return
     */
    Boolean isRetryable(String ipId) throws EntityNotFoundException;

}
