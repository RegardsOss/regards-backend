/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.rest.abort;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureDeletionRequestRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureNotificationRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.*;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.rest.FeatureRequestController;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Test for {@link FeatureRequestAbortController} to verify if valid feature requests can be aborted via the REST
 * endpoint.
 *
 * @author Iliana Ghazali
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=rest_feature_requests_abort_it",
                                   "spring.jpa.properties.hibernate.jdbc.batch_size=1024" })
@ActiveProfiles(value = { "noscheduler" })
public class FeatureRequestAbortControllerIT extends AbstractRegardsIT {

    public static final String SOURCE = "abort-test";

    @Autowired
    private IFeatureCreationRequestRepository featureRequestCreationRepo;

    @Autowired
    private IFeatureDeletionRequestRepository featureRequestDeletionRepo;

    @Autowired
    private IFeatureNotificationRequestRepository featureRequestNotificationRepo;

    @Value("${regards.feature.abort.delay.hours:1}")
    private long abortDelayInHours;

    @Before
    public void init() {
        featureRequestCreationRepo.deleteAll();
        featureRequestDeletionRepo.deleteAll();
        featureRequestNotificationRepo.deleteAll();
    }

    @Test
    public void givenValidCreationAbortRequests_whenSent_thenAborted() {
        // GIVEN
        // build valid requests that can be aborted
        FeatureRequestTypeEnum requestType = FeatureRequestTypeEnum.CREATION;
        int nbRequests = 3;
        createRequestsToAbort(requestType, RequestState.GRANTED, nbRequests);
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk()
                                                                        .expectValue("$.totalHandled", nbRequests)
                                                                        .expectValue("$.totalRequested", nbRequests);
        SearchFeatureRequestParameters selection = new SearchFeatureRequestParameters().withSource(SOURCE);

        // WHEN
        // abort action is requested
        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestAbortController.ABORT_PATH,
                           selection,
                           requestBuilderCustomizer,
                           "Error aborting requests",
                           requestType);

        // THEN
        // Now all feature of the source should be on ERROR state
        searchAbortedRequests(nbRequests, nbRequests, requestType, selection);
    }

    @Test
    public void givenInvalidUpdateAbortRequests_whenSent_thenAborted() {
        // GIVEN
        // build invalid requests that can not be aborted
        FeatureRequestTypeEnum requestType = FeatureRequestTypeEnum.DELETION;
        FeatureDeletionRequest featureSaved = featureRequestDeletionRepo.save(FeatureDeletionRequest.build(UUID.randomUUID()
                                                                                                               .toString(),
                                                                                                           SOURCE,
                                                                                                           OffsetDateTime.now(),
                                                                                                           RequestState.GRANTED,
                                                                                                           Sets.newHashSet(),
                                                                                                           FeatureRequestStep.LOCAL_ERROR,
                                                                                                           PriorityLevel.NORMAL,
                                                                                                           FeatureUniformResourceName.pseudoRandomUrn(
                                                                                                               FeatureIdentifier.FEATURE,
                                                                                                               EntityType.DATA,
                                                                                                               this.getDefaultTenant(),
                                                                                                               1)));

        // WHEN
        // abort action is requested
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk()
                                                                        .expectValue("$.totalHandled", 0)
                                                                        .expectValue("$.totalRequested", 0);
        SearchFeatureRequestParameters selection = new SearchFeatureRequestParameters().withIdsIncluded(Set.of(
            featureSaved.getId()));

        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestAbortController.ABORT_PATH,
                           selection,
                           requestBuilderCustomizer,
                           "Error aborting requests",
                           requestType);

        // THEN
        // Request should not be aborted
        searchAbortedRequests(1, 0, requestType, selection);
    }

    private void searchAbortedRequests(int nbExpectedRequests,
                                       int nbExpectedRequestsInError,
                                       FeatureRequestTypeEnum requestType,
                                       SearchFeatureRequestParameters searchParams) {
        RequestBuilderCustomizer requestBuilderCustomizer;
        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectIsArray("$.content")
                                               .expectToHaveSize("$.content", nbExpectedRequests)
                                               .expectValue("$.info.nbErrors", nbExpectedRequestsInError)
                                               .skipDocumentation();
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");

        performDefaultPost(FeatureRequestController.ROOT_PATH + FeatureRequestController.REQUEST_SEARCH_TYPE_PATH,
                           searchParams,
                           requestBuilderCustomizer,
                           "Error retrying feature requests in creation",
                           requestType);
    }

    private void createRequestsToAbort(FeatureRequestTypeEnum type, RequestState state, int nbRequests) {

        int nbRequestsSaved = switch (type) {
            case NOTIFICATION -> {
                List<FeatureNotificationRequest> requests = new ArrayList<>(nbRequests);
                for (int i = 0; i < nbRequests; i++) {
                    FeatureNotificationRequest request = FeatureNotificationRequest.build(UUID.randomUUID().toString(),
                                                                                          SOURCE,
                                                                                          OffsetDateTime.now()
                                                                                                        .minusHours(
                                                                                                            abortDelayInHours),
                                                                                          FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED,
                                                                                          PriorityLevel.NORMAL,
                                                                                          FeatureUniformResourceName.pseudoRandomUrn(
                                                                                              FeatureIdentifier.FEATURE,
                                                                                              EntityType.DATA,
                                                                                              this.getDefaultTenant(),
                                                                                              1),
                                                                                          state);
                    request.setRegistrationDate(OffsetDateTime.now().minusHours(abortDelayInHours));
                    requests.add(request);
                }
                yield featureRequestNotificationRepo.saveAll(requests).size();
            }
            case CREATION -> {
                List<FeatureCreationRequest> requests = new ArrayList<>(nbRequests);
                for (int i = 0; i < nbRequests; i++) {
                    FeatureCreationRequest request = FeatureCreationRequest.build(UUID.randomUUID().toString(),
                                                                                  "owner",
                                                                                  OffsetDateTime.now()
                                                                                                .minusHours(
                                                                                                    abortDelayInHours),
                                                                                  state,
                                                                                  Set.of(),
                                                                                  Feature.build("feature-" + i,
                                                                                                "owner",
                                                                                                FeatureUniformResourceName.pseudoRandomUrn(
                                                                                                    FeatureIdentifier.FEATURE,
                                                                                                    EntityType.DATA,
                                                                                                    this.getDefaultTenant(),
                                                                                                    1),
                                                                                                null,
                                                                                                EntityType.DATA,
                                                                                                "model"),
                                                                                  FeatureCreationMetadataEntity.build(
                                                                                      SOURCE,
                                                                                      "testStorage",
                                                                                      List.of(),
                                                                                      true),
                                                                                  FeatureRequestStep.REMOTE_STORAGE_REQUESTED,
                                                                                  PriorityLevel.NORMAL);
                    request.setRegistrationDate(OffsetDateTime.now().minusHours(abortDelayInHours));
                    requests.add(request);
                }
                yield featureRequestCreationRepo.saveAll(requests).size();

            }
            default -> throw new UnsupportedOperationException(type
                                                               + " is not supported for the moment. Add it to the"
                                                               + " list if this case must be tested.");
        };
        Assertions.assertThat(nbRequestsSaved).isEqualTo(nbRequests);

    }

}
