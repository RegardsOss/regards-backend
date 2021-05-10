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
package fr.cnes.regards.framework.modules.session.management.service.update;

import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.StepState;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionStepEvent;
import fr.cnes.regards.framework.modules.session.management.domain.Session;
import fr.cnes.regards.framework.modules.session.management.domain.Source;
import fr.cnes.regards.framework.modules.session.management.service.AbstractManagerServiceUtilsTest;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
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
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=manager_performance_it" })
public class ManagerSnapshotPerformanceJobServiceIT extends AbstractManagerServiceUtilsTest {

    /**
     * Tested service
     */
    @Autowired
    private ManagerSnapshotJobService managerSnapshotJobService;

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerSnapshotPerformanceJobServiceIT.class);

    /**
     * Reference date for tests
     */
    private static final OffsetDateTime UPDATE_DATE = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);

    @Test
    @Purpose("Test the performance while generating sessions and sources from sessionStep")
    public void performanceGenerateSessionsSources() throws InterruptedException {
        // launch the generation of sessionSteps from sessionStep events
        int nbSessionSteps = 100;
        createSessionStepEvents(nbSessionSteps);

        // wait for sessionSteps to be stored in database
        boolean isEventRegistered = waitForSessionStepEventsStored(nbSessionSteps);
        if (!isEventRegistered) {
            Assert.fail("Events were not stored in database");
        }

        // Schedule jobs
        long start = System.currentTimeMillis();
        long timeout = 200000L;
        LOGGER.info("Launching performance test to create Sessions and Sources Aggregations from {} SessionSteps from "
                            + "{} different sources", nbSessionSteps, nbSessionSteps);
        managerSnapshotJobService.scheduleJob();

        // wait for job to be in success state
        boolean isJobSuccess = waitForJobSuccesses(ManagerSnapshotJob.class.getName(), nbSessionSteps, timeout);
        LOGGER.info(
                "Performance test handled in {}ms to create Sessions and Sources Aggregations from {} SessionSteps from {} different "
                        + "sources", System.currentTimeMillis() - start, nbSessionSteps, nbSessionSteps);
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

    private void createSessionStepEvents(int nbSessionSteps) {
        List<SessionStepEvent> stepEvents = new ArrayList<>();

        // create list of session step events
        for (int i = 0; i < nbSessionSteps; i++) {
            String source = "SOURCE_" + i;

            // ACQUISITION - scan event SOURCE 0-nbSources / SESSION 1
            SessionStep sessionStep = new SessionStep("scan", source, SESSION_1, StepTypeEnum.ACQUISITION, new StepState(0, 0, 1));
            sessionStep.setLastUpdateDate(UPDATE_DATE);
            stepEvents.add(new SessionStepEvent(sessionStep));
        }

        // Publish events
        this.publisher.publish(stepEvents);
    }
}

