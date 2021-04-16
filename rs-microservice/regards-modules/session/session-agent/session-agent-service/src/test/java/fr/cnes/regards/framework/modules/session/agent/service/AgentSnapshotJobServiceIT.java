package fr.cnes.regards.framework.modules.session.agent.service;

import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventStateEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.service.update.AgentSnapshotService;
import fr.cnes.regards.framework.modules.session.sessioncommons.domain.StepTypeEnum;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.application.name=rs-test",
        "spring.jpa.properties.hibernate" + ".default_schema=service_it",
        "regards.cipher.key-location=src/test/resources" + "/testKey", "regards.cipher.iv=1234567812345678" })
@ActiveProfiles(value = { "noscheduler" })
public class AgentSnapshotJobServiceIT extends AbstractRegardsServiceTransactionalIT {

    @Autowired
    private AgentSnapshotService agentSnapshotService;

    private static final String SOURCE = "SOURCE 1";

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyRepo;

    @Autowired
    private EntityManager entityManager;


    @Test
    @Purpose("The the generation of SessionStep following the publication of StepEvents")
    public void generateSessionStepTest() {
        List<StepPropertyUpdateRequest> stepPropertyRequest = createStepEvents();
        int nbSessionStepsCreated = agentSnapshotService.generateSessionStep(SOURCE, stepPropertyRequest);
        Assert.assertEquals("Wrong number of session steps created", 3, nbSessionStepsCreated);
    }

    private List<StepPropertyUpdateRequest> createStepEvents() {
        List<StepPropertyUpdateRequest> stepPropertyUpdateRequests = new ArrayList<>();

        // create INC events
        stepPropertyUpdateRequests.add(new StepPropertyUpdateRequest("scan", SOURCE, "session 1", OffsetDateTime.now(),
                                                            StepTypeEnum.ACQUISITION, StepPropertyEventStateEnum.OK,
                                                            "gen.products", "2", StepPropertyEventTypeEnum.INC, true,
                                                            false));

        stepPropertyUpdateRequests.add(new StepPropertyUpdateRequest("scan", SOURCE, "session 1", OffsetDateTime.now(),
                                                            StepTypeEnum.STORAGE, StepPropertyEventStateEnum.OK,
                                                            "store.products", "10", StepPropertyEventTypeEnum.INC, true,
                                                            false));
        // create dec events
        stepPropertyUpdateRequests.add(new StepPropertyUpdateRequest("storage", SOURCE, "session 1", OffsetDateTime.now(),
                                                            StepTypeEnum.STORAGE, StepPropertyEventStateEnum.OK,
                                                            "store.products", "2", StepPropertyEventTypeEnum.DEC, false,
                                                            true));

        // create value events
        stepPropertyUpdateRequests.add(new StepPropertyUpdateRequest("oais", SOURCE, "session 2", OffsetDateTime.now(),
                                                            StepTypeEnum.REFERENCEMENT, StepPropertyEventStateEnum.OK,
                                                            "gen.state", "RUNNING", StepPropertyEventTypeEnum.VALUE,
                                                            false, true));

        return this.stepPropertyRepo.saveAll(stepPropertyUpdateRequests);
    }
}
