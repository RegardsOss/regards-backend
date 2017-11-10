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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPSession;

/**
 * Service to handle {@link SIPSession} entities.
 * @author Sébastien Binda
 */
public interface ISIPSessionService {

    /**
     * Retrieve one {@link SIPSession} by id.
     * @param sessionId {@link String}
     * @param createIfNotExists if true, the session with sessionId is created is it does not exists.
     * @return {@link SIPSession}
     */
    SIPSession getSession(String sessionId, Boolean createIfNotExists);

    /**
     * Retrieve all {@link SIPSession}
     * @param pageable pagination information
     * @return {@link SIPSession}s
     */
    Page<SIPSession> search(String id, OffsetDateTime from, OffsetDateTime to, Pageable pageable);

    /**
     * Delete all {@link SIPEntity}s associated to the given SIPSession
     * @param id
     * @return Rejected {@link SIPEntity}s for deletion
     */
    Collection<SIPEntity> deleteSIPSession(String id) throws ModuleException;

}
