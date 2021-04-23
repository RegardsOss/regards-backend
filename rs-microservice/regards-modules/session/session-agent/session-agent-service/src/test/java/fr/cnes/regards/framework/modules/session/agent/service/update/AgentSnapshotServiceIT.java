package fr.cnes.regards.framework.modules.session.agent.service.update;

import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventStateEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for {@link AgentSnapshotService}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=agent_snapshot_service_it",
        "regards.cipher.key-location=src/test/resources" + "/testKey", "regards.cipher.iv=1234567812345678", "regards"
        + ".session.agent.step.requests.page.size=2" })
@ActiveProfiles(value = { "noscheduler" })
public class AgentSnapshotServiceIT extends AbstractRegardsServiceTransactionalIT {

    @Autowired
    private AgentSnapshotService agentSnapshotService;

    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyRepo;

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    private static final String SOURCE = "SOURCE 1";

    private static final String OWNER_1 = "OWNER 1";

    private static final String OWNER_2 = "OWNER 2";

    private static final String OWNER_3 = "OWNER 3";

    private static final OffsetDateTime CREATION_DATE = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);

    @Before
    public void init() {
        this.stepPropertyRepo.deleteAll();
        this.sessionStepRepo.deleteAll();
    }

    @Test
    @Purpose("Test if session steps are correctly generated from step property requests")
    public void generateSessionStepTest() {
        // launch the generation of sessionSteps from StepPropertyUpdateRequest
        SnapshotProcess snapshotProcess = new SnapshotProcess(SOURCE, CREATION_DATE, null);
        List<StepPropertyUpdateRequest> stepRequests = createRun1StepEvents();
        Assert.assertEquals("Wrong number of stepPropertyUpdateRequests created", 10, stepRequests.size());

        OffsetDateTime freezeDate = CREATION_DATE.plusMinutes(22);
        int nbSessionStepsCreated = agentSnapshotService.generateSessionStep(snapshotProcess, freezeDate);
        checkRun1Test(nbSessionStepsCreated);

        // launch the second run with same source and session owner, fields should be updated
        SnapshotProcess snapshotProcess2 = new SnapshotProcess(SOURCE, CREATION_DATE.plusMinutes(25), null);
        List<StepPropertyUpdateRequest> stepRequests2 = createRun2StepEvents();
        Assert.assertEquals("Wrong number of stepPropertyUpdateRequests created", 4, stepRequests2.size());

        OffsetDateTime freezeDate2 = CREATION_DATE.plusMinutes(50);
        int nbSessionStepsCreated2 = agentSnapshotService.generateSessionStep(snapshotProcess2, freezeDate2);
        checkRun2Test(nbSessionStepsCreated2);
    }

    private List<StepPropertyUpdateRequest> createRun1StepEvents() {
        List<StepPropertyUpdateRequest> stepRequests = new ArrayList<>();

        // ACQUISITION - scan event
        stepRequests.add(new StepPropertyUpdateRequest("scan", SOURCE, OWNER_1, CREATION_DATE,
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyInfo(StepTypeEnum.ACQUISITION,
                                                                            StepPropertyEventStateEnum.OK,
                                                                            "gen.products", "2", true, false)));

        stepRequests.add(new StepPropertyUpdateRequest("scan", SOURCE, OWNER_1, CREATION_DATE.plusMinutes(1),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyInfo(StepTypeEnum.ACQUISITION,
                                                                            StepPropertyEventStateEnum.OK,
                                                                            "gen.products", "4", true, false)));

        // REFERENCING - oais event
        stepRequests.add(new StepPropertyUpdateRequest("oais", SOURCE, OWNER_1, CREATION_DATE.plusMinutes(2),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyInfo(StepTypeEnum.REFERENCING,
                                                                            StepPropertyEventStateEnum.OK,
                                                                            "gen.products", "6", false, true)));

        // STORAGE - storage event
        stepRequests.add(new StepPropertyUpdateRequest("storage", SOURCE, OWNER_1, CREATION_DATE.plusMinutes(5),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyInfo(StepTypeEnum.STORAGE,
                                                                            StepPropertyEventStateEnum.OK,
                                                                            "store.products", "2", false, true)));
        stepRequests.add(new StepPropertyUpdateRequest("storage", SOURCE, OWNER_1, CREATION_DATE.plusMinutes(3),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyInfo(StepTypeEnum.STORAGE,
                                                                            StepPropertyEventStateEnum.ERROR,
                                                                            "store.products.errors", "4", false,
                                                                            false)));
        stepRequests.add(new StepPropertyUpdateRequest("storage", SOURCE, OWNER_1, CREATION_DATE.plusMinutes(4),
                                                       StepPropertyEventTypeEnum.VALUE,
                                                       new StepPropertyInfo(StepTypeEnum.STORAGE,
                                                                            StepPropertyEventStateEnum.ERROR,
                                                                            "store.products.state", "ERROR", false,
                                                                            false)));
        // DISSEMINATION - metacatalog event
        stepRequests.add(new StepPropertyUpdateRequest("metacatalog", SOURCE, OWNER_1, CREATION_DATE.plusMinutes(7),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyInfo(StepTypeEnum.DISSEMINATION,
                                                                            StepPropertyEventStateEnum.RUNNING,
                                                                            "gen.products", "1", false, true)));
        stepRequests.add(new StepPropertyUpdateRequest("metacatalog", SOURCE, OWNER_1, CREATION_DATE.plusMinutes(6),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyInfo(StepTypeEnum.DISSEMINATION,
                                                                            StepPropertyEventStateEnum.WAITING,
                                                                            "gen.products", "1", false, true)));

        // OTHER EVENTS NOT RELATED TO SESSION 1
        stepRequests.add(new StepPropertyUpdateRequest("scan", SOURCE, OWNER_2, CREATION_DATE.plusMinutes(20),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyInfo(StepTypeEnum.ACQUISITION,
                                                                            StepPropertyEventStateEnum.OK,
                                                                            "gen.products", "2", true, false)));
        stepRequests.add(new StepPropertyUpdateRequest("storage", SOURCE, OWNER_3, CREATION_DATE.plusMinutes(20),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyInfo(StepTypeEnum.STORAGE,
                                                                            StepPropertyEventStateEnum.OK,
                                                                            "store.products", "6", false, true)));

        return this.stepPropertyRepo.saveAll(stepRequests);
    }

    private List<StepPropertyUpdateRequest> createRun2StepEvents() {
        List<StepPropertyUpdateRequest> stepRequests = new ArrayList<>();
        // STORAGE - storage event
        stepRequests.add(new StepPropertyUpdateRequest("storage", SOURCE, OWNER_1, CREATION_DATE.plusMinutes(30),
                                                       StepPropertyEventTypeEnum.DEC,
                                                       new StepPropertyInfo(StepTypeEnum.STORAGE,
                                                                            StepPropertyEventStateEnum.OK,
                                                                            "store.products.errors", "4", false,
                                                                            false)));
        stepRequests.add(new StepPropertyUpdateRequest("storage", SOURCE, OWNER_1, CREATION_DATE.plusMinutes(35),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyInfo(StepTypeEnum.STORAGE,
                                                                            StepPropertyEventStateEnum.OK,
                                                                            "store.products", "4", false, true)));
        stepRequests.add(new StepPropertyUpdateRequest("storage", SOURCE, OWNER_1, CREATION_DATE.plusMinutes(38),
                                                       StepPropertyEventTypeEnum.VALUE,
                                                       new StepPropertyInfo(StepTypeEnum.STORAGE,
                                                                            StepPropertyEventStateEnum.OK,
                                                                            "store.products.state", "OK", false,
                                                                            true)));
        // DISSEMINATION - metacatalog event
        stepRequests.add(new StepPropertyUpdateRequest("metacatalog", SOURCE, OWNER_1, CREATION_DATE.plusMinutes(40),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyInfo(StepTypeEnum.DISSEMINATION,
                                                                            StepPropertyEventStateEnum.OK,
                                                                            "gen.products", "4", false, true)));
        return this.stepPropertyRepo.saveAll(stepRequests);
    }

    public void checkRun1Test(int nbSessionStepsCreated) {
        Assert.assertEquals("Wrong number of session steps created/updated", 6, nbSessionStepsCreated);
        List<SessionStep> sessionSteps = this.sessionStepRepo.findAll();

        // loop on every session steps and check parameters
        for (SessionStep sessionStep : sessionSteps) {
            Assert.assertEquals("Wrong source", SOURCE, sessionStep.getSource());
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
                        Assert.assertFalse("Should not be in running state", sessionStep.getState().isRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(1),
                                            sessionStep.getLastUpdate());
                        Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                        Assert.assertEquals("Wrong properties", "6", properties.get("gen.products"));
                        break;
                    case "oais":
                        Assert.assertEquals("Wrong type", StepTypeEnum.REFERENCING, sessionStep.getType());
                        Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                        Assert.assertEquals("Wrong num of output related", 6L, sessionStep.getOutputRelated());
                        Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                        Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                        Assert.assertFalse("Should not be in running state", sessionStep.getState().isRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(2),
                                            sessionStep.getLastUpdate());
                        Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                        Assert.assertEquals("Wrong properties", "6", properties.get("gen.products"));
                        break;
                    case "storage":
                        Assert.assertEquals("Wrong type", StepTypeEnum.STORAGE, sessionStep.getType());
                        Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                        Assert.assertEquals("Wrong num of output related", 2L, sessionStep.getOutputRelated());
                        Assert.assertEquals("Wrong num of errors", 2L, sessionStep.getState().getErrors());
                        Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                        Assert.assertFalse("Should not be in running state", sessionStep.getState().isRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(5),
                                            sessionStep.getLastUpdate());
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
                        Assert.assertEquals("Wrong num of output related", 2L, sessionStep.getOutputRelated());
                        Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                        Assert.assertEquals("Wrong num of waiting", 1L, sessionStep.getState().getWaiting());
                        Assert.assertTrue("Should not be in running state", sessionStep.getState().isRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(7),
                                            sessionStep.getLastUpdate());
                        Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                        Assert.assertEquals("Wrong properties", "2", properties.get("gen.products"));
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
                Assert.assertFalse("Should not be in running state", sessionStep.getState().isRunning());
                Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(20),
                                    sessionStep.getLastUpdate());
                Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                Assert.assertEquals("Wrong properties", "2", properties.get("gen.products"));
            } else if (session.equals(OWNER_3)) {
                Assert.assertEquals("Wrong stepId", "storage", sessionStep.getStepId());
                Assert.assertEquals("Wrong type", StepTypeEnum.STORAGE, sessionStep.getType());
                Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                Assert.assertEquals("Wrong num of output related", 6L, sessionStep.getOutputRelated());
                Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                Assert.assertFalse("Should not be in running state", sessionStep.getState().isRunning());
                Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(20),
                                    sessionStep.getLastUpdate());
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
            Assert.assertEquals("Wrong source", SOURCE, sessionStep.getSource());
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
                        Assert.assertFalse("Should not be in running state", sessionStep.getState().isRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(1),
                                            sessionStep.getLastUpdate());
                        Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                        Assert.assertEquals("Wrong properties", "6", properties.get("gen.products"));
                        break;
                    case "oais":
                        Assert.assertEquals("Wrong type", StepTypeEnum.REFERENCING, sessionStep.getType());
                        Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                        Assert.assertEquals("Wrong num of output related", 6L, sessionStep.getOutputRelated());
                        Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                        Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                        Assert.assertFalse("Should not be in running state", sessionStep.getState().isRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(2),
                                            sessionStep.getLastUpdate());
                        Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                        Assert.assertEquals("Wrong properties", "6", properties.get("gen.products"));
                        break;
                    case "storage":
                        Assert.assertEquals("Wrong type", StepTypeEnum.STORAGE, sessionStep.getType());
                        Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                        Assert.assertEquals("Wrong num of output related", 6L, sessionStep.getOutputRelated());
                        Assert.assertEquals("Wrong num of errors", 2L, sessionStep.getState().getErrors());
                        Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                        Assert.assertFalse("Should not be in running state", sessionStep.getState().isRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(38),
                                            sessionStep.getLastUpdate());
                        Assert.assertTrue("Wrong properties", properties.containsKey("store.products"));
                        Assert.assertEquals("Wrong properties", "6", properties.get("store.products"));
                        Assert.assertTrue("Wrong properties", properties.containsKey("store.products.state"));
                        Assert.assertEquals("Wrong properties", "OK", properties.get("store.products.state"));
                        Assert.assertTrue("Wrong properties", properties.containsKey("store.products.errors"));
                        Assert.assertEquals("Wrong properties", "0", properties.get("store.products.errors"));
                        break;
                    case "metacatalog":
                        Assert.assertEquals("Wrong type", StepTypeEnum.DISSEMINATION, sessionStep.getType());
                        Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                        Assert.assertEquals("Wrong num of output related", 6L, sessionStep.getOutputRelated());
                        Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                        Assert.assertEquals("Wrong num of waiting", 1L, sessionStep.getState().getWaiting());
                        Assert.assertFalse("Should not be in running state", sessionStep.getState().isRunning());
                        Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(40),
                                            sessionStep.getLastUpdate());
                        Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                        Assert.assertEquals("Wrong properties", "6", properties.get("gen.products"));
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
                Assert.assertFalse("Should not be in running state", sessionStep.getState().isRunning());
                Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(20),
                                    sessionStep.getLastUpdate());
                Assert.assertTrue("Wrong properties", properties.containsKey("gen.products"));
                Assert.assertEquals("Wrong properties", "2", properties.get("gen.products"));
            } else if (session.equals(OWNER_3)) {
                Assert.assertEquals("Wrong stepId", "storage", sessionStep.getStepId());
                Assert.assertEquals("Wrong type", StepTypeEnum.STORAGE, sessionStep.getType());
                Assert.assertEquals("Wrong num of input related", 0L, sessionStep.getInputRelated());
                Assert.assertEquals("Wrong num of output related", 6L, sessionStep.getOutputRelated());
                Assert.assertEquals("Wrong num of errors", 0L, sessionStep.getState().getErrors());
                Assert.assertEquals("Wrong num of waiting", 0L, sessionStep.getState().getWaiting());
                Assert.assertFalse("Should not be in running state", sessionStep.getState().isRunning());
                Assert.assertEquals("Wrong last update date", CREATION_DATE.plusMinutes(20),
                                    sessionStep.getLastUpdate());
                Assert.assertTrue("Wrong properties", properties.containsKey("store.products"));
                Assert.assertEquals("Wrong properties", "6", properties.get("store.products"));
            } else {
                Assert.fail(String.format("Unexpected session created", session));
            }
        }
    }
}
