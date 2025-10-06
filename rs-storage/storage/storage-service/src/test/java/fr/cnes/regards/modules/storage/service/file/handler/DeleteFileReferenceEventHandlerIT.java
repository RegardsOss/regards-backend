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

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.request.FileDeletionDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesDeletionEvent;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEventType;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceRequestPublisher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum.DEC;
import static fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum.INC;
import static fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceConstants.*;
import static fr.cnes.regards.modules.storage.service.session.SessionNotifierPropertyEnum.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests" },
                    locations = { "classpath:application-test.properties" })
public class DeleteFileReferenceEventHandlerIT extends AbstractStorageIT {

    @Autowired
    private FilesDeletionEventHandler filesDeletionEventHandler;

    @Autowired
    private FileReferenceRequestPublisher referenceRequestPublisher;

    @Before
    public void initialize() throws ModuleException {
        Mockito.clearInvocations(publisher);
        super.init();
    }

    /**
     * Test deletion for a file not referenced
     * Expected results :
     * - No change on files. (no fileReference event)
     */
    @Test
    public void delete_file_not_existing() {
        FilesDeletionEvent item = new FilesDeletionEvent(FileDeletionDto.build(RandomChecksumUtils.generateRandomChecksum(),
                                                                               "some-storage",
                                                                               "owner",
                                                                               SESSION1_OWNER,
                                                                               SESSION1,
                                                                               false), UUID.randomUUID().toString());

        filesDeletionEventHandler.handleBatch(List.of(item));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.never()).publish(Mockito.any(FileReferenceEvent.class));
        // Check step events were correctly send
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 1, stepEventList.size());
        checkStepEvent(stepEventList.get(0), DELETE_REQUESTS, INC, SESSION1_OWNER, SESSION1, "1");
    }

    /**
     * Test deletion of a file for only one of his owners.
     * Expected results :
     * - File should not belongs to the given owners
     * - File should not be fully deleted as it is owned by other owners.
     */
    @Test
    public void delete_file_one_owner() {
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        String storage = "some-storage";
        String owner = "owner";
        this.referenceFile(checksum, owner, null, "file.test", storage, SESSION1_OWNER, SESSION1, false);
        Mockito.clearInvocations(publisher);
        FilesDeletionEvent item = new FilesDeletionEvent(FileDeletionDto.build(checksum,
                                                                               storage,
                                                                               owner,
                                                                               SESSION1_OWNER,
                                                                               SESSION1,
                                                                               false), UUID.randomUUID().toString());

        filesDeletionEventHandler.handleBatch(List.of(item));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Collection<FileReferenceEvent> events = getFileReferenceEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("There should be two events. One DELETED_FOR_WONER and one FULLY_DELETED",
                            Set.of(FileReferenceEventType.DELETED_FOR_OWNER, FileReferenceEventType.FULLY_DELETED),
                            events.stream().map(r -> r.getType()).collect(Collectors.toSet()));
        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 5, stepEventList.size());
        checkStepEvent(stepEventList.get(0), DELETE_REQUESTS, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(1), REQUESTS_RUNNING, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(2), REQUESTS_RUNNING, DEC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(3), DELETED_FILES, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(4), STORED_FILES, DEC, SESSION1_OWNER, SESSION1, "1");
    }

    /**
     * Test deletion of a file with no access to storage location for the last owner of the file.
     * Expected results :
     * - File should not belongs to the given owner
     * - File should be fully deleted
     */
    @Test
    public void delete_file_multiple_owners() {
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        String storage = "some-storage";
        String owner = "owner";
        this.referenceFile(checksum, owner, null, "file.test", storage, SESSION1_OWNER, SESSION1, false);
        this.referenceFile(checksum, "other-owner", null, "file.test", storage, SESSION2_OWNER, SESSION1, false);
        Mockito.clearInvocations(publisher);
        FilesDeletionEvent item = new FilesDeletionEvent(FileDeletionDto.build(checksum,
                                                                               storage,
                                                                               owner,
                                                                               SESSION1_OWNER,
                                                                               SESSION1,
                                                                               false), UUID.randomUUID().toString());
        filesDeletionEventHandler.handleBatch(List.of(item));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should be deleted for the given owner",
                            FileReferenceEventType.DELETED_FOR_OWNER,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 3, stepEventList.size());
        checkStepEvent(stepEventList.get(0), DELETE_REQUESTS, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(1), DELETED_FILES, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(2), STORED_FILES, DEC, SESSION1_OWNER, SESSION1, "1");
    }

    /**
     * Test deletion of a file with access to storage location for the last owner of the file.
     * Expected results :
     * - File should not belongs to the given owner
     * - File should be fully deleted
     */
    @Test
    public void delete_file_last_owner() throws InterruptedException, ExecutionException {
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        String owner = "owner";
        FileReference fileRef = this.generateStoredFileReference(checksum,
                                                                 owner,
                                                                 "file.test",
                                                                 ONLINE_CONF_LABEL,
                                                                 Optional.empty(),
                                                                 Optional.empty(),
                                                                 SESSION1_OWNER,
                                                                 SESSION1);
        String storage = fileRef.getLocation().getStorage();
        Mockito.clearInvocations(publisher);
        FilesDeletionEvent item = new FilesDeletionEvent(FileDeletionDto.build(checksum,
                                                                               storage,
                                                                               owner,
                                                                               SESSION1_OWNER,
                                                                               SESSION1,
                                                                               false), UUID.randomUUID().toString());
        filesDeletionEventHandler.handleBatch(List.of(item));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should not belongs to owner anymore",
                            FileReferenceEventType.DELETED_FOR_OWNER,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 2, stepEventList.size());
        checkStepEvent(stepEventList.get(0), DELETE_REQUESTS, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(1), REQUESTS_RUNNING, INC, SESSION1_OWNER, SESSION1, "1");

        // A new File deletion request should be sent
        Assert.assertTrue("A file deletion request should be created",
                          deletionRequestService.search(fileRef).isPresent());
        Assert.assertEquals("A file deletion request should be created in TO_DO state",
                            FileRequestStatus.TO_DO,
                            deletionRequestService.search(fileRef).get().getStatus());
        Mockito.clearInvocations(publisher);

        // Now schedule deletion jobs
        Collection<JobInfo> jobs = deletionRequestService.scheduleJobs(FileRequestStatus.TO_DO, List.of());
        runAndWaitJob(jobs);

        argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should not belongs to owner anymore",
                            FileReferenceEventType.FULLY_DELETED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
        // Check step events were correctly send
        stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 3, stepEventList.size());
        checkStepEvent(stepEventList.get(0), REQUESTS_RUNNING, DEC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(1), DELETED_FILES, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(2), STORED_FILES, DEC, SESSION1_OWNER, SESSION1, "1");

    }

    /**
     * Test deletion of a file with for the last owner of the file with  error occurs during file deletion on storage location.
     * <p>
     * Expected results :
     * - File should not belongs to the given owner
     * - Error is sent for the file deletion on storage
     */
    @Test
    public void delete_file_error() throws InterruptedException, ExecutionException {
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        String owner = "owner";
        FileReference fileRef = this.generateStoredFileReference(checksum,
                                                                 owner,
                                                                 "delErr.file.test",
                                                                 ONLINE_CONF_LABEL,
                                                                 Optional.empty(),
                                                                 Optional.empty(),
                                                                 SESSION1_OWNER,
                                                                 SESSION1);
        String storage = fileRef.getLocation().getStorage();
        Mockito.clearInvocations(publisher);
        FilesDeletionEvent item = new FilesDeletionEvent(FileDeletionDto.build(checksum,
                                                                               storage,
                                                                               owner,
                                                                               SESSION1_OWNER,
                                                                               SESSION1,
                                                                               false), UUID.randomUUID().toString());
        filesDeletionEventHandler.handleBatch(List.of(item));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should not belongs to owner anymore",
                            FileReferenceEventType.DELETED_FOR_OWNER,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
        // A new File deletion request should be sent
        Assert.assertTrue("A file deletion request should be created",
                          deletionRequestService.search(fileRef).isPresent());
        Assert.assertEquals("A file deletion request should be created in TO_DO state",
                            FileRequestStatus.TO_DO,
                            deletionRequestService.search(fileRef).get().getStatus());
        Mockito.clearInvocations(publisher);

        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 2, stepEventList.size());
        checkStepEvent(stepEventList.get(0), DELETE_REQUESTS, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(1), REQUESTS_RUNNING, INC, SESSION1_OWNER, SESSION1, "1");

        // Now schedule deletion jobs
        Collection<JobInfo> jobs = deletionRequestService.scheduleJobs(FileRequestStatus.TO_DO, List.of());
        runAndWaitJob(jobs);

        argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should not belongs to owner anymore",
                            FileReferenceEventType.DELETION_ERROR,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());

        // Check step events were correctly send
        stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 2, stepEventList.size());
        checkStepEvent(stepEventList.get(0), REQUESTS_RUNNING, DEC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(1), REQUESTS_ERRORS, INC, SESSION1_OWNER, SESSION1, "1");
    }

    /**
     * Test deletion of a file with for the last owner of the file with error occurs during file deletion on storage location.
     * <p>
     * Expected results :
     * - File should not belongs to the given owner
     * - File should be fully deleted
     */
    @Test
    public void delete_file_error_force() throws InterruptedException, ExecutionException {
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        String owner = "owner";
        FileReference fileRef = this.generateStoredFileReference(checksum,
                                                                 owner,
                                                                 "delErr.file.test",
                                                                 ONLINE_CONF_LABEL,
                                                                 Optional.empty(),
                                                                 Optional.empty(),
                                                                 SESSION1_OWNER,
                                                                 SESSION1);
        String storage = fileRef.getLocation().getStorage();
        Mockito.clearInvocations(publisher);
        FilesDeletionEvent item = new FilesDeletionEvent(FileDeletionDto.build(checksum,
                                                                               storage,
                                                                               owner,
                                                                               SESSION1_OWNER,
                                                                               SESSION1,
                                                                               true), UUID.randomUUID().toString());
        filesDeletionEventHandler.handleBatch(List.of(item));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should not belongs to owner anymore",
                            FileReferenceEventType.DELETED_FOR_OWNER,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
        // A new File deletion request should be sent
        Assert.assertTrue("A file deletion request should be created",
                          deletionRequestService.search(fileRef).isPresent());
        Assert.assertEquals("A file deletion request should be created in TO_DO state",
                            FileRequestStatus.TO_DO,
                            deletionRequestService.search(fileRef).get().getStatus());
        Mockito.clearInvocations(publisher);

        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 2, stepEventList.size());
        checkStepEvent(stepEventList.get(0), DELETE_REQUESTS, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(1), REQUESTS_RUNNING, INC, SESSION1_OWNER, SESSION1, "1");

        // Now schedule deletion jobs
        Collection<JobInfo> jobs = deletionRequestService.scheduleJobs(FileRequestStatus.TO_DO, List.of());
        runAndWaitJob(jobs);

        argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should not belongs to owner anymore",
                            FileReferenceEventType.FULLY_DELETED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());

        // Check step events were correctly send
        stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 3, stepEventList.size());
        checkStepEvent(stepEventList.get(0), REQUESTS_RUNNING, DEC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(1), DELETED_FILES, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(2), STORED_FILES, DEC, SESSION1_OWNER, SESSION1, "1");
    }

    /**
     * Test deletion in case the file is not stored physically
     */
    @Test
    public void delete_referenced_file() {
        // GIVEN
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        String storage = "local";
        String owner = "owner-test";

        // create and process a reference request
        FilesReferenceEvent refItem = new FilesReferenceEvent(FileReferenceRequestDto.build("file.name",
                                                                                            checksum,
                                                                                            "MD5",
                                                                                            "application/octet-stream",
                                                                                            10L,
                                                                                            owner,
                                                                                            storage,
                                                                                            "file://storage/location/file.name",
                                                                                            SESSION1_OWNER,
                                                                                            SESSION1),
                                                              UUID.randomUUID().toString());

        referenceRequestPublisher.publishReferenceEvents(1, refItem);
        Mockito.clearInvocations(publisher);

        // WHEN
        // create deletion request
        FilesDeletionEvent delItem = new FilesDeletionEvent(FileDeletionDto.build(checksum,
                                                                                  storage,
                                                                                  owner,
                                                                                  SESSION1_OWNER,
                                                                                  SESSION1,
                                                                                  false), UUID.randomUUID().toString());

        filesDeletionEventHandler.handleBatch(List.of(delItem));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // THEN
        // check events sent
        final ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());

        final Collection<FileReferenceEvent> events = getFileReferenceEvents(argumentCaptor.getAllValues());
        assertThat(events).hasSize(2);
        final Set<FileReferenceEventType> eventTypes = events.stream()
                                                             .map(FileReferenceEvent::getType)
                                                             .collect(Collectors.toSet());
        assertThat(eventTypes).as("There should be two events. One DELETED_FOR_OWNER and one FULLY_DELETED")
                              .containsExactlyInAnyOrder(FileReferenceEventType.DELETED_FOR_OWNER,
                                                         FileReferenceEventType.FULLY_DELETED);

        // Check step events were correctly send
        final List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        assertThat(stepEventList).as("Unexpected number of StepPropertyUpdateRequestEvents").hasSize(5);
        checkStepEvent(stepEventList.get(0), DELETE_REQUESTS, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(1), REQUESTS_RUNNING, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(2), REQUESTS_RUNNING, DEC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(3), DELETED_FILES, INC, SESSION1_OWNER, SESSION1, "1");
        checkStepEvent(stepEventList.get(4), REFERENCED_FILES, DEC, SESSION1_OWNER, SESSION1, "1");
    }
}
