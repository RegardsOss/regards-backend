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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.sessionmanager.dao.ISessionRepository;
import fr.cnes.regards.modules.sessionmanager.dao.SessionSpecifications;
import fr.cnes.regards.modules.sessionmanager.domain.Session;
import fr.cnes.regards.modules.sessionmanager.domain.SessionState;
import fr.cnes.regards.modules.sessionmanager.domain.event.DeleteSessionEvent;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationOperator;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationState;

@Service
@MultitenantTransactional
public class SessionService implements ISessionService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(SessionService.class);

    /**
     * CRUD repository managing sessions. Autowired by Spring.
     */
    @Autowired
    private ISessionRepository sessionRepository;

    /**
     * Publisher to notify system of files events (stored, retrieved or deleted).
     */
    @Autowired
    private IPublisher publisher;

    @Override
    @Transactional(readOnly = true)
    public Page<Session> retrieveSessions(String source, String name, OffsetDateTime from, OffsetDateTime to,
            SessionState state, boolean onlyLastSession, Pageable page) {
        return sessionRepository.findAll(SessionSpecifications.search(source, name, from, to, state, onlyLastSession),
                                         page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> retrieveSessionNames(String name) {
        return sessionRepository.findAllSessionName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> retrieveSessionSources(String source) {
        return sessionRepository.findAllSessionSource(source);
    }

    @Override
    public Session updateSessionState(Long id, SessionState state) throws ModuleException {
        Session s = getSession(id);
        // Allow user to mark as acknowledged a session previously in error
        if ((s.getState() == SessionState.ERROR) && (state == SessionState.ACKNOWLEDGED)) {
            s.setState(state);
            return this.updateSession(s);
        }
        String errorMessage = String.format("Changing session state from %s to %s isn't allowed", s.getState(), state);
        LOG.debug(errorMessage);
        throw new EntityOperationForbiddenException(String.valueOf(s.getId()), Session.class, errorMessage);
    }

    @Override
    public void deleteSession(Long id, boolean force) throws ModuleException {
        Session s = getSession(id);
        this.sendDeleteNotification(s);
        String source = s.getSource();
        if (force) {
            LOG.info("Delete definitely session {} {}", s.getSource(), s.getName());
            sessionRepository.delete(s);
        } else {
            LOG.info("Mark session {} {} as deleted", s.getSource(), s.getName());
            if (s.getState() != SessionState.DELETED) {
                s.setState(SessionState.DELETED);
                updateSession(s);
            }
        }
        updateSourceLastSession(source);
    }

    /**
     * Update all session for the given source to set the isLatest flag on the last updated session
     * @param source
     */
    private void updateSourceLastSession(String source) {
        sessionRepository.updateSourceSessionsIsLatest(source, false);
        Page<Session> pageResponse = sessionRepository.findAllBySourceOrderByLastUpdateDateDesc(source,
                                                                                                PageRequest.of(0, 1));
        if (pageResponse.hasContent()) {
            Session latestSession = pageResponse.getContent().get(0);
            latestSession.setLatest(true);
            sessionRepository.save(latestSession);
        }
    }

    @Override
    public List<Session> updateSessionProperties(List<SessionMonitoringEvent> events) {
        return sessionRepository.saveAll(mergeEvents(events));
    }

    private Collection<Session> mergeEvents(Collection<SessionMonitoringEvent> events) {
        Map<String, Session> sessionsToUpdate = new HashMap<>();
        // events have to be dismantle to be used by handleEventMerge so code is the same in both cases
        for (SessionMonitoringEvent sessionMonitoringEvent : events) {
            if (sessionMonitoringEvent.isGlobal()) {
                // alreadyCreated = session in DB + session already handled by mergeEvents
                Set<Session> alreadyCreatedSessions = Sets.newHashSet(sessionRepository.findAll());
                alreadyCreatedSessions.addAll(sessionsToUpdate.values());
                for (Session session : alreadyCreatedSessions) {
                    handleEventMerge(sessionsToUpdate, session.getSource(), session.getName(),
                                     sessionMonitoringEvent.getStep(), sessionMonitoringEvent.getProperty(),
                                     sessionMonitoringEvent.getOperator(), sessionMonitoringEvent.getValue(),
                                     sessionMonitoringEvent.getState());
                }
            } else {
                handleEventMerge(sessionsToUpdate, sessionMonitoringEvent.getSource(), sessionMonitoringEvent.getName(),
                                 sessionMonitoringEvent.getStep(), sessionMonitoringEvent.getProperty(),
                                 sessionMonitoringEvent.getOperator(), sessionMonitoringEvent.getValue(),
                                 sessionMonitoringEvent.getState());
            }
        }
        return sessionsToUpdate.values();
    }

    private void handleEventMerge(Map<String, Session> sessionsToUpdate, String source, String name, String step,
            String property, SessionNotificationOperator operator, Object value, SessionNotificationState state) {
        String sessionKey = source + "__" + name;
        Session sessionToUpdate = sessionsToUpdate.get(sessionKey);
        if (sessionToUpdate == null) {
            Optional<Session> sessionOpt = sessionRepository.findOneBySourceAndName(source, name);
            if (!sessionOpt.isPresent()) {
                sessionToUpdate = createSession(name, source);
            } else {
                sessionToUpdate = sessionOpt.get();
            }
        }
        sessionToUpdate.setLastUpdateDate(OffsetDateTime.now());
        sessionsToUpdate.put(sessionKey,
                             updateSessionProperty(sessionToUpdate, step, property, operator, value, state));
    }

    private Session updateSessionProperty(Session sessionToUpdate, String step, String property,
            SessionNotificationOperator operator, Object value, SessionNotificationState state) {
        // Set the new value inside the map
        boolean isKeyExisting = sessionToUpdate.isStepPropertyExisting(step, property);
        if (isKeyExisting
                && ((operator == SessionNotificationOperator.INC) || (operator == SessionNotificationOperator.DEC))) {
            // Handle mathematical operators
            Object previousValueAsObject = sessionToUpdate.getStepPropertyValue(step, property);
            Long previousValue;
            // We support only numerical value, so we fallback previousValue to zero if its type is string
            if (previousValueAsObject instanceof String) {
                previousValue = 0L;
            } else {
                previousValue = getLongValue(previousValueAsObject).orElse(0L);
            }
            Long updatedValue;
            // Only possible operators are INC and DEC thanks to if condition
            switch (operator) {
                case INC:
                    updatedValue = previousValue + getLongValue(value).orElse(0L);
                    break;
                case DEC:
                default:
                    updatedValue = previousValue - getLongValue(value).orElse(0L);
                    break;
            }
            sessionToUpdate.setStepPropertyValue(step, property, updatedValue);
        } else {
            switch (operator) {
                case INC:
                case REPLACE:
                    // Just use the provided value
                    sessionToUpdate.setStepPropertyValue(step, property, value);
                    break;
                case DEC:
                    // If we create using the DEC operator, we use the opposite value
                    Long valueDec = -getLongValue(value).orElse(0L);
                    sessionToUpdate.setStepPropertyValue(step, property, valueDec);
                    break;
            }
        }
        // Update the state if we receive an error
        if (state == SessionNotificationState.ERROR) {
            sessionToUpdate.setState(SessionState.ERROR);
        }
        return sessionToUpdate;
    }

    /**
     * Create a new session and remove the flag isLatest to the previous entity having the same source
     * @param name   The session name
     * @param source The session source
     */
    private Session createSession(String name, String source) {

        // Does a session exists for the same source ?
        Optional<Session> oldLatestSessionOpt = sessionRepository.findOneBySourceAndIsLatestTrue(source);
        Session newSession = sessionRepository.save(new Session(source, name));
        // Remove the flag isLatest to the previous one session sharing the same source
        if (oldLatestSessionOpt.isPresent()) {
            Session oldLatestSession = oldLatestSessionOpt.get();
            oldLatestSession.setLatest(false);
            sessionRepository.save(oldLatestSession);
        }
        return newSession;
    }

    private Session getSession(Long id) throws EntityNotFoundException {
        Optional<Session> sessionOpt = sessionRepository.findById(id);
        if (!sessionOpt.isPresent()) {
            throw new EntityNotFoundException(sessionOpt.toString(), Session.class);
        }
        return sessionOpt.get();
    }

    /**
     * Update the provided session and set the lastUpdate date
     * @param session
     * @return
     */
    private Session updateSession(Session session) {
        session.setLastUpdateDate(OffsetDateTime.now());
        return sessionRepository.save(session);
    }

    /**
     * Send a notification this session have been deleted
     * @param session
     */
    private void sendDeleteNotification(Session session) {
        DeleteSessionEvent notif = DeleteSessionEvent.build(session.getSource(), session.getName());
        publisher.publish(notif);
    }

    private Optional<Long> getLongValue(Object value) {
        Long longValue = null;
        if (value instanceof Integer) {
            longValue = ((Integer) value).longValue();
        } else if (value instanceof Long) {
            longValue = ((Long) value);
        } else if (value instanceof Double) {
            longValue = ((Double) value).longValue();
        } else {
            LOG.error("Error getting long value from object", value);
        }
        return Optional.ofNullable(longValue);
    }
}
