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
package fr.cnes.regards.modules.storage.service.file.handler;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesRetryRequestEvent;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEventType;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceRequestArgs;
import fr.cnes.regards.modules.storage.service.session.SessionNotifierPropertyEnum;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum.DEC;
import static fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum.INC;
import static fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceConstants.SESSION1;
import static fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceConstants.SESSION1_OWNER;
import static fr.cnes.regards.modules.storage.service.session.SessionNotifierPropertyEnum.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests" },
                    locations = { "classpath:application-test.properties" })
public class StoreFileEventIT extends AbstractStorageIT {

    public static final PageRequest PAGE_1ST_1000 = PageRequest.of(0, 1_000);

    @Autowired
    private FilesStorageRequestEventHandler storeHandler;

    @Autowired
    private FilesRetryRequestEventHandler retryHandler;

    @Before
    public void initialize() throws ModuleException {
        Mockito.clearInvocations(publisher);
        super.init();
    }

    @Test(expected = IllegalArgumentException.class)
    @Requirement("REGARDS_DSL_STO_AIP_080")
    @Purpose("Check that a storage request without checksum is denied")
    public void store_file_no_checksum() {
        // Create a new bus message File reference request
        newStorageRequestEvent(FileReferenceRequestArgs.builder()
                                                       .fileName("file.name")
                                                       .storage(ONLINE_CONF_LABEL)
                                                       .owner("owner")
                                                       .build());
    }

