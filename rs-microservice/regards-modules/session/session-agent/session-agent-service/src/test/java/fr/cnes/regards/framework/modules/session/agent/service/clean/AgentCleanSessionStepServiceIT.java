package fr.cnes.regards.framework.modules.session.agent.service.clean;

import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventStateEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.service.clean.sessionstep.AgentCleanSessionStepService;
import fr.cnes.regards.framework.modules.session.agent.service.update.AgentSnapshotService;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for {@link AgentCleanSessionStepService}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=agent_clean_service_it",
        "regards.session-agent.limit.store.session-steps=30", "regards.cipher.key-location=src/test/resources/testKey",
        "regards.cipher.iv=1234567812345678" })
@ActiveProfiles(value = { "noscheduler" })
public class AgentCleanSessionStepServiceIT extends AbstractRegardsServiceTransactionalIT {

    @Autowired
    private AgentSnapshotService agentSnapshotService;

    @Autowired
    private AgentCleanSessionStepService agentCleanSessionStepService;

    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyRepo;

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    @Value("${regards.session-agent.limit.store.session-steps}")
    private int limitStore;

    private static final String SOURCE_1 = "SOURCE 1";

    private static final String SOURCE_2 = "SOURCE 2";

    private static final String OWNER_1 = "OWNER 1";

    private static final String OWNER_2 = "OWNER 2";

    private static OffsetDateTime CREATION_DATE;

    @Before
    public void init() {
        this.stepPropertyRepo.deleteAll();
        this.sessionStepRepo.deleteAll();
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
        // SOURCE 1 - OWNER 1  should be the only step deleted
        Assert.assertEquals("Wrong number of session steps deleted", 2, nbSessionStepsDeleted);

        // SOURCE 1 - OWNER 1 - scan should be deleted with all related events
        Assert.assertFalse("Step property request should have been deleted",
                           stepPropertyRepo.findById(stepRequests.get(0).getId()).isPresent());
        Assert.assertFalse("Step property request  should have been deleted",
                           stepPropertyRepo.findById(stepRequests.get(1).getId()).isPresent());
        Assert.assertFalse("Session step should have been deleted",
                           sessionStepRepo.findBySourceAndSessionAndStepId(SOURCE_1, OWNER_1, "scan").isPresent());

        // SOURCE 1 - OWNER 2 - scan should be present with all related events
        Assert.assertTrue("Step property request should have been deleted",
                          stepPropertyRepo.findById(stepRequests.get(2).getId()).isPresent());
        Assert.assertTrue("Step property request  should have been deleted",
                          stepPropertyRepo.findById(stepRequests.get(3).getId()).isPresent());
        Assert.assertTrue("Session step should have been deleted",
                          sessionStepRepo.findBySourceAndSessionAndStepId(SOURCE_1, OWNER_2, "scan").isPresent());

        // SOURCE 2 - OWNER 1 - oais should be present with all related events
        Assert.assertTrue("Step property request  should have been deleted",
                          stepPropertyRepo.findById(stepRequests.get(4).getId()).isPresent());
        Assert.assertTrue("Session step should have been deleted",
                          sessionStepRepo.findBySourceAndSessionAndStepId(SOURCE_2, OWNER_1, "oais").isPresent());

        // SOURCE 1 - OWNER 1 - oais should be deleted with all related events
        Assert.assertFalse("Step property request should have been deleted",
                           stepPropertyRepo.findById(stepRequests.get(5).getId()).isPresent());
        Assert.assertFalse("Session step should have been deleted",
                           sessionStepRepo.findBySourceAndSessionAndStepId(SOURCE_1, OWNER_1, "oais").isPresent());
    }

    private List<StepPropertyUpdateRequest> createRunStepEvents() {
        List<StepPropertyUpdateRequest> stepRequests = new ArrayList<>();

        // ACQUISITION - scan event OWNER 1
        stepRequests.add(new StepPropertyUpdateRequest("scan", SOURCE_1, OWNER_1, CREATION_DATE,
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyInfo(StepTypeEnum.ACQUISITION,
                                                                            StepPropertyEventStateEnum.OK,
                                                                            "gen.products", "10", true, false)));

        stepRequests.add(new StepPropertyUpdateRequest("scan", SOURCE_1, OWNER_1, CREATION_DATE.plusMinutes(1),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyInfo(StepTypeEnum.ACQUISITION,
                                                                            StepPropertyEventStateEnum.OK,
                                                                            "gen.products", "10", true, false)));
        // ACQUISITION - scan event OWNER 2
        stepRequests.add(new StepPropertyUpdateRequest("scan", SOURCE_1, OWNER_2, CREATION_DATE,
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyInfo(StepTypeEnum.ACQUISITION,
                                                                            StepPropertyEventStateEnum.OK,
                                                                            "gen.products", "8", true, false)));

        stepRequests
                .add(new StepPropertyUpdateRequest("scan", SOURCE_1, OWNER_2, CREATION_DATE.plusDays(limitStore + 1),
                                                   StepPropertyEventTypeEnum.INC,
                                                   new StepPropertyInfo(StepTypeEnum.ACQUISITION,
                                                                        StepPropertyEventStateEnum.OK, "gen.products",
                                                                        "4", true, false)));

        // REFERENCING - oais event OWNER 1
        stepRequests
                .add(new StepPropertyUpdateRequest("oais", SOURCE_2, OWNER_1, CREATION_DATE.plusDays(limitStore + 1),
                                                   StepPropertyEventTypeEnum.INC,
                                                   new StepPropertyInfo(StepTypeEnum.REFERENCING,
                                                                        StepPropertyEventStateEnum.OK, "gen.products",
                                                                        "6", false, true)));
        // REFERENCING - oais event OWNER 1
        stepRequests.add(new StepPropertyUpdateRequest("oais", SOURCE_1, OWNER_1, CREATION_DATE.plusDays(limitStore),
                                                       StepPropertyEventTypeEnum.INC,
                                                       new StepPropertyInfo(StepTypeEnum.REFERENCING,
                                                                            StepPropertyEventStateEnum.OK,
                                                                            "gen.products", "6", false, true)));

        return this.stepPropertyRepo.saveAll(stepRequests);
    }

}
