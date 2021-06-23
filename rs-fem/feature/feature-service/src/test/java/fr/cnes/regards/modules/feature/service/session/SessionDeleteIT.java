package fr.cnes.regards.modules.feature.service.session;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.DataSourcesAutoConfiguration;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionDeleteEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SourceDeleteEvent;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceTest;
import fr.cnes.regards.modules.feature.service.settings.IFeatureNotificationSettingsService;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@TestPropertySource(
        properties = {"spring.jpa.properties.hibernate.default_schema=feature_session_delete", "regards.amqp.enabled=true"},
        locations = {"classpath:regards_perf.properties", "classpath:batch.properties", "classpath:metrics.properties"})
@ActiveProfiles(value = {"testAmqp", "noscheduler"})
public class SessionDeleteIT extends AbstractFeatureMultitenantServiceTest {

    private static final String SOURCE1 = "SOURCE 1";
    private static final String SOURCE2 = "SOURCE 2";
    private static final String SESSION1 = "SESSION 1";
    private static final String SESSION2 = "SESSION 2";

    @Autowired
    private IFeatureEntityRepository featureEntityRepository;

    @Autowired
    private IFeatureNotificationSettingsService notificationSettingsService;

    @Override
    public void doInit() throws Exception {
        notificationSettingsService.setActiveNotification(false);
    }

    private void prepareData() throws InterruptedException {
        prepareCreationTestData(true, 2, false, false, SOURCE2, SESSION1);
        prepareCreationTestData(true, 2, false, false, SOURCE1, SESSION1);
        prepareCreationTestData(true, 2, false, false, SOURCE2, SESSION2);
        prepareCreationTestData(true, 2, false, false, SOURCE1, SESSION2);

        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return featureEntityRepository.findAll().size() == 8;
        });

        waitCreationRequestDeletion(0, 60000);
    }

    @Test
    public void testSessionDeleteBySource() throws InterruptedException, SQLException {
        prepareData();
        publisher.publish(new SourceDeleteEvent(SOURCE1));
        // wait for 4 delete requests created
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return featureDeletionRequestRepo.count() == 4;
        });
        featureDeletionService.scheduleRequests();
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return featureEntityRepository.findAll().size() == 4;
        });
        // Wait deletion requests done
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return featureDeletionRequestRepo.count() == 0;
        });

        Assertions.assertEquals(4, featureEntityRepository.findAll().size());
        Assertions.assertEquals(0, featureEntityRepository.findBySessionOwner(SOURCE1, Pageable.unpaged()).getTotalElements());
        // Creation : 8 products *4steps (request+running+referenced+running)
        // Deletion : 4 products *5steps (deleteRequest+running+deleted+referenced+running)
        computeSessionStep((8*4) + (4*5),SOURCE1, SESSION1);
        computeSessionStep(0,SOURCE1, SESSION2);
        SessionStep sessionStep = getSessionStep(SOURCE1, SESSION1);
        checkKey(0, "referencedProducts", sessionStep.getProperties());
        checkKey(2, "deletedProducts", sessionStep.getProperties());
        checkKey(2, "deleteRequests", sessionStep.getProperties());
        checkKey(0, "runningDeleteRequests", sessionStep.getProperties());

        sessionStep = getSessionStep(SOURCE1, SESSION2);
        checkKey(0, "referencedProducts", sessionStep.getProperties());
        checkKey(2, "deletedProducts", sessionStep.getProperties());
        checkKey(2, "deleteRequests", sessionStep.getProperties());
        checkKey(0, "runningDeleteRequests", sessionStep.getProperties());
    }

    @Test
    public void testSessionDeleteBySourceAndSession() throws InterruptedException, SQLException {
        prepareData();
        publisher.publish(new SessionDeleteEvent(SOURCE1, SESSION1));
        // wait for 2 delete requests created
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return featureDeletionRequestRepo.count() == 2;
        });
        // Schedule requests
        featureDeletionService.scheduleRequests();
        // Wait for request done
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return featureDeletionRequestRepo.findAll().size() == 2;
        });
        featureDeletionService.scheduleRequests();
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return featureEntityRepository.findAll().size() == 6;
        });
        // Wait deletion requests done
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return featureEntityRepository.findAll().size() == 6;
        });
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return featureDeletionRequestRepo.count() == 0;
        });

        Assertions.assertEquals(6, featureEntityRepository.findAll().size());
        Assertions.assertEquals(0, featureEntityRepository.findBySessionOwnerAndSession(SOURCE1, SESSION1, Pageable.unpaged()).getTotalElements());
        // Creation : 8 products * 4 events (request+running+referenced+running)
        // Deletion : 2 products * 5 events (request+running+deleted+referenced+running)
        computeSessionStep((8*4) + (2*5),SOURCE1, SESSION1);
        computeSessionStep(0,SOURCE1, SESSION2);
        SessionStep sessionStep = getSessionStep(SOURCE1, SESSION1);
        checkKey(0, "referencedProducts", sessionStep.getProperties());
        checkKey(2, "deletedProducts", sessionStep.getProperties());
        checkKey(2, "deleteRequests", sessionStep.getProperties());
        checkKey(0, "runningDeleteRequests", sessionStep.getProperties());
        sessionStep = getSessionStep(SOURCE1, SESSION2);
        checkKey(2, "referencedProducts", sessionStep.getProperties());
        Assertions.assertFalse(sessionStep.getProperties().containsKey("deletedProducts"));
        Assertions.assertFalse(sessionStep.getProperties().containsKey("deleteRequests"));
        Assertions.assertFalse(sessionStep.getProperties().containsKey("runningDeleteRequests"));
    }

}