    /**
     * Test request to reference a file already stored.
     * The file is not stored by the service as the origin storage and the destination storage are identical
     */
    @Test
    public void store_file_already_stored() {
        // GIVEN
        final String checksum = RandomChecksumUtils.generateRandomChecksum();
        // Create a new bus message File reference request
        final FileReferenceRequestArgs args = FileReferenceRequestArgs.builder()
                                                                      .fileName("file.name")
                                                                      .checksum(checksum)
                                                                      .storage(ONLINE_CONF_LABEL)
                                                                      .owner("new-owner")
                                                                      .build();
        final FilesStorageRequestEvent item = newStorageRequestEvent(args);
        storeHandler.handleBatch(List.of(item));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Check file is not referenced yet
        FileReference fileRef = referenceService.search(ONLINE_CONF_LABEL, checksum).orElse(null);
        assumeThat(fileRef).as("File should not be referenced").isNull();

        // Check a file reference request is created
        assumeThat(storageRequestService.search(ONLINE_CONF_LABEL, checksum)).as("File request should be created")
                                                                             .hasSize(1);
        // Now check for event published
        Mockito.verify(this.publisher, Mockito.times(0)).publish(any(FileReferenceEvent.class));

        // Simulate job schedule
        // WHEN
        Collection<JobInfo> jobs = storageRequestService.scheduleJobs(FileRequestStatus.TO_DO,
                                                                      List.of(ONLINE_CONF_LABEL),
                                                                      List.of(args.getOwner()));
        assertThat(jobs).hasSize(1);
        runAndWaitJob(jobs);

        // THEN
        // File is referenced
        fileRef = referenceService.search(ONLINE_CONF_LABEL, checksum).orElse(null);
        assertThat(fileRef).as("File should be referenced").isNotNull();
        assertThat(fileRef.isReferenced()).as("File should in stored state").isFalse();
        assertThat(fileRef.getLocation().isPendingActionRemaining()).as("File should in stored state").isFalse();

        // Request has been processed and is now deleted.
        assertThat(storageRequestService.search(ONLINE_CONF_LABEL, checksum)).as("File request should be deleted")
                                                                             .isEmpty();

        // check for published event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(1)).publish(any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published",
                            FileReferenceEventType.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());

        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 4, stepEventList.size());
        checkStepEvent(stepEventList.get(0), STORE_REQUESTS, INC);
        checkStepEvent(stepEventList.get(1), REQUESTS_RUNNING, INC);
        checkStepEvent(stepEventList.get(2), REQUESTS_RUNNING, DEC);
        checkStepEvent(stepEventList.get(3), STORED_FILES, INC);
    }

    @Test
    public void store_file_nearline_with_pending_actions() {
        // GIVEN
        final String checksum = RandomChecksumUtils.generateRandomChecksum();
        // Create a new bus message File reference request
        final FileReferenceRequestArgs args = FileReferenceRequestArgs.builder()
                                                                      .fileName("pending.file.name")
                                                                      .checksum(checksum)
                                                                      .storage(NEARLINE_CONF_LABEL)
                                                                      .owner("new-owner")
                                                                      .build();
        final FilesStorageRequestEvent item = newStorageRequestEvent(args);
        storeHandler.handleBatch(List.of(item));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // WHEN
        final Collection<JobInfo> jobs = storageRequestService.scheduleJobs(FileRequestStatus.TO_DO,
                                                                            List.of(NEARLINE_CONF_LABEL),
                                                                            List.of(args.getOwner()));
        runAndWaitJob(jobs);

        // THEN
        final FileReference fileRef = referenceService.search(NEARLINE_CONF_LABEL, checksum).orElse(null);
        Assert.assertNotNull("File should be referenced", fileRef);
        Assert.assertFalse("File should in stored state", fileRef.isReferenced());
        Assert.assertTrue("File should be referenced with pending action remaining",
                          fileRef.getLocation().isPendingActionRemaining());
    }

    @Test
    public void store_file_while_previous_request_exists() {
        String owner = "new-owner";
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        // Create a new bus message File reference request
        String algorithm = "MD5";
        String fileName = "file.name";

        String groupId = UUID.randomUUID().toString();

        FileRequestStatus oldRequestStatus = FileRequestStatus.TO_DO;
        FileStorageRequestAggregation request = storageRequestService.createNewFileStorageRequest(Collections.singleton(
                                                                                                      owner),
                                                                                                  new FileReferenceMetaInfo(
                                                                                                      checksum,
                                                                                                      algorithm,
                                                                                                      fileName,
                                                                                                      null,
                                                                                                      MediaType.APPLICATION_OCTET_STREAM).withType(
                                                                                                      DataType.RAWDATA.toString()),
                                                                                                  ORIGIN_URL,
                                                                                                  ONLINE_CONF_LABEL,
                                                                                                  Optional.empty(),
                                                                                                  groupId,
                                                                                                  Optional.of("File "
                                                                                                              + fileName
                                                                                                              + " (checksum: "
                                                                                                              + checksum
                                                                                                              + ") not handled by storage job. Storage job failed cause : For input string: \"Killed\""),
                                                                                                  Optional.of(
                                                                                                      oldRequestStatus),
                                                                                                  SESSION1_OWNER,
                                                                                                  SESSION1);

        final FileReferenceRequestArgs args = FileReferenceRequestArgs.builder()
                                                                      .fileName(fileName)
                                                                      .checksum(checksum)
                                                                      .storage(ONLINE_CONF_LABEL)
                                                                      .owner(owner)
                                                                      .build();
        // 2 event in distinct group.
        final FilesStorageRequestEvent storageItem1 = newStorageRequestEvent(args);
        final FilesStorageRequestEvent storageItem2 = newStorageRequestEvent(args);

        storeHandler.handleBatch(List.of(storageItem1, storageItem2));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Check file is not referenced yet
        Assert.assertFalse("File should not be referenced yet",
                           referenceService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        // Check a file reference request is created
        Collection<FileStorageRequestAggregation> fileStorageRequests = storageRequestService.search(ONLINE_CONF_LABEL,
                                                                                                     checksum);
        Assert.assertEquals("New storage request in DELAYED status should have been created",
                            3,
                            fileStorageRequests.size());
        List<FileStorageRequestAggregation> newRequests = fileStorageRequests.stream()
                                                                             .filter(r -> !r.getId()
                                                                                            .equals(request.getId()))
                                                                             .toList();
        Assert.assertTrue("New request should be in state " + FileRequestStatus.DELAYED,
                          newRequests.stream().allMatch(r -> r.getStatus() == FileRequestStatus.DELAYED));
        // Now check for event published
        Mockito.verify(this.publisher, Mockito.times(0)).publish(any(FileReferenceEvent.class));

        // WHEN
        // Simulate job schedule -> Run first request
        Collection<JobInfo> jobs = storageRequestService.scheduleJobs(FileRequestStatus.TO_DO,
                                                                      List.of(ONLINE_CONF_LABEL),
                                                                      List.of(owner));
        runAndWaitJob(jobs);

        // THEN
        FileReference fileRef = referenceService.search(ONLINE_CONF_LABEL, checksum).orElse(null);
        Assert.assertNotNull("File should be referenced", fileRef);
        Assert.assertFalse("File should in stored state", fileRef.isReferenced());
        // Request should still be delayed
        fileStorageRequests = storageRequestService.search(ONLINE_CONF_LABEL, checksum);
        Assert.assertEquals("There should be two delayed request remaining", 2L, fileStorageRequests.size());
        Assert.assertTrue("New request should be in state " + FileRequestStatus.DELAYED,
                          fileStorageRequests.stream().allMatch(r -> r.getStatus() == FileRequestStatus.DELAYED));

        // As no request is still running, the two requests will resume but the file they aim to store is now already
        // stored. So no more request will be processed
        statusService.checkDelayedStorageRequests(storageRequestService);
        fileStorageRequests = storageRequestService.search(ONLINE_CONF_LABEL, checksum);
        Assert.assertEquals("There should be no more requests", 0L, fileStorageRequests.size());
    }

    /**
     * Test request to reference a file already stored.
     * The file is not stored by the service as the origin storage and the destination storage are identical
     */
    // TODO random
    @Ignore
    @Test
    public void store_same_file() {
        String owner1 = "new-owner";
        String owner2 = "new-owner-23";
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        // Create a new bus message File reference request
        final FileReferenceRequestArgs args = FileReferenceRequestArgs.builder()
                                                                      .fileName("file.name")
                                                                      .checksum(checksum)
                                                                      .storage(ONLINE_CONF_LABEL)
                                                                      .owner(owner1)
                                                                      .build();
        final FilesStorageRequestEvent item1 = newStorageRequestEvent(args);
        final FilesStorageRequestEvent item2 = newStorageRequestEvent(args.withOwner(owner2));
        storeHandler.handleBatch(List.of(item1, item2));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Check file is not referenced yet
        FileReference fileReference = referenceService.search(ONLINE_CONF_LABEL, checksum).orElse(null);
        assumeThat(fileReference).as("File should not be referenced yet").isNull();

        // Check all file reference request are created one is in a TO_DO status the other is DELAYED
        Collection<FileStorageRequestAggregation> requests = storageRequestService.search(ONLINE_CONF_LABEL, checksum);
        assumeThat(requests).as("there should be two store requests").hasSize(2);

        final Set<FileRequestStatus> statuses = requests.stream()
                                                        .map(FileStorageRequestAggregation::getStatus)
                                                        .collect(Collectors.toSet());
        assumeThat(statuses).as("One request is in a TO_DO status the other is DELAYED")
                            .hasSize(2)
                            .containsExactlyInAnyOrder(FileRequestStatus.TO_DO, FileRequestStatus.DELAYED);

        // Now check for event published
        Mockito.verify(this.publisher, Mockito.times(0)).publish(any(FileReferenceEvent.class));
        // Awaitility.await().pollDelay(Duration.ofSeconds(1)).until(Boolean.TRUE::booleanValue);
        // Simulate job schedule for the first storage request
        Collection<JobInfo> jobs = storageRequestService.scheduleJobs(FileRequestStatus.TO_DO,
                                                                      List.of(ONLINE_CONF_LABEL),
                                                                      List.of(owner1));
        assertThat(jobs).hasSize(1);
        runAndWaitJob(jobs);
        Awaitility.await().pollDelay(Duration.ofSeconds(1)).until(Boolean.TRUE::booleanValue);

        requests = storageRequestService.search(ONLINE_CONF_LABEL, checksum);
        // The 1st request should be done and deleted
        // the 2nd request should be delayed
        Assert.assertEquals("there should be one store request", 1, requests.size());
        FileStorageRequestAggregation request2 = requests.iterator().next();
        assertThat(request2.getStatus()).as("2nd request should be DELAYED").isEqualTo(FileRequestStatus.DELAYED);

        fileReference = referenceService.search(ONLINE_CONF_LABEL, checksum).orElse(null);
        assertThat(fileReference).as("File should now be referenced").isNotNull();
        Collection<String> owners = referenceWithOwnersRepository.findOneById(fileReference.getId()).getLazzyOwners();
        assertThat(owners).as("FileReference should have 1 owners").hasSize(1);

        // simulate job for the second storage request,
        // the delayed request will not be processed since the file is now stored but be deleted
        statusService.checkDelayedStorageRequests(storageRequestService);
        requests = storageRequestService.search(ONLINE_CONF_LABEL, checksum);
        Assert.assertEquals("there should be no store request", 0, requests.size());

        jobs = storageRequestService.scheduleJobs(FileRequestStatus.TO_DO, List.of(ONLINE_CONF_LABEL), List.of(owner2));
        assertThat(jobs).hasSize(0);
        runAndWaitJob(jobs);
        Awaitility.await().pollDelay(Duration.ofSeconds(1)).until(Boolean.TRUE::booleanValue);

        // Check results
        // FileReference created ...
        fileReference = referenceService.search(ONLINE_CONF_LABEL, checksum).orElse(null);
        assertThat(fileReference).as("File should be referenced").isNotNull();

        // with 2 owners
        owners = referenceWithOwnersRepository.findOneById(fileReference.getId()).getLazzyOwners();
        assertThat(owners).as("FileReference should have 2 owners")
                          .hasSize(2)
                          .containsExactlyInAnyOrder(owner1, owner2);

        // whereas all the have been processed and deleted
        assertThat(storageRequestService.search(ONLINE_CONF_LABEL, checksum)).as("File request should be deleted")
                                                                             .isEmpty();

        // Now check for event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(2)).publish(any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published",
                            FileReferenceEventType.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());

        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 8, stepEventList.size());
        checkStepEvent(stepEventList.get(0), STORE_REQUESTS, INC);
        checkStepEvent(stepEventList.get(1), REQUESTS_RUNNING, INC);
        checkStepEvent(stepEventList.get(2), STORE_REQUESTS, INC);
        checkStepEvent(stepEventList.get(3), REQUESTS_RUNNING, INC);
        checkStepEvent(stepEventList.get(4), REQUESTS_RUNNING, DEC);
        checkStepEvent(stepEventList.get(5), STORED_FILES, INC);
        checkStepEvent(stepEventList.get(6), REQUESTS_RUNNING, DEC);
        checkStepEvent(stepEventList.get(7), STORED_FILES, INC);
    }

    @Test
    public void store_files() {
        // Create a new bus message File reference request
        final String checksum1 = RandomChecksumUtils.generateRandomChecksum();
        final String checksum2 = RandomChecksumUtils.generateRandomChecksum();

        final FileReferenceRequestArgs args = FileReferenceRequestArgs.builder()
                                                                      .fileName("file.name")
                                                                      .checksum(checksum1)
                                                                      .storage(ONLINE_CONF_LABEL)
                                                                      .owner("owner")
                                                                      .build();
        final FileStorageRequestDto request1 = newStorageRequest(args);
        final FileStorageRequestDto request2 = newStorageRequest(args.withChecksum(checksum2));

        FilesStorageRequestEvent item = new FilesStorageRequestEvent(Set.of(request1, request2),
                                                                     UUID.randomUUID().toString());

        List<FilesStorageRequestEvent> items = new ArrayList<>();
        items.add(item);
        storeHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Check file is not referenced yet
        Assert.assertFalse("File should not be referenced yet",
                           referenceService.search(ONLINE_CONF_LABEL, checksum1).isPresent());
        Assert.assertFalse("File should not be referenced yet",
                           referenceService.search(ONLINE_CONF_LABEL, checksum2).isPresent());
        // Check a file reference request is created
        Collection<FileStorageRequestAggregation> storageReqs1 = storageRequestService.search(ONLINE_CONF_LABEL,
                                                                                              checksum1);
        Collection<FileStorageRequestAggregation> storageReqs2 = storageRequestService.search(ONLINE_CONF_LABEL,
                                                                                              checksum2);
        Assert.assertEquals("File request should be created", 1, storageReqs1.size());
        Assert.assertEquals("File request should be created", 1, storageReqs2.size());
        Assert.assertEquals("",
                            storageReqs1.iterator().next().getGroupIds().iterator().next(),
                            storageReqs2.iterator().next().getGroupIds().iterator().next());

        // Now check for event published
        Mockito.verify(this.publisher, Mockito.times(0)).publish(any(FileReferenceEvent.class));

        // Simulate job schedule
        Collection<JobInfo> jobs = storageRequestService.scheduleJobs(FileRequestStatus.TO_DO,
                                                                      List.of(ONLINE_CONF_LABEL),
                                                                      List.of());
        runAndWaitJob(jobs);
        Assert.assertTrue("File should be referenced",
                          referenceService.search(ONLINE_CONF_LABEL, checksum1).isPresent());
        Assert.assertTrue("File should be referenced",
                          referenceService.search(ONLINE_CONF_LABEL, checksum2).isPresent());
        Assert.assertTrue("File request should be deleted",
                          storageRequestService.search(ONLINE_CONF_LABEL, checksum1).isEmpty());
        Assert.assertTrue("File request should be deleted",
                          storageRequestService.search(ONLINE_CONF_LABEL, checksum2).isEmpty());
        // Now check for event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(2)).publish(any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published",
                            FileReferenceEventType.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
        Mockito.verify(this.storageMetricService, Mockito.times(2))
               .incrementStorageRequests(eq(ONLINE_CONF_LABEL), eq(runtimeTenantResolver.getTenant()));
        Mockito.verify(this.storageMetricService, Mockito.times(2))
               .incrementStorageRequestSuccess(eq(ONLINE_CONF_LABEL), eq(runtimeTenantResolver.getTenant()));
    }

    /**
     * Test request to reference and store a file. An error should be thrown as the destination storage is unknown
     * The file is not stored by the service as the origin storage and the destination storage are identical
     */
    @Test
    public void store_file_unknown_storage() {
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        String storageDestination = "somewhere";
        // Create a new bus message File reference request
        final FileReferenceRequestArgs args = FileReferenceRequestArgs.builder()
                                                                      .fileName("file.name")
                                                                      .checksum(checksum)
                                                                      .storage(storageDestination)
                                                                      .owner("owner-test")
                                                                      .build();
        final FilesStorageRequestEvent item = newStorageRequestEvent(args);

        storeHandler.handleBatch(List.of(item));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertFalse("File should not be referenced",
                           referenceService.search(storageDestination, checksum).isPresent());
        // Now check for event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(1)).publish(any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published",
                            FileReferenceEventType.STORE_ERROR,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 4, stepEventList.size());
        checkStepEvent(stepEventList.get(0), STORE_REQUESTS, INC);
        checkStepEvent(stepEventList.get(1), REQUESTS_RUNNING, INC);
        checkStepEvent(stepEventList.get(2), REQUESTS_RUNNING, DEC);
        checkStepEvent(stepEventList.get(3), REQUESTS_ERRORS, INC);
    }

    /**
     * Test request to reference and store a file. An error should be thrown during storage by plugin
     */
    @Test
    public void store_file_error() {
        final String checksum = RandomChecksumUtils.generateRandomChecksum();
        // Create a new bus message File reference request
        final FileReferenceRequestArgs args = FileReferenceRequestArgs.builder()
                                                                      .fileName("error.file.name")
                                                                      .checksum(checksum)
                                                                      .storage(ONLINE_CONF_LABEL)
                                                                      .owner("owner-test")
                                                                      .build();
        final FilesStorageRequestEvent item = newStorageRequestEvent(args);
        final List<FilesStorageRequestEvent> items = List.of(item);

        storeHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertFalse("File should not be referenced",
                           referenceService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        // Now check for event published
        Mockito.verify(publisher, Mockito.times(0)).publish(any(FileReferenceEvent.class));
        Mockito.clearInvocations(publisher);

        // Simulate job schedule
        Collection<JobInfo> jobs = storageRequestService.scheduleJobs(FileRequestStatus.TO_DO,
                                                                      List.of(ONLINE_CONF_LABEL),
                                                                      List.of());
        runAndWaitJob(jobs);

        Assert.assertFalse("File should not be referenced",
                           referenceService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        Assert.assertEquals("File request should be still present",
                            1,
                            storageRequestService.search(ONLINE_CONF_LABEL, checksum).size());
        Assert.assertEquals("File request should be in ERROR state",
                            FileRequestStatus.ERROR,
                            storageRequestService.search(ONLINE_CONF_LABEL, checksum).iterator().next().getStatus());

        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(1)).publish(any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published",
                            FileReferenceEventType.STORE_ERROR,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());

        Assert.assertEquals("File request still present",
                            1,
                            storageRequestService.search(ONLINE_CONF_LABEL, checksum).size());
        Assert.assertEquals("File request in ERROR state",
                            FileRequestStatus.ERROR,
                            storageRequestService.search(ONLINE_CONF_LABEL, checksum).iterator().next().getStatus());

        // Retry same storage request
        storeHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // There should be one storage request. Same as previous error one but updated to to_do thanks to new request
        Collection<FileStorageRequestAggregation> storeRequests = storageRequestService.search(ONLINE_CONF_LABEL,
                                                                                               checksum);
        Assert.assertEquals("File request still present", 1, storeRequests.size());
        // One in TO_DO state
        Assert.assertEquals("There should be one request in TO_DO state",
                            1L,
                            storeRequests.stream().filter(r -> r.getStatus() == FileRequestStatus.TO_DO).count());

        Mockito.verify(this.storageMetricService, Mockito.times(1))
               .incrementStorageRequests(eq(ONLINE_CONF_LABEL), eq(runtimeTenantResolver.getTenant()));
        Mockito.verify(this.storageMetricService, Mockito.times(1))
               .incrementStorageRequestError(eq(ONLINE_CONF_LABEL), eq(runtimeTenantResolver.getTenant()));
    }

    @Test
    public void retry_byGroupId() {
        String storageDestination = "somewhere";
        String owner = "retry-test";

        Set<FileStorageRequestDto> files = IntStream.rangeClosed(1, 3)
                                                    .mapToObj(i -> "file" + i + ".test")
                                                    .map(fileName -> FileReferenceRequestArgs.builder()
                                                                                             .fileName(fileName)
                                                                                             .checksum(
                                                                                                 RandomChecksumUtils.generateRandomChecksum())
                                                                                             .storage(storageDestination)
                                                                                             .owner("retry-test")
                                                                                             .build())
                                                    .map(this::newStorageRequest)
                                                    .collect(Collectors.toSet());

        // Create a new bus message File reference request

        final FilesStorageRequestEvent item = new FilesStorageRequestEvent(files, UUID.randomUUID().toString());
        storeHandler.handleBatch(List.of(item));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check request in error
        Page<FileStorageRequestAggregation> requests = storageRequestRepository.findByOwnersInAndStatus(List.of(owner),
                                                                                                        FileRequestStatus.ERROR,
                                                                                                        PAGE_1ST_1000);
        Assert.assertEquals("The 3 requests should be in error", 3, requests.getTotalElements());

        FilesRetryRequestEvent retry = FilesRetryRequestEvent.buildStorageRetry(List.of(owner));
        TenantWrapper<FilesRetryRequestEvent> retryWrapper = TenantWrapper.build(retry, getDefaultTenant());
        retryHandler.handle(retryWrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Check request in {@link FileRequestStatus#TO_DO}
        requests = storageRequestRepository.findByOwnersInAndStatus(List.of(owner),
                                                                    FileRequestStatus.TO_DO,
                                                                    PAGE_1ST_1000);
        Assert.assertEquals("The 3 requests should be in TO_DO", 3, requests.getTotalElements());

        Collection<JobInfo> jobs = storageRequestService.scheduleJobs(FileRequestStatus.TO_DO, List.of(), List.of());
        runAndWaitJob(jobs);

        requests = storageRequestRepository.findByOwnersInAndStatus(List.of(owner),
                                                                    FileRequestStatus.ERROR,
                                                                    PAGE_1ST_1000);
        Assert.assertEquals("The 3 requests should be in error again", 3, requests.getTotalElements());
    }

    @Test
    public void retry_byOwners() {
        String storageDestination = "somewhere";
        List<String> owners = List.of("retry-test-1", "retry-test-2", "retry-test-3");
        Set<FileStorageRequestDto> files = IntStream.rangeClosed(1, 3)
                                                    .mapToObj(i -> FileReferenceRequestArgs.builder()
                                                                                           .fileName("file.test" + i)
                                                                                           .checksum(RandomChecksumUtils.generateRandomChecksum())
                                                                                           .storage(storageDestination)
                                                                                           .owner("retry-test-" + i)
                                                                                           .build())
                                                    .map(this::newStorageRequest)
                                                    .collect(Collectors.toSet());

        // Create a new bus message File reference request
        FilesStorageRequestEvent item = new FilesStorageRequestEvent(files,
                                                                     RandomChecksumUtils.generateRandomChecksum());
        List<FilesStorageRequestEvent> items = new ArrayList<>();
        items.add(item);
        storeHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check request in error
        Page<FileStorageRequestAggregation> requests = storageRequestRepository.findByOwnersInAndStatus(owners,
                                                                                                        FileRequestStatus.ERROR,
                                                                                                        PAGE_1ST_1000);
        Assert.assertEquals("The 3 requests should be in error", 3, requests.getTotalElements());

        FilesRetryRequestEvent retry = FilesRetryRequestEvent.buildStorageRetry(owners);
        TenantWrapper<FilesRetryRequestEvent> retryWrapper = TenantWrapper.build(retry, getDefaultTenant());
        retryHandler.handle(retryWrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Check request in {@link FileRequestStatus#TO_DO}
        requests = storageRequestRepository.findByOwnersInAndStatus(owners, FileRequestStatus.TO_DO, PAGE_1ST_1000);
        Assert.assertEquals("The 3 requests should be in TO_DO", 3, requests.getTotalElements());

        Collection<JobInfo> jobs = storageRequestService.scheduleJobs(FileRequestStatus.TO_DO, List.of(), List.of());
        runAndWaitJob(jobs);

        requests = storageRequestRepository.findByOwnersInAndStatus(owners, FileRequestStatus.ERROR, PAGE_1ST_1000);
        Assert.assertEquals("The 3 requests should be in error again", 3, requests.getTotalElements());

        // Check step events were correctly send (check only for the first request)
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 24, stepEventList.size());
        checkStepEvent(stepEventList.get(0), STORE_REQUESTS, INC);
        checkStepEvent(stepEventList.get(1), REQUESTS_RUNNING, INC);
        checkStepEvent(stepEventList.get(2), REQUESTS_RUNNING, DEC);
        checkStepEvent(stepEventList.get(3), REQUESTS_ERRORS, INC);
        checkStepEvent(stepEventList.get(12), REQUESTS_ERRORS, DEC);
        checkStepEvent(stepEventList.get(13), REQUESTS_RUNNING, INC);
        checkStepEvent(stepEventList.get(18), REQUESTS_RUNNING, DEC);
        checkStepEvent(stepEventList.get(19), REQUESTS_ERRORS, INC);
    }

    private void checkStepEvent(StepPropertyUpdateRequestEvent event,
                                SessionNotifierPropertyEnum expectedEventProperty,
                                StepPropertyEventTypeEnum expectedType) {
        checkStepEvent(event, expectedEventProperty, expectedType, SESSION1_OWNER, SESSION1, "1");
    }

    private FileStorageRequestDto newStorageRequest(FileReferenceRequestArgs args) {
        return FileStorageRequestDto.build(args.getFileName(),
                                           args.getChecksum(),
                                           "MD5",
                                           MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                           args.getOwner(),
                                           SESSION1_OWNER,
                                           SESSION1,
                                           ORIGIN_URL,
                                           args.getStorage(),
                                           Optional.ofNullable(args.getSubDirectory()));
    }

    private FilesStorageRequestEvent newStorageRequestEvent(FileReferenceRequestArgs args) {
        return new FilesStorageRequestEvent(newStorageRequest(args), UUID.randomUUID().toString());
    }

}