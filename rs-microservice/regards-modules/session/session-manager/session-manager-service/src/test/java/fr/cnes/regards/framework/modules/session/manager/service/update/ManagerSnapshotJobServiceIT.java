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
package fr.cnes.regards.framework.modules.session.manager.service.update;

import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.StepState;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionStepEvent;
import fr.cnes.regards.framework.modules.session.manager.domain.Session;
import fr.cnes.regards.framework.modules.session.manager.domain.Source;
import fr.cnes.regards.framework.modules.session.manager.domain.SourceStepAggregation;
import fr.cnes.regards.framework.modules.session.manager.service.AbstractManagerServiceUtilsTest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for {@link ManagerSnapshotJobService}
 *
 * @author Iliana Ghazali
 **/

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=manager_snapshot_job_service_it"})
@ActiveProfiles({ "testAmqp", "noscheduler" })
public class ManagerSnapshotJobServiceIT extends AbstractManagerServiceUtilsTest {

    @Autowired
    private ManagerSnapshotJobService managerSnapshotJobService;

    private static final OffsetDateTime LAST_UPDATED = OffsetDateTime.now(ZoneOffset.UTC);

    @Test
    @Purpose("Test the generation of session steps from step request events")
    public void generateSessionStepTest() throws InterruptedException {
        // ---- RUN 1 ----
        // launch the generation of sessionSteps
        int nbEvents = createRunStepEvents();

        // wait for sessionStepEvent to be stored in database
        waitForSessionStepEventsStored(nbEvents);

        // retrieve associated snapshot processes
        List<SnapshotProcess> snapshotProcessesCreated = this.snapshotProcessRepo.findAll();
        Assert.assertEquals("Wrong number of snapshot processes created", 2, snapshotProcessesCreated.size());

        // launch snapshot jobs
        this.managerSnapshotJobService.scheduleJob();
        waitForJobStates(ManagerSnapshotJob.class.getName(), 2, 20000L, new JobStatus[] { JobStatus.SUCCEEDED });
        waitForSnapshotUpdateSuccesses();
        checkResults();

        // ---- RUN 2 ----
        // create sessionStepEvents
        createRun2StepEvents();
        Thread.sleep(5000L);

        // launch snapshot jobs
        this.managerSnapshotJobService.scheduleJob();
        waitForJobStates(ManagerSnapshotJob.class.getName(), 3, 20000L, new JobStatus[] { JobStatus.SUCCEEDED });
        waitForSnapshotUpdateSuccesses();
        // check snapshot process was updated
        Optional<SnapshotProcess> snapshotProcessOpt = this.snapshotProcessRepo.findBySource(SOURCE_2);
        Assert.assertTrue("SnapshotProcessDate should have been present", snapshotProcessOpt.isPresent());
        Assert.assertEquals("SnapshotProcessDate was not updated", LAST_UPDATED.minusSeconds(1),
                            snapshotProcessOpt.get().getLastUpdateDate());

        checkResults2();

    }

    private int createRunStepEvents() {
        List<SessionStep> sessionStepList = new ArrayList<>();

        // SOURCE 1 -  SESSION 1
        SessionStep sessionStep0 = new SessionStep("scan", SOURCE_1, SESSION_1, StepTypeEnum.ACQUISITION,
                                                   new StepState(0, 0, 2));
        sessionStep0.setInputRelated(2);
        sessionStep0.setLastUpdateDate(LAST_UPDATED.minusMinutes(2));
        sessionStepList.add(sessionStep0);

        SessionStep sessionStep1 = new SessionStep("oais", SOURCE_1, SESSION_1, StepTypeEnum.REFERENCING,
                                                   new StepState(0, 0, 3));
        sessionStep1.setOutputRelated(2);
        sessionStep1.setLastUpdateDate(LAST_UPDATED.minusMinutes(1));
        sessionStepList.add(sessionStep1);

        // SOURCE 1 -  SESSION 2
        SessionStep sessionStep2 = new SessionStep("scan", SOURCE_1, SESSION_2, StepTypeEnum.ACQUISITION,
                                                   new StepState(5, 0, 0));
        sessionStep2.setInputRelated(5);
        sessionStep2.setLastUpdateDate(LAST_UPDATED.minusMinutes(5));
        sessionStepList.add(sessionStep2);

        // SOURCE 2 - SESSION 1
        SessionStep sessionStep3 = new SessionStep("scan", SOURCE_2, SESSION_1, StepTypeEnum.DISSEMINATION,
                                                   new StepState(0, 10, 0));
        sessionStep3.setOutputRelated(10);
        sessionStep3.setLastUpdateDate(LAST_UPDATED.minusMinutes(12));
        sessionStepList.add(sessionStep3);

        // PUBLISH EVENTS
        List<SessionStepEvent> eventSet = new ArrayList<>();
        sessionStepList.forEach(step -> eventSet.add(new SessionStepEvent(step)));
        publisher.publish(eventSet);

        return sessionStepList.size();
    }

