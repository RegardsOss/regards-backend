/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.session.agent.service.update;

import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;
import fr.cnes.regards.framework.modules.session.agent.service.AbstractAgentServiceUtilsIT;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionStepEvent;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Test for {@link AgentSnapshotJobService}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=agent_job_service_it",
                                   "regards.session.step.merge-similar-event=false" })
@ActiveProfiles({ "testAmqp", "noscheduler" })
public class AgentSnapshotJobServiceIT extends AbstractAgentServiceUtilsIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSnapshotJobServiceIT.class);

    @Autowired
    private AgentSnapshotJobService agentJobSnapshotService;

    @Test
    @Purpose("Test the generation of session steps from step request events")
    public void generateSessionStepTest1() throws InterruptedException {
        // ---- RUN 1 ----
        // create stepPropertyUpdateRequestEvents
        int nbEvents = createRun1StepEvents();

        // wait for stepPropertyUpdateRequestEvent to be stored in database
        waitForStepPropertyEventsStored(nbEvents);

        // retrieve associated snapshot process
        try {
            Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                return this.snapshotProcessRepo.findAll().size() == 3;
            });
        } catch (ConditionTimeoutException e) {
            LOGGER.error("Wrong number of snapshot process created");
            throw e;
        }

        // wait for job to be in success state
        agentJobSnapshotService.scheduleJob();
        waitForJobSuccesses(AgentSnapshotJob.class.getName(), 3, 20000L);

        // wait for snapshot process to be updated
        boolean isSnapshotProcessesUpdated = waitForSnapshotUpdateSuccesses();
        if (!isSnapshotProcessesUpdated) {
            Assert.fail("Snapshot were not updated");
        }
        // check SessionStep event was published and verify results
        Mockito.verify(publisher, Mockito.times(4)).publish(Mockito.any(SessionStepEvent.class));
        checkResult1();

        // ---- RUN 2 ----
        // init parameters
        Mockito.clearInvocations(publisher);
        List<SnapshotProcess> snapshotProcessesCreated = this.snapshotProcessRepo.findAll();

        // create stepPropertyUpdateRequestEvents
        nbEvents += createRun2StepEvents();
        waitForStepPropertyEventsStored(nbEvents);

        // wait for job to be in success state
        agentJobSnapshotService.scheduleJob();
        waitForJobSuccesses(AgentSnapshotJob.class.getName(), 4, 20000L);

        // wait for snapshot process to be updated
        isSnapshotProcessesUpdated = waitForSnapshotUpdateSuccesses();
        if (!isSnapshotProcessesUpdated) {
            Assert.fail("Snapshot were not updated");
        }

        // check SessionStep event was published and verify results
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(SessionStepEvent.class));
        checkResult2(snapshotProcessesCreated);
    }

    private int createRun1StepEvents() {
        List<StepPropertyUpdateRequestEvent> stepRequests = new ArrayList<>();

        // ACQUISITION - scan event SOURCE 1 OWNER 1
        stepRequests.add(new StepPropertyUpdateRequestEvent(new StepProperty("scan",
                                                                             SOURCE_1,
                                                                             OWNER_1,
                                                                             new StepPropertyInfo(StepTypeEnum.ACQUISITION,
                                                                                                  StepPropertyStateEnum.SUCCESS,
                                                                                                  "gen.products",
                                                                                                  "10",
                                                                                                  true,
                                                                                                  false)),
                                                            StepPropertyEventTypeEnum.INC));

        stepRequests.add(new StepPropertyUpdateRequestEvent(new StepProperty("scan",
                                                                             SOURCE_1,
                                                                             OWNER_1,
                                                                             new StepPropertyInfo(StepTypeEnum.ACQUISITION,
                                                                                                  StepPropertyStateEnum.SUCCESS,
                                                                                                  "gen.products",
                                                                                                  "5",
                                                                                                  true,
                                                                                                  false)),
                                                            StepPropertyEventTypeEnum.INC));
        // ACQUISITION - scan event SOURCE 1 OWNER 2
        stepRequests.add(new StepPropertyUpdateRequestEvent(new StepProperty("scan",
                                                                             SOURCE_1,
                                                                             OWNER_2,
                                                                             new StepPropertyInfo(StepTypeEnum.ACQUISITION,
                                                                                                  StepPropertyStateEnum.SUCCESS,
                                                                                                  "gen.products",
                                                                                                  "8",
                                                                                                  true,
                                                                                                  false)),
                                                            StepPropertyEventTypeEnum.INC));

        stepRequests.add(new StepPropertyUpdateRequestEvent(new StepProperty("scan",
                                                                             SOURCE_1,
                                                                             OWNER_2,
                                                                             new StepPropertyInfo(StepTypeEnum.ACQUISITION,
                                                                                                  StepPropertyStateEnum.RUNNING,
                                                                                                  "gen.products",
                                                                                                  "4",
                                                                                                  true,
                                                                                                  false)),
                                                            StepPropertyEventTypeEnum.INC));

        // REFERENCING - oais event SOURCE 2 OWNER 1
        stepRequests.add(new StepPropertyUpdateRequestEvent(new StepProperty("oais",
                                                                             SOURCE_2,
                                                                             OWNER_1,
                                                                             new StepPropertyInfo(StepTypeEnum.REFERENCING,
                                                                                                  StepPropertyStateEnum.ERROR,
                                                                                                  "ref.products.errors",
                                                                                                  "6",
                                                                                                  false,
                                                                                                  false)),
                                                            StepPropertyEventTypeEnum.INC));
        stepRequests.add(new StepPropertyUpdateRequestEvent(new StepProperty("oais",
                                                                             SOURCE_2,
                                                                             OWNER_1,
                                                                             new StepPropertyInfo(StepTypeEnum.REFERENCING,
                                                                                                  StepPropertyStateEnum.ERROR,
                                                                                                  "ref.products.state",
                                                                                                  "ERROR",
                                                                                                  false,
                                                                                                  false)),
                                                            StepPropertyEventTypeEnum.VALUE));
        // REFERENCING - oais event SOURCE 3 OWNER 1
        stepRequests.add(new StepPropertyUpdateRequestEvent(new StepProperty("oais",
                                                                             SOURCE_3,
                                                                             OWNER_1,
                                                                             new StepPropertyInfo(StepTypeEnum.REFERENCING,
                                                                                                  StepPropertyStateEnum.WAITING,
                                                                                                  "ref.products.pending",
                                                                                                  "3",
                                                                                                  false,
                                                                                                  false)),
                                                            StepPropertyEventTypeEnum.INC));

        // Publish events
        this.publisher.publish(stepRequests);
        return stepRequests.size();
    }

    public int createRun2StepEvents() {
        List<StepPropertyUpdateRequestEvent> stepRequests = new ArrayList<>();

        // UPDATE REFERENCING - oais event SOURCE 3 OWNER 1
        stepRequests.add(new StepPropertyUpdateRequestEvent(new StepProperty("oais",
                                                                             SOURCE_3,
                                                                             OWNER_1,
                                                                             new StepPropertyInfo(StepTypeEnum.REFERENCING,
                                                                                                  StepPropertyStateEnum.WAITING,
                                                                                                  "ref.products.pending",
                                                                                                  "3",
                                                                                                  false,
                                                                                                  false)),
                                                            StepPropertyEventTypeEnum.DEC));
        stepRequests.add(new StepPropertyUpdateRequestEvent(new StepProperty("oais",
                                                                             SOURCE_3,
                                                                             OWNER_1,
                                                                             new StepPropertyInfo(StepTypeEnum.REFERENCING,
                                                                                                  StepPropertyStateEnum.SUCCESS,
                                                                                                  "ref.products",
                                                                                                  "3",
                                                                                                  false,
                                                                                                  true)),
                                                            StepPropertyEventTypeEnum.INC));
        // Publish events
        this.publisher.publish(stepRequests);
        return stepRequests.size();
    }

    private void checkResult1() {
        List<SessionStep> sessionSteps = this.sessionStepRepo.findAll();
        Assert.assertEquals("Wrong number of session steps created", 4, sessionSteps.size());
        for (SessionStep sessionStep : sessionSteps) {
            String source = sessionStep.getSource();
            String session = sessionStep.getSession();
            SessionStepProperties properties = sessionStep.getProperties();

            if (source.equals(SOURCE_1) && session.equals(OWNER_1)) {
                Assert.assertEquals("Wrong type", StepTypeEnum.ACQUISITION, sessionStep.getType());
                Assert.assertEquals("Wrong num of input related", 15L, sessionStep.getInputRelated());
                Assert.assertEquals("Wrong num of output related", 0L, sessionStep.getOutputRelated());
                Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
                Assert.assertNotNull("Wrong last update date", sessionStep.getLastUpdateDate());
                Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                Assert.assertEquals("Wrong properties", "15", properties.get("gen.products"));
            } else if (source.equals(SOURCE_1) && session.equals(OWNER_2)) {
                Assert.assertEquals("Wrong type", StepTypeEnum.ACQUISITION, sessionStep.getType());
                Assert.assertEquals("Wrong num of input related", 12L, sessionStep.getInputRelated());
                Assert.assertEquals("Wrong num of output related", 0L, sessionStep.getOutputRelated());
                Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                Assert.assertEquals("Wrong num of running", 4L, sessionStep.getState().getRunning());
                Assert.assertNotNull("Wrong last update date", sessionStep.getLastUpdateDate());
                Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                Assert.assertEquals("Wrong properties", "12", properties.get("gen.products"));
            } else if (source.equals(SOURCE_2) && session.equals(OWNER_1)) {
                Assert.assertEquals("Wrong type", StepTypeEnum.REFERENCING, sessionStep.getType());
                Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                Assert.assertEquals("Wrong num of output related", 0L, sessionStep.getOutputRelated());
                Assert.assertEquals("Wrong num of errors", 6L, sessionStep.getState().getErrors());
                Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
                Assert.assertNotNull("Wrong last update date", sessionStep.getLastUpdateDate());
                Assert.assertTrue("Wrong properties", properties.containsKey("ref.products.errors"));
                Assert.assertEquals("Wrong properties", "6", properties.get("ref.products.errors"));
                Assert.assertTrue("Wrong properties", properties.containsKey("ref.products.state"));
                Assert.assertEquals("Wrong properties", "ERROR", properties.get("ref.products.state"));
            } else if (source.equals(SOURCE_3) && session.equals(OWNER_1)) {
                Assert.assertEquals("Wrong type", StepTypeEnum.REFERENCING, sessionStep.getType());
                Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                Assert.assertEquals("Wrong num of output related", 0L, sessionStep.getOutputRelated());
                Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                Assert.assertEquals("Wrong num of waiting", 3L, sessionStep.getState().getWaiting());
                Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
                Assert.assertNotNull("Wrong last update date", sessionStep.getLastUpdateDate());
                Assert.assertTrue("Wrong properties", properties.containsKey("ref.products.pending"));
                Assert.assertEquals("Wrong properties", "3", properties.get("ref.products.pending"));
            } else {
                Assert.fail(String.format("Unexpected step source %s or session %s created", source, session));
            }
        }
    }

    private void checkResult2(List<SnapshotProcess> snapshotProcessesCreated) {
        // check properties were updated
        Optional<SessionStep> sessionStepOpt = this.sessionStepRepo.findBySourceAndSessionAndStepId(SOURCE_3,
                                                                                                    OWNER_1,
                                                                                                    "oais");
        Assert.assertTrue("Session should have been present", sessionStepOpt.isPresent());
        SessionStep sessionStep = sessionStepOpt.get();
        SessionStepProperties properties = sessionStep.getProperties();
        Assert.assertEquals("Wrong type", StepTypeEnum.REFERENCING, sessionStep.getType());
        Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
        Assert.assertEquals("Wrong num of output related", 3L, sessionStep.getOutputRelated());
        Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
        Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
        Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
        Assert.assertNotNull("Wrong last update date", sessionStep.getLastUpdateDate());
        Assert.assertTrue("Wrong properties", properties.containsKey("ref.products.pending"));
        Assert.assertEquals("Wrong properties", "0", properties.get("ref.products.pending"));
        Assert.assertTrue("Wrong properties", properties.containsKey("ref.products"));
        Assert.assertEquals("Wrong properties", "3", properties.get("ref.products"));

        // check if only SOURCE_3 snapshotProcess was updated
        List<SnapshotProcess> snapshotProcessUpdated = this.snapshotProcessRepo.findAll();
        for (SnapshotProcess snapshotProcess : snapshotProcessUpdated) {
            SnapshotProcess oldSnapshotProcess = snapshotProcessesCreated.stream()
                                                                         .filter(snapshotProcess::equals)
                                                                         .findFirst()
                                                                         .orElse(null);
            Assert.assertNotNull("SnapshotProcess should have been present", oldSnapshotProcess);
            if (!oldSnapshotProcess.getSource().equals(SOURCE_3)) {
                Assert.assertEquals("lastUpdateDate should not have changed",
                                    oldSnapshotProcess.getLastUpdateDate(),
                                    snapshotProcess.getLastUpdateDate());
            } else {
                Assert.assertNotEquals("lastUpdateDate should have changed",
                                       oldSnapshotProcess.getLastUpdateDate(),
                                       snapshotProcess.getLastUpdateDate());
            }
        }
    }
}