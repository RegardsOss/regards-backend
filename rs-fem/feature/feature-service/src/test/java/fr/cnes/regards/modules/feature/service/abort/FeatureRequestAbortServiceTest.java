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
package fr.cnes.regards.modules.feature.service.abort;

import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestTypeEnum;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestHandledResponse;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsInfo;
import fr.cnes.regards.modules.feature.dto.hateoas.RequestsPage;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.request.FeatureRequestService;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionNotifier;
import fr.cnes.regards.modules.feature.service.session.FeatureSessionProperty;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Test for {@link FeatureRequestAbortService} to verify if valid feature requests can be aborted and other requests
 * are ignored.</p>
 * TEST PLAN
 * <ul>
 *  <li>Nominal cases :
 *    <ul>
 *      <li>{@link #givenValidCreationRequestsToAbort_whenHandled_thenAborted()}</li>
 *      <li>{@link #givenValidNotificationRequestsToAbort_whenHandled_thenAborted()}</li>
 *      <li>{@link #givenValidAndInvalidRequestsToAbort_whenHandled_thenAbortedAndIgnored()}</li>
 *    </ul></li>
 *  <li>Error cases :
 *    <ul>
 *      <li>{@link #givenUnknownRequestStateToAbort_whenHandled_thenIgnored()}</li>
 *      <li>{@link #givenUnknownRequestStepToAbort_whenHandled_thenIgnored()}</li>
 *      <li>{@link #givenUnknownRequestTypeToAbort_whenHandled_thenIgnored()}</li>
 *      <li>{@link #givenRequestWithInvalidDelayToAbort_whenHandled_thenIgnored()}</li>
 *    </ul></li>
 * </ul>
 **/

@ExtendWith(MockitoExtension.class)
class FeatureRequestAbortServiceTest {

    public static final String SOURCE_TEST = "sourceTest";

    public static final String SESSION_TEST = "sessionTest";

    public static final int ABORT_DELAY_IN_HOURS = 1;

    private FeatureRequestAbortService featureRequestAbortService;

    @Mock
    private FeatureRequestService featureRequestService;

    @Mock
    private FeatureSessionNotifier featureSessionNotifier;

    @BeforeEach
    void init() {
        this.featureRequestAbortService = new FeatureRequestAbortService(featureRequestService,
                                                                         featureSessionNotifier,
                                                                         2,
                                                                         ABORT_DELAY_IN_HOURS);
    }

    @Test
    void givenValidCreationRequestsToAbort_whenHandled_thenAborted() {
        // GIVEN
        // build creation requests, this type can be aborted
        FeatureRequestTypeEnum requestType = FeatureRequestTypeEnum.CREATION;
        List<FeatureRequestDTO> simulatedFeatures = buildRequestsToAbort(requestType,
                                                                         RequestState.GRANTED,
                                                                         FeatureRequestStep.REMOTE_STORAGE_REQUESTED,
                                                                         OffsetDateTime.now()
                                                                                       .minusHours(ABORT_DELAY_IN_HOURS),
                                                                         2);
        mockFindAllRequestsMethod(simulatedFeatures);

        // WHEN
        RequestHandledResponse result = featureRequestAbortService.abortRequests(new SearchFeatureRequestParameters(),
                                                                                 requestType);

        // THEN
        // requests should be aborted and the source/session should be updated
        int nbExpectedRequests = simulatedFeatures.size();
        Assertions.assertThat(result.getTotalRequested()).isEqualTo(nbExpectedRequests);
        Assertions.assertThat(result.getTotalHandled()).isEqualTo(nbExpectedRequests);
        Assertions.assertThat(result.getMessage()).isNull();
        verifyUpdateRequests(simulatedFeatures, requestType);
        verifyUpdateSourceAndSession(simulatedFeatures, requestType);
    }

    @Test
    void givenValidNotificationRequestsToAbort_whenHandled_thenAborted() {
        // GIVEN
        // build notification requests, this type can be aborted
        FeatureRequestTypeEnum requestType = FeatureRequestTypeEnum.NOTIFICATION;
        List<FeatureRequestDTO> simulatedFeatures = buildRequestsToAbort(requestType,
                                                                         RequestState.GRANTED,
                                                                         FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED,
                                                                         OffsetDateTime.now()
                                                                                       .minusHours(ABORT_DELAY_IN_HOURS),
                                                                         5);
        mockFindAllRequestsMethod(simulatedFeatures);

        // WHEN
        RequestHandledResponse result = featureRequestAbortService.abortRequests(new SearchFeatureRequestParameters(),
                                                                                 requestType);

        // THEN
        // requests should be aborted and the source/session should be updated
        int nbExpectedRequests = simulatedFeatures.size();
        Assertions.assertThat(result.getTotalRequested()).isEqualTo(nbExpectedRequests);
        Assertions.assertThat(result.getTotalHandled()).isEqualTo(nbExpectedRequests);
        Assertions.assertThat(result.getMessage()).isNull();
        verifyUpdateRequests(simulatedFeatures, requestType);
        verifyUpdateSourceAndSession(simulatedFeatures, requestType);
    }

    @Test
    void givenValidAndInvalidRequestsToAbort_whenHandled_thenAbortedAndIgnored() {
        // GIVEN
        // build creation requests with state that can be aborted
        FeatureRequestTypeEnum requestType = FeatureRequestTypeEnum.CREATION;
        List<FeatureRequestDTO> validRequestsToAbort = buildRequestsToAbort(requestType,
                                                                            RequestState.GRANTED,
                                                                            FeatureRequestStep.REMOTE_STORAGE_REQUESTED,
                                                                            OffsetDateTime.now()
                                                                                          .minusHours(
                                                                                              ABORT_DELAY_IN_HOURS),
                                                                            3);
        // build creation requests with state that cannot be aborted
        List<FeatureRequestDTO> invalidRequestsToIgnore = buildRequestsToAbort(requestType,
                                                                               RequestState.ERROR,
                                                                               FeatureRequestStep.REMOTE_NOTIFICATION_ERROR,
                                                                               OffsetDateTime.now()
                                                                                             .minusHours(
                                                                                                 ABORT_DELAY_IN_HOURS),
                                                                               5);
        List<FeatureRequestDTO> simulatedFeatures = ListUtils.union(validRequestsToAbort, invalidRequestsToIgnore);
        mockFindAllRequestsMethod(simulatedFeatures);

        // WHEN
        RequestHandledResponse result = featureRequestAbortService.abortRequests(new SearchFeatureRequestParameters(),
                                                                                 requestType);

        // THEN
        // not all requests are aborted
        int nbExpectedRequests = simulatedFeatures.size();
        Assertions.assertThat(result.getTotalRequested()).isEqualTo(nbExpectedRequests);
        Assertions.assertThat(result.getTotalHandled()).isEqualTo(validRequestsToAbort.size());
        Assertions.assertThat(result.getMessage()).contains("not valid");
        verifyUpdateRequests(validRequestsToAbort, requestType);
        verifyUpdateSourceAndSession(validRequestsToAbort, requestType);
    }

    @Test
    void givenUnknownRequestStateToAbort_whenHandled_thenIgnored() {
        // GIVEN
        // build notification requests with state that cannot be aborted
        FeatureRequestTypeEnum requestType = FeatureRequestTypeEnum.NOTIFICATION;
        List<FeatureRequestDTO> simulatedFeatures = buildRequestsToAbort(requestType,
                                                                         RequestState.ERROR,
                                                                         FeatureRequestStep.REMOTE_NOTIFICATION_ERROR,
                                                                         OffsetDateTime.now()
                                                                                       .minusHours(ABORT_DELAY_IN_HOURS),
                                                                         2);
        mockFindAllRequestsMethod(simulatedFeatures);

        // WHEN
        RequestHandledResponse result = featureRequestAbortService.abortRequests(new SearchFeatureRequestParameters(),
                                                                                 requestType);

        // THEN
        // requests should be ignored
        Assertions.assertThat(result.getTotalRequested()).isEqualTo(simulatedFeatures.size());
        Assertions.assertThat(result.getTotalHandled()).isZero();
        Assertions.assertThat(result.getMessage()).contains("not valid");
    }

    @Test
    void givenUnknownRequestStepToAbort_whenHandled_thenIgnored() {
        // GIVEN
        // build creation requests with step that cannot be aborted
        FeatureRequestTypeEnum requestType = FeatureRequestTypeEnum.CREATION;
        List<FeatureRequestDTO> simulatedFeatures = buildRequestsToAbort(requestType,
                                                                         RequestState.GRANTED,
                                                                         FeatureRequestStep.LOCAL_SCHEDULED,
                                                                         OffsetDateTime.now()
                                                                                       .minusHours(ABORT_DELAY_IN_HOURS),
                                                                         2);
        mockFindAllRequestsMethod(simulatedFeatures);

        // WHEN
        RequestHandledResponse result = featureRequestAbortService.abortRequests(new SearchFeatureRequestParameters(),
                                                                                 requestType);

        // THEN
        // requests should be ignored
        Assertions.assertThat(result.getTotalRequested()).isEqualTo(simulatedFeatures.size());
        Assertions.assertThat(result.getTotalHandled()).isZero();
        Assertions.assertThat(result.getMessage()).contains("not valid");
    }

    @Test
    void givenUpdateRequestTypeWithUnknownStepToAbort_whenHandled_thenIgnored() {
        // GIVEN
        // build requests with type that cannot be aborted
        FeatureRequestTypeEnum requestType = FeatureRequestTypeEnum.UPDATE;
        List<FeatureRequestDTO> simulatedFeatures = buildRequestsToAbort(requestType,
                                                                         RequestState.GRANTED,
                                                                         FeatureRequestStep.WAITING_BLOCKING_DISSEMINATION,
                                                                         OffsetDateTime.now()
                                                                                       .minusHours(ABORT_DELAY_IN_HOURS),
                                                                         3);
        mockFindAllRequestsMethod(simulatedFeatures);

        // WHEN
        RequestHandledResponse result = featureRequestAbortService.abortRequests(new SearchFeatureRequestParameters(),
                                                                                 requestType);

        // THEN
        // requests should be ignored
        Assertions.assertThat(result.getTotalRequested()).isEqualTo(3);
        Assertions.assertThat(result.getTotalHandled()).isZero();
        Assertions.assertThat(result.getMessage()).contains("number of requests to abort is different");
    }

    @Test
    void givenUpdateRequestTypeWithStepToAbort_whenHandled_thenIgnored() {
        // GIVEN
        // build requests with type that cannot be aborted
        FeatureRequestTypeEnum requestType = FeatureRequestTypeEnum.UPDATE;
        List<FeatureRequestDTO> simulatedFeatures = buildRequestsToAbort(requestType,
                                                                         RequestState.GRANTED,
                                                                         FeatureRequestStep.REMOTE_NOTIFICATION_REQUESTED,
                                                                         OffsetDateTime.now()
                                                                                       .minusHours(ABORT_DELAY_IN_HOURS),
                                                                         3);
        mockFindAllRequestsMethod(simulatedFeatures);

        // WHEN
        RequestHandledResponse result = featureRequestAbortService.abortRequests(new SearchFeatureRequestParameters(),
                                                                                 requestType);

        // THEN
        // requests should be ignored
        Assertions.assertThat(result.getTotalRequested()).isEqualTo(3);
        Assertions.assertThat(result.getTotalHandled()).isEqualTo(3);
        Assertions.assertThat(result.getMessage()).isNull();
    }

    @Test
    void givenDeletionRequestTypeWithStepToAbort_whenHandled_thenIgnored() {
        // GIVEN
        // build requests with type that cannot be aborted
        FeatureRequestTypeEnum requestType = FeatureRequestTypeEnum.DELETION;
        List<FeatureRequestDTO> simulatedFeatures = buildRequestsToAbort(requestType,
                                                                         RequestState.GRANTED,
                                                                         FeatureRequestStep.REMOTE_STORAGE_DELETION_REQUESTED,
                                                                         OffsetDateTime.now()
                                                                                       .minusHours(ABORT_DELAY_IN_HOURS),
                                                                         3);
        mockFindAllRequestsMethod(simulatedFeatures);

        // WHEN
        RequestHandledResponse result = featureRequestAbortService.abortRequests(new SearchFeatureRequestParameters(),
                                                                                 requestType);

        // THEN
        // requests should be ignored
        Assertions.assertThat(result.getTotalRequested()).isEqualTo(3);
        Assertions.assertThat(result.getTotalHandled()).isEqualTo(3);
        Assertions.assertThat(result.getMessage()).isNull();
    }

    @Test
    void givenRequestWithInvalidDelayToAbort_whenHandled_thenIgnored() {
        // GIVEN
        // build creation requests with type that can be aborted but not yet
        FeatureRequestTypeEnum requestType = FeatureRequestTypeEnum.CREATION;
        List<FeatureRequestDTO> simulatedFeatures = buildRequestsToAbort(requestType,
                                                                         RequestState.GRANTED,
                                                                         FeatureRequestStep.REMOTE_STORAGE_REQUESTED,
                                                                         OffsetDateTime.now().minusSeconds(1),
                                                                         2);
        mockFindAllRequestsMethod(simulatedFeatures);

        // WHEN
        RequestHandledResponse result = featureRequestAbortService.abortRequests(new SearchFeatureRequestParameters(),
                                                                                 requestType);

        // THEN
        // requests should be ignored
        Assertions.assertThat(result.getTotalRequested()).isEqualTo(simulatedFeatures.size());
        Assertions.assertThat(result.getTotalHandled()).isZero();
        Assertions.assertThat(result.getMessage()).contains("not valid");
    }

    // ---------------
    // ---- UTILS ----
    // ---------------

    private void mockFindAllRequestsMethod(List<FeatureRequestDTO> simulatedFeatures) {
        Mockito.when(featureRequestService.findAll(Mockito.any(), Mockito.any(), Mockito.any())).thenAnswer(ans -> {
            Pageable capturedPageable = ans.getArgument(2);
            Page<FeatureRequestDTO> featurePage = toFeatureRequestDtoPage(simulatedFeatures, capturedPageable);
            return new RequestsPage<>(featurePage.getContent(),
                                      RequestsInfo.build(0L),
                                      capturedPageable,
                                      featurePage.getTotalElements());
        });
    }

    private void verifyUpdateRequests(List<FeatureRequestDTO> simulatedFeatures, FeatureRequestTypeEnum requestType) {
        ArgumentCaptor<Set<Long>> requestsIdsCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<FeatureRequestStep> argumentCaptor = ArgumentCaptor.forClass(FeatureRequestStep.class);

        Mockito.verify(featureRequestService, Mockito.timeout(5_000).atLeastOnce())
               .updateRequestStateAndStep(requestsIdsCaptor.capture(),
                                          ArgumentMatchers.eq(RequestState.ERROR),
                                          argumentCaptor.capture());

        List<Long> idsProcessed = requestsIdsCaptor.getAllValues().stream().flatMap(Set::stream).toList();
        Assertions.assertThat(idsProcessed)
                  .containsExactlyInAnyOrderElementsOf(simulatedFeatures.stream()
                                                                        .map(FeatureRequestDTO::getId)
                                                                        .toList());

        Set<FeatureRequestStep> stepsProcessed = new HashSet<>(argumentCaptor.getAllValues());
        Assertions.assertThat(stepsProcessed)
                  .containsExactlyInAnyOrderElementsOf(simulatedFeatures.stream()
                                                                        .map(feature -> FeatureRequestAbortService.STEPS_CORRELATION_TABLE.get(
                                                                            requestType).get(feature.getStep()))
                                                                        .collect(Collectors.toSet()));
    }

    private void verifyUpdateSourceAndSession(List<FeatureRequestDTO> simulatedFeatures,
                                              FeatureRequestTypeEnum requestType) {
        ArgumentCaptor<Long> numberOfRunningDecrementCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> numberOfErrorIncrementsCaptor = ArgumentCaptor.forClass(Long.class);

        switch (requestType) {
            case NOTIFICATION -> {
                Mockito.verify(featureSessionNotifier, Mockito.timeout(5_000).atLeastOnce())
                       .decrementCount(ArgumentMatchers.eq(SOURCE_TEST),
                                       ArgumentMatchers.eq(SESSION_TEST),
                                       ArgumentMatchers.eq(FeatureSessionProperty.RUNNING_NOTIFY_REQUESTS),
                                       numberOfRunningDecrementCaptor.capture());
                Mockito.verify(featureSessionNotifier, Mockito.timeout(5_000).atLeastOnce())
                       .incrementCount(ArgumentMatchers.eq(SOURCE_TEST),
                                       ArgumentMatchers.eq(SESSION_TEST),
                                       ArgumentMatchers.eq(FeatureSessionProperty.IN_ERROR_NOTIFY_REQUESTS),
                                       numberOfErrorIncrementsCaptor.capture());
            }
            case CREATION -> {
                Mockito.verify(featureSessionNotifier, Mockito.timeout(5_000).atLeastOnce())
                       .decrementCount(ArgumentMatchers.eq(SOURCE_TEST),
                                       ArgumentMatchers.eq(SESSION_TEST),
                                       ArgumentMatchers.eq(FeatureSessionProperty.RUNNING_REFERENCING_REQUESTS),
                                       numberOfRunningDecrementCaptor.capture());
                Mockito.verify(featureSessionNotifier, Mockito.timeout(5_000).atLeastOnce())
                       .incrementCount(ArgumentMatchers.eq(SOURCE_TEST),
                                       ArgumentMatchers.eq(SESSION_TEST),
                                       ArgumentMatchers.eq(FeatureSessionProperty.IN_ERROR_REFERENCING_REQUESTS),
                                       numberOfErrorIncrementsCaptor.capture());
            }
            default -> Assertions.fail("Unexpected request type " + requestType);
        }
        Assertions.assertThat(numberOfRunningDecrementCaptor.getAllValues().stream().mapToLong(Long::longValue).sum())
                  .isEqualTo(simulatedFeatures.size());
        Assertions.assertThat(numberOfErrorIncrementsCaptor.getAllValues().stream().mapToLong(Long::longValue).sum())
                  .isEqualTo(simulatedFeatures.size());
    }

    public List<FeatureRequestDTO> buildRequestsToAbort(FeatureRequestTypeEnum typeEnum,
                                                        RequestState requestState,
                                                        FeatureRequestStep step,
                                                        OffsetDateTime requestRegistrationDate,
                                                        int nbFeatures) {
        List<FeatureRequestDTO> features = new ArrayList<>(nbFeatures);
        for (int i = 0; i < nbFeatures; i++) {
            FeatureRequestDTO requestDto = new FeatureRequestDTO();
            requestDto.setId(RandomUtils.nextLong());
            requestDto.setState(requestState);
            requestDto.setStep(step);
            requestDto.setRegistrationDate(requestRegistrationDate);
            requestDto.setSession(SESSION_TEST);
            requestDto.setSource(SOURCE_TEST);
            requestDto.setType(typeEnum.name());
            requestDto.setUrn(FeatureUniformResourceName.pseudoRandomUrn(FeatureIdentifier.FEATURE,
                                                                         EntityType.DATA,
                                                                         "defaultTenant",
                                                                         ABORT_DELAY_IN_HOURS));
            requestDto.setProviderId("feature-" + requestDto.getId());
            features.add(requestDto);
        }

        return features;
    }

    public Page<FeatureRequestDTO> toFeatureRequestDtoPage(List<FeatureRequestDTO> featureRequestDtos,
                                                           Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), featureRequestDtos.size());
        if (start > featureRequestDtos.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, featureRequestDtos.size());
        } else {
            return new PageImpl<>(featureRequestDtos.subList(start, end), pageable, featureRequestDtos.size());

        }
    }
}
