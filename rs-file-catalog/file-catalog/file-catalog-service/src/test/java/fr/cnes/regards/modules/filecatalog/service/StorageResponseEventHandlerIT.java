/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.filecatalog.service;

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageResponseEvent;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.fileaccess.dto.output.StorageResponseErrorEnum;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileArchiveRequestEvent;
import fr.cnes.regards.modules.filecatalog.dao.IFileStorageRequestAggregationRepository;
import fr.cnes.regards.modules.filecatalog.domain.*;
import fr.cnes.regards.modules.filecatalog.domain.request.FileStorageRequestAggregation;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Test class for {@link fr.cnes.regards.modules.filecatalog.service.handler.StorageResponseEventHandler}
 *
 * @author Thibaud Michaudel
 **/
@ActiveProfiles({ "noscheduler", "nojobs", "test" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate"
                                   + ".default_schema=storage_response_event_handler_tests" },
                    locations = { "classpath:application-test.properties" })
public class StorageResponseEventHandlerIT extends AbstractFileCatalogIT {

    public static final long FILE_SIZE = 1024L;

    @Autowired
    private FileStorageRequestService fileStorageRequestService;

    @Autowired
    private IFileStorageRequestAggregationRepository fileStorageRequestAggregationRepository;

    @MockBean
    private FileReferenceRequestService fileReferenceRequestService;

    @MockBean
    private RequestsGroupService requestsGroupService;

    @MockBean
    private SessionNotifier sessionNotifier;

    @MockBean
    private INotificationClient notificationClient;

    @MockBean
    private FileReferenceEventPublisher fileReferenceEventPublisher;

