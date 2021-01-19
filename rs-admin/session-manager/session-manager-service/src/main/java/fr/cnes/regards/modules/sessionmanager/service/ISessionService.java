/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.sessionmanager.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.sessionmanager.domain.Session;
import fr.cnes.regards.modules.sessionmanager.domain.SessionState;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;

public interface ISessionService {

    /**
     * Retrieve the list of sessions
     * @param source          A subset of the source name, case ignored
     * @param name            A subset of the session name, case ignored
     * @param from            The min session creation date
     * @param to              The max session creation date
     * @param onlyLastSession Return the lastest session from a source. This parameter is agnostic from others search filters
     * @param state           Return only session matching this state
     * @return A {@link List} of {@link Session}
     */
    Page<Session> retrieveSessions(String source, String name, OffsetDateTime from, OffsetDateTime to,
            SessionState state, boolean onlyLastSession, Pageable page);

    /**
     * Retrive the list of session names
     * @param name Return only session matching the name, case insensitive
     * @return A {@link List} of {@link String} session name
     */
    List<String> retrieveSessionNames(String name);

    /**
     * Retrive the list of session names
     * @param source Return only session matching the source, case insensitive
     * @return A {@link List} of {@link String} session name
     */
    List<String> retrieveSessionSources(String source);

    /**
     * Update the {@link Session#state}
     * @param id    The session <code>id</code>
     * @param state The new state value
     * @return The {@link Session}
     * @throws ModuleException Thrown when the state cannot be changed
     */
    Session updateSessionState(Long id, SessionState state) throws ModuleException;

    /**
     * Delete a session
     * @param id    The session <code>id</code>
     * @param force When set to true, the entity is also deleted in db
     * @throws EntityNotFoundException Thrown when no session with provided <code>id</code> could be found
     * @throws ModuleException         Thrown when the state cannot be changed
     */
    void deleteSession(Long id, boolean force) throws ModuleException;

    /**
     * Update session properties
     * @param sessionMonitoringEvents The notifications with everything to update the sessions
     * @return updated {@link Session}
     */
    List<Session> updateSessionProperties(List<SessionMonitoringEvent> list);

}
