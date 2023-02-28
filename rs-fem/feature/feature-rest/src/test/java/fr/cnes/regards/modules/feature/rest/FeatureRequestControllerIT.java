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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureDeletionRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureNotificationRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureUpdateRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.*;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_requests",
                                   "spring.jpa.properties.hibernate.jdbc.batch_size=1024" })
@ActiveProfiles(value = { "noscheduler" })
public class FeatureRequestControllerIT extends AbstractRegardsIT {

    @Autowired
    private IFeatureCreationRequestRepository featureRequestCreationRepo;

    @Autowired
    private IFeatureDeletionRequestRepository featureRequestDeletionRepo;

    @Autowired
    private IFeatureUpdateRequestRepository featureRequestUpdateRepo;

    @Autowired
    private IFeatureNotificationRequestRepository featureRequestNotificationRepo;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    OffsetDateTime date;

    @Before
    public void init() {
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());
        // Delete existing requests
        featureRequestCreationRepo.deleteAll();
        featureRequestDeletionRepo.deleteAll();
        featureRequestUpdateRepo.deleteAll();
        featureRequestNotificationRepo.deleteAll();
        // Create creation requests
        // Source1/Sesssion1
        createCreationRequests("feature1_",
                               "source1",
                               "session1",
                               RequestState.GRANTED,
                               FeatureRequestStep.LOCAL_DELAYED,
                               100);

        // Source1/Sesssion2
        createCreationRequests("feature2_",
                               "source1",
                               "session2",
                               RequestState.GRANTED,
                               FeatureRequestStep.LOCAL_DELAYED,
                               50);

        // Create creation requests with error state
        createCreationRequests("feature3_",
                               "source1",
                               "session2",
                               RequestState.ERROR,
                               FeatureRequestStep.LOCAL_ERROR,
                               30);
        date = OffsetDateTime.now();
        createCreationRequests("feature4_",
                               "source1",
                               "session3",
                               RequestState.ERROR,
                               FeatureRequestStep.LOCAL_ERROR,
                               20);

