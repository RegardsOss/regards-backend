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
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionStepEvent;
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
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Performance test for {@link ManagerSnapshotJobService}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=source_delete_service_it" })
@Ignore
public class SourceDeleteServiceIT extends AbstractManagerServiceUtilsTest {

    /**
     * Service to create source and session
     */
    @Autowired
    private ManagerSnapshotJobService managerSnapshotJobService;

    @Autowired
    private SourceDeleteService sourceDeleteService;

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceDeleteServiceIT.class);

    /**
     * Reference date for tests
     */
    private static final OffsetDateTime UPDATE_DATE = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);


    @Test
    @Purpose("Test the deletion of a source")
    public void testSourceDeletion() throws InterruptedException {
        sourceDeleteService.deleteSource(SOURCE_1);

     /*   createSourcesAndSession();
        this.publisher.publish(new SourceDeleteEvent(SOURCE_1));
*/
        boolean isSourceDeleted = waitForSourceDeleted(SOURCE_1);
        if (!isSourceDeleted) {
            Assert.fail("Source was not deleted");
        }

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
        boolean isJobSuccess = waitForJobSuccesses(ManagerSnapshotJob.class.getName(), nbSessionSteps, timeout);
        if (!isJobSuccess) {
            Assert.fail(String.format("The number of jobs in success state is not expected. Check if all jobs were "
                                              + "created in the required amount of time (max. %d ms)", timeout));
        }
        checkResult(nbSessionSteps);
    }

    private void checkResult(int nbSourcesExpected) {
        List<Session> sessions = this.sessionRepo.findAll();
        Assert.assertEquals("Wrong number of sessions created", nbSourcesExpected, sessions.size());

        List<Source> sources = this.sourceRepo.findAll();
        Assert.assertEquals("Wrong number of sources created", nbSourcesExpected, sources.size());
    }

    private int createSessionStepEvents() {
        List<SessionStepEvent> stepEvents = new ArrayList<>();
        SessionStep sessionStep = new SessionStep("scan", SOURCE_1, SESSION_1, StepTypeEnum.ACQUISITION,
                                                  new StepState(0, 0, 1), null);
        sessionStep.setLastUpdateDate(UPDATE_DATE);
        stepEvents.add(new SessionStepEvent(sessionStep));

        SessionStep sessionStep2 = new SessionStep("scan", SOURCE_2, SESSION_1, StepTypeEnum.ACQUISITION,
                                                   new StepState(0, 0, 1), null);
        sessionStep2.setLastUpdateDate(UPDATE_DATE);
        stepEvents.add(new SessionStepEvent(sessionStep2));

        // Publish events
        this.publisher.publish(stepEvents);

        return stepEvents.size();
    }
}

