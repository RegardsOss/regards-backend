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
package fr.cnes.regards.framework.modules.session.manager.service.clean;

import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.StepState;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.manager.domain.AggregationState;
import fr.cnes.regards.framework.modules.session.manager.domain.Session;
import fr.cnes.regards.framework.modules.session.manager.domain.Source;
import fr.cnes.regards.framework.modules.session.manager.domain.SourceStepAggregation;
import fr.cnes.regards.framework.modules.session.manager.service.AbstractManagerServiceUtilsTest;
import fr.cnes.regards.framework.modules.session.manager.service.clean.session.ManagerCleanService;
import fr.cnes.regards.framework.modules.session.manager.service.update.ManagerSnapshotService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for {@link ManagerCleanService}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=manager_clean_process_it",
        "regards.session.manager.clean.session.limit.store=30" })
@ActiveProfiles({ "noscheduler" })
public class ManagerCleanServiceIT extends AbstractManagerServiceUtilsTest {
    
    private static OffsetDateTime UPDATE_DATE;
    
    @Autowired
    private ManagerCleanService managerCleanService;
    
    @Autowired
    private ManagerSnapshotService managerSnapshotService;

    @Value("${regards.session.manager.clean.session.limit.store}")
    private int limitStoreSessionSteps;

    @Override
    public void doInit() {
        UPDATE_DATE = OffsetDateTime.now(ZoneOffset.UTC).minusDays(limitStoreSessionSteps);
    }

    @Test
    @Purpose("Test the deletion of outdated sessions and check if statistics were correctly updated")
    public void cleanTest() {
        // create session step requests and snapshots
        createSessionStep();
        SnapshotProcess snapshotProcess1 = this.snapshotProcessRepo.save(new SnapshotProcess(SOURCE_1, null, null));
        SnapshotProcess snapshotProcess2 = this.snapshotProcessRepo.save(new SnapshotProcess(SOURCE_2, null, null));

        // create sessions
        managerSnapshotService.generateSnapshots(snapshotProcess1, OffsetDateTime.now());
        managerSnapshotService.generateSnapshots(snapshotProcess2, OffsetDateTime.now());

        // launch cleaning process
        int nbSessionsDeleted = managerCleanService.clean();
        checkResult(nbSessionsDeleted);
    }

    private void createSessionStep() {
        List<SessionStep> stepRequests = new ArrayList<>();

        // ACQUISITION
        SessionStep step1 = new SessionStep("scan", SOURCE_1, SESSION_1, StepTypeEnum.ACQUISITION,
                                            new StepState(3L, 2L, 5L));
        step1.setLastUpdateDate(UPDATE_DATE.plusSeconds(1L));
        stepRequests.add(step1);

        // REFERENCING
        SessionStep step2 = new SessionStep("oais", SOURCE_1, SESSION_1, StepTypeEnum.REFERENCING,
                                            new StepState(0L, 1L, 0L));
        step2.setLastUpdateDate(UPDATE_DATE.plusMinutes(1L));
        stepRequests.add(step2);

        SessionStep step3 = new SessionStep("oais", SOURCE_1, SESSION_2, StepTypeEnum.REFERENCING,
                                            new StepState(1L, 6L, 10L));
        step3.setLastUpdateDate(UPDATE_DATE.minusDays(1));
        stepRequests.add(step3);

        // STORAGE
        SessionStep step4 = new SessionStep("storage", SOURCE_2, SESSION_1, StepTypeEnum.STORAGE,
                                            new StepState(0L, 2L, 0L));
        step4.setLastUpdateDate(UPDATE_DATE.minusSeconds(1L));
        stepRequests.add(step4);

        // SAVE
        this.sessionStepRepo.saveAll(stepRequests);
    }

