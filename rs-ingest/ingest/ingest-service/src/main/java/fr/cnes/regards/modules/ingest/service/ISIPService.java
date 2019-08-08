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
package fr.cnes.regards.modules.ingest.service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.RejectedSip;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * Service to handle access to {@link SIPEntity} entities.
 * @author Sébastien Binda
 */
public interface ISIPService {

    /**
     * Retrieve all submitted SIP with same provider id
     * @param providerId provider id
     * @return all version of related SIP with specified provider id
     */
    Collection<SIPEntity> getAllVersions(String providerId);

    /**
     * Does a version of asked SIP into "after valid" state already exist ? (see {@link SIPState} for accepted states
     */
    boolean validatedVersionExists(String providerId);

    /**
     * Retrieve all {@link SIPEntity}s matching the parameters. SIPs are ordered by {@link SIPEntity#getIngestDate()}
     */
    Page<SIPEntity> search(String providerId, String clientId, String clientSession, String owner, OffsetDateTime from,
            List<SIPState> state, String ingestChain, Pageable page);

    /**
     * Retrieve one {@link SIPEntity} for the given sipId
     */
    SIPEntity getSIPEntity(UniformResourceName sipId) throws EntityNotFoundException;

    /**
     * Delete one {@link SIPEntity} for the given ipId
     * @param sipIds
     * @return rejected or undeletable {@link SIPEntity}s
     * @throws EntityNotFoundException
     */
    Collection<RejectedSip> deleteSIPEntitiesBySipIds(Collection<UniformResourceName> sipIds) throws ModuleException;

    /**
     * Delete all {@link SIPEntity} for the given provider id
     * @param providerId
     * @return rejected or undeletable {@link SIPEntity}s
     * @throws ModuleException
     */
    Collection<RejectedSip> deleteSIPEntitiesForProviderId(String providerId) throws ModuleException;

    /**
     * Delete all {@link SIPEntity}s associated to the given session.
     * @param sessionId
     * @return rejected or undeletable {@link SIPEntity}s
     * @throws ModuleException
     */
    Collection<RejectedSip> deleteSIPEntitiesForSession(String clientId, String clientSession) throws ModuleException;

    /**
     * Delete all {@link SIPEntity}s.
     * @param sips
     * @return rejected or undeletable {@link SIPEntity}s
     * @throws ModuleException
     */
    Collection<RejectedSip> deleteSIPEntities(Collection<SIPEntity> sips) throws ModuleException;

    /**
     * Check if the SIP with the given ipId is deletable
     */
    Boolean isDeletable(UniformResourceName sipId) throws EntityNotFoundException;

    /**
     * Save the given {@link SIPEntity} in DAO, update the associated session and publish a change event
     * @param sip {@link SIPEntity} to update
     * @return {@link SIPEntity} updated
     */
    SIPEntity saveSIPEntity(SIPEntity sip);

    /**
     * Notify single SIP state change
     * @param metadata ingest metadata
     * @param previousState previous state
     * @param nextState next state
     */
    void notifySipChangedState(IngestMetadata metadata, SIPState previousState, SIPState nextState);

    /**
     * Notify multiple SIP state change
     * @param metadata ingest metadata
     * @param previousState previous state
     * @param nextState next state
     * @param nbSip SIP count
     */
    void notifySipsChangedState(IngestMetadata metadata, SIPState previousState, SIPState nextState, int nbSip);

}
