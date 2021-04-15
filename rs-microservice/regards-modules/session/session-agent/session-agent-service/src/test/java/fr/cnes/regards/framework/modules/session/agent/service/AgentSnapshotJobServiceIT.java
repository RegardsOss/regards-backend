package fr.cnes.regards.framework.modules.session.agent.service;

import fr.cnes.regards.framework.modules.session.agent.domain.EventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.StepEvent;
import fr.cnes.regards.framework.modules.session.agent.domain.StepEventStateEnum;
import fr.cnes.regards.framework.modules.session.sessioncommons.domain.StepTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.service.jobs.AgentSnapshotService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = {"spring.application.name=rs-test", "spring.jpa.properties.hibernate"
        + ".default_schema=service_it", "regards.cipher.key-location=src/test/resources"
        + "/testKey", "regards.cipher.iv=1234567812345678" })
@ActiveProfiles(value = { "noscheduler"})
public class AgentSnapshotJobServiceIT extends AbstractRegardsServiceTransactionalIT {

    @Autowired
    private AgentSnapshotService agentSnapshotService;

    private static final String SOURCE = "SOURCE 1";

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;


    @Test
    @Purpose("The the generation of SessionStep following the publication of StepEvents")
    public void generateSessionStepTest() {
        Set<StepEvent> stepEvents = createStepEvents();
        int nbSessionStepsCreated = agentSnapshotService.generateSessionStep(SOURCE, stepEvents);
        Assert.assertEquals("Wrong number of session steps created", 3, nbSessionStepsCreated);
    }


    private Set<StepEvent> createStepEvents() {
        Set<StepEvent> stepEvents = new HashSet<>();

        // create INC events
        stepEvents.add(new StepEvent("scan", SOURCE, "session 1", StepTypeEnum.ACQUISITION,
                                             StepEventStateEnum.OK, "gen.products", "2", EventTypeEnum.INC, true,
                                             false));

        stepEvents.add(new StepEvent("scan", SOURCE, "session 1", StepTypeEnum.STORAGE, StepEventStateEnum.OK,
                                             "store.products", "10", EventTypeEnum.INC, true, false));
        // create dec events
        stepEvents.add(new StepEvent("storage", SOURCE, "session 1", StepTypeEnum.STORAGE,
                                             StepEventStateEnum.OK, "store.products", "2", EventTypeEnum.DEC, false,
                                             true));

        // create value events
        stepEvents.add(new StepEvent("oais", SOURCE, "session 2", StepTypeEnum.REFERENCEMENT,
                                             StepEventStateEnum.OK, "gen.state", "RUNNING", EventTypeEnum.VALUE,
                                             false, true));




        return stepEvents;
    }


}
