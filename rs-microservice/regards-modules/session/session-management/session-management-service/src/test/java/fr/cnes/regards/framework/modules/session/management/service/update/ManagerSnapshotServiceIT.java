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

import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.StepState;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.management.dao.ISessionRepository;
import fr.cnes.regards.framework.modules.session.management.dao.ISourceRepository;
import fr.cnes.regards.framework.modules.session.management.domain.Session;
import fr.cnes.regards.framework.modules.session.management.domain.Source;
import fr.cnes.regards.framework.modules.session.management.domain.SourceStepAggregation;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for {@link ManagerSnapshotService}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=manager_snapshot_service_it",
        "regards.cipher.key-location=src/test/resources/testKey", "regards.cipher.iv=1234567812345678" })
@ActiveProfiles(value = { "noscheduler" })
public class ManagerSnapshotServiceIT extends AbstractRegardsServiceTransactionalIT {

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    @Autowired
    private ISessionRepository sessionRepo;

    @Autowired
    private ISourceRepository sourceRepo;

    @Autowired
    private ManagerSnapshotService managerSnapshotService;

    private static final String SOURCE_1 = "SOURCE 1";

    private static final String SESSION_1 = "SESSION 1";

    private static final String SESSION_2 = "SESSION 2";

    private static final String SESSION_3 = "SESSION 3";

    private static final String SESSION_4 = "SESSION 4";

    private static final OffsetDateTime LAST_UPDATED = OffsetDateTime.now(ZoneOffset.UTC);

    @Before
    public void init() {
        this.sessionStepRepo.deleteAll();
        this.sessionRepo.deleteAll();
        this.sourceRepo.deleteAll();
    }

    @Test
    @Purpose("Test the generation of session and source snapshots")
    public void generateSnapshotsTest() {
        // init session steps
        List<SessionStep> sessionStepsCreated = createSessionSteps();
        SnapshotProcess snapshotProcess = new SnapshotProcess(SOURCE_1, null, null);

        // --- RUN 1 ---
        // generate sessions
        managerSnapshotService.generateSnapshots(snapshotProcess, LAST_UPDATED);
        checkRun1Results(sessionStepsCreated);

        // --- RUN 2 ---
        // update session snapshot
        snapshotProcess = new SnapshotProcess(SOURCE_1, LAST_UPDATED, null);

        // modify step2
        SessionStep sessionStep2Updated = sessionStepsCreated.get(2);
        sessionStep2Updated.getState().setWaiting(2);
        sessionStep2Updated.setLastUpdateDate(LAST_UPDATED.plusMinutes(9));
        this.sessionStepRepo.save(sessionStep2Updated);

        // generate sessions for run 2
        managerSnapshotService.generateSnapshots(snapshotProcess, LAST_UPDATED.plusMinutes(10));
        checkRun2Results(sessionStepsCreated);

        // --- RUN 3 ---
        // update session snapshot
        snapshotProcess = new SnapshotProcess(SOURCE_1, LAST_UPDATED.plusMinutes(50), null);

        // update steps
        SessionStep sessionStep1Updated = sessionStepsCreated.get(1);
        sessionStep1Updated.getState().setErrors(0);
        sessionStep1Updated.setLastUpdateDate(LAST_UPDATED.plusMinutes(62));

        sessionStep2Updated.getState().setWaiting(0);
        sessionStep2Updated.getState().setRunning(0);
        sessionStep2Updated.setLastUpdateDate(LAST_UPDATED.plusMinutes(56));

        SessionStep sessionStep4Updated = sessionStepsCreated.get(4);
        sessionStep4Updated.getState().setRunning(0);
        sessionStep4Updated.setLastUpdateDate(LAST_UPDATED.plusMinutes(60));

        this.sessionStepRepo.saveAll(sessionStepsCreated);
        managerSnapshotService.generateSnapshots(snapshotProcess, LAST_UPDATED.plusMinutes(70));
        checkRun3Results(sessionStepsCreated);

    }

