package fr.cnes.regards.modules.feature.service;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
import fr.cnes.regards.modules.model.dto.properties.IProperty;

@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=feature_perf", "regards.amqp.enabled=true",
                "regards.feature.metrics.enabled=true" },
        locations = { "classpath:regards_perf.properties", "classpath:batch.properties",
                "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler", "nohandler" })
public class FeaturePerformanceTest extends AbstractFeatureMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturePerformanceTest.class);

    private static final Integer NB_FEATURES = 10_000;

    @Autowired
    private IFeatureCreationService featureService;

    /**
     * Test creation of EVENTS_NUMBER features Check if
     * {@link FeatureCreationRequest} and {@link FeatureEntity}are stored in
     * database then at the end of the job test if all
     * {@link FeatureCreationRequest} are deleted
     *
     * @throws InterruptedException
     */
    @Test
    public void createFeatures() throws InterruptedException {

        String format = "F%05d";

        // Register creation requests
        FeatureCreationSessionMetadata metadata = FeatureCreationSessionMetadata
                .build("sessionOwner", "session", PriorityLevel.NORMAL, Lists.emptyList(), true);
        String modelName = mockModelClient(GeodeProperties.getGeodeModel());

        Thread.sleep(5_000);

        long start = System.currentTimeMillis();

        List<FeatureCreationRequestEvent> events = new ArrayList<>();
        int bulk = 0;
        for (int i = 1; i <= NB_FEATURES; i++) {
            bulk++;
            String id = String.format(format, i);
            Feature feature = Feature.build(id, null, IGeometry.unlocated(), EntityType.DATA, modelName);
            GeodeProperties.addGeodeProperties(feature);
            events.add(FeatureCreationRequestEvent.build(metadata, feature));

            if (bulk == properties.getMaxBulkSize()) {
                saveEvents(events);
                events.clear();
                bulk = 0;
            }
        }

        if (bulk > 0) {
            saveEvents(events);
        }

        LOGGER.info(">>>>>>>>>>>>>>>>> {} requests registered in {} ms", NB_FEATURES,
                    System.currentTimeMillis() - start);

        assertEquals(NB_FEATURES.longValue(), this.featureCreationRequestRepo.count());

        boolean schedule;
        do {
            schedule = featureService.scheduleRequests() > 0;
        } while (schedule);

        waitFeature(NB_FEATURES, null, 3600_000);

        LOGGER.info(">>>>>>>>>>>>>>>>> {} requests processed in {} ms", NB_FEATURES,
                    System.currentTimeMillis() - start);

        assertEquals(NB_FEATURES.longValue(), this.featureRepo.count());
    }

    private void saveEvents(List<FeatureCreationRequestEvent> events) {
        long start = System.currentTimeMillis();
        LOGGER.info(">>>>>>>>>>>>>>>>> Registering {} requests", events.size());
        featureService.registerRequests(events);
        LOGGER.info(">>>>>>>>>>>>>>>>> {} requests registered in {} ms", events.size(),
                    System.currentTimeMillis() - start);
    }

    @SuppressWarnings("unused")
    private void addDefaultProperties(Feature feature) {
        feature.addProperty(IProperty.buildString("data_type", "TYPE01"));
        feature.addProperty(IProperty.buildObject("file_characterization",
                                                  IProperty.buildBoolean("valid", Boolean.TRUE)));
    }
}
