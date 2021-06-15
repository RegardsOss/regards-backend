package fr.cnes.regards.modules.feature.service.session;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionDeleteEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SourceDeleteEvent;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceTest;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

@TestPropertySource(
        properties = {"spring.jpa.properties.hibernate.default_schema=feature_session_delete", "regards.amqp.enabled=true"},
        locations = {"classpath:regards_perf.properties", "classpath:batch.properties", "classpath:metrics.properties"})
@ActiveProfiles(value = {"testAmqp", "noscheduler", "nohandler"})
public class SessionDeleteIT extends AbstractFeatureMultitenantServiceTest {

    private static final String SOURCE1 = "SOURCE 1";
    private static final String SOURCE2 = "SOURCE 2";
    private static final String SESSION1 = "SESSION 1";
    private static final String SESSION2 = "SESSION 2";

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IFeatureEntityRepository featureEntityRepository;

    @Override
    protected void doInit() throws Exception {
        prepareCreationTestData(true, 2, true, true, SOURCE1, SESSION1);
        prepareCreationTestData(true, 2, true, true, SOURCE1, SESSION2);
        prepareCreationTestData(true, 2, true, true, SOURCE2, SESSION1);
        prepareCreationTestData(true, 2, true, true, SOURCE2, SESSION2);
        waitCreationRequestDeletion(0, 60000);
    }

    @Test
    public void testSessionDeleteBySource() throws InterruptedException {
        publisher.publish(new SourceDeleteEvent(SOURCE1));
        TimeUnit.SECONDS.sleep(20);
        Assertions.assertEquals(4, featureEntityRepository.findAll().size());
        Assertions.assertEquals(0, featureEntityRepository.findBySessionOwner(SOURCE1, Pageable.unpaged()).getTotalElements());
        computeSessionStep(SOURCE1, SESSION1);
        computeSessionStep(SOURCE1, SESSION2);
        SessionStep sessionStep = getSessionStep(SOURCE1, SESSION1);
        checkKey(0, "referencedProducts", sessionStep.getProperties());
        sessionStep = getSessionStep(SOURCE1, SESSION2);
        checkKey(0, "referencedProducts", sessionStep.getProperties());
    }

    @Test
    public void testSessionDeleteBySourceAndSession() throws InterruptedException {
        publisher.publish(new SessionDeleteEvent(SOURCE1, SESSION1));
        TimeUnit.SECONDS.sleep(20);
        Assertions.assertEquals(6, featureEntityRepository.findAll().size());
        Assertions.assertEquals(0, featureEntityRepository.findBySessionOwnerAndSession(SOURCE1, SESSION1, Pageable.unpaged()).getTotalElements());
        computeSessionStep(SOURCE1, SESSION1);
        computeSessionStep(SOURCE1, SESSION2);
        SessionStep sessionStep = getSessionStep(SOURCE1, SESSION1);
        checkKey(0, "referencedProducts", sessionStep.getProperties());
        sessionStep = getSessionStep(SOURCE1, SESSION2);
        checkKey(2, "referencedProducts", sessionStep.getProperties());
    }

}
