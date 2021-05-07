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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.assertj.core.util.Lists;
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
import fr.cnes.regards.modules.sessionmanager.domain.SessionAdmin;
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
        SessionAdmin session = generateErrorSession("session1");
        SessionAdmin sessionUpdated = sessionService.updateSessionState(session.getId(), SessionState.ACKNOWLEDGED);
        Assert.assertEquals("Should state be updated to Acknowledged", sessionUpdated.getState(),
                            SessionState.ACKNOWLEDGED);
    }

    @Test(expected = ModuleException.class)
    public void testInvalidUpdateState() throws ModuleException {
        SessionAdmin session = generateErrorSession("session1");
        // Set the session state from ERROR to OK
        sessionService.updateSessionState(session.getId(), SessionState.OK);
    }

    @Test
    public void testDelete() throws ModuleException, InterruptedException {
        SessionAdmin session = generateErrorSession("session1");
        Assert.assertTrue(session.isLatest());
        Thread.sleep(1_000);
        SessionAdmin session2 = generateErrorSession("session2");
        Assert.assertTrue(session2.isLatest());
        Assert.assertFalse(sessionRepository.findById(session.getId()).get().isLatest());

        sessionService.deleteSession(session.getId(), false);
        Optional<SessionAdmin> sessionOpt = sessionRepository.findById(session.getId());
        Assert.assertTrue("Session should still exists", sessionOpt.isPresent());
        Assert.assertEquals("When deleted with force=false, the session state is DELETED", SessionState.DELETED,
                            sessionOpt.get().getState());
        // After deletion the session should be set as latest updated session
        Assert.assertTrue(sessionRepository.findById(session.getId()).get().isLatest());
        Assert.assertFalse(sessionRepository.findById(session2.getId()).get().isLatest());
    }

    @Test
    public void testForceDelete() throws ModuleException, InterruptedException {
        SessionAdmin session = generateErrorSession("session1");
        Assert.assertTrue(session.isLatest());
        Thread.sleep(1_000);
        SessionAdmin session2 = generateErrorSession("session2");
        Assert.assertTrue(session2.isLatest());
        Assert.assertFalse(sessionRepository.findById(session.getId()).get().isLatest());

        sessionService.deleteSession(session.getId(), true);
        Optional<SessionAdmin> sessionOpt = sessionRepository.findById(session.getId());
        Assert.assertFalse("When deleted with force=true, the session should not exist anymore",
                           sessionOpt.isPresent());
        Assert.assertTrue("is latest flag should be set for remaining session",
                          sessionRepository.findById(session2.getId()).get().isLatest());
    }

    @Test
    public void testCalculateSessionIsLatetest() {

        String source = "testCalculateSessionIsLatetest";
        String name = "name";
        String step = "step";
        String property = "property";
        Page<SessionAdmin> latests = sessionService.retrieveSessions(source, null, null, null, null, true,
                                                                     PageRequest.of(0, 10));
        Page<SessionAdmin> olds = sessionService.retrieveSessions(source, null, null, null, null, false,
                                                                  PageRequest.of(0, 10));
        Assert.assertEquals(0, latests.getTotalElements());
        Assert.assertEquals(0, olds.getTotalElements());
        // Init session
        sessionService.updateSessionProperties(Lists.newArrayList(SessionMonitoringEvent
                .build(source, name, SessionNotificationState.OK, step, SessionNotificationOperator.INC, property, 1)));
        // Check isLatests property
        latests = sessionService.retrieveSessions(source, null, null, null, null, true, PageRequest.of(0, 10));
        olds = sessionService.retrieveSessions(source, null, null, null, null, false, PageRequest.of(0, 10));
        Assert.assertEquals(1, latests.getTotalElements());
        Assert.assertEquals(1, olds.getTotalElements());
        // update same session
        sessionService
                .updateSessionProperties(Lists
                        .newArrayList(SessionMonitoringEvent.build(source, name, SessionNotificationState.OK, step,
                                                                   SessionNotificationOperator.INC, property, 1)))
                .get(0);
        latests = sessionService.retrieveSessions(source, null, null, null, null, true, PageRequest.of(0, 10));
        olds = sessionService.retrieveSessions(source, null, null, null, null, false, PageRequest.of(0, 10));
        Assert.assertEquals(1, latests.getTotalElements());
        Assert.assertEquals(1, olds.getTotalElements());
        // Create new one
        sessionService
                .updateSessionProperties(Lists
                        .newArrayList(SessionMonitoringEvent.build(source, "name2", SessionNotificationState.OK, step,
                                                                   SessionNotificationOperator.INC, property, 1)))
                .get(0);
        latests = sessionService.retrieveSessions(source, null, null, null, null, true, PageRequest.of(0, 10));
        olds = sessionService.retrieveSessions(source, null, null, null, null, false, PageRequest.of(0, 10));
        Assert.assertEquals(1, latests.getTotalElements());
        Assert.assertEquals(2, olds.getTotalElements());

    }

    @Test
    public void testUpdateSessionsProperties() {
        List<SessionMonitoringEvent> events = new ArrayList<>();
        // source1/name1@step1.property1=2
        // source1/name1@step1.property2=10
        events.add(SessionMonitoringEvent.build("source1", "name1", SessionNotificationState.OK, "STEP1",
                                                SessionNotificationOperator.INC, "PROPERTY1", 1));
        events.add(SessionMonitoringEvent.build("source1", "name1", SessionNotificationState.OK, "STEP1",
                                                SessionNotificationOperator.INC, "PROPERTY1", 1));
        events.add(SessionMonitoringEvent.build("source1", "name1", SessionNotificationState.OK, "STEP1",
                                                SessionNotificationOperator.INC, "PROPERTY2", 10));

        // source1/name2@step1.property1=1
        events.add(SessionMonitoringEvent.build("source1", "name2", SessionNotificationState.OK, "STEP1",
                                                SessionNotificationOperator.INC, "PROPERTY1", 1));

        // source1/name2@step2.property1=1
        events.add(SessionMonitoringEvent.build("source1", "name2", SessionNotificationState.OK, "STEP2",
                                                SessionNotificationOperator.INC, "PROPERTY1", 1));

        // source2/name1@step1.property1=2
        events.add(SessionMonitoringEvent.build("source2", "name1", SessionNotificationState.OK, "STEP1",
                                                SessionNotificationOperator.INC, "PROPERTY1", 2));
        events.add(SessionMonitoringEvent.build("source2", "name1", SessionNotificationState.OK, "STEP1",
                                                SessionNotificationOperator.DEC, "PROPERTY1", 1));
        // lets add a trap and put a REPLACE in the middle of everything
        events.add(SessionMonitoringEvent.build("source2", "name1", SessionNotificationState.OK, "STEP1",
                                                SessionNotificationOperator.REPLACE, "PROPERTY1", 9));
        // for the trap to work properly, lets decrease so we get back to previous value:(2-1)=> 9-(2-1)=8 times
        for (int i = 0; i < 8; i++) {
            events.add(SessionMonitoringEvent.build("source2", "name1", SessionNotificationState.OK, "STEP1",
                                                    SessionNotificationOperator.DEC, "PROPERTY1", 1));
        }
        events.add(SessionMonitoringEvent.build("source2", "name1", SessionNotificationState.OK, "STEP1",
                                                SessionNotificationOperator.INC, "PROPERTY1", 1));
        sessionService.updateSessionProperties(events);

        Optional<SessionAdmin> session = sessionRepository.findOneBySourceAndName("source1", "name1");
        Assert.assertTrue(session.isPresent());
        Assert.assertEquals(2L, session.get().getStepPropertyValue("STEP1", "PROPERTY1"));
        Assert.assertEquals(10L, session.get().getStepPropertyValue("STEP1", "PROPERTY2"));
        session = sessionRepository.findOneBySourceAndName("source1", "name2");
        Assert.assertTrue(session.isPresent());
        Assert.assertEquals(1L, session.get().getStepPropertyValue("STEP1", "PROPERTY1"));
        Assert.assertEquals(1L, session.get().getStepPropertyValue("STEP2", "PROPERTY1"));
        session = sessionRepository.findOneBySourceAndName("source2", "name1");
        Assert.assertTrue(session.isPresent());
        Assert.assertEquals(2L, session.get().getStepPropertyValue("STEP1", "PROPERTY1"));

    }

    @Test
    public void testUpdateSessionsPropertiesWithGlobalEvent() {
        List<SessionMonitoringEvent> events = new ArrayList<>();
        // source1/name1@step1.property1=2
        // source1/name1@step1.property2=10
        events.add(SessionMonitoringEvent.build("source1", "name1", SessionNotificationState.OK, "STEP1",
                                                SessionNotificationOperator.INC, "PROPERTY1", 1));
        events.add(SessionMonitoringEvent.build("source1", "name1", SessionNotificationState.OK, "STEP1",
                                                SessionNotificationOperator.INC, "PROPERTY1", 1));
        events.add(SessionMonitoringEvent.build("source1", "name1", SessionNotificationState.OK, "STEP1",
                                                SessionNotificationOperator.INC, "PROPERTY2", 10));

        // source1/name2@step1.property1=1
        events.add(SessionMonitoringEvent.build("source1", "name2", SessionNotificationState.OK, "STEP1",
                                                SessionNotificationOperator.INC, "PROPERTY1", 1));

        // source1/name2@step2.property1=1
        events.add(SessionMonitoringEvent.build("source1", "name2", SessionNotificationState.OK, "STEP2",
                                                SessionNotificationOperator.INC, "PROPERTY1", 1));

        // source2/name1@step1.property1=2
        events.add(SessionMonitoringEvent.build("source2", "name1", SessionNotificationState.OK, "STEP1",
                                                SessionNotificationOperator.INC, "PROPERTY1", 2));
        events.add(SessionMonitoringEvent.build("source2", "name1", SessionNotificationState.OK, "STEP1",
                                                SessionNotificationOperator.DEC, "PROPERTY1", 1));
        // lets add a trap and put a REPLACE global in the middle of everything ONLY FOR STEP1, PROPERTY1 of all sessions
        events.add(SessionMonitoringEvent.buildGlobal(SessionNotificationState.OK, "STEP1",
                                                      SessionNotificationOperator.REPLACE, "PROPERTY1", 9));
        // for the trap to work properly, lets decrease, only for source2/name1@step1.property1 so we get back to previous value:(2-1)=> 9-(2-1)=8 times
        for (int i = 0; i < 8; i++) {
            events.add(SessionMonitoringEvent.build("source2", "name1", SessionNotificationState.OK, "STEP1",
                                                    SessionNotificationOperator.DEC, "PROPERTY1", 1));
        }
        events.add(SessionMonitoringEvent.build("source2", "name1", SessionNotificationState.OK, "STEP1",
                                                SessionNotificationOperator.INC, "PROPERTY1", 1));
        sessionService.updateSessionProperties(events);

        Optional<SessionAdmin> session = sessionRepository.findOneBySourceAndName("source1", "name1");
        Assert.assertTrue(session.isPresent());
        Assert.assertEquals(9L, session.get().getStepPropertyValue("STEP1", "PROPERTY1"));
        Assert.assertEquals(10L, session.get().getStepPropertyValue("STEP1", "PROPERTY2"));
        session = sessionRepository.findOneBySourceAndName("source1", "name2");
        Assert.assertTrue(session.isPresent());
        Assert.assertEquals(9L, session.get().getStepPropertyValue("STEP1", "PROPERTY1"));
        Assert.assertEquals(1L, session.get().getStepPropertyValue("STEP2", "PROPERTY1"));
        session = sessionRepository.findOneBySourceAndName("source2", "name1");
        Assert.assertTrue(session.isPresent());
        // Thats 2 because we put every STEP1.PROPERTY1 to 9 but we decreased only on this session by 2 times before incrementing by 1
        Assert.assertEquals(2L, session.get().getStepPropertyValue("STEP1", "PROPERTY1"));

    }

    @Test
    public void testupdateSessionProperties() {
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
        SessionAdmin updateSession = sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent))
                .get(0);
        Assert.assertEquals("The session state is OK", SessionState.OK, updateSession.getState());
        Assert.assertEquals("The value is correctly saved in the session", value,
                            updateSession.getStepPropertyValue(step, property));
        Assert.assertEquals("Check there is 1 session saved on DB", 1, sessionRepository.count());

        updateSession = sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);
        Assert.assertEquals("The session state is OK", SessionState.OK, updateSession.getState());
        Assert.assertEquals("The value is correctly saved in the session", 2 * value,
                            updateSession.getStepPropertyValue(step, property));

        long decValue = 5;
        sessionMonitoringEvent = SessionMonitoringEvent.build(source, name, SessionNotificationState.OK, step,
                                                              SessionNotificationOperator.DEC, property, decValue);
        updateSession = sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);
        Assert.assertEquals("The session state is OK", SessionState.OK, updateSession.getState());
        Assert.assertEquals("The value is correctly saved in the session", 15L,
                            updateSession.getStepPropertyValue(step, property));

        sessionMonitoringEvent = SessionMonitoringEvent.build(source, name, SessionNotificationState.ERROR, step,
                                                              SessionNotificationOperator.DEC, property, decValue * 5);
        updateSession = sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);

        Assert.assertEquals("The session state is now in ERROR", SessionState.ERROR, updateSession.getState());
        Assert.assertEquals("The value is correctly saved in the session", -10L,
                            updateSession.getStepPropertyValue(step, property));

        sessionMonitoringEvent = SessionMonitoringEvent.build(source, name, SessionNotificationState.OK, step,
                                                              SessionNotificationOperator.REPLACE, property, 15L);
        updateSession = sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);

        Assert.assertEquals("The session state is still in ERROR", SessionState.ERROR, updateSession.getState());
        Assert.assertEquals("The value have been replaced", 15L, updateSession.getStepPropertyValue(step, property));

        String einstein = "E = mc2";
        sessionMonitoringEvent = SessionMonitoringEvent.build(source, name, SessionNotificationState.OK, step,
                                                              SessionNotificationOperator.REPLACE, property, einstein);
        updateSession = sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);

        Assert.assertEquals("The session state is still in ERROR", SessionState.ERROR, updateSession.getState());
        Assert.assertEquals("The value have been replaced", einstein,
                            updateSession.getStepPropertyValue(step, property));

        // Now the value is text type, let's try to add a number to this text
        sessionMonitoringEvent = SessionMonitoringEvent.build(source, name, SessionNotificationState.OK, step,
                                                              SessionNotificationOperator.INC, property, value);
        updateSession = sessionService.updateSessionProperties(Lists.newArrayList(sessionMonitoringEvent)).get(0);

        Assert.assertEquals("The session state is still in ERROR", SessionState.ERROR, updateSession.getState());
        Assert.assertEquals("The value is replaced by the value", value,
                            updateSession.getStepPropertyValue(step, property));
    }

    private SessionAdmin generateErrorSession(String session) {
        List<SessionMonitoringEvent> events = new ArrayList<>();
        events.add(SessionMonitoringEvent.build("Source 1", session, SessionNotificationState.ERROR, "STEP",
                                                SessionNotificationOperator.INC, "PROPERTY", "1"));
        return sessionService.updateSessionProperties(events).get(0);
    }
}
