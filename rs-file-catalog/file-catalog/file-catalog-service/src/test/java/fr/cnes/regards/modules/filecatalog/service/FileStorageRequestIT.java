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
package fr.cnes.regards.modules.filecatalog.service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.fileaccess.amqp.input.FileStorageRequestReadyToProcessEvent;
import fr.cnes.regards.modules.fileaccess.dto.StorageRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.input.FileStorageRequestReadyToProcessDto;
import fr.cnes.regards.modules.filecatalog.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.filecatalog.dao.IFileReferenceWithOwnersRepository;
import fr.cnes.regards.modules.filecatalog.dao.IFileStorageRequestAggregationRepository;
import fr.cnes.regards.modules.filecatalog.domain.FileLocation;
import fr.cnes.regards.modules.filecatalog.domain.FileReference;
import fr.cnes.regards.modules.filecatalog.domain.FileReferenceMetaInfo;
import fr.cnes.regards.modules.filecatalog.domain.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.filecatalog.service.scheduler.FileStorageRequestCheckScheduler;
import fr.cnes.regards.modules.filecatalog.service.scheduler.FileStorageRequestCompleteScheduler;
import fr.cnes.regards.modules.filecatalog.service.scheduler.FileStorageRequestDispatchScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * This test verify the whole lifecycle of {@link FileStorageRequestAggregation} through the 3 schedulers
 * {@link FileStorageRequestCheckScheduler}, {@link FileStorageRequestDispatchScheduler} and {@link FileStorageRequestCompleteScheduler}
 *
 * @author Thibaud Michaudel
 **/
@ActiveProfiles({ "noscheduler", "nojobs", "test" })
@SpringBootTest
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=file_catalog_storage_requests_test" })
public class FileStorageRequestIT extends AbstractMultitenantServiceIT {

    @Autowired
    private FileStorageRequestService fileStorageRequestService;

    private FileStorageRequestCheckScheduler fileStorageRequestCheckScheduler;

    private FileStorageRequestCompleteScheduler fileStorageRequestCompleteScheduler;

    private FileStorageRequestDispatchScheduler fileStorageRequestDispatchScheduler;

    @Autowired
    private IFileStorageRequestAggregationRepository fileStorageRequestAggregationRepository;

    @Autowired
    private IFileReferenceRepository fileReferenceRepository;

    @Autowired
    private IFileReferenceWithOwnersRepository fileReferenceWithOwnersRepository;

    @Autowired
    private IPublisher publisher;

    @Before
    public void init() {
        // Clean
        fileStorageRequestAggregationRepository.deleteAll();
        fileReferenceRepository.deleteAll();

        // Mock
        Mockito.clearInvocations(publisher);

        fileStorageRequestCheckScheduler = new FileStorageRequestCheckScheduler(Mockito.mock(ILockingTaskExecutors.class),
                                                                                Mockito.mock(IRuntimeTenantResolver.class),
                                                                                Mockito.mock(ITenantResolver.class),
                                                                                fileStorageRequestService);

        fileStorageRequestCompleteScheduler = new FileStorageRequestCompleteScheduler(Mockito.mock(ILockingTaskExecutors.class),
                                                                                      Mockito.mock(
                                                                                          IRuntimeTenantResolver.class),
                                                                                      Mockito.mock(ITenantResolver.class),
                                                                                      fileStorageRequestService);

        fileStorageRequestDispatchScheduler = new FileStorageRequestDispatchScheduler(fileStorageRequestService,
                                                                                      Mockito.mock(RequestStatusService.class),
                                                                                      Mockito.mock(ILockingTaskExecutors.class),
                                                                                      Mockito.mock(
                                                                                          IRuntimeTenantResolver.class),
                                                                                      Mockito.mock(ITenantResolver.class),
                                                                                      fileStorageRequestAggregationRepository);
    }