    @Value("${spring.application.name}")
    private String applicationName = "rs-test";

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();
    }

    @Test
    public void test_one_request_success() throws ModuleException {
        // Given
        int i = 1;
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(i));
        String storedUrl = createUrl(i);
        StorageResponseEvent event1 = createStorageResponseEvent(request1, storedUrl);

        // Mock
        FileReferenceResult mockedReference = mockReferenceResponse(request1,
                                                                    storedUrl,
                                                                    FileReferenceResultStatusEnum.CREATED);

        // When
        fileStorageRequestService.processFileStorageSuccessResponses(Arrays.asList(event1));

        // Then

        // Verify fileReferenceRequestService.reference() is called as expected
        verifyReferenceCall(new ReferenceCallRecord(request1, storedUrl));

        // Verify requestsGroupService.requestSuccess() is called as expected
        verifyGroupsRequestSuccessCall(request1, mockedReference);

        // Verify sessionNotifier.decrementRunningRequests() is called as expected
        verifySessionNotifierDecrementRunningRequestsCall(request1);

        // Verify sessionNotifier.incrementStoredFiles() is called as expected
        verifySessionNotifierIncrementStoredFilesCall(request1);

        // Verify notificationClient.notifyRoles() is never called
        Mockito.verify(notificationClient, Mockito.never())
               .notifyRoles(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        // Verify that no archive event was sent
        Mockito.verify(publisher, Mockito.never()).publish((List<? extends ISubscribable>) Mockito.any());
    }

    @Test
    public void test_one_request_success_no_new_file() throws ModuleException {
        // Given
        int i = 1;
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(i));
        String storedUrl = createUrl(i);
        StorageResponseEvent event1 = createStorageResponseEvent(request1, storedUrl);

        // Mock
        FileReferenceResult mockedReference = mockReferenceResponse(request1,
                                                                    storedUrl,
                                                                    FileReferenceResultStatusEnum.UNMODIFIED);

        // When
        fileStorageRequestService.processFileStorageSuccessResponses(Arrays.asList(event1));

        // Then

        // Verify fileReferenceRequestService.reference() is called as expected
        verifyReferenceCall(new ReferenceCallRecord(request1, storedUrl));

        // Verify requestsGroupService.requestSuccess() is called as expected
        verifyGroupsRequestSuccessCall(request1, mockedReference);

        // Verify sessionNotifier.decrementRunningRequests() is called as expected
        verifySessionNotifierDecrementRunningRequestsCall(request1);

        // Verify sessionNotifier.incrementStoredFiles() is never called
        Mockito.verify(sessionNotifier, Mockito.never())
               .incrementStoredFiles(Mockito.any(), Mockito.any(), Mockito.anyInt());

        // Verify notificationClient.notifyRoles() is never called
        Mockito.verify(notificationClient, Mockito.never())
               .notifyRoles(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        // Verify that no archive event was sent
        Mockito.verify(publisher, Mockito.never()).publish((List<? extends ISubscribable>) Mockito.any());
    }

    @Test
    public void test_one_request_in_cache_success() throws ModuleException {
        // Given
        int i = 1;
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(i));
        String storedUrl = createUrl(i);
        String finalArchiveParentUrl = "http://storage.com/" + request1.getStorageSubDirectory() + "12345.zip";
        String fileCachePath = "/workspace/" + request1.getStorageSubDirectory() + request1.getMetaInfo().getFileName();
        StorageResponseEvent event1 = createCacheStorageResponseEvent(request1,
                                                                      storedUrl,
                                                                      finalArchiveParentUrl,
                                                                      fileCachePath);

        // When
        fileStorageRequestService.processFileStorageSuccessResponses(Arrays.asList(event1));

        // Then

        verifyHandleSuccessNeverCalled();

        // Verify that one archive event was sent
        verifyPublisherPublishCall(request1, finalArchiveParentUrl, fileCachePath);
    }

    @Test
    public void test_multiple_requests_in_cache_success() throws ModuleException {
        // Given
        int i = 1;
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(i));
        String storedUrl1 = createUrl(i);
        String finalArchiveParentUrl1 = "http://storage.com/" + request1.getStorageSubDirectory() + "12345.zip";
        String fileCachePath1 = "/workspace/" + request1.getStorageSubDirectory() + request1.getMetaInfo()
                                                                                            .getFileName();
        StorageResponseEvent event1 = createCacheStorageResponseEvent(request1,
                                                                      storedUrl1,
                                                                      finalArchiveParentUrl1,
                                                                      fileCachePath1);

        int j = 2;
        FileStorageRequestAggregation request2 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(j));
        String storedUrl2 = createUrl(j);
        String finalArchiveParentUrl2 = "http://storage.com/" + request2.getStorageSubDirectory() + "12345.zip";
        String fileCachePath2 = "/workspace/" + request2.getStorageSubDirectory() + request2.getMetaInfo()
                                                                                            .getFileName();
        StorageResponseEvent event2 = createCacheStorageResponseEvent(request2,
                                                                      storedUrl2,
                                                                      finalArchiveParentUrl2,
                                                                      fileCachePath2);

        // When
        fileStorageRequestService.processFileStorageSuccessResponses(Arrays.asList(event1, event2));

        // Then

        verifyHandleSuccessNeverCalled();

        // Verify that two archive events were sent
        verifyPublisherPublishCalls(Arrays.asList(request1, request2),
                                    Arrays.asList(finalArchiveParentUrl1, finalArchiveParentUrl2),
                                    Arrays.asList(fileCachePath1, fileCachePath2));
    }

    @Test
    public void test_multiple_requests_success() throws ModuleException {
        // Given
        int i = 1;
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(i));
        String storedUrl1 = createUrl(i);
        StorageResponseEvent event1 = createStorageResponseEvent(request1, storedUrl1);

        int j = 2;
        FileStorageRequestAggregation request2 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(j));
        String storedUrl2 = createUrl(j);
        StorageResponseEvent event2 = createStorageResponseEvent(request2, storedUrl2);

        // Mock
        List<FileReferenceResult> mockedReferences = mockReferenceResponses(Arrays.asList(request1, request2),
                                                                            Arrays.asList(storedUrl1, storedUrl2),
                                                                            Arrays.asList(FileReferenceResultStatusEnum.CREATED,
                                                                                          FileReferenceResultStatusEnum.CREATED));

        // When
        fileStorageRequestService.processFileStorageSuccessResponses(Arrays.asList(event1, event2));

        // Then

        // Verify fileReferenceRequestService.reference() is called as expected
        verifyReferenceCalls(Arrays.asList(new ReferenceCallRecord(request1, storedUrl1),
                                           new ReferenceCallRecord(request2, storedUrl2)));

        // Verify requestsGroupService.requestSuccess() is called as expected
        verifyGroupsRequestSuccessCalls(Arrays.asList(request1, request2),
                                        Arrays.asList(mockedReferences.get(0), mockedReferences.get(1)));

        // Verify sessionNotifier.decrementRunningRequests() is called as expected
        verifySessionNotifierDecrementRunningRequestsCalls(Arrays.asList(request1, request2));

        // Verify sessionNotifier.incrementStoredFiles() is called as expected
        verifySessionNotifierIncrementStoredFilesCalls(Arrays.asList(request1, request2));

        // Verify notificationClient.notifyRoles() is never called
        Mockito.verify(notificationClient, Mockito.never())
               .notifyRoles(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        // Verify that no archive event was sent
        Mockito.verify(publisher, Mockito.never()).publish((List<? extends ISubscribable>) Mockito.any());

    }

    @Test
    public void test_multiple_requests_success_one_new_file() throws ModuleException {
        // Given
        int i = 1;
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(i));
        String storedUrl1 = createUrl(i);
        StorageResponseEvent event1 = createStorageResponseEvent(request1, storedUrl1);

        int j = 2;
        FileStorageRequestAggregation request2 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(j));
        String storedUrl2 = createUrl(j);
        StorageResponseEvent event2 = createStorageResponseEvent(request2, storedUrl2);

        // Mock
        List<FileReferenceResult> mockedReferences = mockReferenceResponses(Arrays.asList(request1, request2),
                                                                            Arrays.asList(storedUrl1, storedUrl2),
                                                                            Arrays.asList(FileReferenceResultStatusEnum.CREATED,
                                                                                          FileReferenceResultStatusEnum.UNMODIFIED));

        // When
        fileStorageRequestService.processFileStorageSuccessResponses(Arrays.asList(event1, event2));

        // Then

        // Verify fileReferenceRequestService.reference() is called as expected
        verifyReferenceCalls(Arrays.asList(new ReferenceCallRecord(request1, storedUrl1),
                                           new ReferenceCallRecord(request2, storedUrl2)));

        // Verify requestsGroupService.requestSuccess() is called as expected
        verifyGroupsRequestSuccessCalls(Arrays.asList(request1, request2),
                                        Arrays.asList(mockedReferences.get(0), mockedReferences.get(1)));

        // Verify sessionNotifier.decrementRunningRequests() is called as expected
        verifySessionNotifierDecrementRunningRequestsCalls(Arrays.asList(request1, request2));

        // Verify sessionNotifier.incrementStoredFiles() is called as expected
        verifySessionNotifierIncrementStoredFilesCall(request1);

        // Verify notificationClient.notifyRoles() is never called
        Mockito.verify(notificationClient, Mockito.never())
               .notifyRoles(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        // Verify that no archive event was sent
        Mockito.verify(publisher, Mockito.never()).publish((List<? extends ISubscribable>) Mockito.any());

    }

    @Test
    public void test_one_regular_one_cache_success() throws ModuleException {
        // Given
        int i = 1;
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(i));
        String storedUrl1 = createUrl(i);
        String finalArchiveParentUrl1 = "http://storage.com/" + request1.getStorageSubDirectory() + "12345.zip";
        String fileCachePath1 = "/workspace/" + request1.getStorageSubDirectory() + request1.getMetaInfo()
                                                                                            .getFileName();
        StorageResponseEvent event1 = createCacheStorageResponseEvent(request1,
                                                                      storedUrl1,
                                                                      finalArchiveParentUrl1,
                                                                      fileCachePath1);

        int j = 2;
        FileStorageRequestAggregation request2 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(j));
        String storedUrl2 = createUrl(j);
        StorageResponseEvent event2 = createStorageResponseEvent(request2, storedUrl2);

        // Mock
        List<FileReferenceResult> mockedReferences = mockReferenceResponses(List.of(request2),
                                                                            List.of(storedUrl2),
                                                                            List.of(FileReferenceResultStatusEnum.CREATED));

        // When
        fileStorageRequestService.processFileStorageSuccessResponses(Arrays.asList(event1, event2));

        // Then

        // Verify fileReferenceRequestService.reference() is called as expected
        verifyReferenceCalls(Arrays.asList(new ReferenceCallRecord(request2, storedUrl2)));

        // Verify requestsGroupService.requestSuccess() is called as expected
        verifyGroupsRequestSuccessCalls(Arrays.asList(request2), Arrays.asList(mockedReferences.get(0)));

        // Verify sessionNotifier.decrementRunningRequests() is called as expected
        verifySessionNotifierDecrementRunningRequestsCalls(Arrays.asList(request2));

        // Verify sessionNotifier.incrementStoredFiles() is called as expected
        verifySessionNotifierIncrementStoredFilesCalls(Arrays.asList(request2));

        // Verify notificationClient.notifyRoles() is called as expected
        verifyNotifyRolesNeverCalled();

        // Verify that one archive event was sent
        verifyPublisherPublishCall(request1, finalArchiveParentUrl1, fileCachePath1);
    }

    @Test
    public void test_one_request_error() throws ModuleException {
        // Given
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(1));
        String errorCause = "Error !!";
        StorageResponseEvent event1 = StorageResponseEvent.createSimpleErrorResponse(applicationName
                                                                                     + "."
                                                                                     + request1.getId(),
                                                                                     StorageResponseErrorEnum.WORKER_ERROR,
                                                                                     errorCause);

        // When
        fileStorageRequestService.processFileStorageErrorResponses(Arrays.asList(event1));

        // Then

        // Verify fileReferenceEventPublisher.storeError() is called as expected
        verifyFileReferenceEventPublisherCall(request1, errorCause);

        // Verify requestsGroupService.requestError() is called as expected
        verifyGroupsRequestErrorCall(request1, errorCause);

        // Verify sessionNotifier.decrementErrorRequests() is called as expected
        verifySessionNotifierDecrementRunningRequestsCall(request1);

        // Verify sessionNotifier.incrementStoredFiles() is called as expected
        verifySessionNotifierIncrementErrorRequestsCall(request1);
    }

    @Test
    public void test_multiple_requests_error() throws ModuleException {
        // Given
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(1));
        String errorCause1 = "Error !!";
        StorageResponseEvent event1 = StorageResponseEvent.createSimpleErrorResponse(applicationName
                                                                                     + "."
                                                                                     + request1.getId(),
                                                                                     StorageResponseErrorEnum.WORKER_ERROR,
                                                                                     errorCause1);
        FileStorageRequestAggregation request2 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(2));
        String errorCause2 = "Big Mistake !!";
        StorageResponseEvent event2 = StorageResponseEvent.createSimpleErrorResponse(applicationName
                                                                                     + "."
                                                                                     + request2.getId(),
                                                                                     StorageResponseErrorEnum.UNKNOWN_STORAGE_LOCATION,
                                                                                     errorCause2);

        // When
        fileStorageRequestService.processFileStorageErrorResponses(Arrays.asList(event1, event2));

        // Then

        // Verify fileReferenceEventPublisher.storeError() is called as expected
        verifyFileReferenceEventPublisherCalls(Arrays.asList(request1, request2),
                                               Arrays.asList(errorCause1, errorCause2));

        // Verify requestsGroupService.requestError() is called as expected
        verifyGroupsRequestErrorCalls(Arrays.asList(request1, request2), Arrays.asList(errorCause1, errorCause2));

        // Verify sessionNotifier.decrementErrorRequests() is called as expected
        verifySessionNotifierDecrementRunningRequestsCalls(Arrays.asList(request1, request2));

        // Verify sessionNotifier.incrementStoredFiles() is called as expected
        verifySessionNotifierIncrementErrorRequestsCalls(Arrays.asList(request1, request2));
    }

    @Test
    public void test_one_success_one_error() throws ModuleException {
        // Given
        int i = 1;
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(i));
        String storedUrl = createUrl(i);
        StorageResponseEvent event1 = createStorageResponseEvent(request1, storedUrl);

        FileStorageRequestAggregation request2 = fileStorageRequestAggregationRepository.save(
            createFileStorageRequestAggregation(2));
        String errorCause = "Big Mistake !!";
        StorageResponseEvent event2 = StorageResponseEvent.createSimpleErrorResponse(applicationName
                                                                                     + "."
                                                                                     + request2.getId(),
                                                                                     StorageResponseErrorEnum.UNKNOWN_STORAGE_LOCATION,
                                                                                     errorCause);

        // Mock
        FileReferenceResult mockedReference = mockReferenceResponse(request1,
                                                                    storedUrl,
                                                                    FileReferenceResultStatusEnum.CREATED);

        // When
        fileStorageRequestService.processFileStorageSuccessResponses(Arrays.asList(event1));
        fileStorageRequestService.processFileStorageErrorResponses(Arrays.asList(event2));

        // Then

        // Verify fileReferenceRequestService.reference() is called as expected
        verifyReferenceCall(new ReferenceCallRecord(request1, storedUrl));

        // Verify fileReferenceEventPublisher.storeError() is called as expected
        verifyFileReferenceEventPublisherCall(request2, errorCause);

        // Verify requestsGroupService is called as expected
        verifyGroupsRequestSuccessCall(request1, mockedReference);
        verifyGroupsRequestErrorCall(request2, errorCause);

        // Verify sessionNotifier.decrementRunningRequests() is called as expected
        verifySessionNotifierDecrementRunningRequestsCalls(Arrays.asList(request1, request2));

        // Verify sessionNotifier is called as expected
        verifySessionNotifierIncrementStoredFilesCall(request1);
        verifySessionNotifierIncrementErrorRequestsCall(request2);

        // Verify notificationClient.notifyRoles() is never called
        Mockito.verify(notificationClient, Mockito.never())
               .notifyRoles(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        // Verify that no archive event was sent
        Mockito.verify(publisher, Mockito.never()).publish((List<? extends ISubscribable>) Mockito.any());

    }

    private StorageResponseEvent createStorageResponseEvent(FileStorageRequestAggregation request, String storedUrl) {
        return StorageResponseEvent.createSuccessResponse(applicationName + "." + request.getId(),
                                                          storedUrl,
                                                          request.getMetaInfo().getChecksum(),
                                                          FILE_SIZE,
                                                          null,
                                                          null);
    }

    private StorageResponseEvent createCacheStorageResponseEvent(FileStorageRequestAggregation request,
                                                                 String storedUrl,
                                                                 String finalArchiveParentUrl,
                                                                 String fileCachePath) {
        return StorageResponseEvent.createSuccessCacheResponse(applicationName + "." + request.getId(),
                                                               storedUrl,
                                                               request.getMetaInfo().getChecksum(),
                                                               FILE_SIZE,
                                                               null,
                                                               null,
                                                               finalArchiveParentUrl,
                                                               fileCachePath);
    }

    private void verifyFileReferenceEventPublisherCall(FileStorageRequestAggregation request, String errorCause) {
        verifyFileReferenceEventPublisherCalls(Arrays.asList(request), Arrays.asList(errorCause));
    }

    private void verifyFileReferenceEventPublisherCalls(List<FileStorageRequestAggregation> requests,
                                                        List<String> errorCauses) {
        // Argument captors
        ArgumentCaptor<String> checksumCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Collection<String>> ownersCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<String> storageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Collection<String>> groupIdsCaptor = ArgumentCaptor.forClass(Collection.class);

        // Verify the number of calls and capture arguments
        Mockito.verify(fileReferenceEventPublisher, Mockito.times(requests.size()))
               .storeError(checksumCaptor.capture(),
                           ownersCaptor.capture(),
                           storageCaptor.capture(),
                           messageCaptor.capture(),
                           groupIdsCaptor.capture());

        // Get all captured values
        List<String> capturedChecksums = checksumCaptor.getAllValues();
        List<Collection<String>> capturedOwners = ownersCaptor.getAllValues();
        List<String> capturedStorages = storageCaptor.getAllValues();
        List<String> capturedMessages = messageCaptor.getAllValues();
        List<Collection<String>> capturedGroupIds = groupIdsCaptor.getAllValues();

        // Validate each call
        for (int i = 0; i < requests.size(); i++) {
            FileStorageRequestAggregation request = requests.get(i);
            String errorCause = errorCauses.get(i);

            // Assertions
            Assertions.assertEquals(request.getMetaInfo().getChecksum(),
                                    capturedChecksums.get(i),
                                    "Checksum mismatch.");
            Assertions.assertEquals(request.getOwner(), capturedOwners.get(i).iterator().next(), "Owner mismatch.");
            Assertions.assertEquals(request.getStorage(), capturedStorages.get(i), "Storage mismatch.");
            Assertions.assertEquals(errorCause, capturedMessages.get(i), "Error cause mismatch.");
            Assertions.assertEquals(request.getGroupIds(), capturedGroupIds.get(i), "Group IDs mismatch.");
        }
    }

    private void verifyPublisherPublishCalls(List<FileStorageRequestAggregation> requests,
                                             List<String> finalArchiveParentUrls,
                                             List<String> fileCachePaths) {
        // Verify the number of calls to the publisher
        ArgumentCaptor<List<FileArchiveRequestEvent>> eventCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(eventCaptor.capture());

        // Get the captured events for each call
        List<FileArchiveRequestEvent> capturedEvents = eventCaptor.getValue();
        Assertions.assertEquals(requests.size(),
                                capturedEvents.size(),
                                "Number of captured events does not match requests.");

        for (int i = 0; i < requests.size(); i++) {
            FileStorageRequestAggregation request = requests.get(i);
            String finalArchiveParentUrl = finalArchiveParentUrls.get(i);
            String fileCachePath = fileCachePaths.get(i);

            FileArchiveRequestEvent capturedEvent = capturedEvents.get(i);

            // Validate fields
            Assertions.assertEquals(request.getId(), capturedEvent.getFileStorageRequestId());
            Assertions.assertEquals(request.getStorage(), capturedEvent.getStorage());
            Assertions.assertEquals(request.getMetaInfo().getChecksum(), capturedEvent.getChecksum());
            Assertions.assertEquals(request.getMetaInfo().getFileName(), capturedEvent.getFileName());
            Assertions.assertEquals(request.getStorageSubDirectory(), capturedEvent.getStorageSubDirectory());
            Assertions.assertEquals(finalArchiveParentUrl, capturedEvent.getFinalArchiveParentUrl());
            Assertions.assertEquals(fileCachePath, capturedEvent.getFileCachePath());
            Assertions.assertEquals(FILE_SIZE, capturedEvent.getFileSize());
        }
    }

    private void verifyPublisherPublishCall(FileStorageRequestAggregation request,
                                            String finalArchiveParentUrl,
                                            String fileCachePath) {
        verifyPublisherPublishCalls(Arrays.asList(request),
                                    Arrays.asList(finalArchiveParentUrl),
                                    Arrays.asList(fileCachePath));
    }

    private void verifySessionNotifierIncrementStoredFilesCall(FileStorageRequestAggregation request) {
        verifySessionNotifierIncrementStoredFilesCalls(Arrays.asList(request));
    }

    private void verifySessionNotifierIncrementStoredFilesCalls(List<FileStorageRequestAggregation> requests) {
        // Captors for the arguments
        ArgumentCaptor<String> sessionOwnerCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sessionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> numberCaptor = ArgumentCaptor.forClass(Integer.class);

        // Verify the number of calls and capture arguments
        Mockito.verify(sessionNotifier, Mockito.times(requests.size()))
               .incrementStoredFiles(sessionOwnerCaptor.capture(), sessionCaptor.capture(), numberCaptor.capture());

        // Get all captured values
        List<String> capturedSessionOwners = sessionOwnerCaptor.getAllValues();
        List<String> capturedSessions = sessionCaptor.getAllValues();
        List<Integer> capturedNumbers = numberCaptor.getAllValues();

        // Validate each call
        for (int i = 0; i < requests.size(); i++) {
            FileStorageRequestAggregation request = requests.get(i);

            Assertions.assertEquals(request.getSessionOwner(), capturedSessionOwners.get(i), "Session owner mismatch.");
            Assertions.assertEquals(request.getSession(), capturedSessions.get(i), "Session mismatch.");
            Assertions.assertEquals(1, capturedNumbers.get(i), "Number of files mismatch.");
        }
    }

    private void verifySessionNotifierIncrementErrorRequestsCall(FileStorageRequestAggregation request) {
        verifySessionNotifierIncrementErrorRequestsCalls(Arrays.asList(request));
    }

    private void verifySessionNotifierIncrementErrorRequestsCalls(List<FileStorageRequestAggregation> requests) {
        ArgumentCaptor<String> sessionOwnerCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sessionCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(sessionNotifier, Mockito.times(requests.size()))
               .incrementErrorRequests(sessionOwnerCaptor.capture(), sessionCaptor.capture());

        // Get all captured values
        List<String> capturedSessionOwners = sessionOwnerCaptor.getAllValues();
        List<String> capturedSessions = sessionCaptor.getAllValues();

        // Validate each call
        for (int i = 0; i < requests.size(); i++) {
            FileStorageRequestAggregation request = requests.get(i);

            Assertions.assertEquals(request.getSessionOwner(), capturedSessionOwners.get(i), "Session owner mismatch.");
            Assertions.assertEquals(request.getSession(), capturedSessions.get(i), "Session mismatch.");
        }
    }

    private void verifySessionNotifierDecrementRunningRequestsCall(FileStorageRequestAggregation request) {
        verifySessionNotifierDecrementRunningRequestsCalls(Arrays.asList(request));
    }

    private void verifySessionNotifierDecrementRunningRequestsCalls(List<FileStorageRequestAggregation> requests) {
        // Captors for the arguments
        ArgumentCaptor<String> sessionOwnerCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sessionCaptor = ArgumentCaptor.forClass(String.class);

        // Verify the number of calls and capture arguments
        Mockito.verify(sessionNotifier, Mockito.times(requests.size()))
               .decrementRunningRequests(sessionOwnerCaptor.capture(), sessionCaptor.capture());

        // Get all captured values
        List<String> capturedSessionOwners = sessionOwnerCaptor.getAllValues();
        List<String> capturedSessions = sessionCaptor.getAllValues();

        // Validate each call
        for (int i = 0; i < requests.size(); i++) {
            FileStorageRequestAggregation request = requests.get(i);

            Assertions.assertEquals(request.getSessionOwner(), capturedSessionOwners.get(i), "Session owner mismatch.");
            Assertions.assertEquals(request.getSession(), capturedSessions.get(i), "Session mismatch.");
        }
    }

    private void verifyGroupsRequestSuccessCall(FileStorageRequestAggregation request,
                                                FileReferenceResult mockedReferenceResult) {
        verifyGroupsRequestSuccessCalls(Arrays.asList(request), Arrays.asList(mockedReferenceResult));
    }

    private void verifyGroupsRequestSuccessCalls(List<FileStorageRequestAggregation> requests,
                                                 List<FileReferenceResult> mockedReferenceResults) {
        // Argument captors for all parameters
        ArgumentCaptor<String> groupIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<FileRequestType> typeCaptor = ArgumentCaptor.forClass(FileRequestType.class);
        ArgumentCaptor<String> checksumCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> storageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> storePathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Collection<String>> ownersCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<FileReference> fileRefCaptor = ArgumentCaptor.forClass(FileReference.class);

        // Verify the number of calls and capture arguments
        Mockito.verify(requestsGroupService, Mockito.times(requests.size()))
               .requestSuccess(groupIdCaptor.capture(),
                               typeCaptor.capture(),
                               checksumCaptor.capture(),
                               storageCaptor.capture(),
                               storePathCaptor.capture(),
                               ownersCaptor.capture(),
                               fileRefCaptor.capture());

        // Get all captured values
        List<String> capturedGroupIds = groupIdCaptor.getAllValues();
        List<FileRequestType> capturedTypes = typeCaptor.getAllValues();
        List<String> capturedChecksums = checksumCaptor.getAllValues();
        List<String> capturedStorages = storageCaptor.getAllValues();
        List<String> capturedStorePaths = storePathCaptor.getAllValues();
        List<Collection<String>> capturedOwners = ownersCaptor.getAllValues();
        List<FileReference> capturedFileReferences = fileRefCaptor.getAllValues();

        // Validate each call
        for (int i = 0; i < requests.size(); i++) {
            FileStorageRequestAggregation request = requests.get(i);

            // Validate each captured argument
            Assertions.assertEquals(request.getGroupIds().iterator().next(),
                                    capturedGroupIds.get(i),
                                    "Group ID mismatch.");
            Assertions.assertEquals(FileRequestType.STORAGE, capturedTypes.get(i), "Request type mismatch.");
            Assertions.

                          assertEquals(request.getMetaInfo().getChecksum(),
                                       capturedChecksums.get(i),
                                       "Checksum mismatch.");
            Assertions.assertEquals(request.getStorage(), capturedStorages.get(i), "Storage mismatch.");
            Assertions.assertEquals(request.getStorageSubDirectory(),
                                    capturedStorePaths.get(i),
                                    "Store path mismatch.");

            // Validate owners
            Collection<String> owners = capturedOwners.get(i);
            Assertions.assertEquals(1, owners.size(), "Owners collection size mismatch.");
            Assertions.assertEquals(request.getOwner(), owners.iterator().next(), "Owner mismatch.");

            // Validate file reference
            Assertions.assertEquals(mockedReferenceResults.get(i).getFileReference(),
                                    capturedFileReferences.get(i),
                                    "File reference mismatch.");
        }
    }

    private void verifyGroupsRequestErrorCall(FileStorageRequestAggregation request, String errorMessage) {
        verifyGroupsRequestErrorCalls(Arrays.asList(request), Arrays.asList(errorMessage));
    }

    private void verifyGroupsRequestErrorCalls(List<FileStorageRequestAggregation> requests,
                                               List<String> errorMessages) {
        // Argument captors for all parameters
        ArgumentCaptor<String> groupIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<FileRequestType> typeCaptor = ArgumentCaptor.forClass(FileRequestType.class);
        ArgumentCaptor<String> checksumCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> storageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> storePathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Collection<String>> ownersCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);

        // Verify the number of calls and capture arguments
        Mockito.verify(requestsGroupService, Mockito.times(requests.size()))
               .requestError(groupIdCaptor.capture(),
                             typeCaptor.capture(),
                             checksumCaptor.capture(),
                             storageCaptor.capture(),
                             storePathCaptor.capture(),
                             ownersCaptor.capture(),
                             errorMessageCaptor.capture());

        // Get all captured values
        List<String> capturedGroupIds = groupIdCaptor.getAllValues();
        List<FileRequestType> capturedTypes = typeCaptor.getAllValues();
        List<String> capturedChecksums = checksumCaptor.getAllValues();
        List<String> capturedStorages = storageCaptor.getAllValues();
        List<String> capturedStorePaths = storePathCaptor.getAllValues();
        List<Collection<String>> capturedOwners = ownersCaptor.getAllValues();
        List<String> capturedErrorMessages = errorMessageCaptor.getAllValues();

        // Validate each call
        for (int i = 0; i < requests.size(); i++) {
            FileStorageRequestAggregation request = requests.get(i);

            // Validate each captured argument
            Assertions.assertEquals(request.getGroupIds().iterator().next(),
                                    capturedGroupIds.get(i),
                                    "Group ID mismatch.");
            Assertions.assertEquals(FileRequestType.STORAGE, capturedTypes.get(i), "Request type mismatch.");
            Assertions.

                          assertEquals(request.getMetaInfo().getChecksum(),
                                       capturedChecksums.get(i),
                                       "Checksum mismatch.");
            Assertions.assertEquals(request.getStorage(), capturedStorages.get(i), "Storage mismatch.");
            Assertions.assertEquals(request.getStorageSubDirectory(),
                                    capturedStorePaths.get(i),
                                    "Store path mismatch.");

            // Validate owners
            Collection<String> owners = capturedOwners.get(i);
            Assertions.assertEquals(1, owners.size(), "Owners collection size mismatch.");
            Assertions.assertEquals(request.getOwner(), owners.iterator().next(), "Owner mismatch.");

            // Validate error message
            Assertions.assertEquals(errorMessages.get(i), capturedErrorMessages.get(i), "Error message mismatch.");
        }
    }

    private void verifyReferenceCalls(List<ReferenceCallRecord> callRecords) throws ModuleException {
        // Verify the number of calls to the service
        ArgumentCaptor<String> ownerCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<FileReferenceMetaInfo> metaInfoCaptor = ArgumentCaptor.forClass(FileReferenceMetaInfo.class);
        ArgumentCaptor<FileLocation> fileLocationCaptor = ArgumentCaptor.forClass(FileLocation.class);
        ArgumentCaptor<Collection<String>> groupIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<String> sessionOwnerCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sessionCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(fileReferenceRequestService, Mockito.times(callRecords.size()))
               .reference(ownerCaptor.capture(),
                          metaInfoCaptor.capture(),
                          fileLocationCaptor.capture(),
                          groupIdsCaptor.capture(),
                          sessionOwnerCaptor.capture(),
                          sessionCaptor.capture());

        // Capture all invocations
        List<String> capturedOwners = ownerCaptor.getAllValues();
        List<FileReferenceMetaInfo> capturedMetaInfos = metaInfoCaptor.getAllValues();
        List<FileLocation> capturedLocations = fileLocationCaptor.getAllValues();
        List<Collection<String>> capturedGroupIds = groupIdsCaptor.getAllValues();
        List<String> capturedSessionOwners = sessionOwnerCaptor.getAllValues();
        List<String> capturedSessions = sessionCaptor.getAllValues();

        // Order the ReferenceCallRecords so the order match the call order (which is not guaranteed)
        Map<FileReferenceMetaInfo, Integer> orderMap = IntStream.range(0, capturedMetaInfos.size())
                                                                .boxed()
                                                                .collect(Collectors.toMap(capturedMetaInfos::get,
                                                                                          index -> index));
        callRecords.sort(Comparator.comparingInt(record -> orderMap.get(record.request().getMetaInfo())));

        // Verify each invocation
        for (int i = 0; i < callRecords.size(); i++) {
            FileStorageRequestAggregation request = callRecords.get(i).request();
            Assertions.assertEquals(request.getOwner(), capturedOwners.get(i));
            Assertions.assertEquals(request.getMetaInfo(), capturedMetaInfos.get(i));
            Assertions.assertEquals(new FileLocation(request.getStorage(), callRecords.get(i).storedUrl(), null),
                                    capturedLocations.get(i));

            Collection<String> groupIds = capturedGroupIds.get(i);
            Assertions.assertEquals(1, groupIds.size());
            Assertions.assertEquals(request.getGroupIds().iterator().next(), groupIds.iterator().next());

            Assertions.assertEquals(request.getSessionOwner(), capturedSessionOwners.get(i));
            Assertions.assertEquals(request.getSession(), capturedSessions.get(i));
        }
    }

    private void verifyReferenceCall(ReferenceCallRecord referenceCallRecord) throws ModuleException {
        verifyReferenceCalls(Arrays.asList(referenceCallRecord));
    }

    private void verifyReferenceNeverCalled() throws ModuleException {
        Mockito.verify(fileReferenceRequestService, Mockito.never())
               .reference(Mockito.anyString(),
                          Mockito.any(FileReferenceMetaInfo.class),
                          Mockito.any(FileLocation.class),
                          Mockito.anyCollection(),
                          Mockito.anyString(),
                          Mockito.anyString());
    }

    private void verifyNotifyRolesNeverCalled() {
        Mockito.verify(notificationClient, Mockito.never())
               .notifyRoles(Mockito.anyString(),
                            Mockito.anyString(),
                            Mockito.any(NotificationLevel.class),
                            Mockito.any(MimeType.class),
                            Mockito.anySet());
    }

    private void verifyIncrementStoredFilesNeverCalled() {
        Mockito.verify(sessionNotifier, Mockito.never())
               .incrementStoredFiles(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
    }

    private void verifyDecrementRunningRequestsNeverCalled() {
        Mockito.verify(sessionNotifier, Mockito.never())
               .decrementRunningRequests(Mockito.anyString(), Mockito.anyString());
    }

    private void verifyRequestSuccessNeverCalled() {
        Mockito.verify(requestsGroupService, Mockito.never())
               .requestSuccess(Mockito.anyString(),
                               Mockito.any(FileRequestType.class),
                               Mockito.anyString(),
                               Mockito.anyString(),
                               Mockito.anyString(),
                               Mockito.anyCollection(),
                               Mockito.any(FileReference.class));
    }

    private void verifyHandleSuccessNeverCalled() throws ModuleException {
        // Verify fileReferenceRequestService.reference() is never called
        verifyReferenceNeverCalled();

        // Verify requestsGroupService.requestSuccess() is never called
        verifyRequestSuccessNeverCalled();

        // Verify sessionNotifier.decrementRunningRequests() is never called
        verifyDecrementRunningRequestsNeverCalled();

        // Verify sessionNotifier.incrementStoredFiles() is never called
        verifyIncrementStoredFilesNeverCalled();

        // Verify notificationClient.notifyRoles() is never called
        verifyNotifyRolesNeverCalled();
    }

    private static String createUrl(int i) {
        return "http://storage.com/sub/dir/file" + i + ".txt";
    }

    private FileStorageRequestAggregation createFileStorageRequestAggregation(int number) {
        FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(RandomChecksumUtils.generateRandomChecksum(),
                                                                   "MD5",
                                                                   "file" + number + ".txt",
                                                                   0L,
                                                                   MimeType.valueOf("text/plain"));

        return new FileStorageRequestAggregation("testOwner",
                                                 metaInfo,
                                                 "http://example.com/file" + number + ".txt",
                                                 "storage",
                                                 Optional.of("sub/dir"),
                                                 "testGroupId",
                                                 "sessionOwner",
                                                 "session1",
                                                 false);
    }

    private FileReferenceResult mockReferenceResponse(FileStorageRequestAggregation request,
                                                      String storedUrl,
                                                      FileReferenceResultStatusEnum resultStatus)
        throws ModuleException {
        return mockReferenceResponses(Collections.singletonList(request),
                                      Collections.singletonList(storedUrl),
                                      Collections.singletonList(resultStatus)).get(0);
    }

    private List<FileReferenceResult> mockReferenceResponses(List<FileStorageRequestAggregation> requests,
                                                             List<String> storedUrls,
                                                             List<FileReferenceResultStatusEnum> resultStatuses)
        throws ModuleException {

        List<FileReferenceResult> mockedResults = new ArrayList<>();

        // Create mocked results
        for (int i = 0; i < requests.size(); i++) {
            FileStorageRequestAggregation request = requests.get(i);
            request.getMetaInfo().setFileSize(FILE_SIZE);
            mockedResults.add(FileReferenceResult.build(new FileReference(request.getOwner(),
                                                                          request.getMetaInfo(),
                                                                          new FileLocation(request.getStorage(),
                                                                                           storedUrls.get(i),
                                                                                           null)),
                                                        resultStatuses.get(i)));
        }

        // Iterate of the mocked result when answering mocked method call
        Mockito.doAnswer(AdditionalAnswers.returnsElementsOf(mockedResults))
               .when(fileReferenceRequestService)
               .reference(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        return mockedResults;
    }

    private static record ReferenceCallRecord(FileStorageRequestAggregation request,
                                              String storedUrl) {

    }
}