    private List<SessionStep> createSessionSteps() {
        List<SessionStep> sessionStepList = new ArrayList<>();

        // SESSION 1
        SessionStep sessionStep0 = new SessionStep("scan", SOURCE_1, SESSION_1, StepTypeEnum.ACQUISITION,
                                                   new StepState(0, 0, 2), null);
        sessionStep0.setInputRelated(2);
        sessionStep0.setLastUpdateDate(LAST_UPDATED.minusMinutes(10));

        SessionStep sessionStep1 = new SessionStep("oais", SOURCE_1, SESSION_1, StepTypeEnum.REFERENCING,
                                                   new StepState(2, 0, 0), null);
        sessionStep1.setOutputRelated(2);
        sessionStep1.setLastUpdateDate(LAST_UPDATED.minusMinutes(9));

        // SESSION 2
        SessionStep sessionStep2 = new SessionStep("storage", SOURCE_1, SESSION_2, StepTypeEnum.STORAGE,
                                                   new StepState(0, 4, 0), null);
        sessionStep2.setOutputRelated(4);
        sessionStep2.setLastUpdateDate(LAST_UPDATED.minusMinutes(8));

        // SESSION 3
        // create future event - should not be taken into account until run2
        SessionStep sessionStep3 = new SessionStep("storage", SOURCE_1, SESSION_3, StepTypeEnum.STORAGE,
                                                   new StepState(0, 0, 0), null);
        sessionStep3.setOutputRelated(10);
        sessionStep3.setLastUpdateDate(LAST_UPDATED.plusMinutes(2));
        SessionStep sessionStep4 = new SessionStep("metacatalog", SOURCE_1, SESSION_3, StepTypeEnum.DISSEMINATION,
                                                   new StepState(0, 0, 4), null);
        sessionStep4.setOutputRelated(10);
        sessionStep4.setLastUpdateDate(LAST_UPDATED.plusMinutes(7));

        // create future event - should not be taken into account until run3
        SessionStep sessionStep5 = new SessionStep("scan", SOURCE_1, SESSION_4, StepTypeEnum.ACQUISITION,
                                                   new StepState(0, 0, 3), null);
        sessionStep5.setInputRelated(5);
        sessionStep5.setLastUpdateDate(LAST_UPDATED.plusMinutes(52));

        sessionStepList.add(sessionStep0);
        sessionStepList.add(sessionStep1);
        sessionStepList.add(sessionStep2);
        sessionStepList.add(sessionStep3);
        sessionStepList.add(sessionStep4);
        sessionStepList.add(sessionStep5);

        return this.sessionStepRepo.saveAll(sessionStepList);
    }