    private int createRun2StepEvents() {
        List<SessionStep> sessionStepList = new ArrayList<>();
        // SOURCE 2 - SESSION 1
        SessionStep sessionStep3 = new SessionStep("scan", SOURCE_2, SESSION_1, StepTypeEnum.DISSEMINATION,
                                                   new StepState(0, 0, 0));
        sessionStep3.setOutputRelated(10);
        sessionStep3.setLastUpdateDate(LAST_UPDATED.minusSeconds(1));
        sessionStepList.add(sessionStep3);

        // PUBLISH EVENTS
        List<SessionStepEvent> eventSet = new ArrayList<>();
        sessionStepList.forEach(step -> eventSet.add(new SessionStepEvent(step)));
        publisher.publish(eventSet);

        return sessionStepList.size();
    }

    private void checkResults() {
        // check sessions
        List<Session> sessionList = this.sessionRepo.findAll();
        Assert.assertEquals("Wrong number of sessions created", 3, sessionList.size());
        for (Session session : sessionList) {
            String sourceName = session.getSource();
            String sessionName = session.getName();

            if (sourceName.equals(SOURCE_1) && sessionName.equals(SESSION_1)) {
                Assert.assertEquals("Wrong lastUpdateDate", LAST_UPDATED.minusMinutes(1), session.getLastUpdateDate());
                Assert.assertTrue("Wrong running state", session.getManagerState().isRunning());
                Assert.assertFalse("Wrong error state", session.getManagerState().isErrors());
                Assert.assertFalse("Wrong waiting state", session.getManagerState().isWaiting());
                Assert.assertEquals("Wrong number of sessionSteps linked", 2, session.getSteps().size());

            } else if (sourceName.equals(SOURCE_1) && sessionName.equals(SESSION_2)) {
                Assert.assertEquals("Wrong lastUpdateDate", LAST_UPDATED.minusMinutes(5), session.getLastUpdateDate());
                Assert.assertFalse("Wrong running state", session.getManagerState().isRunning());
                Assert.assertTrue("Wrong error state", session.getManagerState().isErrors());
                Assert.assertFalse("Wrong waiting state", session.getManagerState().isWaiting());
                Assert.assertEquals("Wrong number of sessionSteps linked", 1, session.getSteps().size());

            } else if (sourceName.equals(SOURCE_2) && sessionName.equals(SESSION_1)) {
                Assert.assertEquals("Wrong lastUpdateDate", LAST_UPDATED.minusMinutes(12), session.getLastUpdateDate());
                Assert.assertFalse("Wrong running state", session.getManagerState().isRunning());
                Assert.assertFalse("Wrong error state", session.getManagerState().isErrors());
                Assert.assertTrue("Wrong waiting state", session.getManagerState().isWaiting());
                Assert.assertEquals("Wrong number of sessionSteps linked", 1, session.getSteps().size());
            } else {
                Assert.fail(String.format("Unexpected session created %s for source %s", sessionName, sourceName));
            }
        }

        // check sources aggregations
        List<Source> sourcesList = this.sourceRepo.findAll();
        Assert.assertEquals("Wrong number of sources created", 2, sourcesList.size());
        for (Source source : sourcesList) {
            String sourceName = source.getName();
            if (sourceName.equals(SOURCE_1)) {
                Assert.assertEquals("Wrong lastUpdateDate", LAST_UPDATED.minusMinutes(1), source.getLastUpdateDate());
                Assert.assertEquals("Wrong number of sessions", 2, source.getNbSessions());
                Assert.assertTrue("Wrong running state", source.getManagerState().isRunning());
                Assert.assertTrue("Wrong error state", source.getManagerState().isErrors());
                Assert.assertFalse("Wrong waiting state", source.getManagerState().isWaiting());
                Set<SourceStepAggregation> aggSteps = source.getSteps();
                for (SourceStepAggregation agg : aggSteps) {
                    StepTypeEnum type = agg.getType();
                    switch (type) {
                        case ACQUISITION:
                            Assert.assertEquals("Wrong number of in", 7, agg.getTotalIn());
                            Assert.assertEquals("Wrong number of out", 0, agg.getTotalOut());
                            Assert.assertEquals("Wrong number of waiting", 0, agg.getState().getWaiting());
                            Assert.assertEquals("Wrong number of running", 2, agg.getState().getRunning());
                            Assert.assertEquals("Wrong number of errors", 5, agg.getState().getErrors());
                            break;
                        case REFERENCING:
                            Assert.assertEquals("Wrong number of in", 0, agg.getTotalIn());
                            Assert.assertEquals("Wrong number of out", 2, agg.getTotalOut());
                            Assert.assertEquals("Wrong number of waiting", 0, agg.getState().getWaiting());
                            Assert.assertEquals("Wrong number of running", 3, agg.getState().getRunning());
                            Assert.assertEquals("Wrong number of errors", 0, agg.getState().getErrors());
                            break;
                        default:
                            Assert.fail(String.format("Unexpected type %s", type));
                            break;
                    }
                }
            } else if (sourceName.equals(SOURCE_2)) {
                Assert.assertEquals("Wrong lastUpdateDate", LAST_UPDATED.minusMinutes(12), source.getLastUpdateDate());
                Assert.assertEquals("Wrong number of sessions", 1, source.getNbSessions());
                Assert.assertFalse("Wrong running state", source.getManagerState().isRunning());
                Assert.assertFalse("Wrong error state", source.getManagerState().isErrors());
                Assert.assertTrue("Wrong waiting state", source.getManagerState().isWaiting());
                Set<SourceStepAggregation> aggSteps = source.getSteps();
                for (SourceStepAggregation agg : aggSteps) {
                    StepTypeEnum type = agg.getType();
                    switch (type) {
                        case DISSEMINATION:
                            Assert.assertEquals("Wrong number of in", 0, agg.getTotalIn());
                            Assert.assertEquals("Wrong number of out", 10, agg.getTotalOut());
                            Assert.assertEquals("Wrong number of waiting", 10, agg.getState().getWaiting());
                            Assert.assertEquals("Wrong number of running", 0, agg.getState().getRunning());
                            Assert.assertEquals("Wrong number of errors", 0, agg.getState().getErrors());
                            break;
                        default:
                            Assert.fail(String.format("Unexpected type %s", type));
                            break;
                    }
                }
            } else {
                Assert.fail(String.format("Unexpected source created %s", sourceName));
            }
        }
    }

