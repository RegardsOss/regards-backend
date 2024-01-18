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
package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.SearchFeatureRequestParameters;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.service.request.IFeatureRequestService;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_version",
                                   "regards.amqp.enabled=true" },
                    locations = { "classpath:regards_perf.properties",
                                  "classpath:batch.properties",
                                  "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler", "noFemHandler" })
public class FeatureCreationSessionIT extends AbstractFeatureMultitenantServiceIT {

    @Autowired
    private IFeatureRequestService featureRequestService;

    @Test
    public void testSessionNotifierWithNotification() throws InterruptedException {
        int requestCount = 10;
        prepareCreationTestData(true, requestCount, true, true, false);
        waitCreationRequestDeletion(0, 20000);

        // Compute Session step
        computeSessionStep(requestCount * 4, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests(requestCount * 3, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(requestCount, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(requestCount, property("referencingRequests"), requests);
        checkRequests(requestCount, property("referencedProducts"), requests);
        checkRequests(requestCount * 2, property("runningReferencingRequests"), requests);
        checkRequests(requestCount, inputRelated(), requests);
        checkRequests(requestCount, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(requestCount, sessionStep.getInputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(3, sessionStepProperties.size());
        checkKey(requestCount, "referencingRequests", sessionStepProperties);
        checkKey(requestCount, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
    }

    @Test
    public void testSessionNotifierWithoutNotification() throws InterruptedException, EntityException {

        setNotificationSetting(false);

        int requestCount = 10;
        prepareCreationTestData(true, requestCount, false, true, false);
        waitCreationRequestDeletion(0, 20000);

        // Compute Session step
        computeSessionStep(requestCount * 4, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests(requestCount * 3, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(requestCount, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(requestCount, property("referencingRequests"), requests);
        checkRequests(requestCount, property("referencedProducts"), requests);
        checkRequests(requestCount * 2, property("runningReferencingRequests"), requests);
        checkRequests(requestCount, inputRelated(), requests);
        checkRequests(requestCount, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(requestCount, sessionStep.getInputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(3, sessionStepProperties.size());
        checkKey(requestCount, "referencingRequests", sessionStepProperties);
        checkKey(requestCount, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
    }

    @Test
    public void testSessionNotifierWithRetryOnFileError() throws InterruptedException {

        // Init requests
        int requestCount = 10;
        createRequestsWithOneFileError(requestCount);

        // Retry request in error
        featureCreationService.retryRequests(new SearchFeatureRequestParameters());
        featureCreationService.scheduleRequests();
        TimeUnit.SECONDS.sleep(5);
        mockStorageHelper.mockFeatureCreationStorageSuccess(Optional.of(1));

        mockNotificationSuccess();
        // Give it some time
        waitCreationRequestDeletion(0, 40000);

        // Compute Session step
        // for each product 4 events : 1request + 1 requestRunning + 1 referencedProduct  -1 requestRunning
        // for storage error : 1 inErrorReferencingRequest
        // For retry : +1requestRunning -1 inErrorReferencingRequest -1requestRunning
        computeSessionStep((requestCount * 4) + 1 + 3, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests((requestCount * 3) + 2, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(requestCount + 2, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(requestCount, property("referencingRequests"), requests);
        checkRequests(requestCount, property("referencedProducts"), requests);
        checkRequests((requestCount + 1) * 2, property("runningReferencingRequests"), requests);
        checkRequests(2, property("inErrorReferencingRequests"), requests);
        checkRequests(requestCount, inputRelated(), requests);
        checkRequests(requestCount, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(requestCount, sessionStep.getInputRelated());
        Assertions.assertEquals(requestCount, sessionStep.getOutputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(4, sessionStepProperties.size());
        checkKey(requestCount, "referencingRequests", sessionStepProperties);
        checkKey(requestCount, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(0, "inErrorReferencingRequests", sessionStepProperties);
    }

    @Test
    public void testSessionNotifierWithDeleteOnFileError() throws InterruptedException {

        // Init requests
        int requestCount = 10;
        createRequestsWithOneFileError(requestCount);

        // Delete request in error
        featureCreationService.deleteRequests(new SearchFeatureRequestParameters());
        waitCreationRequestDeletion(0, 20000);

        // Compute Session step
        // for each product 4 events : 1request + 1 requestRunning + 1 referencedProduct  -1 requestRunning
        // for storage error : 1 inErrorReferencingRequest
        // for delete referenceRequest : -1inErrorReferencingRequest -1request +1 deleteRequest
        // The additional request is done to delete feature associated to the request in error deleted.
        computeSessionStep((requestCount * 4) + 1 + 3, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests((requestCount * 3) + 1 + 1, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(requestCount + 2, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(requestCount + 1, property("referencingRequests"), requests);
        checkRequests(requestCount, property("referencedProducts"), requests);
        checkRequests(requestCount * 2, property("runningReferencingRequests"), requests);
        checkRequests(2, property("inErrorReferencingRequests"), requests);
        checkRequests(1, property("deleteRequests"), requests);
        checkRequests(requestCount + 1, inputRelated(), requests);
        checkRequests(requestCount, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(requestCount - 1, sessionStep.getInputRelated());
        Assertions.assertEquals(requestCount, sessionStep.getOutputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(5, sessionStepProperties.size());
        checkKey(requestCount - 1, "referencingRequests", sessionStepProperties);
        checkKey(1, "deleteRequests", sessionStepProperties);
        checkKey(requestCount, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(0, "inErrorReferencingRequests", sessionStepProperties);
    }

    @Test
    public void testSessionNotifierWithRetryOnNotificationError() throws InterruptedException {

        // Init requests
        createOneRequestsWithNotificationError();

        // Retry request in error
        featureCreationService.retryRequests(new SearchFeatureRequestParameters());
        featureCreationService.scheduleRequests();
        TimeUnit.SECONDS.sleep(5);
        mockNotificationSuccess();
        waitCreationRequestDeletion(0, 20000);

        // Compute Session step
        computeSessionStep(8, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests(5, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(3, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(1, property("referencingRequests"), requests);
        checkRequests(1, property("referencedProducts"), requests);
        checkRequests(4, property("runningReferencingRequests"), requests);
        checkRequests(2, property("inErrorReferencingRequests"), requests);
        checkRequests(1, inputRelated(), requests);
        checkRequests(1, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(1, sessionStep.getInputRelated());
        Assertions.assertEquals(1, sessionStep.getOutputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(4, sessionStepProperties.size());
        checkKey(1, "referencingRequests", sessionStepProperties);
        checkKey(1, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(0, "inErrorReferencingRequests", sessionStepProperties);
    }

    @Test
    public void testSessionNotifierWithDeleteOnNotificationError() throws InterruptedException {

        // Init requests
        createOneRequestsWithNotificationError();

        // Delete request in error
        featureCreationService.deleteRequests(new SearchFeatureRequestParameters());
        waitCreationRequestDeletion(0, 20000);

        // Compute Session step
        computeSessionStep(7, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests(4, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(3, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(2, property("referencingRequests"), requests);
        checkRequests(2, property("runningReferencingRequests"), requests);
        checkRequests(2, property("inErrorReferencingRequests"), requests);
        checkRequests(1, property("referencedProducts"), requests);
        // No deletion request should be created to delete the feature assoiated to the product when the request
        // error is remote notification.
        checkRequests(0, property("deleteRequests"), requests);
        checkRequests(2, inputRelated(), requests);
        checkRequests(1, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(0, sessionStep.getInputRelated());
        Assertions.assertEquals(1, sessionStep.getOutputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(4, sessionStepProperties.size());
        checkKey(0, "referencingRequests", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(0, "inErrorReferencingRequests", sessionStepProperties);
        checkKey(1, "referencedProducts", sessionStepProperties);
    }

    private void createRequestsWithOneFileError(int requestCount) throws InterruptedException {

        initData(requestCount);
        mockNotificationSuccess();

        Pageable pageToRequest = PageRequest.of(0, requestCount);
        Page<FeatureCreationRequest> fcrPage = featureCreationRequestRepo.findByStep(FeatureRequestStep.REMOTE_STORAGE_REQUESTED,
                                                                                     pageToRequest);
        List<String> requestIds = fcrPage.stream().map(AbstractFeatureRequest::getGroupId).collect(Collectors.toList());
        String errorId = requestIds.remove(0);
        RequestResultInfoDto requestResultInfoDTO = new RequestResultInfoDto();
        ReflectionTestUtils.setField(requestResultInfoDTO, "groupId", errorId);
        featureRequestService.handleStorageError(Sets.newSet(requestResultInfoDTO));
        mockStorageHelper.mockFeatureCreationStorageSuccess(new HashSet<>(requestIds));
        mockNotificationSuccess();
        // Give it some time
        waitCreationRequestDeletion(1, 40000);
        waitForStep(featureCreationRequestRepo, FeatureRequestStep.REMOTE_STORAGE_ERROR, 1, 10_000);

        // Compute Session step
        // for each product : 1request + 1 requestRunning + 1 referencedProduct + 1 requestRunning
        // for storage error : 1 inErrorReferencingRequest
        computeSessionStep((requestCount * 4) + 1, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests((requestCount * 3) + 1, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(requestCount, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(requestCount, property("referencingRequests"), requests);
        checkRequests(requestCount, property("referencedProducts"), requests);
        checkRequests(requestCount * 2, property("runningReferencingRequests"), requests);
        checkRequests(1, property("inErrorReferencingRequests"), requests);
        checkRequests(requestCount, inputRelated(), requests);
        checkRequests(requestCount, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(requestCount, sessionStep.getInputRelated());
        Assertions.assertEquals(requestCount, sessionStep.getOutputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(4, sessionStepProperties.size());
        checkKey(requestCount, "referencingRequests", sessionStepProperties);
        checkKey(requestCount, "referencedProducts", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(1, "inErrorReferencingRequests", sessionStepProperties);
    }

    private void createOneRequestsWithNotificationError() throws InterruptedException {

        prepareCreationTestData(false, 1, false, true, false);
        mockNotificationError();
        waitCreationRequestDeletion(1, 20000);
        waitForStep(featureCreationRequestRepo, FeatureRequestStep.REMOTE_NOTIFICATION_ERROR, 1, 10_000);

        // Compute Session step
        computeSessionStep(5, 1);

        // Check Session step values
        List<StepPropertyUpdateRequest> requests = stepPropertyUpdateRequestRepository.findAll();
        checkRequests(4, type(StepPropertyEventTypeEnum.INC), requests);
        checkRequests(1, type(StepPropertyEventTypeEnum.DEC), requests);
        checkRequests(1, property("referencingRequests"), requests);
        checkRequests(2, property("runningReferencingRequests"), requests);
        checkRequests(1, property("inErrorReferencingRequests"), requests);
        checkRequests(1, property("referencedProducts"), requests);
        checkRequests(1, inputRelated(), requests);
        checkRequests(1, outputRelated(), requests);

        // Check Session step
        SessionStep sessionStep = getSessionStep();
        Assertions.assertEquals(StepTypeEnum.REFERENCING, sessionStep.getType());
        Assertions.assertEquals(1, sessionStep.getInputRelated());
        SessionStepProperties sessionStepProperties = sessionStep.getProperties();
        Assertions.assertEquals(4, sessionStepProperties.size());
        checkKey(1, "referencingRequests", sessionStepProperties);
        checkKey(0, "runningReferencingRequests", sessionStepProperties);
        checkKey(1, "inErrorReferencingRequests", sessionStepProperties);
        // As error is on notification product is well referenced
        checkKey(1, "referencedProducts", sessionStepProperties);
    }

}
