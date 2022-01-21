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
package fr.cnes.regards.framework.modules.session.manager.service.clean;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.StepState;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.manager.service.AbstractManagerServiceUtilsTest;
import fr.cnes.regards.framework.modules.session.manager.service.clean.session.ManagerCleanJob;
import fr.cnes.regards.framework.modules.session.manager.service.clean.session.ManagerCleanScheduler;
import fr.cnes.regards.framework.modules.session.manager.service.update.ManagerSnapshotJob;
import fr.cnes.regards.framework.modules.session.manager.service.update.ManagerSnapshotScheduler;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test blocking of {@link ManagerCleanJob} and {@link ManagerSnapshotJob}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=manager_lock_jobs_it",
        "regards.session.manager.clean.session.limit.store=30", "regards.session.management.snapshot.process"
        + ".scheduler.bulk.initial.delay=10000000" })
@ActiveProfiles({ "testAMQP" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS,
        hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE)
public class ManagerLockSchedulersIT extends AbstractManagerServiceUtilsTest {

    /**
     * Reference date for tests
     */
    private static final OffsetDateTime UPDATE_DATE = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);

    @Autowired
    private ManagerCleanScheduler cleanScheduler;

    @Autowired
    private ManagerSnapshotScheduler snapshotScheduler;

    @Value("${regards.session.manager.clean.session.limit.store}")
    private int limitStoreSessionSteps;

    @Test
    @Purpose("Test if a ManagerCleanJob is queued after all ManagerSnapshotJobs queued, pending or running")
    public void testBlockedCleanProcessBySnapshot() throws InterruptedException {
        int nbSessionSteps = 50;
        createSessionSteps(nbSessionSteps);

        // --- LAUNCH JOBS ---
        // launch SnapshotJobs
        snapshotScheduler.scheduleJob();
        waitForJobStates(ManagerSnapshotJob.class.getName(), nbSessionSteps, 60000L, JobStatus.values());
        // launch CleanJobs
        // if there is a snapshot job ongoing, make sure a clean job are not launched until the end of all snapshot jobs
        cleanScheduler.scheduleJob();
        waitForJobStates(ManagerCleanJob.class.getName(), 1, 60000L, new JobStatus[] { JobStatus.SUCCEEDED });
        // --- CHECK RESULTS ---
        // Check results, Snapshot jobs should have been created, Clean jobs should be blocked
        List<JobInfo> createdJobList = (List<JobInfo>) this.jobInfoRepo.findAll();
        Optional<JobInfo> managerCleanJob = createdJobList.stream()
                .max(Comparator.comparing(job -> job.getStatus().getQueuedDate()));
        Assert.assertTrue("CleanJobs should have been queued in last position",
                          managerCleanJob.isPresent() && managerCleanJob.get().getClassName()
                                  .equals(ManagerCleanJob.class.getName()));
    }

    @Test
    @Purpose("Test if a ManagerSnapshotJob is blocked by a ManagerCleanJob queued, pending or running")
    public void testBlockedSnapshotByCleanProcess() throws InterruptedException {
        int nbSessionSteps = 100;
        createSessionSteps(nbSessionSteps);

        // --- LAUNCH JOBS ---
        // launch CleanJobs
        // if there is a snapshot job ongoing, make sure a clean job are not launched
        cleanScheduler.scheduleJob();
        waitForJobStates(ManagerCleanJob.class.getName(), 1, 60000L, JobStatus.values());

        // launch SnapshotJobs
        snapshotScheduler.scheduleJob();

        // --- CHECK RESULTS ---
        // Check results, snapshot jobs should not be created because there was a clean process ongoing
        List<JobInfo> createdJobList = (List<JobInfo>) this.jobInfoRepo.findAll();
        long nbSnapshotJobs = createdJobList.stream()
                .filter(job -> job.getClassName().equals(ManagerSnapshotJob.class.getName())).count();
        Assert.assertEquals("SnapshotJobs should not have been created because there was a clean process ongoing", 0L,
                            nbSnapshotJobs);
    }

    @Test
    @Purpose("Test if a ManagerSnapshotJob is blocked if one is already running for the same source")
    public void testBlockedSnapshotBySameSource() throws InterruptedException {
        int nbSessionSteps = 20;
        createSessionSteps(nbSessionSteps);

        // --- LAUNCH JOBS ---
        // launch job for the first time
        snapshotScheduler.scheduleJob();
        // launch the same jobs for the second time
        snapshotScheduler.scheduleJob();
        // assert the correct number of jobs are created (not duplicated)
        waitForJobStates(ManagerSnapshotJob.class.getName(), nbSessionSteps, 60000L, JobStatus.values());
    }

    private void createSessionSteps(int nbSessionSteps) {
        List<SessionStep> stepList = new ArrayList<>();
        List<SnapshotProcess> snapshotProcessList = new ArrayList<>();

        // create list of session step events and snapshot
        for (int i = 0; i < nbSessionSteps; i++) {
            String source = "SOURCE_" + i;
            // ACQUISITION - scan event SOURCE 0-nbSources / SESSION 1
            SessionStep sessionStep = new SessionStep("scan", source, "SESSION_1", StepTypeEnum.ACQUISITION,
                                                      new StepState(0, 0, 1));
            sessionStep.setLastUpdateDate(UPDATE_DATE.minusDays(limitStoreSessionSteps + 1));
            sessionStep.setRegistrationDate(sessionStep.getLastUpdateDate());
            stepList.add(sessionStep);

            // snapshot
            snapshotProcessList.add(new SnapshotProcess(source, null, null));
        }
        this.sessionStepRepo.saveAll(stepList);
        this.snapshotProcessRepo.saveAll(snapshotProcessList);
    }
}