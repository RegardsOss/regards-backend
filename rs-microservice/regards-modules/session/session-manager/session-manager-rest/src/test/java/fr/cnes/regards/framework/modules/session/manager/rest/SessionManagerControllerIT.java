/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.session.manager.rest;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.StepState;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionDeleteEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SourceDeleteEvent;
import fr.cnes.regards.framework.modules.session.manager.dao.ISessionManagerRepository;
import fr.cnes.regards.framework.modules.session.manager.domain.Session;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Test for {@link SessionManagerController}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=session_controller_it" })
public class SessionManagerControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private ISessionManagerRepository sessionRepo;

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    @Autowired
    protected IPublisher publisher;

    @Before
    public void init() {
        this.tenantResolver.forceTenant(getDefaultTenant());
        this.sessionStepRepo.deleteAll();
        this.sessionRepo.deleteAll();
        Mockito.clearInvocations(publisher);
    }

    @Test
    public void getSessionsTest() {
        List<Session> sessionList = createSessions();

        // return all sessions
        performDefaultGet(SessionManagerController.ROOT_MAPPING,
                          customizer().expectStatusOk().expectValue("$.metadata" + ".totalElements", 7),
                          "Wrong number of sessions returned");

        // search for session 1
        RequestBuilderCustomizer customizer1 = customizer();
        customizer1.addParameter("sessionName", "SESSION_1");
        customizer1.expectStatusOk();
        customizer1.expectValue("$.metadata.totalElements", 6);
        performDefaultGet(SessionManagerController.ROOT_MAPPING, customizer1, "The session expected was not returned");

        // search for state = errors
        RequestBuilderCustomizer customizer2 = customizer();
        customizer2.addParameter("sessionState", "errors");
        customizer2.expectStatusOk();
        customizer2.expectValue("$.metadata.totalElements", 1);
        customizer2.expectValue("$.content.[0].content.id", sessionList.get(0).getId());
        performDefaultGet(SessionManagerController.ROOT_MAPPING, customizer2, "The session expected was not returned");

        // search for state = waiting
        RequestBuilderCustomizer customizer3 = customizer();
        customizer3.addParameter("sessionState", "waiting");
        customizer3.expectStatusOk();
        customizer3.expectValue("$.metadata.totalElements", 1);
        customizer3.expectValue("$.content.[0].content.id", sessionList.get(1).getId());
        performDefaultGet(SessionManagerController.ROOT_MAPPING, customizer3, "The session expected was not returned");

        // search for state = waiting
        RequestBuilderCustomizer customizer4 = customizer();
        customizer4.addParameter("sessionState", "running");
        customizer4.expectStatusOk();
        customizer4.expectValue("$.metadata.totalElements", 1);
        customizer4.expectValue("$.content.[0].content.id", sessionList.get(2).getId());
        performDefaultGet(SessionManagerController.ROOT_MAPPING, customizer4, "The session expected was not returned");

        // search for state = ok
        RequestBuilderCustomizer customizer5 = customizer();
        customizer5.addParameter("sessionState", "ok");
        customizer5.expectStatusOk();
        customizer5.expectValue("$.metadata.totalElements", 4);
        customizer5.expectValue("$.content.[0].content.id", sessionList.get(3).getId());
        customizer5.expectValue("$.content.[1].content.id", sessionList.get(4).getId());
        customizer5.expectValue("$.content.[2].content.id", sessionList.get(5).getId());
        customizer5.expectValue("$.content.[3].content.id", sessionList.get(6).getId());
        performDefaultGet(SessionManagerController.ROOT_MAPPING, customizer5, "The session expected was not returned");

        // search for source = SOURCE_5
        RequestBuilderCustomizer customizer6 = customizer();
        customizer6.addParameter("sourceName", "SOURCE_5");
        customizer6.expectStatusOk();
        customizer6.expectValue("$.metadata.totalElements", 2);
        customizer6.expectValue("$.content.[0].content.id", sessionList.get(5).getId());
        customizer6.expectValue("$.content.[1].content.id", sessionList.get(6).getId());
        performDefaultGet(SessionManagerController.ROOT_MAPPING, customizer6, "The session expected was not returned");

        // search with combined filters
        RequestBuilderCustomizer customizer7 = customizer();
        customizer7.addParameter("sourceName", "SOURCE_5");
        customizer7.addParameter("sessionName", "SESSION_2");
        customizer7.expectStatusOk();
        customizer7.expectValue("$.metadata.totalElements", 1);
        customizer7.expectValue("$.content.[0].content.id", sessionList.get(6).getId());
        performDefaultGet(SessionManagerController.ROOT_MAPPING, customizer7, "The session expected was not returned");
    }

    @Test
    @Purpose("Test the deletion of a session")
    public void deleteSession() {
        List<Session> sessionList = createSessions();
        performDefaultDelete(SessionManagerController.ROOT_MAPPING + SessionManagerController.ID_MAPPING,
                             customizer().expectStatusOk(),
                             "The order to delete a session was not published",
                             sessionList.get(0).getId());

        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(SessionDeleteEvent.class));
    }

    @Test
    @Purpose("Test the deletion of a not existing session")
    public void deleteNotExistingSource() {
        performDefaultDelete(SessionManagerController.ROOT_MAPPING + SessionManagerController.ID_MAPPING,
                             customizer().expectStatus(HttpStatus.NOT_FOUND),
                             "The order to delete a session was published but the session does not exist",
                             156464635132L);

        Mockito.verify(publisher, Mockito.times(0)).publish(Mockito.any(SourceDeleteEvent.class));
    }

    @Test
    @Purpose("Test retrieve session names")
    public void getSessionsNames() {
        int nbSessions = 11;
        List<Session> sessionList = createSessionName(nbSessions);

        // retrieve all sessions, should be limited
        RequestBuilderCustomizer customizer0 = customizer();
        customizer0.expectStatusOk();
        customizer0.expectToHaveSize("$", ISessionManagerRepository.MAX_SESSION_NAMES_RESULTS);
        performDefaultGet(SessionManagerController.ROOT_MAPPING + SessionManagerController.NAME_MAPPING,
                          customizer0,
                          "All sessions were not retrieved with limited parameter");

        // retrieve unique session
        RequestBuilderCustomizer customizer1 = customizer();
        customizer1.addParameter("name", sessionList.get(0).getName());
        customizer1.expectStatusOk();
        customizer1.expectValue("$.[0]", sessionList.get(0).getName());

        performDefaultGet(SessionManagerController.ROOT_MAPPING + SessionManagerController.NAME_MAPPING,
                          customizer1,
                          "The wrong session name was retrieved");

        // retrieve session duplicated, only one name should be present
        RequestBuilderCustomizer customizer2 = customizer();
        customizer2.addParameter("name", sessionList.get(nbSessions).getName());
        customizer2.expectStatusOk();
        customizer2.expectValue("$.[0]", sessionList.get(nbSessions).getName());

        performDefaultGet(SessionManagerController.ROOT_MAPPING + SessionManagerController.NAME_MAPPING,
                          customizer2,
                          "The wrong session name was retrieved");
    }

    @Test
    @Purpose("Test retrieve a session by id")
    public void getSessionById() {
        List<Session> sessionList = createSessions();
        long testedId = sessionList.get(0).getId();
        performDefaultGet(SessionManagerController.ROOT_MAPPING + SessionManagerController.ID_MAPPING,
                          customizer().expectStatusOk().expectValue("$.content.id", testedId),
                          "The session was not retrieved",
                          testedId);
    }

    /**
     * init sessions
     */
    private List<Session> createSessions() {
        List<Session> sessionList = new ArrayList<>();
        // create a list of sessions with basics settings
        for (int i = 0; i < 6; i++) {
            String sourceName = "SOURCE_" + i;
            String sessionName = "SESSION_1";
            // create sessionStep
            SessionStep sessionStep = new SessionStep("oais",
                                                      sourceName,
                                                      sessionName,
                                                      StepTypeEnum.REFERENCING,
                                                      new StepState());
            sessionStep.setLastUpdateDate(OffsetDateTime.now());
            // create session
            Session session = new Session(sourceName, sessionName);
            session.setSteps(Sets.newHashSet(sessionStep));
            sessionList.add(session);
        }

        // copy last element of list to add session linked to same source
        SessionStep sessionStep = new SessionStep("oais",
                                                  "SOURCE_5",
                                                  "SESSION_2",
                                                  StepTypeEnum.REFERENCING,
                                                  new StepState());
        sessionStep.setLastUpdateDate(OffsetDateTime.now());
        Session session6 = new Session("SOURCE_5", "SESSION_2");
        session6.setSteps(Sets.newHashSet(sessionStep));
        sessionList.add(session6);

        // modify parameters to test filters
        sessionList.get(0).getManagerState().setErrors(true);
        sessionList.get(1).getManagerState().setWaiting(true);
        sessionList.get(2).getManagerState().setRunning(true);

        return this.sessionRepo.saveAll(sessionList);
    }

    /**
     * Init sessions for names
     */
    private List<Session> createSessionName(int nbSessions) {
        List<Session> sessionList = new ArrayList<>();

        for (int i = 0; i < nbSessions; i++) {
            sessionList.add(new Session("SOURCE_" + i, "SESSION_" + i));
        }
        // create session with duplicated name
        sessionList.add(new Session("SOURCE_" + nbSessions, "SESSION_" + (nbSessions - 1)));
        return this.sessionRepo.saveAll(sessionList);
    }
}