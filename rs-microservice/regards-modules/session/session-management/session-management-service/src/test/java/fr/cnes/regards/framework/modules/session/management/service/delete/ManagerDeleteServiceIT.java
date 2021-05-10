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
package fr.cnes.regards.framework.modules.session.management.service.delete;

import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.StepState;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionDeleteEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionStepEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SourceDeleteEvent;
import fr.cnes.regards.framework.modules.session.management.domain.Session;
import fr.cnes.regards.framework.modules.session.management.domain.Source;
import fr.cnes.regards.framework.modules.session.management.service.AbstractManagerServiceUtilsTest;
import fr.cnes.regards.framework.modules.session.management.service.update.ManagerSnapshotJob;
import fr.cnes.regards.framework.modules.session.management.service.update.ManagerSnapshotJobService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Test the deletion of {@link Source} or {@link Session} following a {@link SourceDeleteEvent} or a {@link SessionDeleteEvent}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=manager_delete_service_it" })
public class ManagerDeleteServiceIT extends AbstractManagerServiceUtilsTest {

    /**
     * Service to create source and session
     */
    @Autowired
    private ManagerSnapshotJobService managerSnapshotJobService;

    @Autowired
    private SourceDeleteService sourceDeleteService;

    /**
     * Reference date for tests
     */
    private static final OffsetDateTime UPDATE_DATE = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);

    @Test
    @Purpose("Test the deletion of a source")
    public void testSourceDeletion() throws InterruptedException {
        // create source delete event on SOURCE_1
        createSourcesAndSession();
        this.publisher.publish(new SourceDeleteEvent(SOURCE_1));

        // wait for the deletion of the source
        boolean isSourceDeleted = waitForSourceDeleted(SOURCE_1, 200000L);
        if (!isSourceDeleted) {
            Assert.fail("Source was not deleted");
        }

        // assert other source was not deleted
        Assert.assertTrue("Source should have been present", this.sourceRepo.findByName(SOURCE_2).isPresent());
    }

    @Test
    @Purpose("Test the deletion of a session")
    public void testSessionDeletion() throws InterruptedException {
        // create source delete event on SESSION_1 SOURCE 2
        createSourcesAndSession();
        this.publisher.publish(new SessionDeleteEvent(SOURCE_2, SESSION_1));

        // wait for the deletion of the session
        boolean isSessionDeleted = waitForSessionDeleted(SOURCE_2, SESSION_1);
        if (!isSessionDeleted) {
            Assert.fail("Session was not deleted");
        }
        // assert other sessions were not deleted
        Assert.assertTrue("Session should have been present", this.sessionRepo.findBySourceAndName(SOURCE_1,
                                                                                                   SESSION_1).isPresent());
        Assert.assertTrue("Session should have been present", this.sessionRepo.findBySourceAndName(SOURCE_2,
                                                                                                   SESSION_2).isPresent());
    }

    // ----- UTILS -----
    private void createSourcesAndSession() throws InterruptedException {
        // launch the generation of sessionSteps from sessionStep events
        int nbSessionSteps = createSessionStepEvents();

        // wait for sessionSteps to be stored in database
        boolean isEventRegistered = waitForSessionStepEventsStored(nbSessionSteps);
        if (!isEventRegistered) {
            Assert.fail("Events were not stored in database");
        }

        // Schedule jobs
        managerSnapshotJobService.scheduleJob();

        // wait for job to be in success state
        long timeout = 20000L;
        boolean isJobSuccess = waitForJobSuccesses(ManagerSnapshotJob.class.getName(), 2, timeout);
        if (!isJobSuccess) {
            Assert.fail(String.format("The number of jobs in success state is not expected. Check if all jobs were "
                                              + "created in the required amount of time (max. %d ms)", timeout));
        }
        checkResult();
    }

    private void checkResult() {
        List<Session> sessions = this.sessionRepo.findAll();
        Assert.assertEquals("Wrong number of sessions created", 3, sessions.size());

        List<Source> sources = this.sourceRepo.findAll();
        Assert.assertEquals("Wrong number of sources created", 2, sources.size());
    }

    private int createSessionStepEvents() {
        List<SessionStepEvent> stepEvents = new ArrayList<>();
        SessionStep sessionStep = new SessionStep("scan", SOURCE_1, SESSION_1, StepTypeEnum.ACQUISITION,
                                                  new StepState(0, 0, 1));
        sessionStep.setLastUpdateDate(UPDATE_DATE);
        stepEvents.add(new SessionStepEvent(sessionStep));

        SessionStep sessionStep2 = new SessionStep("scan", SOURCE_2, SESSION_1, StepTypeEnum.ACQUISITION,
                                                   new StepState(0, 0, 1));
        sessionStep2.setLastUpdateDate(UPDATE_DATE);
        stepEvents.add(new SessionStepEvent(sessionStep2));

        SessionStep sessionStep3 = new SessionStep("oais", SOURCE_2, SESSION_2, StepTypeEnum.REFERENCING,
                                                   new StepState(0, 0, 1));
        sessionStep3.setLastUpdateDate(UPDATE_DATE);
        stepEvents.add(new SessionStepEvent(sessionStep3));

        // Publish events
        this.publisher.publish(stepEvents);

        return stepEvents.size();
    }
}