    private void checkRun1Results(List<SessionStep> sessionStepsCreated) {
        // CHECK SESSION SNAPSHOT
        List<Session> sessionList = this.sessionRepo.findAll();
        Assert.assertEquals("Expected two sessions created", 2, sessionList.size());
        for (Session session : sessionList) {
            String sessionName = session.getName();
            Set<SessionStep> sessionStepSet = session.getSteps();
            switch (sessionName) {
                case SESSION_1:
                    sessionStepSet.retainAll(sessionStepsCreated);
                    Assert.assertEquals("Unexpected session step created", 2, sessionStepSet.size());
                    Assert.assertTrue("Unexpected session step associated",
                                      sessionStepSet.contains(sessionStepsCreated.get(0)));
                    Assert.assertTrue("Unexpected session step associated",
                                      sessionStepSet.contains(sessionStepsCreated.get(1)));
                    Assert.assertTrue("Wrong session state", session.getManagerState().isRunning());
                    Assert.assertFalse("Wrong session state", session.getManagerState().isWaiting());
                    Assert.assertTrue("Wrong session state", session.getManagerState().isErrors());
                    Assert.assertEquals("Wrong session lastUpdateDate", LAST_UPDATED.minusMinutes(9),
                                        session.getLastUpdateDate());
                    break;
                case SESSION_2:
                    sessionStepSet.retainAll(sessionStepsCreated);
                    Assert.assertEquals("Unexpected session step created", 1, sessionStepSet.size());
                    Assert.assertTrue("Unexpected session step associated",
                                      sessionStepSet.contains(sessionStepsCreated.get(2)));
                    Assert.assertFalse("Wrong session state", session.getManagerState().isRunning());
                    Assert.assertTrue("Wrong session state", session.getManagerState().isWaiting());
                    Assert.assertFalse("Wrong session state", session.getManagerState().isErrors());
                    Assert.assertEquals("Wrong session lastUpdateDate", LAST_UPDATED.minusMinutes(8),
                                        session.getLastUpdateDate());
                    break;
                default:
                    Assert.fail(String.format("Unexpected session created : %s", sessionName));
                    break;
            }
        }
        // CHECK SOURCE SNAPSHOT
        Optional<Source> sourceOpt = this.sourceRepo.findByName(SOURCE_1);
        Assert.assertTrue("Source snapshot should have been created", sourceOpt.isPresent());
        Source source = sourceOpt.get();
        Assert.assertEquals("Wrong number of sessions", 2L, source.getNbSessions());
        Assert.assertEquals("Wrong last update date", LAST_UPDATED.minusMinutes(8), source.getLastUpdateDate());
        Assert.assertTrue("Wrong source state", source.getManagerState().isErrors());
        Assert.assertTrue("Wrong source state", source.getManagerState().isRunning());
        Assert.assertTrue("Wrong source state", source.getManagerState().isWaiting());
        Set<SourceStepAggregation> aggSet = source.getSteps();
        Assert.assertEquals("Wrong number of aggregation steps", 3, aggSet.size());

        for (SourceStepAggregation agg : aggSet) {
            StepTypeEnum type = agg.getType();
            switch (type) {
                case ACQUISITION:
                    Assert.assertEquals("Wrong number of input related", 2L, agg.getTotalIn());
                    Assert.assertEquals("Wrong number of output related", 0L, agg.getTotalOut());
                    Assert.assertEquals("Wrong number of errors", 0L, agg.getState().getErrors());
                    Assert.assertEquals("Wrong number of waiting", 0L, agg.getState().getWaiting());
                    Assert.assertEquals("Wrong number of running", 2L, agg.getState().getRunning());
                    break;
                case REFERENCING:
                    Assert.assertEquals("Wrong number of input related", 0L, agg.getTotalIn());
                    Assert.assertEquals("Wrong number of output related", 2L, agg.getTotalOut());
                    Assert.assertEquals("Wrong number of errors", 2L, agg.getState().getErrors());
                    Assert.assertEquals("Wrong number of waiting", 0L, agg.getState().getWaiting());
                    Assert.assertEquals("Wrong number of running", 0L, agg.getState().getRunning());
                    break;
                case STORAGE:
                    Assert.assertEquals("Wrong number of input related", 0L, agg.getTotalIn());
                    Assert.assertEquals("Wrong number of output related", 4L, agg.getTotalOut());
                    Assert.assertEquals("Wrong number of errors", 0L, agg.getState().getErrors());
                    Assert.assertEquals("Wrong number of waiting", 4L, agg.getState().getWaiting());
                    Assert.assertEquals("Wrong number of running", 0L, agg.getState().getRunning());
                    break;
                default:
                    Assert.fail(String.format("Unexpected aggregation step created : %s", type));
                    break;
            }
        }

    }

