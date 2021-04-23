package fr.cnes.regards.framework.modules.session.agent.service.update;

import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventStateEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.service.AbstractAgentServiceUtilsTest;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
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
 * Performance test for {@link AgentSnapshotJobService}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=agent_performance_it" })
public class AgentSnapshotPerformanceJobServiceIT extends AbstractAgentServiceUtilsTest {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSnapshotPerformanceJobServiceIT.class);

    @Autowired
    private AgentSnapshotJobService agentJobSnapshotService;

    private static final OffsetDateTime CREATION_DATE = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);

    @Test
    @Purpose("Test the performance while generation session steps from step update requests")
    public void performanceGenerateSessionStepTest() {
        // launch the generation of sessionSteps from StepPropertyUpdateRequest
        SnapshotProcess snapshotProcess = new SnapshotProcess(SOURCE_1, CREATION_DATE, null);
        int nbSources = 10;
        int nbStepRequests = 5000;
        int nbEvents = createRunStepEvents(nbStepRequests, nbSources);

        // wait for stepPropertyUpdateRequestEvent to be stored in database
        boolean isEventRegistered = waitForStepPropertyEventsStored(nbEvents);
        if (!isEventRegistered) {
            Assert.fail("Events were not stored in database");
        }

        // Schedule jobs
        long start = System.currentTimeMillis();
        long timeout = 20000L;
        LOGGER.info("Launching performance test to create SessionSteps from {} step requests from {} different "
                            + "source", nbStepRequests, nbSources);
        agentJobSnapshotService.scheduleJob();

        // wait for job to be in success state
        boolean isJobSuccess = waitForJobSuccesses(AgentSnapshotJob.class.getName(), nbSources, timeout);
        LOGGER.info("Performance test handled in {}ms to create SessionSteps from {} step requests from {} different "
                            + "source", System.currentTimeMillis() - start , nbStepRequests, nbSources);
        if (!isJobSuccess) {
            Assert.fail(String.format("The number of jobs in success state is not expected. Check if all jobs were "
                                              + "created in the required amount of time (max. %d ms)", timeout));
        }

        checkResult(nbSources);
    }

    private void checkResult(int nbSessionStepExpected) {
        List<SessionStep> sessionSteps = this.sessionStepRepo.findAll();
        Assert.assertEquals("Wrong number of session steps created", nbSessionStepExpected, sessionSteps.size());
    }

    private int createRunStepEvents(int nbStepRequests, int nbSources) {
        List<StepPropertyUpdateRequestEvent> stepRequests = new ArrayList<>();

        List<String> sources = new ArrayList<>();
        for (int i = 0; i < nbSources; i++) {
            sources.add("SOURCE_" + i);
        }
        for (int i = 0; i < nbStepRequests; i++) {
            String source = sources.get(i % nbSources);

            // ACQUISITION - scan event SOURCE 1 OWNER 1
            stepRequests.add(new StepPropertyUpdateRequestEvent("scan", source, OWNER_1, StepPropertyEventTypeEnum.INC,
                                                                new StepPropertyEventInfo(StepTypeEnum.ACQUISITION,
                                                                                          StepPropertyEventStateEnum.OK,
                                                                                          "gen.products", "1", true,
                                                                                          false)));
        }
        // Publish events
        this.publisher.publish(stepRequests);
        return stepRequests.size();
    }
}