    @Test
    public void test_request_granted_no_ref() {
        // Given
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 1, StorageRequestStatus.GRANTED));

        // Verify creation
        Optional<FileStorageRequestAggregation> oFoundRequest = fileStorageRequestAggregationRepository.findById(
            request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.GRANTED,
                                oFoundRequest.get().getStatus(),
                                "The request should be in GRANTED status");

        // Test that the FileStorageRequestCompleteScheduler has no effect on GRANTED requests

        // When
        fileStorageRequestCompleteScheduler.handleCompleteRequests();

        // Then
        oFoundRequest = fileStorageRequestAggregationRepository.findById(request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.GRANTED,
                                oFoundRequest.get().getStatus(),
                                "The request should still be in GRANTED status");

        // Test that the FileStorageRequestDispatchScheduler has no effect on GRANTED requests

        // When
        fileStorageRequestDispatchScheduler.handleFileStorageRequests();

        // Then
        oFoundRequest = fileStorageRequestAggregationRepository.findById(request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.GRANTED,
                                oFoundRequest.get().getStatus(),
                                "The request should still be in GRANTED status");

        // Test that the FileStorageRequestCheckScheduler delete process has no effect on GRANTED requests

        // When
        fileStorageRequestCheckScheduler.deleteRequests();

        // Then
        oFoundRequest = fileStorageRequestAggregationRepository.findById(request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.GRANTED,
                                oFoundRequest.get().getStatus(),
                                "The request should still be in GRANTED status");

        // Test that the FileStorageRequestCheckScheduler update the GRANTED request

        // When
        fileStorageRequestCheckScheduler.handleFileStorageCheckRequests();

        // Then
        oFoundRequest = fileStorageRequestAggregationRepository.findById(request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.TO_HANDLE,
                                oFoundRequest.get().getStatus(),
                                "The request should be in TO_HANDLE status");
    }

    @Test
    public void test_request_granted_with_ref() {
        // Given
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 1, StorageRequestStatus.GRANTED));

        FileReference fileRef1 = fileReferenceRepository.save(createNewFileReference(1, 1));

        // Test that the FileStorageRequestCheckScheduler delete the GRANTED request and add an owner to the reference

        // When
        fileStorageRequestCheckScheduler.handleFileStorageCheckRequests();

        // Then
        Optional<FileStorageRequestAggregation> oFoundRequest = fileStorageRequestAggregationRepository.findById(
            request1.getId());
        Assertions.assertTrue(oFoundRequest.isEmpty(), "The request should have been deleted");

        FileReference foundRef = fileReferenceWithOwnersRepository.findOneById(fileRef1.getId());

        Collection<String> owners = foundRef.getLazzyOwners();

        Assertions.assertEquals(2, owners.size(), "There should be 2 owners");
        Assertions.assertTrue(owners.contains("owner0"), "The existing owner should still be there");
        Assertions.assertTrue(owners.contains("owner1"), "The new owner should have been added");

    }

    @Test
    public void test_multiple_request_granted() {
        // Given
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 1, StorageRequestStatus.GRANTED));

        FileStorageRequestAggregation request2 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 2, StorageRequestStatus.GRANTED));

        FileStorageRequestAggregation request3 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(2, 1, 3, StorageRequestStatus.GRANTED));

        // Test that the FileStorageRequestCheckScheduler update the GRANTED request

        // When
        fileStorageRequestCheckScheduler.handleFileStorageCheckRequests();

        // Then
        Optional<FileStorageRequestAggregation> oFoundRequest = fileStorageRequestAggregationRepository.findById(
            request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request 1 should be present");
        Assertions.assertEquals(StorageRequestStatus.TO_HANDLE,
                                oFoundRequest.get().getStatus(),
                                "The request 1 should be in TO_HANDLE status");

        oFoundRequest = fileStorageRequestAggregationRepository.findById(request2.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request 2 should be present");
        Assertions.assertEquals(StorageRequestStatus.TO_HANDLE,
                                oFoundRequest.get().getStatus(),
                                "The request 2 should be in TO_HANDLE status");

        oFoundRequest = fileStorageRequestAggregationRepository.findById(request3.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request 3 should be present");
        Assertions.assertEquals(StorageRequestStatus.TO_HANDLE,
                                oFoundRequest.get().getStatus(),
                                "The request 3 should be in TO_HANDLE status");
    }

    @Test
    public void test_request_to_handle_not_handled() {
        // Given
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 1, StorageRequestStatus.TO_HANDLE));

        // Verify creation
        Optional<FileStorageRequestAggregation> oFoundRequest = fileStorageRequestAggregationRepository.findById(
            request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.TO_HANDLE,
                                oFoundRequest.get().getStatus(),
                                "The request should be in TO_HANDLE status");

        // Test that the FileStorageRequestCompleteScheduler has no effect on TO_HANDLE requests

        // When
        fileStorageRequestCompleteScheduler.handleCompleteRequests();

        // Then
        oFoundRequest = fileStorageRequestAggregationRepository.findById(request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.TO_HANDLE,
                                oFoundRequest.get().getStatus(),
                                "The request should still be in TO_HANDLE status");

        // Test that the FileStorageRequestCheckScheduler check process has no effect on TO_HANDLE requests

        // When
        fileStorageRequestCheckScheduler.handleFileStorageCheckRequests();

        // Then
        oFoundRequest = fileStorageRequestAggregationRepository.findById(request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.TO_HANDLE,
                                oFoundRequest.get().getStatus(),
                                "The request should still be in TO_HANDLE status");

        // Test that the FileStorageRequestCheckScheduler delete process has no effect on TO_HANDLE requests

        // When
        fileStorageRequestCheckScheduler.deleteRequests();

        // Then
        oFoundRequest = fileStorageRequestAggregationRepository.findById(request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.TO_HANDLE,
                                oFoundRequest.get().getStatus(),
                                "The request should still be in TO_HANDLE status");

        // Test that the FileStorageRequestCheckScheduler update the TO_HANDLE request and send an event

        // When
        fileStorageRequestDispatchScheduler.handleFileStorageRequests();

        // Then
        oFoundRequest = fileStorageRequestAggregationRepository.findById(request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request should be in HANDLED status");

        ArgumentCaptor<List<FileStorageRequestReadyToProcessEvent>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher).publish(argumentCaptor.capture());
        List<List<FileStorageRequestReadyToProcessEvent>> allValues = argumentCaptor.getAllValues();
        Assertions.assertEquals(1, allValues.size(), "There should be only one batch of events");
        List<FileStorageRequestReadyToProcessEvent> events = allValues.get(0);
        Assertions.assertEquals(1, events.size(), "There should be only one event");
        Assertions.assertEquals("1aaaabcdefabcdefabcdefabcdefabcd",
                                events.get(0).getChecksum(),
                                "The checksum doesn't match the expected one");
    }

    @Test
    public void test_request_to_handle_already_handled() {
        // Given
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 1, StorageRequestStatus.TO_HANDLE));

        fileStorageRequestAggregationRepository.save(createNewFileStorageRequest(1,
                                                                                 0,
                                                                                 1,
                                                                                 StorageRequestStatus.HANDLED));

        // Verify creation
        Optional<FileStorageRequestAggregation> oFoundRequest = fileStorageRequestAggregationRepository.findById(
            request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.TO_HANDLE,
                                oFoundRequest.get().getStatus(),
                                "The request should be in TO_HANDLE status");

        // Test that the FileStorageRequestCheckScheduler update the TO_HANDLE request and no event was sent

        // When
        fileStorageRequestDispatchScheduler.handleFileStorageRequests();

        // Then
        oFoundRequest = fileStorageRequestAggregationRepository.findById(request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request should be in HANDLED status");
        Mockito.verify(publisher, Mockito.never()).publish(Mockito.anyList());
    }

    @Test
    public void test_multiple_request_to_handle_not_handled() {
        // Given
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 1, StorageRequestStatus.TO_HANDLE));
        FileStorageRequestAggregation request2 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 2, 1, StorageRequestStatus.TO_HANDLE));
        FileStorageRequestAggregation request3 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 3, 1, StorageRequestStatus.TO_HANDLE));

        // Test that the FileStorageRequestCheckScheduler update the 3 TO_HANDLE request and send ONLY ONE event

        // When
        fileStorageRequestDispatchScheduler.handleFileStorageRequests();

        // Then
        Optional<FileStorageRequestAggregation> oFoundRequest = fileStorageRequestAggregationRepository.findById(
            request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request1 should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request1 should be in HANDLED status");

        oFoundRequest = fileStorageRequestAggregationRepository.findById(request2.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request2 should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request2 should be in HANDLED status");

        oFoundRequest = fileStorageRequestAggregationRepository.findById(request3.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request3 should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request3 should be in HANDLED status");

        ArgumentCaptor<List<FileStorageRequestReadyToProcessEvent>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher).publish(argumentCaptor.capture());
        List<List<FileStorageRequestReadyToProcessEvent>> allValues = argumentCaptor.getAllValues();
        Assertions.assertEquals(1, allValues.size(), "There should be only one batch of events");
        List<FileStorageRequestReadyToProcessEvent> events = allValues.get(0);
        Assertions.assertEquals(1, events.size(), "There should be only one event");
        Assertions.assertEquals("1aaaabcdefabcdefabcdefabcdefabcd",
                                events.get(0).getChecksum(),
                                "The checksum doesn't match the expected one");
    }

    @Test
    public void test_multiple_request_to_handle_already_handled() {
        // Given
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 1, StorageRequestStatus.TO_HANDLE));
        FileStorageRequestAggregation request2 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 2, 1, StorageRequestStatus.TO_HANDLE));
        FileStorageRequestAggregation request3 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 3, 1, StorageRequestStatus.TO_HANDLE));

        fileStorageRequestAggregationRepository.save(createNewFileStorageRequest(1,
                                                                                 0,
                                                                                 1,
                                                                                 StorageRequestStatus.HANDLED));

        // Test that the FileStorageRequestCheckScheduler update the 3 TO_HANDLE request and send ONLY ONE event

        // When
        fileStorageRequestDispatchScheduler.handleFileStorageRequests();

        // Then
        Optional<FileStorageRequestAggregation> oFoundRequest = fileStorageRequestAggregationRepository.findById(
            request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request1 should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request1 should be in HANDLED status");

        oFoundRequest = fileStorageRequestAggregationRepository.findById(request2.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request2 should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request2 should be in HANDLED status");

        oFoundRequest = fileStorageRequestAggregationRepository.findById(request3.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request3 should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request3 should be in HANDLED status");

        Mockito.verify(publisher, Mockito.never()).publish(Mockito.anyList());
    }

    @Test
    public void test_different_request_to_handle() {
        // Given
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 1, StorageRequestStatus.TO_HANDLE));
        FileStorageRequestAggregation request2 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 2, 1, StorageRequestStatus.TO_HANDLE));
        FileStorageRequestAggregation request3 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 2, 2, StorageRequestStatus.TO_HANDLE));
        FileStorageRequestAggregation request4 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(2, 1, 1, StorageRequestStatus.TO_HANDLE));
        FileStorageRequestAggregation request5 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 3, StorageRequestStatus.TO_HANDLE));

        fileStorageRequestAggregationRepository.save(createNewFileStorageRequest(1,
                                                                                 0,
                                                                                 3,
                                                                                 StorageRequestStatus.HANDLED));

        // Test that the FileStorageRequestCheckScheduler update the 3 TO_HANDLE request and send ONLY ONE event

        // When
        fileStorageRequestDispatchScheduler.handleFileStorageRequests();

        // Then
        Optional<FileStorageRequestAggregation> oFoundRequest = fileStorageRequestAggregationRepository.findById(
            request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request1 should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request1 should be in HANDLED status");

        oFoundRequest = fileStorageRequestAggregationRepository.findById(request2.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request2 should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request2 should be in HANDLED status");

        oFoundRequest = fileStorageRequestAggregationRepository.findById(request3.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request3 should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request3 should be in HANDLED status");

        oFoundRequest = fileStorageRequestAggregationRepository.findById(request4.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request4 should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request4 should be in HANDLED status");

        oFoundRequest = fileStorageRequestAggregationRepository.findById(request5.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request5 should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request5 should be in HANDLED status");

        ArgumentCaptor<List<FileStorageRequestReadyToProcessEvent>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher, Mockito.times(2)).publish(argumentCaptor.capture());
        List<List<FileStorageRequestReadyToProcessEvent>> allValues = argumentCaptor.getAllValues();
        Assertions.assertEquals(2, allValues.size(), "There should be two batches of events as there are two storages");
        List<FileStorageRequestReadyToProcessEvent> events = allValues.get(0);
        Assertions.assertEquals(2,
                                events.size(),
                                "There should be two events in the first batch as there are two new files");
        Assertions.assertTrue(events.stream()
                                    .map(FileStorageRequestReadyToProcessDto::getChecksum)
                                    .anyMatch(checksum -> checksum.equals("1aaaabcdefabcdefabcdefabcdefabcd")),
                              "The event is not the expected one");
        Assertions.assertTrue(events.stream()
                                    .map(FileStorageRequestReadyToProcessDto::getChecksum)
                                    .anyMatch(checksum -> checksum.equals("2aaaabcdefabcdefabcdefabcdefabcd")),
                              "The event is not the expected one");
        events = allValues.get(1);
        Assertions.assertEquals(1, events.size(), "There should be one event in the second batch");
        Assertions.assertEquals("1aaaabcdefabcdefabcdefabcdefabcd",
                                events.get(0).getChecksum(),
                                "The first checksum doesn't match the expected one");
    }

    @Test
    public void test_request_handled_no_ref() {
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 1, StorageRequestStatus.HANDLED));

        // Verify creation
        Optional<FileStorageRequestAggregation> oFoundRequest = fileStorageRequestAggregationRepository.findById(
            request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request should be in HANDLED status");

        // Test that the FileStorageRequestDispatchScheduler has no effect on HANDLED requests

        // When
        fileStorageRequestDispatchScheduler.handleFileStorageRequests();

        // Then
        oFoundRequest = fileStorageRequestAggregationRepository.findById(request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request should still be in HANDLED status");

        // Test that the FileStorageRequestCheckScheduler check process has no effect on HANDLED requests

        // When
        fileStorageRequestCheckScheduler.handleFileStorageCheckRequests();

        // Then
        oFoundRequest = fileStorageRequestAggregationRepository.findById(request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request should still be in HANDLED status");

        // Test that the FileStorageRequestCheckScheduler delete process has no effect on HANDLED requests

        // When
        fileStorageRequestCheckScheduler.deleteRequests();

        // Then
        oFoundRequest = fileStorageRequestAggregationRepository.findById(request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request should still be in HANDLED status");

        // Test that nothing happens to HANDLED requests if there is no file reference

        // When
        fileStorageRequestCompleteScheduler.handleCompleteRequests();

        // Then
        oFoundRequest = fileStorageRequestAggregationRepository.findById(request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request should still be in HANDLED status as no reference was found");
    }

    @Test
    public void test_request_handled_with_ref() {
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 1, StorageRequestStatus.HANDLED));

        FileReference fileRef1 = fileReferenceRepository.save(createNewFileReference(1, 1));

        // Test that the request is set to TO_DELETE and the owner is added to the existing reference

        // When
        fileStorageRequestCompleteScheduler.handleCompleteRequests();

        // Then
        Optional<FileStorageRequestAggregation> oFoundRequest = fileStorageRequestAggregationRepository.findById(
            request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request should be present");
        Assertions.assertEquals(StorageRequestStatus.TO_DELETE,
                                oFoundRequest.get().getStatus(),
                                "The request should be in TO_DELETE status");

        FileReference foundRef = fileReferenceWithOwnersRepository.findOneById(fileRef1.getId());

        Collection<String> owners = foundRef.getLazzyOwners();

        Assertions.assertEquals(2, owners.size(), "There should be 2 owners");
        Assertions.assertTrue(owners.contains("owner0"), "The existing owner should still be there");
        Assertions.assertTrue(owners.contains("owner1"), "The new owner should have been added");
    }

    @Test
    public void test_multiple_request_handled() {
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 1, StorageRequestStatus.HANDLED));

        FileStorageRequestAggregation request2 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 2, 1, StorageRequestStatus.HANDLED));

        FileStorageRequestAggregation request3 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 2, StorageRequestStatus.HANDLED));

        FileStorageRequestAggregation request4 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(2, 1, 1, StorageRequestStatus.HANDLED));

        FileReference fileRef1 = fileReferenceRepository.save(createNewFileReference(1, 1));

        FileReference fileRef2 = fileReferenceRepository.save(createNewFileReference(1, 2));

        // Test that the request is set to TO_DELETE and the owner is added to the existing reference

        // When
        fileStorageRequestCompleteScheduler.handleCompleteRequests();

        // Then
        Optional<FileStorageRequestAggregation> oFoundRequest = fileStorageRequestAggregationRepository.findById(
            request1.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request1 should be present");
        Assertions.assertEquals(StorageRequestStatus.TO_DELETE,
                                oFoundRequest.get().getStatus(),
                                "The request1 should be in TO_DELETE status");

        oFoundRequest = fileStorageRequestAggregationRepository.findById(request2.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request2 should be present");
        Assertions.assertEquals(StorageRequestStatus.TO_DELETE,
                                oFoundRequest.get().getStatus(),
                                "The request2 should be in TO_DELETE status");

        oFoundRequest = fileStorageRequestAggregationRepository.findById(request3.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request3 should be present");
        Assertions.assertEquals(StorageRequestStatus.TO_DELETE,
                                oFoundRequest.get().getStatus(),
                                "The request3 should be in TO_DELETE status");

        oFoundRequest = fileStorageRequestAggregationRepository.findById(request4.getId());
        Assertions.assertTrue(oFoundRequest.isPresent(), "The request4 should be present");
        Assertions.assertEquals(StorageRequestStatus.HANDLED,
                                oFoundRequest.get().getStatus(),
                                "The request4 should still be in HANDLED status");

        FileReference foundRef = fileReferenceWithOwnersRepository.findOneById(fileRef1.getId());
        Collection<String> owners = foundRef.getLazzyOwners();
        Assertions.assertEquals(3, owners.size(), "There should be 2 owners");
        Assertions.assertTrue(owners.contains("owner0"), "The existing owner should still be there");
        Assertions.assertTrue(owners.contains("owner1"), "The new owner1 should have been added");
        Assertions.assertTrue(owners.contains("owner2"), "The new owner2 should have been added");

        foundRef = fileReferenceWithOwnersRepository.findOneById(fileRef2.getId());
        owners = foundRef.getLazzyOwners();
        Assertions.assertEquals(2, owners.size(), "There should be 3 owners");
        Assertions.assertTrue(owners.contains("owner0"), "The existing owner should still be there");
        Assertions.assertTrue(owners.contains("owner1"), "The new owner1 should have been added");
    }

    @Test
    public void test_multiple_request_to_delete() {
        FileStorageRequestAggregation request1 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 1, StorageRequestStatus.TO_DELETE));

        FileStorageRequestAggregation request2 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 2, 1, StorageRequestStatus.TO_DELETE));

        FileStorageRequestAggregation request3 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(1, 1, 2, StorageRequestStatus.TO_DELETE));

        FileStorageRequestAggregation request4 = fileStorageRequestAggregationRepository.save(
            createNewFileStorageRequest(2, 1, 1, StorageRequestStatus.TO_DELETE));

        // Test that the requests are deleted

        // When
        fileStorageRequestCheckScheduler.deleteRequests();

        // Then
        Optional<FileStorageRequestAggregation> oFoundRequest = fileStorageRequestAggregationRepository.findById(
            request1.getId());
        Assertions.assertTrue(oFoundRequest.isEmpty(), "The request1 should have been deleted");
        oFoundRequest = fileStorageRequestAggregationRepository.findById(request2.getId());

        Assertions.assertTrue(oFoundRequest.isEmpty(), "The request2 should have been deleted");
        oFoundRequest = fileStorageRequestAggregationRepository.findById(request3.getId());

        Assertions.assertTrue(oFoundRequest.isEmpty(), "The request3 should have been deleted");

        oFoundRequest = fileStorageRequestAggregationRepository.findById(request4.getId());
        Assertions.assertTrue(oFoundRequest.isEmpty(), "The request4 should have been deleted");
    }

    private static FileStorageRequestAggregation createNewFileStorageRequest(int storage,
                                                                             int owner,
                                                                             int file,
                                                                             StorageRequestStatus status) {
        FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(file + "aaaabcdefabcdefabcdefabcdefabcd",
                                                                   "MD5",
                                                                   "file" + file,
                                                                   1000L,
                                                                   MimeType.valueOf("text/plain"));
        FileStorageRequestAggregation request = new FileStorageRequestAggregation("owner" + owner,
                                                                                  metaInfo,
                                                                                  "https://originurl.com/file" + file,
                                                                                  "storage" + storage,
                                                                                  Optional.empty(),
                                                                                  "groupId",
                                                                                  "sessionOwner",
                                                                                  "session");
        request.setStatus(status);
        return request;
    }

    private static FileReference createNewFileReference(int storage, int file) {
        FileReferenceMetaInfo metaInfo = new FileReferenceMetaInfo(file + "aaaabcdefabcdefabcdefabcdefabcd",
                                                                   "MD5",
                                                                   "file" + file,
                                                                   1000L,
                                                                   MimeType.valueOf("text/plain"));
        FileLocation fileLoc = new FileLocation("storage" + storage, "https://originurl.com/file" + file, false);

        FileReference fileRef = new FileReference("owner0", metaInfo, fileLoc);

        return fileRef;
    }

}
