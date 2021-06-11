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
package fr.cnes.regards.framework.modules.session.agent.service.update;

import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequestInfo;
import fr.cnes.regards.framework.modules.session.agent.service.AbstractAgentServiceUtilsTest;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for {@link AgentSnapshotService}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=agent_snapshot_service_it",
        "regards.session.agent.step.requests.page.size=2" })
@ActiveProfiles({ "noscheduler" })
public class AgentSnapshotServiceIT extends AbstractAgentServiceUtilsTest {

    private static final OffsetDateTime CREATION_DATE = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);

    @Test
    @Purpose("Test if session steps are correctly generated from step property requests")
    public void generateSessionStepTest() {
        // launch the generation of sessionSteps from StepPropertyUpdateRequest
        SnapshotProcess snapshotProcess = this.snapshotProcessRepo
                .save(new SnapshotProcess(SOURCE_1, CREATION_DATE, null));

        List<StepPropertyUpdateRequest> stepRequests = createRun1StepEvents();
        Assert.assertEquals("Wrong number of stepPropertyUpdateRequests created", 9, stepRequests.size());

        OffsetDateTime freezeDate = CREATION_DATE.plusMinutes(22);
        int nbSessionStepsCreated = agentSnapshotService.generateSessionStep(snapshotProcess, freezeDate);
        checkRun1Test(nbSessionStepsCreated);

        // launch the second run with same source and session owner, fields should be updated
        snapshotProcess = this.snapshotProcessRepo.findBySource(SOURCE_1).orElse(null);
        Assert.assertEquals("Snapshot lastUpdateDate should have been updated", CREATION_DATE.plusMinutes(20),
                            snapshotProcess.getLastUpdateDate());
        List<StepPropertyUpdateRequest> stepRequests2 = createRun2StepEvents();
        Assert.assertEquals("Wrong number of stepPropertyUpdateRequests created", 5, stepRequests2.size());

        OffsetDateTime freezeDate2 = CREATION_DATE.plusMinutes(50);
        int nbSessionStepsCreated2 = agentSnapshotService.generateSessionStep(snapshotProcess, freezeDate2);
        checkRun2Test(nbSessionStepsCreated2);
    }

    private List<StepPropertyUpdateRequest> createRun1StepEvents() {
        List<StepPropertyUpdateRequest> stepRequests = new ArrayList<>();

        // ACQUISITION - scan event
        stepRequests.add(new StepPropertyUpdateRequest("scan", SOURCE_1, OWNER_1, CREATION_DATE.plusSeconds(1),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.ACQUISITION,
                                                                                         StepPropertyStateEnum.SUCCESS,
                                                                                         "gen.products", "2", true,
                                                                                         false)));

        stepRequests.add(new StepPropertyUpdateRequest("scan", SOURCE_1, OWNER_1, CREATION_DATE.plusMinutes(1),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.ACQUISITION,
                                                                                         StepPropertyStateEnum.SUCCESS,
                                                                                         "gen.products", "4", true,
                                                                                         false)));

        // REFERENCING - oais event
        stepRequests.add(new StepPropertyUpdateRequest("oais", SOURCE_1, OWNER_1, CREATION_DATE.plusMinutes(2),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.REFERENCING,
                                                                                         StepPropertyStateEnum.SUCCESS,
                                                                                         "gen.products", "6", false,
                                                                                         true)));

        // STORAGE - storage event
        stepRequests.add(new StepPropertyUpdateRequest("storage", SOURCE_1, OWNER_1, CREATION_DATE.plusMinutes(5),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.STORAGE,
                                                                                         StepPropertyStateEnum.SUCCESS,
                                                                                         "store.products", "2", false,
                                                                                         true)));
        stepRequests.add(new StepPropertyUpdateRequest("storage", SOURCE_1, OWNER_1, CREATION_DATE.plusMinutes(3),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.STORAGE,
                                                                                         StepPropertyStateEnum.ERROR,
                                                                                         "store.products.errors", "4",
                                                                                         false, false)));
        stepRequests.add(new StepPropertyUpdateRequest("storage", SOURCE_1, OWNER_1, CREATION_DATE.plusMinutes(4),
                                                       StepPropertyEventTypeEnum.VALUE,
                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.STORAGE,
                                                                                         StepPropertyStateEnum.ERROR,
                                                                                         "store.products.state",
                                                                                         "ERROR", false, false)));
        // DISSEMINATION - metacatalog event
        stepRequests.add(new StepPropertyUpdateRequest("metacatalog", SOURCE_1, OWNER_1, CREATION_DATE.plusMinutes(7),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.DISSEMINATION,
                                                                                         StepPropertyStateEnum.WAITING,
                                                                                         "dis.products.pending", "2",
                                                                                         false, false)));

        // OTHER EVENTS NOT RELATED TO SESSION 1
        stepRequests.add(new StepPropertyUpdateRequest("scan", SOURCE_1, OWNER_2, CREATION_DATE.plusMinutes(20),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.ACQUISITION,
                                                                                         StepPropertyStateEnum.SUCCESS,
                                                                                         "gen.products", "2", true,
                                                                                         false)));
        stepRequests.add(new StepPropertyUpdateRequest("storage", SOURCE_1, OWNER_3, CREATION_DATE.plusMinutes(20),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.STORAGE,
                                                                                         StepPropertyStateEnum.SUCCESS,
                                                                                         "store.products", "6", false,
                                                                                         true)));

        return this.stepPropertyRepo.saveAll(stepRequests);
    }

    private List<StepPropertyUpdateRequest> createRun2StepEvents() {
        List<StepPropertyUpdateRequest> stepRequests = new ArrayList<>();
        // STORAGE - storage event
        stepRequests.add(new StepPropertyUpdateRequest("storage", SOURCE_1, OWNER_1, CREATION_DATE.plusMinutes(30),
                                                       StepPropertyEventTypeEnum.DEC,
                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.STORAGE,
                                                                                         StepPropertyStateEnum.ERROR,
                                                                                         "store.products.errors", "4",
                                                                                         false, false)));
        stepRequests.add(new StepPropertyUpdateRequest("storage", SOURCE_1, OWNER_1, CREATION_DATE.plusMinutes(35),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.STORAGE,
                                                                                         StepPropertyStateEnum.RUNNING,
                                                                                         "store.products", "4", false,
                                                                                         true)));
        stepRequests.add(new StepPropertyUpdateRequest("storage", SOURCE_1, OWNER_1, CREATION_DATE.plusMinutes(38),
                                                       StepPropertyEventTypeEnum.VALUE,
                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.STORAGE,
                                                                                         StepPropertyStateEnum.INFO,
                                                                                         "store.products.state",
                                                                                         "RUNNING", false, false)));
        // DISSEMINATION - metacatalog event
        stepRequests.add(new StepPropertyUpdateRequest("metacatalog", SOURCE_1, OWNER_1, CREATION_DATE.plusMinutes(40),
                                                       StepPropertyEventTypeEnum.DEC,
                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.DISSEMINATION,
                                                                                         StepPropertyStateEnum.WAITING,
                                                                                         "dis.products.pending", "2",
                                                                                         false, false)));

        stepRequests.add(new StepPropertyUpdateRequest("metacatalog", SOURCE_1, OWNER_1, CREATION_DATE.plusMinutes(40),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyUpdateRequestInfo(StepTypeEnum.DISSEMINATION,
                                                                                         StepPropertyStateEnum.SUCCESS,
                                                                                         "dis.products", "2", false,
                                                                                         true)));
        return this.stepPropertyRepo.saveAll(stepRequests);
    }

    public void checkRun1Test(int nbSessionStepsCreated) {
        Assert.assertEquals("Wrong number of session steps created/updated", 6, nbSessionStepsCreated);
        List<SessionStep> sessionSteps = this.sessionStepRepo.findAll();

        // loop on every session steps and check parameters
        for (SessionStep sessionStep : sessionSteps) {
            Assert.assertEquals("Wrong source", SOURCE_1, sessionStep.getSource());
            String session = sessionStep.getSession();
            SessionStepProperties properties = sessionStep.getProperties();
            // IF SESSION IS OWNER 1
            if (session.equals(OWNER_1)) {
                String step = sessionStep.getStepId();
                switch (step) {
                    case "scan":
                        Assert.assertEquals("Wrong type", StepTypeEnum.ACQUISITION, sessionStep.getType());
                        Assert.assertEquals("Wrong num of input related", 6L, sessionStep.getInputRelated());
                        Assert.assertEquals("Wrong num of output related", 0L, sessionStep.getOutputRelated());
                        Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                        Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                        Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(1),
                                            sessionStep.getLastUpdateDate());
                        Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                        Assert.assertEquals("Wrong properties", "6", properties.get("gen.products"));
                        break;
                    case "oais":
                        Assert.assertEquals("Wrong type", StepTypeEnum.REFERENCING, sessionStep.getType());
                        Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                        Assert.assertEquals("Wrong num of output related", 6L, sessionStep.getOutputRelated());
                        Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                        Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                        Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(2),
                                            sessionStep.getLastUpdateDate());
                        Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                        Assert.assertEquals("Wrong properties", "6", properties.get("gen.products"));
                        break;
                    case "storage":
                        Assert.assertEquals("Wrong type", StepTypeEnum.STORAGE, sessionStep.getType());
                        Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                        Assert.assertEquals("Wrong num of output related", 2L, sessionStep.getOutputRelated());
                        Assert.assertEquals("Wrong num of errors", 4L, sessionStep.getState().getErrors());
                        Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                        Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(5),
                                            sessionStep.getLastUpdateDate());
                        Assert.assertTrue("Wrong properties", properties.containsKey("store.products"));
                        Assert.assertEquals("Wrong properties", "2", properties.get("store.products"));
                        Assert.assertTrue("Wrong properties", properties.containsKey("store.products.state"));
                        Assert.assertEquals("Wrong properties", "ERROR", properties.get("store.products.state"));
                        Assert.assertTrue("Wrong properties", properties.containsKey("store.products.errors"));
                        Assert.assertEquals("Wrong properties", "4", properties.get("store.products.errors"));
                        break;
                    case "metacatalog":
                        Assert.assertEquals("Wrong type", StepTypeEnum.DISSEMINATION, sessionStep.getType());
                        Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                        Assert.assertEquals("Wrong num of output related", 0L, sessionStep.getOutputRelated());
                        Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                        Assert.assertEquals("Wrong num of waiting", 2L, sessionStep.getState().getWaiting());
                        Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(7),
                                            sessionStep.getLastUpdateDate());
                        Assert.assertTrue("Wrong properties", properties.containsKey("dis.products.pending"));
                        Assert.assertEquals("Wrong properties", "2", properties.get("dis.products.pending"));
                        break;
                    default:
                        Assert.fail(String.format("Unexpected step created", step));
                        break;
                }
            } else if (session.equals(OWNER_2)) {
                Assert.assertEquals("Wrong stepId", "scan", sessionStep.getStepId());
                Assert.assertEquals("Wrong type", StepTypeEnum.ACQUISITION, sessionStep.getType());
                Assert.assertEquals("Wrong num of input related", 2L, sessionStep.getInputRelated());
                Assert.assertEquals("Wrong num of output related", 0L, sessionStep.getOutputRelated());
                Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
                Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(20),
                                    sessionStep.getLastUpdateDate());
                Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                Assert.assertEquals("Wrong properties", "2", properties.get("gen.products"));
            } else if (session.equals(OWNER_3)) {
                Assert.assertEquals("Wrong stepId", "storage", sessionStep.getStepId());
                Assert.assertEquals("Wrong type", StepTypeEnum.STORAGE, sessionStep.getType());
                Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                Assert.assertEquals("Wrong num of output related", 6L, sessionStep.getOutputRelated());
                Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
                Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(20),
                                    sessionStep.getLastUpdateDate());
                Assert.assertTrue("Wrong properties", properties.containsKey("store.products"));
                Assert.assertEquals("Wrong properties", "6", properties.get("store.products"));
            } else {
                Assert.fail(String.format("Unexpected session created", session));
            }
        }
    }

    public void checkRun2Test(int nbSessionStepsCreated) {
        Assert.assertEquals("Wrong number of session steps created/updated", 2, nbSessionStepsCreated);
        List<SessionStep> sessionSteps = this.sessionStepRepo.findAll();

        // loop on every session steps and check parameters
        for (SessionStep sessionStep : sessionSteps) {
            Assert.assertEquals("Wrong source", SOURCE_1, sessionStep.getSource());
            String session = sessionStep.getSession();
            SessionStepProperties properties = sessionStep.getProperties();
            // IF SESSION IS OWNER 1
            if (session.equals(OWNER_1)) {
                String step = sessionStep.getStepId();
                switch (step) {
                    case "scan":
                        Assert.assertEquals("Wrong type", StepTypeEnum.ACQUISITION, sessionStep.getType());
                        Assert.assertEquals("Wrong num of input related", 6L, sessionStep.getInputRelated());
                        Assert.assertEquals("Wrong num of output related", 0L, sessionStep.getOutputRelated());
                        Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                        Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                        Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(1),
                                            sessionStep.getLastUpdateDate());
                        Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                        Assert.assertEquals("Wrong properties", "6", properties.get("gen.products"));
                        break;
                    case "oais":
                        Assert.assertEquals("Wrong type", StepTypeEnum.REFERENCING, sessionStep.getType());
                        Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                        Assert.assertEquals("Wrong num of output related", 6L, sessionStep.getOutputRelated());
                        Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                        Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                        Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(2),
                                            sessionStep.getLastUpdateDate());
                        Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                        Assert.assertEquals("Wrong properties", "6", properties.get("gen.products"));
                        break;
                    case "storage":
                        Assert.assertEquals("Wrong type", StepTypeEnum.STORAGE, sessionStep.getType());
                        Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                        Assert.assertEquals("Wrong num of output related", 6L, sessionStep.getOutputRelated());
                        Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                        Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                        Assert.assertEquals("Wrong num of running", 4L, sessionStep.getState().getRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(38),
                                            sessionStep.getLastUpdateDate());
                        Assert.assertTrue("Wrong properties", properties.containsKey("store.products"));
                        Assert.assertEquals("Wrong properties", "6", properties.get("store.products"));
                        Assert.assertTrue("Wrong properties", properties.containsKey("store.products.state"));
                        Assert.assertEquals("Wrong properties", "RUNNING", properties.get("store.products.state"));
                        Assert.assertTrue("Wrong properties", properties.containsKey("store.products.errors"));
                        Assert.assertEquals("Wrong properties", "0", properties.get("store.products.errors"));
                        break;
                    case "metacatalog":
                        Assert.assertEquals("Wrong type", StepTypeEnum.DISSEMINATION, sessionStep.getType());
                        Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                        Assert.assertEquals("Wrong num of output related", 2L, sessionStep.getOutputRelated());
                        Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                        Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                        Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(40),
                                            sessionStep.getLastUpdateDate());
                        Assert.assertTrue("Wrong properties", properties.containsKey("dis.products"));
                        Assert.assertEquals("Wrong properties", "2", properties.get("dis.products"));
                        Assert.assertTrue("Wrong properties", properties.containsKey("dis.products.pending"));
                        Assert.assertEquals("Wrong properties", "0", properties.get("dis.products.pending"));
                        break;
                    default:
                        Assert.fail(String.format("Unexpected step created", step));
                        break;
                }
            } else if (session.equals(OWNER_2)) {
                Assert.assertEquals("Wrong stepId", "scan", sessionStep.getStepId());
                Assert.assertEquals("Wrong type", StepTypeEnum.ACQUISITION, sessionStep.getType());
                Assert.assertEquals("Wrong num of input related", 2L, sessionStep.getInputRelated());
                Assert.assertEquals("Wrong num of output related", 0L, sessionStep.getOutputRelated());
                Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
                Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(20),
                                    sessionStep.getLastUpdateDate());
                Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                Assert.assertEquals("Wrong properties", "2", properties.get("gen.products"));
            } else if (session.equals(OWNER_3)) {
                Assert.assertEquals("Wrong stepId", "storage", sessionStep.getStepId());
                Assert.assertEquals("Wrong type", StepTypeEnum.STORAGE, sessionStep.getType());
                Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                Assert.assertEquals("Wrong num of output related", 6L, sessionStep.getOutputRelated());
                Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
                Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(20),
                                    sessionStep.getLastUpdateDate());
                Assert.assertTrue("Wrong properties", properties.containsKey("store.products"));
                Assert.assertEquals("Wrong properties", "6", properties.get("store.products"));
            } else {
                Assert.fail(String.format("Unexpected session created", session));
            }
        }
    }
}
