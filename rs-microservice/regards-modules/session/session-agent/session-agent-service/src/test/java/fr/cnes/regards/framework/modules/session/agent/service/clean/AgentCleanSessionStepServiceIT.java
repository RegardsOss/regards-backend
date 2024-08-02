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
package fr.cnes.regards.framework.modules.session.agent.service.clean;

import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequestInfo;
import fr.cnes.regards.framework.modules.session.agent.service.AbstractAgentServiceUtilsIT;
import fr.cnes.regards.framework.modules.session.agent.service.clean.sessionstep.AgentCleanSessionStepService;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Test for {@link AgentCleanSessionStepService}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=agent_clean_service_it",
                                   "regards.session-agent.limit.store.session-steps=30" })
@ActiveProfiles({ "noscheduler" })
public class AgentCleanSessionStepServiceIT extends AbstractAgentServiceUtilsIT {

    private static OffsetDateTime CREATION_DATE;

    @Value("${regards.session-agent.limit.store.session-steps}")
    private int limitStore;

    @Override
    public void doInit() {
        CREATION_DATE = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2 * limitStore);
    }

    @Test
    @Purpose("Test if old session steps and related step requests are correctly deleted")
    public void cleanSessionStepsTest() {
        OffsetDateTime freezeDate = OffsetDateTime.now(ZoneOffset.UTC);

        // GENERATE STEP UPDATE REQUEST
        List<StepPropertyUpdateRequest> stepRequests = createRunStepEvents();
        Assert.assertEquals("Wrong number of stepPropertyUpdateRequests created", 6, stepRequests.size());

        // SOURCE 1 - launch the generation of sessionSteps from StepPropertyUpdateRequest
        SnapshotProcess snapshotProcess = new SnapshotProcess(SOURCE_1, CREATION_DATE, null);
        agentSnapshotService.generateSessionStep(snapshotProcess, freezeDate);

        // SOURCE 2 - launch the generation of sessionSteps from StepPropertyUpdateRequest
        SnapshotProcess snapshotProcess2 = new SnapshotProcess(SOURCE_2, CREATION_DATE, null);
        agentSnapshotService.generateSessionStep(snapshotProcess2, freezeDate);

        // CLEAN SESSION STEPS AND STEP PROPERTY UPDATE REQUESTS
        int nbSessionStepsDeleted = agentCleanSessionStepService.clean();
        checkClean(nbSessionStepsDeleted, stepRequests);
    }

    private void checkClean(int nbSessionStepsDeleted, List<StepPropertyUpdateRequest> stepRequests) {
        // SOURCE 1 - OWNER 1  should be the only session deleted
        Assert.assertEquals("Wrong number of session steps deleted", 2, nbSessionStepsDeleted);

        // SOURCE 1 - OWNER 1 - scan should be deleted with all related events
        Assert.assertFalse("Step property request should have been deleted",
                           stepPropertyRepo.findById(stepRequests.get(0).getId()).isPresent());
        Assert.assertFalse("Step property request  should have been deleted",
                           stepPropertyRepo.findById(stepRequests.get(1).getId()).isPresent());
        Assert.assertFalse("Session step should have been deleted",
                           sessionStepRepo.findBySourceAndSessionAndStepId(SOURCE_1, OWNER_1, "scan").isPresent());

        // SOURCE 1 - OWNER 1 - oais should be deleted with all related events
        Assert.assertFalse("Step property request should have been deleted",
                           stepPropertyRepo.findById(stepRequests.get(2).getId()).isPresent());
        Assert.assertFalse("Session step should have been deleted",
                           sessionStepRepo.findBySourceAndSessionAndStepId(SOURCE_1, OWNER_1, "oais").isPresent());

        // SOURCE 1 - OWNER 2 - scan should be present with all related events
        Assert.assertTrue("Step property request should have been present",
                          stepPropertyRepo.findById(stepRequests.get(3).getId()).isPresent());
        Assert.assertTrue("Step property request  should have been present",
                          stepPropertyRepo.findById(stepRequests.get(4).getId()).isPresent());
        Assert.assertTrue("Session step should have been present",
                          sessionStepRepo.findBySourceAndSessionAndStepId(SOURCE_1, OWNER_2, "scan").isPresent());

        // SOURCE 2 - OWNER 1 - oais should be present with all related events
        Assert.assertTrue("Step property request  should have been present",
                          stepPropertyRepo.findById(stepRequests.get(5).getId()).isPresent());
        Assert.assertTrue("Session step should have been present",
                          sessionStepRepo.findBySourceAndSessionAndStepId(SOURCE_2, OWNER_1, "oais").isPresent());

    }

    private List<StepPropertyUpdateRequest> createRunStepEvents() {
        List<StepPropertyUpdateRequest> stepRequests = new ArrayList<>();

        // ACQUISITION - SOURCE 1 / scan event OWNER 1
        StepPropertyUpdateRequest step1 = new StepPropertyUpdateRequest("scan",
                                                                        SOURCE_1,
                                                                        OWNER_1,
                                                                        CREATION_DATE.plusSeconds(1),
                                                                        StepPropertyEventTypeEnum.INC,
                                                                        new StepPropertyUpdateRequestInfo(StepTypeEnum.ACQUISITION,
                                                                                                          StepPropertyStateEnum.SUCCESS,
                                                                                                          "gen.products",
                                                                                                          "10",
                                                                                                          true,
                                                                                                          false));

        StepPropertyUpdateRequest step2 = new StepPropertyUpdateRequest("scan",
                                                                        SOURCE_1,
                                                                        OWNER_1,
                                                                        CREATION_DATE.plusMinutes(1),
                                                                        StepPropertyEventTypeEnum.INC,
                                                                        new StepPropertyUpdateRequestInfo(StepTypeEnum.ACQUISITION,
                                                                                                          StepPropertyStateEnum.SUCCESS,
                                                                                                          "gen.products",
                                                                                                          "10",
                                                                                                          true,
                                                                                                          false));
        step2.setRegistrationDate(step2.getCreationDate());

        // REFERENCING - SOURCE 1 / oais event OWNER 1
        StepPropertyUpdateRequest step3 = new StepPropertyUpdateRequest("oais",
                                                                        SOURCE_1,
                                                                        OWNER_1,
                                                                        CREATION_DATE.plusDays(limitStore),
                                                                        StepPropertyEventTypeEnum.INC,
                                                                        new StepPropertyUpdateRequestInfo(StepTypeEnum.REFERENCING,
                                                                                                          StepPropertyStateEnum.SUCCESS,
                                                                                                          "gen.products",
                                                                                                          "6",
                                                                                                          false,
                                                                                                          true));

        // ACQUISITION - SOURCE 1 / scan event OWNER 2
        StepPropertyUpdateRequest step4 = new StepPropertyUpdateRequest("scan",
                                                                        SOURCE_1,
                                                                        OWNER_2,
                                                                        CREATION_DATE.plusSeconds(1),
                                                                        StepPropertyEventTypeEnum.INC,
                                                                        new StepPropertyUpdateRequestInfo(StepTypeEnum.ACQUISITION,
                                                                                                          StepPropertyStateEnum.SUCCESS,
                                                                                                          "gen.products",
                                                                                                          "8",
                                                                                                          true,
                                                                                                          false));

        StepPropertyUpdateRequest step5 = new StepPropertyUpdateRequest("scan",
                                                                        SOURCE_1,
                                                                        OWNER_2,
                                                                        CREATION_DATE.plusDays(limitStore + 1),
                                                                        StepPropertyEventTypeEnum.INC,
                                                                        new StepPropertyUpdateRequestInfo(StepTypeEnum.ACQUISITION,
                                                                                                          StepPropertyStateEnum.SUCCESS,
                                                                                                          "gen.products",
                                                                                                          "4",
                                                                                                          true,
                                                                                                          false));

        // REFERENCING - SOURCE 2 / oais event OWNER 1
        StepPropertyUpdateRequest step6 = new StepPropertyUpdateRequest("oais",
                                                                        SOURCE_2,
                                                                        OWNER_1,
                                                                        CREATION_DATE.plusDays(limitStore + 1),
                                                                        StepPropertyEventTypeEnum.INC,
                                                                        new StepPropertyUpdateRequestInfo(StepTypeEnum.REFERENCING,
                                                                                                          StepPropertyStateEnum.SUCCESS,
                                                                                                          "gen.products",
                                                                                                          "6",
                                                                                                          false,
                                                                                                          true));
        // set registration dates to creation dates
        step1.setRegistrationDate(step1.getCreationDate());
        step2.setRegistrationDate(step2.getCreationDate());
        step3.setRegistrationDate(step3.getCreationDate());
        step4.setRegistrationDate(step4.getCreationDate());
        step5.setRegistrationDate(step5.getCreationDate());
        step6.setRegistrationDate(step6.getCreationDate());

        // add steps
        stepRequests.add(step1);
        stepRequests.add(step2);
        stepRequests.add(step3);
        stepRequests.add(step4);
        stepRequests.add(step5);
        stepRequests.add(step6);

        return this.stepPropertyRepo.saveAll(stepRequests);
    }
}