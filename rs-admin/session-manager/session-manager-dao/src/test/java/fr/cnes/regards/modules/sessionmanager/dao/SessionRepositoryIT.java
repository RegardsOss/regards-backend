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
package fr.cnes.regards.modules.sessionmanager.dao;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.modules.sessionmanager.domain.SessionAdmin;
import fr.cnes.regards.modules.sessionmanager.domain.SessionState;

/**
 * Testing Session DAO
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=session_dao" })
public class SessionRepositoryIT extends AbstractMultitenantServiceTest {

    @Autowired
    private ISessionRepository sessionRepo;

    @Before
    public void beforeTest() {
        sessionRepo.deleteAll();
    }

    @Test
    public void testCreateSession() {
        SessionAdmin session1 = new SessionAdmin("Acquisition chain number one", OffsetDateTime.now().toString());
        sessionRepo.save(session1);
        Assert.assertTrue(session1.getId() > 0);
    }

    @Test
    public void testFilters() {
        // Add some basic sessions
        SessionAdmin session1 = new SessionAdmin("Ok1", OffsetDateTime.now().toString());
        sessionRepo.save(session1);
        SessionAdmin session2 = new SessionAdmin("Ok2", OffsetDateTime.now().toString());
        sessionRepo.save(session2);
        OffsetDateTime oldDate = OffsetDateTime.now().minusMonths(50);
        SessionAdmin oldSession = new SessionAdmin("oldSession", oldDate.toString());
        oldSession.setCreationDate(oldDate);
        sessionRepo.save(oldSession);

        SessionAdmin sessionError1 = new SessionAdmin("Error1", OffsetDateTime.now().toString());
        sessionError1.setCreationDate(OffsetDateTime.now().minusMonths(25));
        sessionError1.setState(SessionState.ERROR);
        sessionRepo.save(sessionError1);
        SessionAdmin sessionError2 = new SessionAdmin("Error2", OffsetDateTime.now().toString());
        sessionError2.setState(SessionState.ERROR);
        sessionRepo.save(sessionError2);
        SessionAdmin sessionError3 = new SessionAdmin("Error3", OffsetDateTime.now().toString());
        sessionError3.setState(SessionState.ERROR);
        sessionRepo.save(sessionError3);

        // Create some sessions sharing the source but with the flag latest to false
        SessionAdmin notLast = new SessionAdmin("Error3", OffsetDateTime.now().minusMinutes(2).toString());
        notLast.setState(SessionState.ERROR);
        notLast.setLatest(false);
        sessionRepo.save(notLast);
        SessionAdmin notLast2 = new SessionAdmin("Error3", OffsetDateTime.now().minusMinutes(5).toString());
        notLast2.setState(SessionState.ERROR);
        notLast2.setLatest(false);
        sessionRepo.save(notLast2);

        SessionAdmin sessionAcknowledge = new SessionAdmin("sessionAcknowledge", OffsetDateTime.now().toString());
        sessionAcknowledge.setCreationDate(OffsetDateTime.now().minusMonths(12));
        sessionAcknowledge.setState(SessionState.ACKNOWLEDGED);
        sessionRepo.save(sessionAcknowledge);
        SessionAdmin sessionDelete = new SessionAdmin("sessionDelete", OffsetDateTime.now().toString());
        sessionDelete.setState(SessionState.DELETED);
        sessionRepo.save(sessionDelete);

        // Test retrieving only session marked as error
        Page<SessionAdmin> sessionInError = sessionRepo
                .findAll(SessionSpecifications.search(null, null, null, null, SessionState.ERROR, false),
                         PageRequest.of(0, 100));
        Assert.assertEquals("Should return all sessions marked as error", 5, sessionInError.getTotalElements());

        // Test retrieving only session marked as error and latest
        Page<SessionAdmin> sessionInError2 = sessionRepo
                .findAll(SessionSpecifications.search(null, null, null, null, SessionState.ERROR, true),
                         PageRequest.of(0, 100));
        Assert.assertEquals("Should return all latest sessions marked as error", 3, sessionInError2.getTotalElements());

        // Test retrieving a different SessionState
        Page<SessionAdmin> sessionAcknowledged = sessionRepo
                .findAll(SessionSpecifications.search(null, null, null, null, SessionState.ACKNOWLEDGED, false),
                         PageRequest.of(0, 100));
        Assert.assertEquals("Should return the only session marked as acknowledged", 1,
                            sessionAcknowledged.getTotalElements());

        // Test research by date
        Page<SessionAdmin> oldSessions = sessionRepo
                .findAll(SessionSpecifications.search(null, null, OffsetDateTime.now().minusYears(10),
                                                      OffsetDateTime.now().minusMonths(6), null, false),
                         PageRequest.of(0, 100));
        Assert.assertEquals("Should return all sessions with an old creation date", 3, oldSessions.getTotalElements());

        // Test research by source, containing the current year in the name and latest from their source
        Page<SessionAdmin> sessionMatchingSourceAndName = sessionRepo.findAll(
                                                                         SessionSpecifications
                                                                                 .search("error",
                                                                                         String.valueOf(OffsetDateTime
                                                                                                 .now().getYear()),
                                                                                         null, null, null, true),
                                                                         PageRequest.of(0, 100));
        Assert.assertEquals("Should return all sessions matching the source and name", 3,
                            sessionMatchingSourceAndName.getTotalElements());

        // All in one
        Page<SessionAdmin> researchAllInOne = sessionRepo.findAll(
                                                             SessionSpecifications
                                                                     .search("error",
                                                                             String.valueOf(OffsetDateTime.now()
                                                                                     .getYear()),
                                                                             OffsetDateTime.now().minusYears(10),
                                                                             OffsetDateTime.now().minusMonths(6),
                                                                             SessionState.ERROR, true),
                                                             PageRequest.of(0, 100));
        Assert.assertEquals("Should return one session matching all criteria in the same time", 1,
                            researchAllInOne.getTotalElements());
    }

    @Test
    public void testUnflagPreviousSessionFromLastestFlag() {

        String sessionSource = "SESSION_1_2_3";
        SessionAdmin sessionError3 = new SessionAdmin(sessionSource, OffsetDateTime.now().toString());
        sessionError3.setState(SessionState.ERROR);
        sessionRepo.save(sessionError3);

        Optional<SessionAdmin> sessionQuery = sessionRepo.findOneBySourceAndIsLatestTrue(sessionSource);

        Assert.assertEquals("Should return the entity previously created", true, sessionQuery.isPresent());

        sessionError3.setLatest(false);
        sessionRepo.save(sessionError3);

        sessionQuery = sessionRepo.findOneBySourceAndIsLatestTrue(sessionSource);
        Assert.assertEquals("Should not retrieve any result", false, sessionQuery.isPresent());
    }

    @Test
    public void testSearchSessionByName() {
        String sessionName = "SESSION_1_2_3";
        SessionAdmin session1 = new SessionAdmin("Source", sessionName);
        sessionRepo.save(session1);

        List<String> query1 = sessionRepo.findAllSessionName(sessionName);
        Assert.assertEquals("Should retrieve 1 result", 1, query1.size());
        Assert.assertEquals("Should retrieve the session name", sessionName, query1.get(0));

        List<String> query2 = sessionRepo.findAllSessionName("Sess");
        Assert.assertEquals("Should be case insensitive", 1, query2.size());

        List<String> queryEmpty = sessionRepo.findAllSessionName("");
        Assert.assertEquals("Should be case insensitive", 1, queryEmpty.size());
    }

    @Test
    public void testSearchSessionBySource() {
        String sourceName = "Source 1 2 3";
        SessionAdmin session1 = new SessionAdmin(sourceName, OffsetDateTime.now().toString());
        sessionRepo.save(session1);

        List<String> query1 = sessionRepo.findAllSessionSource(sourceName);
        Assert.assertEquals("Should retrieve 1 result", 1, query1.size());
        Assert.assertEquals("Should retrieve the session name", sourceName, query1.get(0));

        List<String> query2 = sessionRepo.findAllSessionSource("SOUR");
        Assert.assertEquals("Should be case insensitive", 1, query2.size());

        List<String> queryEmpty = sessionRepo.findAllSessionSource("");
        Assert.assertEquals("Should be case insensitive", 1, queryEmpty.size());
    }

    @Test
    public void testSearchLatestSession() {
        String sourceName = "Source 1 2 3";
        String name = OffsetDateTime.now().toString();
        SessionAdmin session1 = new SessionAdmin(sourceName, name);
        sessionRepo.save(session1);

        String name2 = OffsetDateTime.now().toString();
        SessionAdmin session2 = new SessionAdmin(sourceName, name2);
        session2.setLatest(false);
        sessionRepo.save(session2);

        List<SessionAdmin> results = sessionRepo
                .findAll(SessionSpecifications.search(sourceName, null, null, null, null, true));
        Assert.assertEquals(1, results.size());
    }

}