    private void checkRun2Results(List<SessionStep> sessionStepsCreated) {
        List<Session> sessionList = this.sessionRepo.findAll();
        Assert.assertEquals("Expected three sessions", 3, sessionList.size());
        for (Session session : sessionList) {
            String sessionName = session.getName();
            Set<SessionStep> sessionStepSet = session.getSteps();
            switch (sessionName) {
                case SESSION_1:
                    sessionStepSet.retainAll(sessionStepsCreated);
                    Assert.assertEquals("Unexpected session step created", 2, sessionStepSet.size());
                    Assert.assertTrue("Unexpected session step associated",
                                      sessionStepSet.contains(sessionStepsCreated.get(0)));
                    Assert.assertTrue("Unexpected session step associated",
                                      sessionStepSet.contains(sessionStepsCreated.get(1)));
                    Assert.assertTrue("Wrong session state", session.getManagerState().isRunning());
                    Assert.assertFalse("Wrong session state", session.getManagerState().isWaiting());
                    Assert.assertTrue("Wrong session state", session.getManagerState().isErrors());
                    Assert.assertEquals("Wrong session lastUpdateDate", LAST_UPDATED.minusMinutes(9),
                                        session.getLastUpdateDate());
                    break;
                case SESSION_2:
                    sessionStepSet.retainAll(sessionStepsCreated);
                    Assert.assertEquals("Unexpected session step created", 1, sessionStepSet.size());
                    Assert.assertTrue("Unexpected session step associated",
                                      sessionStepSet.contains(sessionStepsCreated.get(2)));
                    Assert.assertFalse("Wrong session state", session.getManagerState().isRunning());
                    Assert.assertTrue("Wrong session state", session.getManagerState().isWaiting());
                    Assert.assertFalse("Wrong session state", session.getManagerState().isErrors());
                    Assert.assertEquals("Wrong session lastUpdateDate", LAST_UPDATED.plusMinutes(9),
                                        session.getLastUpdateDate());
                    break;

                case SESSION_3:
                    sessionStepSet.retainAll(sessionStepsCreated);
                    Assert.assertEquals("Unexpected session step created", 2, sessionStepSet.size());
                    Assert.assertTrue("Unexpected session step associated",
                                      sessionStepSet.contains(sessionStepsCreated.get(3)));
                    Assert.assertTrue("Unexpected session step associated",
                                      sessionStepSet.contains(sessionStepsCreated.get(4)));
                    Assert.assertTrue("Wrong session state", session.getManagerState().isRunning());
                    Assert.assertFalse("Wrong session state", session.getManagerState().isWaiting());
                    Assert.assertFalse("Wrong session state", session.getManagerState().isErrors());
                    Assert.assertEquals("Wrong session lastUpdateDate", LAST_UPDATED.plusMinutes(7),
                                        session.getLastUpdateDate());
                    break;
                default:
                    Assert.fail("Unexpected session created");
                    break;
            }
        }

        // CHECK SOURCE SNAPSHOT
        Optional<Source> sourceOpt = this.sourceRepo.findByName(SOURCE_1);
        Assert.assertTrue("Source snapshot should have been created", sourceOpt.isPresent());
        Source source = sourceOpt.get();
        Assert.assertEquals("Wrong number of sessions", 3L, source.getNbSessions());
        Assert.assertEquals("Wrong last update date", LAST_UPDATED.plusMinutes(9), source.getLastUpdateDate());
        Assert.assertTrue("Wrong state", source.getManagerState().isErrors());
        Assert.assertTrue("Wrong state", source.getManagerState().isRunning());
        Assert.assertTrue("Wrong state", source.getManagerState().isWaiting());
        Set<SourceStepAggregation> aggSet = source.getSteps();
        Assert.assertEquals("Wrong number of aggregation steps", 4, aggSet.size());

        for (SourceStepAggregation agg : aggSet) {
            StepTypeEnum type = agg.getType();
            switch (type) {
                case ACQUISITION:
                    Assert.assertEquals("Wrong number of input related", 2L, agg.getTotalIn());
                    Assert.assertEquals("Wrong number of output related", 0L, agg.getTotalOut());
                    Assert.assertEquals("Wrong number of errors", 0L, agg.getState().getErrors());
                    Assert.assertEquals("Wrong number of waiting", 0L, agg.getState().getWaiting());
                    Assert.assertEquals("Wrong number of running", 2L, agg.getState().getRunning());
                    break;
                case REFERENCING:
                    Assert.assertEquals("Wrong number of input related", 0L, agg.getTotalIn());
                    Assert.assertEquals("Wrong number of output related", 2L, agg.getTotalOut());
                    Assert.assertEquals("Wrong number of errors", 2L, agg.getState().getErrors());
                    Assert.assertEquals("Wrong number of waiting", 0L, agg.getState().getWaiting());
                    Assert.assertEquals("Wrong number of running", 0L, agg.getState().getRunning());
                    break;
                case STORAGE:
                    Assert.assertEquals("Wrong number of input related", 0L, agg.getTotalIn());
                    Assert.assertEquals("Wrong number of output related", 14L, agg.getTotalOut());
                    Assert.assertEquals("Wrong number of errors", 0L, agg.getState().getErrors());
                    Assert.assertEquals("Wrong number of waiting", 2L, agg.getState().getWaiting());
                    Assert.assertEquals("Wrong number of running", 0L, agg.getState().getRunning());
                    break;
                case DISSEMINATION:
                    Assert.assertEquals("Wrong number of input related", 0L, agg.getTotalIn());
                    Assert.assertEquals("Wrong number of output related", 10L, agg.getTotalOut());
                    Assert.assertEquals("Wrong number of errors", 0L, agg.getState().getErrors());
                    Assert.assertEquals("Wrong number of waiting", 0L, agg.getState().getWaiting());
                    Assert.assertEquals("Wrong number of running", 4L, agg.getState().getRunning());
                    break;
                default:
                    Assert.fail(String.format("Unexpected aggregation step created : %s", type));
                    break;
            }
        }
    }

