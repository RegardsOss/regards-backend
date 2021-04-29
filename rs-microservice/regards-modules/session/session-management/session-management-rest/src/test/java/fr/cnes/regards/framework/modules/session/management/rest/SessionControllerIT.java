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
package fr.cnes.regards.framework.modules.session.management.rest;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.StepState;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionDeleteEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SourceDeleteEvent;
import fr.cnes.regards.framework.modules.session.management.dao.ISessionRepository;
import fr.cnes.regards.framework.modules.session.management.domain.Session;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for {@link SessionController}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=session_controller_it" })
public class SessionControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private ISessionRepository sessionRepo;

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    @SpyBean
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
        performDefaultGet(SessionController.ROOT_MAPPING,
                          customizer().expectStatusOk().expectValue("$.metadata" + ".totalElements", 7),
                          "Wrong number of sessions returned");

        // search for session 1
        RequestBuilderCustomizer customizer1 = customizer();
        customizer1.addParameter("name", "SESSION_1");
        customizer1.expectStatusOk();
        customizer1.expectValue("$.metadata.totalElements", 6);
        performDefaultGet(SessionController.ROOT_MAPPING, customizer1, "The session expected was not returned");

        // search for state = error
        RequestBuilderCustomizer customizer2 = customizer();
        customizer2.addParameter("state", "error");
        customizer2.expectStatusOk();
        customizer2.expectValue("$.metadata.totalElements", 1);
        customizer2.expectValue("$.content.[0].content.id", sessionList.get(0).getId());
        performDefaultGet(SessionController.ROOT_MAPPING, customizer2, "The session expected was not returned");

        // search for state = waiting
        RequestBuilderCustomizer customizer3 = customizer();
        customizer3.addParameter("state", "waiting");
        customizer3.expectStatusOk();
        customizer3.expectValue("$.metadata.totalElements", 1);
        customizer3.expectValue("$.content.[0].content.id", sessionList.get(1).getId());
        performDefaultGet(SessionController.ROOT_MAPPING, customizer3, "The session expected was not returned");

        // search for state = waiting
        RequestBuilderCustomizer customizer4 = customizer();
        customizer4.addParameter("state", "running");
        customizer4.expectStatusOk();
        customizer4.expectValue("$.metadata.totalElements", 1);
        customizer4.expectValue("$.content.[0].content.id", sessionList.get(2).getId());
        performDefaultGet(SessionController.ROOT_MAPPING, customizer4, "The session expected was not returned");

        // search for state = ok
        RequestBuilderCustomizer customizer5 = customizer();
        customizer5.addParameter("state", "ok");
        customizer5.expectStatusOk();
        customizer5.expectValue("$.metadata.totalElements", 4);
        customizer5.expectValue("$.content.[0].content.id", sessionList.get(3).getId());
        customizer5.expectValue("$.content.[1].content.id", sessionList.get(4).getId());
        customizer5.expectValue("$.content.[2].content.id", sessionList.get(5).getId());
        customizer5.expectValue("$.content.[3].content.id", sessionList.get(6).getId());
        performDefaultGet(SessionController.ROOT_MAPPING, customizer5, "The session expected was not returned");

        // search for source = SOURCE_5
        RequestBuilderCustomizer customizer6 = customizer();
        customizer6.addParameter("source", "SOURCE_5");
        customizer6.expectStatusOk();
        customizer6.expectValue("$.metadata.totalElements", 2);
        customizer6.expectValue("$.content.[0].content.id", sessionList.get(5).getId());
        customizer6.expectValue("$.content.[1].content.id", sessionList.get(6).getId());
        performDefaultGet(SessionController.ROOT_MAPPING, customizer6, "The session expected was not returned");

        // search with combined filters
        RequestBuilderCustomizer customizer7 = customizer();
        customizer7.addParameter("source", "SOURCE_5");
        customizer7.addParameter("name", "SESSION_2");
        customizer7.expectStatusOk();
        customizer7.expectValue("$.metadata.totalElements", 1);
        customizer7.expectValue("$.content.[0].content.id", sessionList.get(6).getId());
        performDefaultGet(SessionController.ROOT_MAPPING, customizer7, "The session expected was not returned");
    }

    @Test
    @Purpose("Test the deletion of a session")
    public void deleteSession() {
        List<Session> sessionList = createSessions();
        performDefaultDelete(SessionController.ROOT_MAPPING + SessionController.DELETE_SESSION_MAPPING,
                             customizer().expectStatusOk(), "The order to delete a session was not published",
                             sessionList.get(0).getId());

        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(SessionDeleteEvent.class));
    }

    @Test
    @Purpose("Test the deletion of a not existing session")
    public void deleteNotExistingSource() {
        performDefaultDelete(SessionController.ROOT_MAPPING + SessionController.DELETE_SESSION_MAPPING,
                             customizer().expectStatus(HttpStatus.NOT_FOUND),
                             "The order to delete a session was published but the session does not exist",
                             156464635132L);

        Mockito.verify(publisher, Mockito.times(0)).publish(Mockito.any(SourceDeleteEvent.class));
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
            Set<SessionStep> sessionStepSet = Sets.newHashSet(
                    new SessionStep("oais", sourceName, sessionName, StepTypeEnum.REFERENCING, new StepState(), null));
            Session session = new Session(sourceName, sessionName);
            session.setSteps(sessionStepSet);
            sessionList.add(session);
        }

        // copy last element of list to add session linked to same source
        Set<SessionStep> sessionStepSet6 = Sets.newHashSet(
                new SessionStep("oais", "SOURCE_5", "SESSION_2", StepTypeEnum.REFERENCING, new StepState(), null));
        Session session6 = new Session("SOURCE_5", "SESSION_2");
        session6.setSteps(sessionStepSet6);
        sessionList.add(session6);

        // modify parameters to test filters
        sessionList.get(0).getManagerState().setError(true);
        sessionList.get(1).getManagerState().setWaiting(true);
        sessionList.get(2).getManagerState().setRunning(true);

        return this.sessionRepo.saveAll(sessionList);
    }
}