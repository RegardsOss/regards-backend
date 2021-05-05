package fr.cnes.regards.framework.modules.session.agent.service.update;

import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventStateEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.service.AbstractAgentServiceUtilsTest;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionStepEvent;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for {@link AgentSnapshotJobService}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=agent_job_service_it" })
public class AgentSnapshotJobServiceIT extends AbstractAgentServiceUtilsTest {

    @Autowired
    private AgentSnapshotJobService agentJobSnapshotService;

    @Test
    @Purpose("Test the generation of session steps from step request events")
    public void generateSessionStepTest() throws InterruptedException {
        // launch the generation of sessionSteps from StepPropertyUpdateRequest
        int nbEvents = createRunStepEvents();

        // wait for stepPropertyUpdateRequestEvent to be stored in database
        boolean isEventRegistered = waitForStepPropertyEventsStored(nbEvents);
        if (!isEventRegistered) {
            Assert.fail("Events were not stored in database");
        }

        // retrieve associated snapshot processes
        List<SnapshotProcess> snapshotProcessesCreated = this.snapshotProcessRepo.findAll();
        Assert.assertEquals("Wrong number of snapshot processes created", 3, snapshotProcessesCreated.size());

        // wait for job to be in success state
        agentJobSnapshotService.scheduleJob();

        boolean isJobAgentSuccess = waitForJobSuccesses(AgentSnapshotJob.class.getName(), 3, 10000L);
        if (!isJobAgentSuccess) {
            Assert.fail("AgentSnapshotJob was not launched or was not in success state");
        }

        Mockito.verify(publisher, Mockito.times(4)).publish(Mockito.any(SessionStepEvent.class));

        // verify results
        checkResult();

        boolean isSnapshotProcessesUpdated = waitForSnapshotUpdateSuccesses();
        if (!isSnapshotProcessesUpdated) {
            Assert.fail("Snapshot were not updated");
        }

    }

    private int createRunStepEvents() {
        List<StepPropertyUpdateRequestEvent> stepRequests = new ArrayList<>();

        // ACQUISITION - scan event SOURCE 1 OWNER 1
        stepRequests.add(new StepPropertyUpdateRequestEvent("scan", SOURCE_1, OWNER_1, StepPropertyEventTypeEnum.INC,
                                                            new StepPropertyEventInfo(StepTypeEnum.ACQUISITION,
                                                                                      StepPropertyEventStateEnum.SUCCESS,
                                                                                      "gen.products", "10", true,
                                                                                      false)));

        stepRequests.add(new StepPropertyUpdateRequestEvent("scan", SOURCE_1, OWNER_1, StepPropertyEventTypeEnum.INC,
                                                            new StepPropertyEventInfo(StepTypeEnum.ACQUISITION,
                                                                                      StepPropertyEventStateEnum.SUCCESS,
                                                                                      "gen.products", "5", true,
                                                                                      false)));
        // ACQUISITION - scan event SOURCE 1 OWNER 2
        stepRequests.add(new StepPropertyUpdateRequestEvent("scan", SOURCE_1, OWNER_2, StepPropertyEventTypeEnum.INC,
                                                            new StepPropertyEventInfo(StepTypeEnum.ACQUISITION,
                                                                                      StepPropertyEventStateEnum.SUCCESS,
                                                                                      "gen.products", "8", true,
                                                                                      false)));

        stepRequests.add(new StepPropertyUpdateRequestEvent("scan", SOURCE_1, OWNER_2, StepPropertyEventTypeEnum.INC,
                                                            new StepPropertyEventInfo(StepTypeEnum.ACQUISITION,
                                                                                      StepPropertyEventStateEnum.RUNNING,
                                                                                      "gen.products", "4", true,
                                                                                      false)));

        // REFERENCING - oais event SOURCE 2 OWNER 1
        stepRequests.add(new StepPropertyUpdateRequestEvent("oais", SOURCE_2, OWNER_1, StepPropertyEventTypeEnum.INC,
                                                            new StepPropertyEventInfo(StepTypeEnum.REFERENCING,
                                                                                      StepPropertyEventStateEnum.ERROR,
                                                                                      "ref.products.errors", "6", false,
                                                                                      false)));
        stepRequests.add(new StepPropertyUpdateRequestEvent("oais", SOURCE_2, OWNER_1, StepPropertyEventTypeEnum.VALUE,
                                                            new StepPropertyEventInfo(StepTypeEnum.REFERENCING,
                                                                                      StepPropertyEventStateEnum.ERROR,
                                                                                      "ref.products.state", "ERROR",
                                                                                      false, false)));
        // REFERENCING - oais event SOURCE 3 OWNER 1
        stepRequests.add(new StepPropertyUpdateRequestEvent("oais", SOURCE_3, OWNER_1, StepPropertyEventTypeEnum.INC,
                                                            new StepPropertyEventInfo(StepTypeEnum.REFERENCING,
                                                                                      StepPropertyEventStateEnum.WAITING,
                                                                                      "ref.products", "3", false,
                                                                                      true)));

        // Publish events
        this.publisher.publish(stepRequests);
        return stepRequests.size();
    }

    private void checkResult() {
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
                Assert.assertEquals("Wrong num of output related", 3L, sessionStep.getOutputRelated());
                Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                Assert.assertEquals("Wrong num of waiting", 3L, sessionStep.getState().getWaiting());
                Assert.assertEquals("Wrong num of running", 0L, sessionStep.getState().getRunning());
                Assert.assertNotNull("Wrong last update date", sessionStep.getLastUpdateDate());
                Assert.assertTrue("Wrong properties", properties.containsKey("ref.products"));
                Assert.assertEquals("Wrong properties", "3", properties.get("ref.products"));
            } else {
                Assert.fail(String.format("Unexpected step source %s or session %s created", source, session));
            }
        }
    }
}