package fr.cnes.regards.framework.modules.session.agent.service.update;

import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventStateEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.service.AbstractAgentServiceUtilsTest;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepEvent;
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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Iliana Ghazali
 **/

@TestPropertySource(properties = { "spring.application.name=rs-test",
        "spring.jpa.properties.hibernate.default_schema=agent_job_service_it" })
public class AgentSnapshotJobServiceIT extends AbstractAgentServiceUtilsTest {

    @Autowired
    private AgentSnapshotJobService agentJobSnapshotService;

    @Autowired
    private IJobInfoService jobInfoService;

    private static final OffsetDateTime CREATION_DATE = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);

    @Test
    @Purpose("The the generation of SessionStep following the publication of StepEvents")
    public void generateSessionStepTest() {
        // launch the generation of sessionSteps from StepPropertyUpdateRequest
        SnapshotProcess snapshotProcess = new SnapshotProcess(SOURCE_1, CREATION_DATE, null);
        int nbEvents = createRunStepEvents();

        // wait for stepPropertyUpdateRequestEvent to be stored in database
        boolean isEventRegistered = waitForStepPropertyEventsStored(nbEvents);
        if (!isEventRegistered) {
            Assert.fail("Events were not stored in database");
        }

        // wait for job to be in success state
        agentJobSnapshotService.scheduleJob();

        boolean isJobAgentSuccess = waitForJobSuccesses(AgentSnapshotJob.class.getName(), 3, 10000L);
        if (!isJobAgentSuccess) {
            Assert.fail("AgentSnapshotJob was not launched or was not in success state");
        }

        checkResult();
        Mockito.verify(publisher, Mockito.times(4)).publish(Mockito.any(SessionStepEvent.class));

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
                Assert.assertEquals("Wrong num of input related", 2L, sessionStep.getInputRelated());
                Assert.assertEquals("Wrong num of output related", 0L, sessionStep.getOutputRelated());
                Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                Assert.assertFalse("Should not be in running state", sessionStep.getState().isRunning());
                Assert.assertNotNull("Wrong last update date", sessionStep.getLastUpdate());
                Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                Assert.assertEquals("Wrong properties", "15", properties.get("gen.products"));
            } else if (source.equals(SOURCE_1) && session.equals(OWNER_2)) {
                Assert.assertEquals("Wrong type", StepTypeEnum.ACQUISITION, sessionStep.getType());
                Assert.assertEquals("Wrong num of input related", 2L, sessionStep.getInputRelated());
                Assert.assertEquals("Wrong num of output related", 0L, sessionStep.getOutputRelated());
                Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                Assert.assertTrue("Should not be in running state", sessionStep.getState().isRunning());
                Assert.assertNotNull("Wrong last update date", sessionStep.getLastUpdate());
                Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                Assert.assertEquals("Wrong properties", "12", properties.get("gen.products"));
            } else if (source.equals(SOURCE_2) && session.equals(OWNER_1)) {
                Assert.assertEquals("Wrong type", StepTypeEnum.REFERENCING, sessionStep.getType());
                Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                Assert.assertEquals("Wrong num of output related", 1L, sessionStep.getOutputRelated());
                Assert.assertEquals("Wrong num of errors", 1L, sessionStep.getState().getErrors());
                Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                Assert.assertFalse("Should not be in running state", sessionStep.getState().isRunning());
                Assert.assertNotNull("Wrong last update date", sessionStep.getLastUpdate());
                Assert.assertTrue("Wrong properties", properties.containsKey("ref.products.state"));
                Assert.assertEquals("Wrong properties", "ERROR", properties.get("ref.products.state"));
            } else if (source.equals(SOURCE_3) && session.equals(OWNER_1)) {
                Assert.assertEquals("Wrong type", StepTypeEnum.REFERENCING, sessionStep.getType());
                Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                Assert.assertEquals("Wrong num of output related", 1L, sessionStep.getOutputRelated());
                Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                Assert.assertEquals("Wrong num of waiting", 1L, sessionStep.getState().getWaiting());
                Assert.assertFalse("Should not be in running state", sessionStep.getState().isRunning());
                Assert.assertNotNull("Wrong last update date", sessionStep.getLastUpdate());
                Assert.assertTrue("Wrong properties", properties.containsKey("ref.products"));
                Assert.assertEquals("Wrong properties", "1", properties.get("ref.products"));
            } else {
                Assert.fail(String.format("Unexpected step source %s or session %s created", source, session));
            }
        }
    }

    private int createRunStepEvents() {
        List<StepPropertyUpdateRequestEvent> stepRequests = new ArrayList<>();

        // ACQUISITION - scan event SOURCE 1 OWNER 1
        stepRequests.add(new StepPropertyUpdateRequestEvent("scan", SOURCE_1, OWNER_1, StepPropertyEventTypeEnum.INC,
                                                            new StepPropertyEventInfo(StepTypeEnum.ACQUISITION,
                                                                                      StepPropertyEventStateEnum.OK,
                                                                                      "gen.products", "10", true,
                                                                                      false)));

        stepRequests.add(new StepPropertyUpdateRequestEvent("scan", SOURCE_1, OWNER_1, StepPropertyEventTypeEnum.INC,
                                                            new StepPropertyEventInfo(StepTypeEnum.ACQUISITION,
                                                                                      StepPropertyEventStateEnum.OK,
                                                                                      "gen.products", "5", true,
                                                                                      false)));
        // ACQUISITION - scan event SOURCE 1 OWNER 2
        stepRequests.add(new StepPropertyUpdateRequestEvent("scan", SOURCE_1, OWNER_2, StepPropertyEventTypeEnum.INC,
                                                            new StepPropertyEventInfo(StepTypeEnum.ACQUISITION,
                                                                                      StepPropertyEventStateEnum.OK,
                                                                                      "gen.products", "8", true,
                                                                                      false)));

        stepRequests.add(new StepPropertyUpdateRequestEvent("scan", SOURCE_1, OWNER_2, StepPropertyEventTypeEnum.INC,
                                                            new StepPropertyEventInfo(StepTypeEnum.ACQUISITION,
                                                                                      StepPropertyEventStateEnum.RUNNING,
                                                                                      "gen.products", "4", true,
                                                                                      false)));

        // REFERENCING - oais event SOURCE 2 OWNER 1
        stepRequests.add(new StepPropertyUpdateRequestEvent("oais", SOURCE_2, OWNER_1, StepPropertyEventTypeEnum.VALUE,
                                                            new StepPropertyEventInfo(StepTypeEnum.REFERENCING,
                                                                                      StepPropertyEventStateEnum.ERROR,
                                                                                      "ref.products.state", "ERROR",
                                                                                      false, true)));
        // REFERENCING - oais event SOURCE 3 OWNER 1
        stepRequests.add(new StepPropertyUpdateRequestEvent("oais", SOURCE_3, OWNER_1, StepPropertyEventTypeEnum.INC,
                                                            new StepPropertyEventInfo(StepTypeEnum.REFERENCING,
                                                                                      StepPropertyEventStateEnum.WAITING,
                                                                                      "ref.products", "1", false,
                                                                                      true)));

        // Publish events
        this.publisher.publish(stepRequests);
        return stepRequests.size();
    }
}