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
package fr.cnes.regards.framework.modules.session.management.service.clean;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.StepState;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionStepEvent;
import fr.cnes.regards.framework.modules.session.management.service.AbstractManagerServiceUtilsTest;
import fr.cnes.regards.framework.modules.session.management.service.clean.session.ManagerCleanJob;
import fr.cnes.regards.framework.modules.session.management.service.clean.session.ManagerCleanScheduler;
import fr.cnes.regards.framework.modules.session.management.service.update.ManagerSnapshotJob;
import fr.cnes.regards.framework.modules.session.management.service.update.ManagerSnapshotScheduler;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

/**
 * Test mutual blocking of {@link ManagerCleanJob} and {@link ManagerSnapshotJob}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=manager_lock_jobs_it",
        "regards.session.manager.clean.session.limit.store.session=30" })
public class ManagerLockSchedulersIT extends AbstractManagerServiceUtilsTest {

    /**
     * Reference date for tests
     */
    private static final OffsetDateTime UPDATE_DATE = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);

    @Autowired
    private ManagerCleanScheduler cleanScheduler;

    @Autowired
    private ManagerSnapshotScheduler snapshotScheduler;

    @Value("${regards.session.manager.clean.session.limit.store.session}")
    private int limitStoreSessionSteps;

    @Test
    @Purpose("Test if a ManagerCleanJob is queued after all ManagerSnapshotJobs queued, pending or running")
    public void testBlockedCleanProcess() throws InterruptedException {
        int nbSessionSteps = 100;
        createSessionStepEvents(nbSessionSteps);

        // --- LAUNCH JOBS ---
        // launch SnapshotJobs
        snapshotScheduler.scheduleManagerSnapshot();
        Thread.sleep(100L);

        // launch CleanJobs
        // if there is a snapshot job ongoing, make sure a clean job are not launched
        cleanScheduler.scheduleCleanSession();
        Thread.sleep(100L);

        // --- CHECK RESULTS ---
        // Check results, Snapshot jobs should have been created, Clean jobs should be blocked
        List<JobInfo> createdJobList = (List<JobInfo>) this.jobInfoRepo.findAll();
        long nbSnapshotJobs = createdJobList.stream()
                .filter(job -> job.getClassName().equals(ManagerSnapshotJob.class.getName())).count();
        Assert.assertEquals(String.format("%s snapshot jobs should have been created", nbSessionSteps), nbSessionSteps,
                            nbSnapshotJobs);
        Optional<JobInfo> managerCleanJob = createdJobList.stream()
                .max(Comparator.comparing(job -> job.getStatus().getQueuedDate()));
        Assert.assertTrue("CleanJobs should have been queued in last position",
                          managerCleanJob.isPresent() && managerCleanJob.get().getClassName()
                                  .equals(ManagerCleanJob.class.getName()));
    }

    @Test
    @Purpose("Test if a ManagerSnapshotJob is blocked by a ManagerCleanJob queued, pending or running")
    public void testBlockedSnapshot() throws InterruptedException {
        int nbSessionSteps = 100;
        createSessionStepEvents(nbSessionSteps);

        // --- LAUNCH JOBS ---
        // launch CleanJobs
        // if there is a snapshot job ongoing, make sure a clean job are not launched
        cleanScheduler.scheduleCleanSession();
        Thread.sleep(100L);

        // launch SnapshotJobs
        snapshotScheduler.scheduleManagerSnapshot();
        Thread.sleep(100L);

        // --- CHECK RESULTS ---
        // Check results, snapshot jobs should not be created because there was a clean process ongoing
        List<JobInfo> createdJobList = (List<JobInfo>) this.jobInfoRepo.findAll();
        long nbSnapshotJobs = createdJobList.stream()
                .filter(job -> job.getClassName().equals(ManagerSnapshotJob.class.getName())).count();
        Assert.assertEquals("SnapshotJobs should not have been created because there was a clean process ongoing", 0L,
                            nbSnapshotJobs);
        long nbCleanJobs = createdJobList.stream()
                .filter(job -> job.getClassName().equals(ManagerCleanJob.class.getName())).count();
        Assert.assertEquals("One clean job should have been created", 1L, nbCleanJobs);

    }

    private void createSessionStepEvents(int nbSessionSteps) throws InterruptedException {
        List<SessionStepEvent> stepEvents = new ArrayList<>();

        // create list of session step events
        for (int i = 0; i < nbSessionSteps; i++) {
            String source = "SOURCE_" + i;
            // ACQUISITION - scan event SOURCE 0-nbSources / SESSION 1
            SessionStep sessionStep = new SessionStep("scan", source, SESSION_1, StepTypeEnum.ACQUISITION,
                                                      new StepState(0, 0, 1));
            sessionStep.setLastUpdateDate(UPDATE_DATE.minusDays(limitStoreSessionSteps + 1));
            stepEvents.add(new SessionStepEvent(sessionStep));
        }

        // Publish events
        this.publisher.publish(stepEvents);

        // wait for sessionSteps to be stored in database
        boolean isEventRegistered = waitForSessionStepEventsStored(nbSessionSteps);
        if (!isEventRegistered) {
            Assert.fail("Events were not stored in database");
        }
    }
}