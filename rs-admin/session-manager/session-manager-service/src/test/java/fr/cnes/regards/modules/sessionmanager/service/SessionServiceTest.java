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
package fr.cnes.regards.modules.sessionmanager.service;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.sessionmanager.dao.ISessionRepository;
import fr.cnes.regards.modules.sessionmanager.domain.Session;
import fr.cnes.regards.modules.sessionmanager.domain.SessionState;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationOperator;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationState;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=session_service" })
public class SessionServiceTest extends AbstractMultitenantServiceTest {

    @Autowired
    private ISessionService sessionService;

    @Autowired
    private ISessionRepository sessionRepository;

    @Before
    public void beforeTests() {
        sessionRepository.deleteAll();
    }

    @Test
    public void testUpdateState() throws ModuleException {
        Session session = generateErrorSession();

        Session sessionUpdated = sessionService.updateSessionState(session.getId(), SessionState.ACKNOWLEDGED);
        Assert.assertEquals("Should state be updated to Acknowledged", sessionUpdated.getState(),
                            SessionState.ACKNOWLEDGED);
    }

    @Test(expected = ModuleException.class)
    public void testInvalidUpdateState() throws ModuleException {
        Session session = generateErrorSession();

        // Set the session state from ERROR to OK
        sessionService.updateSessionState(session.getId(), SessionState.OK);
    }

    @Test
    public void testDelete() throws ModuleException {
        Session session = generateErrorSession();

        sessionService.deleteSession(session.getId(), false);
        Optional<Session> sessionOpt = sessionRepository.findById(session.getId());
        Assert.assertTrue("Session should still exists", sessionOpt.isPresent());
        Assert.assertEquals("When deleted with force=false, the session state is DELETED", SessionState.DELETED,
                            sessionOpt.get().getState());
    }

    @Test
    public void testForceDelete() throws ModuleException {
        Session session = generateErrorSession();
        sessionService.deleteSession(session.getId(), true);
        Optional<Session> sessionOpt = sessionRepository.findById(session.getId());
        Assert.assertFalse("When deleted with force=true, the session should not exist anymore",
                           sessionOpt.isPresent());
    }

    @Test
    public void testCalculateSessionIsLatetest() {

        String source = "testCalculateSessionIsLatetest";
        String name = "name";
        String step = "step";
        String property = "property";
        Page<Session> latests = sessionService.retrieveSessions(source, null, null, null, null, true,
                                                                PageRequest.of(0, 10));
        Page<Session> olds = sessionService.retrieveSessions(source, null, null, null, null, false,
                                                             PageRequest.of(0, 10));
        Assert.assertEquals(0, latests.getTotalElements());
        Assert.assertEquals(0, olds.getTotalElements());
        // Init session
        sessionService.updateSessionProperty(SessionMonitoringEvent
                .build(source, name, SessionNotificationState.OK, step, SessionNotificationOperator.INC, property, 1));
        // Check isLatests property
        latests = sessionService.retrieveSessions(source, null, null, null, null, true, PageRequest.of(0, 10));
        olds = sessionService.retrieveSessions(source, null, null, null, null, false, PageRequest.of(0, 10));
        Assert.assertEquals(1, latests.getTotalElements());
        Assert.assertEquals(1, olds.getTotalElements());
        // update same session
        sessionService.updateSessionProperty(SessionMonitoringEvent
                .build(source, name, SessionNotificationState.OK, step, SessionNotificationOperator.INC, property, 1));
        latests = sessionService.retrieveSessions(source, null, null, null, null, true, PageRequest.of(0, 10));
        olds = sessionService.retrieveSessions(source, null, null, null, null, false, PageRequest.of(0, 10));
        Assert.assertEquals(1, latests.getTotalElements());
        Assert.assertEquals(1, olds.getTotalElements());
        // Create new one
        sessionService
                .updateSessionProperty(SessionMonitoringEvent.build(source, "name2", SessionNotificationState.OK, step,
                                                                    SessionNotificationOperator.INC, property, 1));
        latests = sessionService.retrieveSessions(source, null, null, null, null, true, PageRequest.of(0, 10));
        olds = sessionService.retrieveSessions(source, null, null, null, null, false, PageRequest.of(0, 10));
        Assert.assertEquals(1, latests.getTotalElements());
        Assert.assertEquals(2, olds.getTotalElements());

    }