        // Create deletion requests
        createDeletionRequests(RequestState.GRANTED, 10);
        // Create update requests
        createUpdateRequests(RequestState.GRANTED, 20);
        // Create notification requests
        createNotificationRequests(RequestState.GRANTED, 5);

    }

    @Test
    public void retrieveRequests() {
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());

        // Retrieve without filters
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk()
                                                                        .expectIsArray("$.content")
                                                                        .expectToHaveSize("$.content", 200)
                                                                        .expectValue("$.info.nbErrors", 50);
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");

        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestController.REQUEST_SEARCH_TYPE_PATH,
                           new SearchFeatureRequestParameters(),
                           requestBuilderCustomizer,
                           "Error retrying feature requests in creation",
                           FeatureRequestTypeEnum.CREATION);

        // Retrieve with unknown providerId filter
        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectIsEmpty("$.content")
                                               .expectValue("$.info.nbErrors", 0);
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "10");

        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestController.REQUEST_SEARCH_TYPE_PATH,
                           new SearchFeatureRequestParameters().withProviderIdsIncluded(List.of("toto")),
                           requestBuilderCustomizer,
                           "Error retrying feature requests in creation",
                           FeatureRequestTypeEnum.CREATION);

        // Retrieve with valid providerId filter
        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 1)
                                               .expectValue("$.info.nbErrors", 0);
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "10");

        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestController.REQUEST_SEARCH_TYPE_PATH,
                           new SearchFeatureRequestParameters().withProviderIdsIncluded(List.of("feature1_10")),
                           requestBuilderCustomizer,
                           "Error retrying feature requests in creation",
                           FeatureRequestTypeEnum.CREATION);

        // Retrieve by state
        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 50)
                                               .expectValue("$.info.nbErrors", 50);
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");
        requestBuilderCustomizer.addParameter("sort", "registrationDate,ASC");

        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestController.REQUEST_SEARCH_TYPE_PATH,
                           new SearchFeatureRequestParameters().withStatesIncluded(List.of(RequestState.ERROR)),
                           requestBuilderCustomizer,
                           "Error retrying feature requests in creation",
                           FeatureRequestTypeEnum.CREATION);

        // Retrieve by date
        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 20)
                                               .expectValue("$.info.nbErrors", 20);
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");
        requestBuilderCustomizer.addParameter("sort", "session,DESC");

        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestController.REQUEST_SEARCH_TYPE_PATH,
                           new SearchFeatureRequestParameters().withLastUpdateAfter(date),
                           requestBuilderCustomizer,
                           "Error retrying feature requests in creation",
                           FeatureRequestTypeEnum.CREATION);

        // Retrieve by source and session
        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 100)
                                               .expectValue("$.info.nbErrors", 0);
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");
        requestBuilderCustomizer.addParameter("sort", "state,ASC");

        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestController.REQUEST_SEARCH_TYPE_PATH,
                           new SearchFeatureRequestParameters().withSource("source1").withSession("session1"),
                           requestBuilderCustomizer,
                           "Error retrying feature requests in creation",
                           FeatureRequestTypeEnum.CREATION);

        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 80)
                                               .expectValue("$.info.nbErrors", 30);
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");
        requestBuilderCustomizer.addParameter("sort", FeatureRequestDTO.PROVIDER_ID_FIELD_NAME + ",ASC");

        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestController.REQUEST_SEARCH_TYPE_PATH,
                           new SearchFeatureRequestParameters().withSource("source1").withSession("session2"),
                           requestBuilderCustomizer,
                           "Error retrying feature requests in creation",
                           FeatureRequestTypeEnum.CREATION);
        //
        //
        // Retrieve deletion without filters
        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 10)
                                               .expectValue("$.info.nbErrors", 0)
                                               .skipDocumentation();
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");
        requestBuilderCustomizer.addParameter("sort", FeatureRequestDTO.PROVIDER_ID_FIELD_NAME + ",ASC");

        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestController.REQUEST_SEARCH_TYPE_PATH,
                           new SearchFeatureRequestParameters(),
                           requestBuilderCustomizer,
                           "Error retrying feature requests in deletion",
                           FeatureRequestTypeEnum.DELETION);

        // Retrieve deletion with source/session filter should never return results
        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 0)
                                               .expectValue("$.info.nbErrors", 0)
                                               .skipDocumentation();
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");
        requestBuilderCustomizer.addParameter("sort", FeatureRequestDTO.PROVIDER_ID_FIELD_NAME + ",ASC");

        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestController.REQUEST_SEARCH_TYPE_PATH,
                           new SearchFeatureRequestParameters().withSource("source1").withSession("session2"),
                           requestBuilderCustomizer,
                           "Error retrying feature requests in deletion",
                           FeatureRequestTypeEnum.DELETION);
        //
        //
        // Retrieve update without filters
        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 20)
                                               .expectValue("$.info.nbErrors", 0)
                                               .skipDocumentation();
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");
        requestBuilderCustomizer.addParameter("sort", FeatureRequestDTO.PROVIDER_ID_FIELD_NAME + ",ASC");
        requestBuilderCustomizer.addParameter("sort", "source,DESC");

        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestController.REQUEST_SEARCH_TYPE_PATH,
                           new SearchFeatureRequestParameters(),
                           requestBuilderCustomizer,
                           "Error retrying feature requests in update",
                           FeatureRequestTypeEnum.UPDATE);
        //
        //
        // Retrieve notification without filters
        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 5)
                                               .expectValue("$.info.nbErrors", 0)
                                               .skipDocumentation();
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");

        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestController.REQUEST_SEARCH_TYPE_PATH,
                           new SearchFeatureRequestParameters(),
                           requestBuilderCustomizer,
                           "Error retrying feature requests in notification",
                           FeatureRequestTypeEnum.NOTIFICATION);
    }

    @Test
    public void deleteRequests() {
        // Create 10 requests scheduled, so they cannot be deleted
        List<FeatureCreationRequest> notDeletable = createCreationRequests("to_delete_",
                                                                           "deletion_test",
                                                                           "test1",
                                                                           RequestState.GRANTED,
                                                                           FeatureRequestStep.LOCAL_SCHEDULED,
                                                                           10);
        // Create 10 requests in error status, so theu can be deleted
        List<FeatureCreationRequest> deletable = createCreationRequests("to_delete_",
                                                                        "deletion_test",
                                                                        "test1",
                                                                        RequestState.ERROR,
                                                                        FeatureRequestStep.LOCAL_ERROR,
                                                                        10);

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        SearchFeatureRequestParameters selection = new SearchFeatureRequestParameters().withIdsIncluded(List.of(
            notDeletable.get(0).getId()));
        performDefaultDelete(FeatureRequestController.ROOT_PATH + FeatureRequestController.DELETE_TYPE_PATH,
                             selection,
                             requestBuilderCustomizer,
                             "Error deleting requests",
                             FeatureRequestTypeEnum.CREATION);

        Assert.assertTrue("Feature request should not be deleted",
                          featureRequestCreationRepo.findById(notDeletable.get(0).getId()).isPresent());

        selection = new SearchFeatureRequestParameters().withIdsIncluded(List.of(deletable.get(0).getId()));
        performDefaultDelete(FeatureRequestController.ROOT_PATH + FeatureRequestController.DELETE_TYPE_PATH,
                             selection,
                             requestBuilderCustomizer,
                             "Error deleting requests",
                             FeatureRequestTypeEnum.CREATION);
        Assert.assertFalse("Feature request should be deleted",
                           featureRequestCreationRepo.findById(deletable.get(0).getId()).isPresent());

    }

    @Test
    public void retryRequests() {
        // Create 10 requests scheduled, so they cannot be deleted
        createCreationRequests("to_delete_",
                               "retry_source",
                               "test1",
                               RequestState.GRANTED,
                               FeatureRequestStep.LOCAL_SCHEDULED,
                               10);
        // Create 10 requests in error status, so they can be deleted
        createCreationRequests("to_delete_",
                               "retry_source",
                               "test1",
                               RequestState.ERROR,
                               FeatureRequestStep.LOCAL_ERROR,
                               10);

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk()
                                                                        .expectValue("$.totalHandled", 10)
                                                                        .expectValue("$.totalRequested", 10);
        SearchFeatureRequestParameters selection = new SearchFeatureRequestParameters().withSource("retry_source");
        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestController.RETRY_TYPE_PATH,
                           selection,
                           requestBuilderCustomizer,
                           "Error retrying requests",
                           FeatureRequestTypeEnum.CREATION);

        // Now all feature of source retry_source should be on GRANTED state
        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", 20)
                                               .expectValue("$.info.nbErrors", 0)
                                               .skipDocumentation();
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");

        SearchFeatureRequestParameters body = new SearchFeatureRequestParameters().withSource("retry_source");

        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestController.REQUEST_SEARCH_TYPE_PATH,
                           body,
                           requestBuilderCustomizer,
                           "Error retrying feature requests in creation",
                           FeatureRequestTypeEnum.CREATION);

    }

    private List<FeatureCreationRequest> createCreationRequests(String featureIdPrefix,
                                                                String source,
                                                                String session,
                                                                RequestState state,
                                                                FeatureRequestStep step,
                                                                int nbRequests) {
        List<FeatureCreationRequest> requests = Lists.newArrayList();
        for (int i = 0; i < nbRequests; i++) {
            Feature feature = Feature.build(featureIdPrefix + i,
                                            "owner",
                                            FeatureUniformResourceName.pseudoRandomUrn(FeatureIdentifier.FEATURE,
                                                                                       EntityType.DATA,
                                                                                       this.getDefaultTenant(),
                                                                                       1),
                                            null,
                                            EntityType.DATA,
                                            "model");
            FeatureCreationMetadataEntity metadata = FeatureCreationMetadataEntity.build(source,
                                                                                         session,
                                                                                         Lists.newArrayList(),
                                                                                         true);
            requests.add(FeatureCreationRequest.build(UUID.randomUUID().toString(),
                                                      "owner",
                                                      OffsetDateTime.now(),
                                                      state,
                                                      Sets.newHashSet(),
                                                      feature,
                                                      metadata,
                                                      step,
                                                      PriorityLevel.NORMAL));
        }
        requests = featureRequestCreationRepo.saveAll(requests);
        Assert.assertEquals(nbRequests, requests.size());
        return requests;
    }

    private int createDeletionRequests(RequestState state, int nbRequests) {
        List<FeatureDeletionRequest> deletions = Lists.newArrayList();
        for (int i = 0; i < nbRequests; i++) {
            deletions.add(FeatureDeletionRequest.build(UUID.randomUUID().toString(),
                                                       "owner",
                                                       OffsetDateTime.now(),
                                                       state,
                                                       Sets.newHashSet(),
                                                       FeatureRequestStep.LOCAL_ERROR,
                                                       PriorityLevel.NORMAL,
                                                       FeatureUniformResourceName.pseudoRandomUrn(FeatureIdentifier.FEATURE,
                                                                                                  EntityType.DATA,
                                                                                                  this.getDefaultTenant(),
                                                                                                  1)));
        }
        deletions = featureRequestDeletionRepo.saveAll(deletions);
        Assert.assertEquals(nbRequests, deletions.size());
        return deletions.size();
    }

    private int createUpdateRequests(RequestState state, int nbRequests) {
        List<FeatureUpdateRequest> requests = Lists.newArrayList();
        for (int i = 0; i < nbRequests; i++) {
            Feature feature = Feature.build("feature_update_" + i,
                                            "owner",
                                            FeatureUniformResourceName.pseudoRandomUrn(FeatureIdentifier.FEATURE,
                                                                                       EntityType.DATA,
                                                                                       this.getDefaultTenant(),
                                                                                       1),
                                            null,
                                            EntityType.DATA,
                                            "model");
            requests.add(FeatureUpdateRequest.build(UUID.randomUUID().toString(),
                                                    "owner",
                                                    OffsetDateTime.now(),
                                                    state,
                                                    Sets.newHashSet(),
                                                    feature,
                                                    PriorityLevel.NORMAL,
                                                    FeatureRequestStep.LOCAL_DELAYED));
        }
        requests = featureRequestUpdateRepo.saveAll(requests);
        Assert.assertEquals(nbRequests, requests.size());
        return requests.size();
    }

    private int createNotificationRequests(RequestState state, int nbRequests) {
        List<FeatureNotificationRequest> requests = Lists.newArrayList();
        for (int i = 0; i < nbRequests; i++) {
            requests.add(FeatureNotificationRequest.build(UUID.randomUUID().toString(),
                                                          "owner",
                                                          OffsetDateTime.now(),
                                                          FeatureRequestStep.LOCAL_DELAYED,
                                                          PriorityLevel.NORMAL,
                                                          FeatureUniformResourceName.pseudoRandomUrn(FeatureIdentifier.FEATURE,
                                                                                                     EntityType.DATA,
                                                                                                     this.getDefaultTenant(),
                                                                                                     1),
                                                          state));
        }
        requests = featureRequestNotificationRepo.saveAll(requests);
        Assert.assertEquals(nbRequests, requests.size());
        return requests.size();
    }
}
