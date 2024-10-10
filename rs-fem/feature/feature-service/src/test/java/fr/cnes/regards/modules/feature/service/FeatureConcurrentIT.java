package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_cct",
                                   "regards.amqp.enabled=true" },
                    locations = { "classpath:regards_perf.properties",
                                  "classpath:batch.properties",
                                  "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler", "noFemHandler" })
public class FeatureConcurrentIT extends AbstractFeatureMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureConcurrentIT.class);

    @Autowired
    private IFeatureCreationService featureService;

    @Test
    public void test_create_process_concurrentFeatures_same_id() throws InterruptedException {
        // Given : register creation requests
        String owner = "sessionOwner";
        FeatureCreationSessionMetadata metadata = FeatureCreationSessionMetadata.build(owner,
                                                                                       "session",
                                                                                       PriorityLevel.NORMAL,
                                                                                       Lists.emptyList(),
                                                                                       true,
                                                                                       false);

        List<FeatureCreationRequestEvent> events = new ArrayList<>();

        OffsetDateTime requestDate = OffsetDateTime.now();

        String id = "SAME ID";
        // First feature
        IGeometry firstPosition = IGeometry.point(0, 0);
        Feature feature = Feature.build(id, "owner", null, firstPosition, EntityType.DATA, geoModelName);
        GeodeProperties.addGeodeProperties(feature);
        events.add(FeatureCreationRequestEvent.build(owner, metadata, feature, requestDate));

        // Second feature with same id
        IGeometry secondPosition = IGeometry.point(10, 10);
        feature = Feature.build(id, "owner", null, secondPosition, EntityType.DATA, geoModelName);
        GeodeProperties.addGeodeProperties(feature);
        events.add(FeatureCreationRequestEvent.build(owner, metadata, feature, requestDate.minusSeconds(1)));

        // Third feature with same id
        IGeometry thirdPosition = IGeometry.point(22, 22);
        feature = Feature.build(id, "owner", null, thirdPosition, EntityType.DATA, geoModelName);
        GeodeProperties.addGeodeProperties(feature);
        events.add(FeatureCreationRequestEvent.build(owner, metadata, feature, requestDate.minusSeconds(2)));

        saveEvents(events);

        // When
        featureService.scheduleRequests();
        waitFeature(1, null, 60_000);
        //Process request 1
        featureNotificationService.sendToNotifier();
        mockNotificationResponseSuccess();

        featureService.scheduleRequests();
        waitFeature(2, null, 60_000);
        //Process request 2
        featureNotificationService.sendToNotifier();
        mockNotificationResponseSuccess();

        featureService.scheduleRequests();
        waitFeature(3, null, 60_000);
        // Last request 3 not processed

        // Then
        // Control request processing order (older to newer, look at request date)
        List<FeatureEntity> entities = featureRepo.findAll(Sort.by(Order.asc("version")));

        Assert.assertEquals(thirdPosition, entities.get(0).getFeature().getGeometry());
        Assert.assertEquals(Integer.valueOf(1), entities.get(0).getFeature().getUrn().getVersion());

        Assert.assertEquals(secondPosition, entities.get(1).getFeature().getGeometry());
        Assert.assertEquals(Integer.valueOf(2), entities.get(1).getFeature().getUrn().getVersion());

        Assert.assertEquals(firstPosition, entities.get(2).getFeature().getGeometry());
        Assert.assertEquals(Integer.valueOf(3), entities.get(2).getFeature().getUrn().getVersion());

        // Check successful requests are deleted now in database
        // Only one request, the last request is not processed
        Assert.assertEquals(1, this.abstractFeatureRequestRepo.findAll().size());
        Assert.assertEquals(FeatureRequestStep.LOCAL_TO_BE_NOTIFIED,
                            this.abstractFeatureRequestRepo.findAll().get(0).getStep());
    }

    private void saveEvents(List<FeatureCreationRequestEvent> events) {
        long start = System.currentTimeMillis();
        LOGGER.info(">>>>>>>>>>>>>>>>> Registering {} requests", events.size());
        featureService.registerRequests(events);
        LOGGER.info(">>>>>>>>>>>>>>>>> {} requests registered in {} ms",
                    events.size(),
                    System.currentTimeMillis() - start);
    }

    @SuppressWarnings("unused")
    private void addDefaultProperties(Feature feature) {
        feature.addProperty(IProperty.buildString("data_type", "TYPE01"));
        feature.addProperty(IProperty.buildObject("file_characterization",
                                                  IProperty.buildBoolean("valid", Boolean.TRUE)));
    }
}