    private void checkRun3Results(List<SessionStep> sessionStepsCreated) {
        List<Session> sessionList = this.sessionRepo.findAll();
        Assert.assertEquals("Expected four sessions", 4, sessionList.size());
        for (Session session : sessionList) {
            String sessionName = session.getName();
            Set<SessionStep> sessionStepSet = session.getSteps();
            switch (sessionName) {
                case SESSION_1:
                    sessionStepSet.retainAll(sessionStepsCreated);
                    Assert.assertEquals("Unexpected session step created", 2, sessionStepSet.size());
                    Assert.assertTrue("Unexpected session step associated",
                                      sessionStepSet.contains(sessionStepsCreated.get(0)));
                    Assert.assertTrue("Unexpected session step associated",
                                      sessionStepSet.contains(sessionStepsCreated.get(1)));
                    Assert.assertTrue("Wrong session state", session.getManagerState().isRunning());
                    Assert.assertFalse("Wrong session state", session.getManagerState().isWaiting());
                    Assert.assertFalse("Wrong session state", session.getManagerState().isErrors());
                    Assert.assertEquals("Wrong session lastUpdateDate", LAST_UPDATED.plusMinutes(62),
                                        session.getLastUpdateDate());
                    break;
                case SESSION_2:
                    sessionStepSet.retainAll(sessionStepsCreated);
                    Assert.assertEquals("Unexpected session step created", 1, sessionStepSet.size());
                    Assert.assertTrue("Unexpected session step associated",
                                      sessionStepSet.contains(sessionStepsCreated.get(2)));
                    Assert.assertFalse("Wrong session state", session.getManagerState().isRunning());
                    Assert.assertFalse("Wrong session state", session.getManagerState().isWaiting());
                    Assert.assertFalse("Wrong session state", session.getManagerState().isErrors());
                    Assert.assertEquals("Wrong session lastUpdateDate", LAST_UPDATED.plusMinutes(56),
                                        session.getLastUpdateDate());
                    break;

                case SESSION_3:
                    sessionStepSet.retainAll(sessionStepsCreated);
                    Assert.assertEquals("Unexpected session step created", 2, sessionStepSet.size());
                    Assert.assertTrue("Unexpected session step associated",
                                      sessionStepSet.contains(sessionStepsCreated.get(3)));
                    Assert.assertTrue("Unexpected session step associated",
                                      sessionStepSet.contains(sessionStepsCreated.get(4)));
                    Assert.assertFalse("Wrong session state", session.getManagerState().isRunning());
                    Assert.assertFalse("Wrong session state", session.getManagerState().isWaiting());
                    Assert.assertFalse("Wrong session state", session.getManagerState().isErrors());
                    Assert.assertEquals("Wrong session lastUpdateDate", LAST_UPDATED.plusMinutes(60),
                                        session.getLastUpdateDate());
                    break;
                case SESSION_4:
                    sessionStepSet.retainAll(sessionStepsCreated);
                    Assert.assertEquals("Unexpected session step created", 1, sessionStepSet.size());
                    Assert.assertTrue("Unexpected session step associated",
                                      sessionStepSet.contains(sessionStepsCreated.get(5)));
                    Assert.assertTrue("Wrong session state", session.getManagerState().isRunning());
                    Assert.assertFalse("Wrong session state", session.getManagerState().isWaiting());
                    Assert.assertFalse("Wrong session state", session.getManagerState().isErrors());
                    Assert.assertEquals("Wrong session lastUpdateDate", LAST_UPDATED.plusMinutes(52),
                                        session.getLastUpdateDate());
                    break;
                default:
                    Assert.fail("Unexpected session created");
                    break;
            }
        }

        // CHECK SOURCE SNAPSHOT
        Optional<Source> sourceOpt = this.sourceRepo.findByName(SOURCE_1);
        Assert.assertTrue("Source snapshot should have been created", sourceOpt.isPresent());
        Source source = sourceOpt.get();
        Assert.assertEquals("Wrong number of sessions", 4L, source.getNbSessions());
        Assert.assertEquals("Wrong last update date", LAST_UPDATED.plusMinutes(62), source.getLastUpdateDate());
        Assert.assertFalse("Wrong source state", source.getManagerState().isErrors());
        Assert.assertTrue("Wrong source state", source.getManagerState().isRunning());
        Assert.assertFalse("Wrong source state", source.getManagerState().isWaiting());
        Set<SourceStepAggregation> aggSet = source.getSteps();
        Assert.assertEquals("Wrong number of aggregation steps", 4, aggSet.size());

        for (SourceStepAggregation agg : aggSet) {
            StepTypeEnum type = agg.getType();
            switch (type) {
                case ACQUISITION:
                    Assert.assertEquals("Wrong number of input related", 7L, agg.getTotalIn());
                    Assert.assertEquals("Wrong number of output related", 0L, agg.getTotalOut());
                    Assert.assertEquals("Wrong number of errors", 0L, agg.getState().getErrors());
                    Assert.assertEquals("Wrong number of waiting", 0L, agg.getState().getWaiting());
                    Assert.assertEquals("Wrong number of running", 5L, agg.getState().getRunning());
                    break;
                case REFERENCING:
                    Assert.assertEquals("Wrong number of input related", 0L, agg.getTotalIn());
                    Assert.assertEquals("Wrong number of output related", 2L, agg.getTotalOut());
                    Assert.assertEquals("Wrong number of errors", 0L, agg.getState().getErrors());
                    Assert.assertEquals("Wrong number of waiting", 0L, agg.getState().getWaiting());
                    Assert.assertEquals("Wrong number of running", 0L, agg.getState().getRunning());
                    break;
                case STORAGE:
                    Assert.assertEquals("Wrong number of input related", 0L, agg.getTotalIn());
                    Assert.assertEquals("Wrong number of output related", 14L, agg.getTotalOut());
                    Assert.assertEquals("Wrong number of errors", 0L, agg.getState().getErrors());
                    Assert.assertEquals("Wrong number of waiting", 0L, agg.getState().getWaiting());
                    Assert.assertEquals("Wrong number of running", 0L, agg.getState().getRunning());
                    break;
                case DISSEMINATION:
                    Assert.assertEquals("Wrong number of input related", 0L, agg.getTotalIn());
                    Assert.assertEquals("Wrong number of output related", 10L, agg.getTotalOut());
                    Assert.assertEquals("Wrong number of errors", 0L, agg.getState().getErrors());
                    Assert.assertEquals("Wrong number of waiting", 0L, agg.getState().getWaiting());
                    Assert.assertEquals("Wrong number of running", 0L, agg.getState().getRunning());
                    break;
                default:
                    Assert.fail(String.format("Unexpected aggregation step created : %s", type));
                    break;
            }
        }
    }
}