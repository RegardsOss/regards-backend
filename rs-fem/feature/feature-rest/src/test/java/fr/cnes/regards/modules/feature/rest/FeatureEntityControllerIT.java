/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.feature.rest;

import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityWithDisseminationRepository;
import fr.cnes.regards.modules.feature.documentation.FeatureEntityControllerDocumentationHelper;
import fr.cnes.regards.modules.feature.documentation.RequestsControllerDocumentationHelper;
import fr.cnes.regards.modules.feature.domain.FeatureDisseminationInfo;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.dto.FeaturesSelectionDTO;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.job.PublishFeatureNotificationJob;
import fr.cnes.regards.modules.feature.service.job.ScheduleFeatureDeletionJobsJob;
import org.assertj.core.util.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @author Sébastien Binda
 */
@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=feature", "regards.amqp.enabled=true",
        "spring.jpa.properties.hibernate.jdbc.batch_size=1024", "spring.jpa.properties.hibernate.order_inserts=true" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
@ContextConfiguration(classes = { AbstractMultitenantServiceIT.ScanningConfiguration.class })
public class FeatureEntityControllerIT extends AbstractFeatureIT {

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    protected IFeatureEntityWithDisseminationRepository featureEntityWithDisseminationRepository;

    @Test
    public void getFeature() {
        createFeatures("feature_1_", 10, "source1", "session1");
        FeatureUniformResourceName urn = featureRepo.findAll().stream().findFirst().get().getUrn();

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.documentResponseBody(FeatureEntityControllerDocumentationHelper.featureEntityDTOResponseDoc(
            Optional.empty()));
        performDefaultGet(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT + FeatureEntityControler.URN_PATH,
                          requestBuilderCustomizer,
                          "Error retrieving features",
                          urn.toString());

    }

    @Test
    public void getFeatureWithDissemination() {
        createFeatures("feature_1_", 10, "source1", "session1");
        List<FeatureEntity> allFeaturesEntity = this.featureEntityWithDisseminationRepository.findAll();
        for (FeatureEntity featureEntity : allFeaturesEntity) {
            featureEntity.setDisseminationPending(true);
            featureEntity.setDisseminationsInfo(Sets.newLinkedHashSet(new FeatureDisseminationInfo("SomeRemote", true),
                                                                      new FeatureDisseminationInfo("AnotherRemote",
                                                                                                   false)));
        }
        this.featureEntityWithDisseminationRepository.saveAll(allFeaturesEntity);

        FeatureUniformResourceName urn = featureRepo.findAll().stream().findFirst().get().getUrn();

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk()
                                                                        .expectToHaveSize("$.disseminationsInfo", 2)
                                                                        .skipDocumentation();
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.documentResponseBody(FeatureEntityControllerDocumentationHelper.featureEntityDTOResponseDoc(
            Optional.empty()));
        performDefaultGet(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT + FeatureEntityControler.URN_PATH,
                          requestBuilderCustomizer,
                          "Error retrieving features",
                          urn.toString());

    }

    @Test
    public void getFeatures() throws Exception {
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());

        OffsetDateTime start = OffsetDateTime.now();
        createFeatures("feature_1_", 10, "source1", "session1");
        OffsetDateTime between = OffsetDateTime.now();
        createFeatures("feature_2_", 10, "source1", "session2");
        OffsetDateTime end = OffsetDateTime.now();
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk()
                                                                        .expectIsArray("$.content")
                                                                        .expectToHaveSize("$.content", 20);
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.addParameter("model", "FEATURE01").skipDocumentation();
        performDefaultGet(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT,
                          requestBuilderCustomizer,
                          "Error retrieving features");

        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 10)
                                               .skipDocumentation();
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.addParameter("session", "session2");
        performDefaultGet(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT,
                          requestBuilderCustomizer,
                          "Error retrieving features");

        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 1);
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.addParameter("source", "source1");
        requestBuilderCustomizer.addParameter("model", "FEATURE01");
        requestBuilderCustomizer.addParameter("providerId", "feature_1_5");
        List<ParameterDescriptor> params = FeatureEntityControllerDocumentationHelper.featuresSearchParametersDoc();
        params.addAll(RequestsControllerDocumentationHelper.paginationDoc());
        requestBuilderCustomizer.documentRequestParameters(params);
        requestBuilderCustomizer.documentResponseBody(FeatureEntityControllerDocumentationHelper.featureEntityDTOPageResponseDoc());
        performDefaultGet(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT,
                          requestBuilderCustomizer,
                          "Error retrieving features");

        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 20)
                                               .skipDocumentation();
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.addParameter("from", start.toString());
        requestBuilderCustomizer.addParameter("to", end.toString());
        performDefaultGet(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT,
                          requestBuilderCustomizer,
                          "Error retrieving features");

        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 10)
                                               .skipDocumentation();
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.addParameter("from", start.toString());
        requestBuilderCustomizer.addParameter("to", between.toString());
        performDefaultGet(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT,
                          requestBuilderCustomizer,
                          "Error retrieving features");

        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 10)
                                               .skipDocumentation();
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.addParameter("from", between.toString());
        performDefaultGet(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT,
                          requestBuilderCustomizer,
                          "Error retrieving features");

        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 0)
                                               .skipDocumentation();
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.addParameter("source", "source1");
        requestBuilderCustomizer.addParameter("session", "session2");
        requestBuilderCustomizer.addParameter("model", "FEATURE01");
        requestBuilderCustomizer.addParameter("providerId", "feature_1_5");
        performDefaultGet(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT,
                          requestBuilderCustomizer,
                          "Error retrieving features");
    }

    @Test
    public void notifyFeatures() {
        createFeatures("feature", 10, "source1", "session1");
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.documentRequestBody(FeatureEntityControllerDocumentationHelper.featureSelectionDTODoc());
        performDefaultPost(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT + FeatureEntityControler.NOTIFY_PATH,
                           FeaturesSelectionDTO.build(),
                           requestBuilderCustomizer,
                           "Error during feature notification request");
        Assert.assertEquals(Long.valueOf(1),
                            jobInfoService.retrieveJobsCount(PublishFeatureNotificationJob.class.getName(),
                                                             JobStatus.QUEUED,
                                                             JobStatus.TO_BE_RUN,
                                                             JobStatus.SUCCEEDED,
                                                             JobStatus.RUNNING));
    }

    @Test
    public void deleteFeatures() {
        createFeatures("feature", 10, "source1", "session1");
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        requestBuilderCustomizer.documentRequestBody(FeatureEntityControllerDocumentationHelper.featureSelectionDTODoc());
        performDefaultDelete(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT + FeatureEntityControler.DELETE_PATH,
                             FeaturesSelectionDTO.build(),
                             requestBuilderCustomizer,
                             "Error during feature deltion request");
        Assert.assertEquals(Long.valueOf(1),
                            jobInfoService.retrieveJobsCount(ScheduleFeatureDeletionJobsJob.class.getName(),
                                                             JobStatus.QUEUED,
                                                             JobStatus.TO_BE_RUN,
                                                             JobStatus.SUCCEEDED,
                                                             JobStatus.RUNNING));
    }

}