    private void checkResults2() {
        // check sessions
        Session session = this.sessionRepo.findBySourceAndName(SOURCE_2, SESSION_1).orElse(null);
        Assert.assertEquals("Wrong lastUpdateDate", LAST_UPDATED.minusSeconds(1), session.getLastUpdateDate());
        Assert.assertFalse("Wrong running state", session.getManagerState().isRunning());
        Assert.assertFalse("Wrong error state", session.getManagerState().isErrors());
        Assert.assertFalse("Wrong waiting state", session.getManagerState().isWaiting());
        Assert.assertEquals("Wrong number of sessionSteps linked", 1, session.getSteps().size());

        // check source aggregations
        Source source = this.sourceRepo.findByName(SOURCE_2).orElse(null);
        Assert.assertEquals("Wrong lastUpdateDate", LAST_UPDATED.minusSeconds(1), source.getLastUpdateDate());
        Assert.assertEquals("Wrong number of sessions", 1, source.getNbSessions());
        Assert.assertFalse("Wrong running state", source.getManagerState().isRunning());
        Assert.assertFalse("Wrong error state", source.getManagerState().isErrors());
        Assert.assertFalse("Wrong waiting state", source.getManagerState().isWaiting());
        Set<SourceStepAggregation> aggSteps = source.getSteps();
        for (SourceStepAggregation agg : aggSteps) {
            StepTypeEnum type = agg.getType();
            switch (type) {
                case DISSEMINATION:
                    Assert.assertEquals("Wrong number of in", 0, agg.getTotalIn());
                    Assert.assertEquals("Wrong number of out", 10, agg.getTotalOut());
                    Assert.assertEquals("Wrong number of waiting", 0, agg.getState().getWaiting());
                    Assert.assertEquals("Wrong number of running", 0, agg.getState().getRunning());
                    Assert.assertEquals("Wrong number of errors", 0, agg.getState().getErrors());
                    break;
                default:
                    Assert.fail(String.format("Unexpected type %s", type));
                    break;
            }
        }
    }
}