    private void checkResult(int nbSessionDeleted) {
        Assert.assertEquals(
                String.format("There should be 2 sessions deleted (with lastUpdateDate before %s)", UPDATE_DATE), 2,
                nbSessionDeleted);

        Optional<Session> session1Opt = this.sessionRepo.findBySourceAndName(SOURCE_1, SESSION_1);
        Assert.assertTrue(
                String.format("Session \"%s\" of source \"%s\" should have been present", SESSION_1, SOURCE_1),
                session1Opt.isPresent());

        // --- SOURCE 1 ---
        // CHECK SESSION 1/SOURCE 1 was created with two session steps. Its update date is after the limit so it
        // should remain the same
        Session session1 = session1Opt.get();
        Set<SessionStep> steps = session1.getSteps();
        Assert.assertEquals("There should be two session steps in the session", 2, steps.size());
        for (SessionStep sessionStep : steps) {
            StepState state = sessionStep.getState();
            if (sessionStep.getStepId().equals("scan")) {
                Assert.assertEquals("Wrong property, check snapshot generation", 3L, state.getErrors());
                Assert.assertEquals("Wrong property, check snapshot generation", 2L, state.getWaiting());
                Assert.assertEquals("Wrong property, check snapshot generation", 5L, state.getRunning());
            } else if (sessionStep.getStepId().equals("oais")) {
                Assert.assertEquals("Wrong property, check snapshot generation", 0L, state.getErrors());
                Assert.assertEquals("Wrong property, check snapshot generation", 1L, state.getWaiting());
                Assert.assertEquals("Wrong property, check snapshot generation", 0L, state.getRunning());
            } else {
                Assert.fail(String.format("Was not expecting stepId \"%s\"", sessionStep.getStepId()));
            }
        }

        // CHECK SESSION 2/SOURCE 1 was correctly deleted
        Optional<Session> session2Opt = this.sessionRepo.findBySourceAndName(SOURCE_1, SESSION_2);
        Assert.assertFalse(
                String.format("Session \"%s\" of source \"%s\" should have been deleted", SESSION_2, SOURCE_1),
                session2Opt.isPresent());

        // CHECK SOURCE 1 was created and updated after the cleaning process
        Optional<Source> source1Opt = this.sourceRepo.findByName(SOURCE_1);
        Assert.assertTrue(String.format("Source \"%s\" should have been present", SOURCE_1), source1Opt.isPresent());
        Source source1 = source1Opt.get();
        Assert.assertEquals("Wrong number of sessions expected", 1L, source1.getNbSessions());

        Set<SourceStepAggregation> aggSteps = source1.getSteps();
        for (SourceStepAggregation agg : aggSteps) {
            AggregationState state = agg.getState();
            if (agg.getType().equals(StepTypeEnum.ACQUISITION)) {
                Assert.assertEquals("Wrong property, check the snapshot generation and the cleaning process", 3L,
                                    state.getErrors());
                Assert.assertEquals("Wrong property, check the snapshot generation and the cleaning process", 2L,
                                    state.getWaiting());
                Assert.assertEquals("Wrong property, check the snapshot generation the cleaning process ", 5L,
                                    state.getRunning());
            } else if (agg.getType().equals(StepTypeEnum.REFERENCING)) {
                Assert.assertEquals("Wrong property, check the snapshot generation and the cleaning process", 0L,
                                    state.getErrors());
                Assert.assertEquals("Wrong property, check the snapshot generation and the cleaning process", 1L,
                                    state.getWaiting());
                Assert.assertEquals("Wrong property, check the snapshot generation the cleaning process ", 0L,
                                    state.getRunning());
            } else {
                Assert.fail(String.format("Was not expecting type \"%s\"", agg.getType()));
            }
        }

        // outdated sessionSteps from the sessionRepo should be deleted because it is a temporary tables. All
        // sessionSteps have been processed within sessions
        Assert.assertTrue("SessionStep be present because its lastUpdateDate is after the limit",
                           this.sessionStepRepo.findBySourceAndSessionAndStepId(SOURCE_1, SESSION_1, "scan").isPresent());
        Assert.assertFalse("Outdated sessionStep should have been deleted from the temporary table",
                           this.sessionStepRepo.findBySourceAndSessionAndStepId(SOURCE_1, SESSION_2, "oais").isPresent());

        // --- SOURCE 2 ---
        // CHECK SESSION 1/SOURCE 2 was correctly deleted
        Optional<Session> session3Opt = this.sessionRepo.findBySourceAndName(SOURCE_2, SESSION_1);
        Assert.assertFalse(
                String.format("Session \"%s\" of source \"%s\" should have been deleted", SESSION_1, SOURCE_2),
                session3Opt.isPresent());

        // CHECK SOURCE 2 was correctly deleted
        Optional<Source> source2Opt = this.sourceRepo.findByName(SOURCE_2);
        Assert.assertFalse(
                String.format("Source \"%s\" should have been deleted because there is no session is " + "related",
                              SOURCE_2), source2Opt.isPresent());
    }
}