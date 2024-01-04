package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureSessionMetadata;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureUpdateRequestEvent;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.assertj.core.util.Lists;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_uperf",
                                   "regards.feature.max.bulk.size=10",
                                   "regards.amqp.enabled=true",
                                   "regards.feature.metrics.enabled=true"
                                   //                        , "spring.jpa.show-sql=true"
}, locations = { "classpath:regards_perf.properties", "classpath:batch.properties", "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler", "noFemHandler" })
public class FeatureUpdatePerformanceIT extends AbstractFeatureMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureUpdatePerformanceIT.class);

    private static final Integer NB_FEATURES = 50;

    // Expected performance : 1_000 features/min
    private static final long MAX_DURATION_IN_MS = NB_FEATURES * 60;

    private static final String PROVIDER_ID_FORMAT = "F%05d";

    @Autowired
    private IFeatureUpdateService featureService;

    /**
     * Test creation of EVENTS_NUMBER features Check if
     * {@link FeatureCreationRequest} and {@link FeatureEntity}are stored in
     * database then at the end of the job test if all
     * {@link FeatureCreationRequest} are deleted
     */
    @Test
    public void createFeatures() throws InterruptedException {

        // Register creation requests
        FeatureSessionMetadata metadata = FeatureSessionMetadata.build("sessionOwner",
                                                                       "session",
                                                                       PriorityLevel.NORMAL,
                                                                       Lists.emptyList());

        // Register referenced features
        Map<String, FeatureUniformResourceName> refs = savePreviousVersions(geoModelName);

        List<FeatureUpdateRequestEvent> events = new ArrayList<>();
        OffsetDateTime requestDate = OffsetDateTime.now();
        int bulk = 0;
        for (int i = 1; i <= NB_FEATURES; i++) {
            bulk++;
            String id = String.format(PROVIDER_ID_FORMAT, i);
            Feature feature = Feature.build(id,
                                            "owner",
                                            refs.get(id),
                                            IGeometry.unlocated(),
                                            EntityType.DATA,
                                            geoModelName);
            events.add(FeatureUpdateRequestEvent.build("test", metadata, feature, requestDate));

            if (bulk == properties.getMaxBulkSize()) {
                saveEvents(events);
                events.clear();
                bulk = 0;
            }
        }

        if (bulk > 0) {
            saveEvents(events);
        }

        assertEquals(NB_FEATURES.longValue(), this.featureUpdateRequestRepo.count());

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return featureUpdateRequestRepo.count() > 0;
        });
        Thread.sleep(properties.getDelayBeforeProcessing() * 1000);

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return featureService.scheduleRequests() == 0;
        });
        long start = System.currentTimeMillis();
        waitFeature(NB_FEATURES, requestDate, 6_000);
        long duration = System.currentTimeMillis() - start;
        Assert.assertTrue(String.format("Performance not reached! (%d/%d)", duration, MAX_DURATION_IN_MS),
                          duration < MAX_DURATION_IN_MS);
        LOGGER.info(">>>>>>>>>>>>>>>>> {} requests processed in {} ms", NB_FEATURES, duration);

        assertEquals(NB_FEATURES.longValue(), this.featureRepo.count());
    }

    /**
     * Save features to update
     */
    private Map<String, FeatureUniformResourceName> savePreviousVersions(String modelName) {

        long start = System.currentTimeMillis();
        Map<String, FeatureUniformResourceName> urns = new HashMap<>();

        List<FeatureEntity> refEntities = new ArrayList<>();
        int bulk = 0;
        for (int i = 1; i <= NB_FEATURES; i++) {
            bulk++;
            String id = String.format(PROVIDER_ID_FORMAT, i);
            Feature feature = Feature.build(id, "owner", null, IGeometry.unlocated(), EntityType.DATA, modelName);
            UUID uuid = UUID.nameUUIDFromBytes(feature.getId().getBytes());
            feature.setUrn(FeatureUniformResourceName.build(FeatureIdentifier.FEATURE,
                                                            feature.getEntityType(),
                                                            runtimeTenantResolver.getTenant(),
                                                            uuid,
                                                            1));
            GeodeProperties.addGeodeProperties(feature);
            // Keep track of urn for further update
            urns.put(feature.getId(), feature.getUrn());
            refEntities.add(FeatureEntity.build("sessionOwner", "session", feature, null, "model"));

            if (bulk == properties.getMaxBulkSize()) {
                featureRepo.saveAll(refEntities);
                refEntities.clear();
                bulk = 0;
            }
        }

        if (bulk > 0) {
            featureRepo.saveAll(refEntities);
        }

        LOGGER.info(">>>>>>>>>>>>>>>>> {} features registered in {} ms",
                    NB_FEATURES,
                    System.currentTimeMillis() - start);
        return urns;
    }

    private void saveEvents(List<FeatureUpdateRequestEvent> events) {
        long start = System.currentTimeMillis();
        LOGGER.info(">>>>>>>>>>>>>>>>> Registering {} requests", events.size());
        featureService.registerRequests(events);
        LOGGER.info(">>>>>>>>>>>>>>>>> {} requests registered in {} ms",
                    events.size(),
                    System.currentTimeMillis() - start);
    }

}
