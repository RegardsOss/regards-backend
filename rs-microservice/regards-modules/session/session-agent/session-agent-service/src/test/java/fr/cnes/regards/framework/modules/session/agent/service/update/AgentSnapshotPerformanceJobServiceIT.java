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
package fr.cnes.regards.framework.modules.session.agent.service.update;

import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;
import fr.cnes.regards.framework.modules.session.agent.service.AbstractAgentServiceUtilsIT;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Performance test for {@link AgentSnapshotJobService}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=agent_performance_it",
                                   "regards.jobs.pool.size=15",
                                   "regards.jpa.multitenant.maxPoolSize=3",
                                   "regards.session.step.merge-similar-event=false" })
@ActiveProfiles({ "testAmqp", "noscheduler" })
public class AgentSnapshotPerformanceJobServiceIT extends AbstractAgentServiceUtilsIT {

    /**
     * Tested service
     */
    @Autowired
    private AgentSnapshotJobService agentJobSnapshotService;

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSnapshotPerformanceJobServiceIT.class);

    /**
     * Reference date for tests
     */
    private static final OffsetDateTime CREATION_DATE = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);

    @Test
    @Purpose("Test the performance while generating session steps from step update requests")
    public void performanceGenerateSessionStepTest() throws InterruptedException {
        // launch the generation of sessionSteps from StepPropertyUpdateRequest
        int nbSources = 10;
        int nbStepRequests = 5000;
        createRunStepEvents(nbStepRequests, nbSources);

        // wait for stepPropertyUpdateRequestEvent to be stored in database
        waitForStepPropertyEventsStored(nbStepRequests);

        // Schedule jobs
        long start = System.currentTimeMillis();
        long timeout = 20_000L;
        LOGGER.info("Launching performance test to create SessionSteps from {} step requests from {} different "
                    + "source", nbStepRequests, nbSources);
        agentJobSnapshotService.scheduleJob();

        // wait for job to be in success state
        waitForJobSuccesses(AgentSnapshotJob.class.getName(), nbSources, timeout);
        LOGGER.info("Performance test handled in {}ms to create SessionSteps from {} step requests from {} different "
                    + "source", System.currentTimeMillis() - start, nbStepRequests, nbSources);

        checkResult(nbSources);
    }

    private void checkResult(int nbSessionStepExpected) {
        List<SessionStep> sessionSteps = this.sessionStepRepo.findAll();
        Assert.assertEquals("Wrong number of session steps created", nbSessionStepExpected, sessionSteps.size());
    }

    private void createRunStepEvents(int nbStepRequests, int nbSources) {
        List<StepPropertyUpdateRequestEvent> stepRequests = new ArrayList<>();

        // create list of sources
        List<String> sources = new ArrayList<>();
        for (int i = 0; i < nbSources; i++) {
            sources.add("SOURCE_" + i);
        }

        // create list of step request events
        for (int i = 0; i < nbStepRequests; i++) {
            String source = sources.get(i % nbSources);

            // ACQUISITION - scan event SOURCE 0-nbSources / OWNER 1
            stepRequests.add(new StepPropertyUpdateRequestEvent(new StepProperty("scan",
                                                                                 source,
                                                                                 OWNER_1,
                                                                                 new StepPropertyInfo(StepTypeEnum.ACQUISITION,
                                                                                                      StepPropertyStateEnum.SUCCESS,
                                                                                                      "gen.products",
                                                                                                      "1",
                                                                                                      true,
                                                                                                      false)),
                                                                StepPropertyEventTypeEnum.INC));
        }
        // Publish events
        this.publisher.publish(stepRequests);
    }
}