    @Test
    public void testUpdateSessionProperty() {
        String source = "Source 2";
        String name = OffsetDateTime.now().toString();
        String step = "STEP";
        String property = "PROPERTY";
        long value = 10;
        Assert.assertEquals("Check there is no session saved on DB before running the test", 0,
                            sessionRepository.count());

        SessionMonitoringEvent sessionMonitoringEvent = SessionMonitoringEvent
                .build(source, name, SessionNotificationState.OK, step, SessionNotificationOperator.INC, property,
                       value);
        Session updateSession = sessionService.updateSessionProperty(sessionMonitoringEvent);
        Assert.assertEquals("The session state is OK", SessionState.OK, updateSession.getState());
        Assert.assertEquals("The value is correctly saved in the session", value,
                            updateSession.getStepPropertyValue(step, property));
        Assert.assertEquals("Check there is 1 session saved on DB", 1, sessionRepository.count());

        updateSession = sessionService.updateSessionProperty(sessionMonitoringEvent);
        Assert.assertEquals("The session state is OK", SessionState.OK, updateSession.getState());
        Assert.assertEquals("The value is correctly saved in the session", 2 * value,
                            updateSession.getStepPropertyValue(step, property));

        long decValue = 5;
        sessionMonitoringEvent = SessionMonitoringEvent.build(source, name, SessionNotificationState.OK, step,
                                                              SessionNotificationOperator.DEC, property, decValue);
        updateSession = sessionService.updateSessionProperty(sessionMonitoringEvent);
        Assert.assertEquals("The session state is OK", SessionState.OK, updateSession.getState());
        Assert.assertEquals("The value is correctly saved in the session", 15L,
                            updateSession.getStepPropertyValue(step, property));

        sessionMonitoringEvent = SessionMonitoringEvent.build(source, name, SessionNotificationState.ERROR, step,
                                                              SessionNotificationOperator.DEC, property, decValue * 5);
        updateSession = sessionService.updateSessionProperty(sessionMonitoringEvent);

        Assert.assertEquals("The session state is now in ERROR", SessionState.ERROR, updateSession.getState());
        Assert.assertEquals("The value is correctly saved in the session", -10L,
                            updateSession.getStepPropertyValue(step, property));

        sessionMonitoringEvent = SessionMonitoringEvent.build(source, name, SessionNotificationState.OK, step,
                                                              SessionNotificationOperator.REPLACE, property, 15L);
        updateSession = sessionService.updateSessionProperty(sessionMonitoringEvent);

        Assert.assertEquals("The session state is still in ERROR", SessionState.ERROR, updateSession.getState());
        Assert.assertEquals("The value have been replaced", 15L, updateSession.getStepPropertyValue(step, property));

        String einstein = "E = mc2";
        sessionMonitoringEvent = SessionMonitoringEvent.build(source, name, SessionNotificationState.OK, step,
                                                              SessionNotificationOperator.REPLACE, property, einstein);
        updateSession = sessionService.updateSessionProperty(sessionMonitoringEvent);

        Assert.assertEquals("The session state is still in ERROR", SessionState.ERROR, updateSession.getState());
        Assert.assertEquals("The value have been replaced", einstein,
                            updateSession.getStepPropertyValue(step, property));

        // Now the value is text type, let's try to add a number to this text
        sessionMonitoringEvent = SessionMonitoringEvent.build(source, name, SessionNotificationState.OK, step,
                                                              SessionNotificationOperator.INC, property, value);
        updateSession = sessionService.updateSessionProperty(sessionMonitoringEvent);

        Assert.assertEquals("The session state is still in ERROR", SessionState.ERROR, updateSession.getState());
        Assert.assertEquals("The value is replaced by the value", value,
                            updateSession.getStepPropertyValue(step, property));
    }

    private Session generateErrorSession() {
        Session session = new Session("Source 1", OffsetDateTime.now().toString());
        session.setState(SessionState.ERROR);
        session = sessionRepository.save(session);
        return session;
    }

    private Session generateSession() {
        Session session = new Session("Source 2", OffsetDateTime.now().toString());
        session = sessionRepository.save(session);
        return session;
    }
}
