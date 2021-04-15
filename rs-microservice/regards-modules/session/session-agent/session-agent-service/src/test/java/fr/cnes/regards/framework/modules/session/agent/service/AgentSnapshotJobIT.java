package fr.cnes.regards.framework.modules.session.agent.service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.session.agent.domain.EventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.StepEvent;
import fr.cnes.regards.framework.modules.session.agent.domain.StepEventStateEnum;
import fr.cnes.regards.framework.modules.session.sessioncommons.domain.StepTypeEnum;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Iliana Ghazali
 **/
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=agent_job_it", "regards.amqp.enabled=true",
                "regards.snapshot.process.bulk.initial.delay=10",
                "regards.snapshot.process.bulk.delay=100", "eureka.client.enabled=false" })
@ActiveProfiles(value = { "testAmqp"})
public class AgentSnapshotJobIT extends AbstractRegardsServiceTransactionalIT {

    @Autowired
    private IPublisher publisher;

    @Test
    public void handleStepEvents() throws InterruptedException {
        // public INC event
        StepEvent stepEvent = new StepEvent("extract", "source 1", "session 1", StepTypeEnum.ACQUISITION,
                                            StepEventStateEnum.OK, "gen.products", "RUNNING", EventTypeEnum.INC, true,
                                            false);
        publisher.publish(stepEvent);

        Thread.sleep(10000L);
    }



}
