/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.session.manager.service.handlers;

import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.StepState;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionStepEvent;
import fr.cnes.regards.framework.modules.session.manager.service.AbstractManagerServiceUtilsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for {@link SessionManagerHandler}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=manager_session_service_it"})
@ActiveProfiles({ "testAmqp", "noscheduler" })
public class SessionManagerHandlerIT extends AbstractManagerServiceUtilsIT {

    private static final OffsetDateTime LAST_UPDATED = OffsetDateTime.now(ZoneOffset.UTC);

    @Test
    @Purpose("Test if session step is stored with its associated snapshot process")
    public void createSessionStepTest() throws InterruptedException {
        // CREATE FIRST SESSION STEPS EVENT
        createRunStepEvents();
        Thread.sleep(10000L);
        // check first event is created
        Optional<SessionStep> sessionStep1Opt = this.sessionStepRepo
                .findBySourceAndSessionAndStepId(SOURCE_1, SESSION_1, "scan");
        Assert.assertTrue("Session step should have been created", sessionStep1Opt.isPresent());
        SessionStep sessionStep1 = sessionStep1Opt.get();
        Assert.assertEquals("Wrong property", 2L, sessionStep1.getState().getRunning());
        // check second event is created
        Optional<SessionStep> sessionStep2Opt = this.sessionStepRepo
                .findBySourceAndSessionAndStepId(SOURCE_2, SESSION_1, "scan");
        Assert.assertTrue("Session step should have been created", sessionStep2Opt.isPresent());

        // UPDATE FIRST SESSION STEP EVENT
        createRun2StepEvents();
        Thread.sleep(10000L);
        // check same event is updated
        Optional<SessionStep> sessionStep1UpdatedOpt = this.sessionStepRepo
                .findBySourceAndSessionAndStepId(SOURCE_1, SESSION_1, "scan");
        Assert.assertTrue("Session step should have been present", sessionStep1UpdatedOpt.isPresent());
        SessionStep sessionStep1Updated = sessionStep1UpdatedOpt.get();
        Assert.assertEquals("Wrong property", 0L, sessionStep1Updated.getState().getRunning());
        Assert.assertEquals("Wrong property", 2L, sessionStep1Updated.getState().getErrors());
        Assert.assertNotEquals("Wrong lastUpdateDate", sessionStep1.getLastUpdateDate(),
                               sessionStep1Updated.getLastUpdateDate());
        // check values of second event are not changed
        sessionStep2Opt = this.sessionStepRepo.findBySourceAndSessionAndStepId(SOURCE_2, SESSION_1, "scan");
        Assert.assertTrue("Session step should have been present", sessionStep2Opt.isPresent());
        SessionStep sessionStep2 = sessionStep2Opt.get();
        Assert.assertEquals("Wrong property", 2L, sessionStep2.getState().getRunning());
        Assert.assertEquals("Wrong property", 0L, sessionStep2.getState().getErrors());
        Assert.assertNotEquals("Wrong lastUpdateDate", sessionStep2.getLastUpdateDate(),
                               sessionStep1Updated.getLastUpdateDate());

    }

    private void createRunStepEvents() {
        List<SessionStep> sessionStepList = new ArrayList<>();

        // SOURCE 1 -  SESSION 1
        SessionStep sessionStep0 = new SessionStep("scan", SOURCE_1, SESSION_1, StepTypeEnum.ACQUISITION,
                                                   new StepState(0, 0, 2));
        sessionStep0.setInputRelated(2);
        sessionStep0.setLastUpdateDate(LAST_UPDATED.minusMinutes(3));
        sessionStepList.add(sessionStep0);

        // SOURCE 1 -  SESSION 2
        SessionStep sessionStep2 = new SessionStep("scan", SOURCE_2, SESSION_1, StepTypeEnum.ACQUISITION,
                                                   new StepState(0, 0, 2));
        sessionStep2.setInputRelated(2);
        sessionStep2.setLastUpdateDate(LAST_UPDATED.minusMinutes(3));
        sessionStepList.add(sessionStep2);

        // PUBLISH EVENTS
        List<SessionStepEvent> eventSet = new ArrayList<>();
        sessionStepList.forEach(step -> eventSet.add(new SessionStepEvent(step)));
        publisher.publish(eventSet);
    }

    private void createRun2StepEvents() {
        List<SessionStep> sessionStepList = new ArrayList<>();

        // SOURCE 1 -  SESSION 1
        SessionStep sessionStep0 = new SessionStep("scan", SOURCE_1, SESSION_1, StepTypeEnum.ACQUISITION,
                                                   new StepState(2, 0, 0));
        sessionStep0.setInputRelated(2);
        sessionStep0.setLastUpdateDate(LAST_UPDATED.minusMinutes(2));
        sessionStepList.add(sessionStep0);

        // PUBLISH EVENTS
        List<SessionStepEvent> eventSet = new ArrayList<>();
        sessionStepList.forEach(step -> eventSet.add(new SessionStepEvent(step)));
        publisher.publish(eventSet);
    }
}
