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
package fr.cnes.regards.modules.storage.service.file.flow;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesDeletionEvent;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEventType;
import fr.cnes.regards.modules.filecatalog.dto.FileRequestStatus;
import fr.cnes.regards.modules.filecatalog.dto.request.FileDeletionDto;
import fr.cnes.regards.modules.filecatalog.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.request.FileReferenceRequestService;
import fr.cnes.regards.modules.storage.service.file.request.FileStorageRequestService;
import fr.cnes.regards.modules.storage.service.session.SessionNotifierPropertyEnum;
import org.apache.commons.compress.utils.Sets;
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

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests" },
                    locations = { "classpath:application-test.properties" })
public class DeleteFileReferenceFlowItemIT extends AbstractStorageIT {

    private static final String SESSION_OWNER_1 = "SOURCE 1";

    private static final String SESSION_OWNER_2 = "SOURCE 2";

    private static final String SESSION_1 = "SESSION 1";

    private static final boolean waitForLock = false;

    @Autowired
    FileReferenceRequestService fileRefReqService;

    @Autowired
    FileReferenceService fileRefService;

    @Autowired
    FileStorageRequestService fileStorageReqService;

    @Autowired
    private DeletionFlowHandler deletionFlowHandler;

    @Autowired
    private ReferenceFlowItemHandler referenceFlowItemHandler;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

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
    public void deleteFlowItemNotExists() {
        FilesDeletionEvent item = new FilesDeletionEvent(FileDeletionDto.build(UUID.randomUUID().toString(),
                                                                               "some-stprage",
                                                                               "owner",
                                                                               SESSION_OWNER_1,
                                                                               SESSION_1,
                                                                               false), UUID.randomUUID().toString());
        List<FilesDeletionEvent> items = new ArrayList<>();
        items.add(item);
        deletionFlowHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.never()).publish(Mockito.any(FileReferenceEvent.class));
        // Check step events were correctly send
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 1, stepEventList.size());
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.DELETE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
    }

    /**
     * Test deletion of a file for only one of his owners.
     * Expected results :
     * - File should not belongs to the given owners
     * - File should not be fully deleted as it is owned by other owners.
     */
    @Test
    public void deleteFlowItemOnlyOneOwner() {
        String checksum = UUID.randomUUID().toString();
        String storage = "some-storage";
        String owner = "owner";
        this.referenceFile(checksum, owner, null, "file.test", storage, SESSION_OWNER_1, SESSION_1, false);
        Mockito.clearInvocations(publisher);
        FilesDeletionEvent item = new FilesDeletionEvent(FileDeletionDto.build(checksum,
                                                                               storage,
                                                                               owner,
                                                                               SESSION_OWNER_1,
                                                                               SESSION_1,
                                                                               false), UUID.randomUUID().toString());
        List<FilesDeletionEvent> items = new ArrayList<>();
        items.add(item);
        deletionFlowHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Collection<FileReferenceEvent> events = getFileReferenceEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("There should be two events. One DELETED_FOR_WONER and one FULLY_DELETED",
                            Sets.newHashSet(FileReferenceEventType.DELETED_FOR_OWNER,
                                            FileReferenceEventType.FULLY_DELETED),
                            events.stream().map(r -> r.getType()).collect(Collectors.toSet()));
        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 5, stepEventList.size());
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.DELETE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(2),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(3),
                       SessionNotifierPropertyEnum.DELETED_FILES,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(4),
                       SessionNotifierPropertyEnum.STORED_FILES,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
    }

    /**
     * Test deletion of a file with no access to storage location for the last owner of the file.
     * Expected results :
     * - File should not belongs to the given owner
     * - File should be fully deleted
     */
    @Test
    public void deleteFlowItemMultipleOwners() {
        String checksum = UUID.randomUUID().toString();
        String storage = "some-storage";
        String owner = "owner";
        this.referenceFile(checksum, owner, null, "file.test", storage, SESSION_OWNER_1, SESSION_1, false);
        this.referenceFile(checksum, "other-owner", null, "file.test", storage, SESSION_OWNER_2, SESSION_1, false);
        Mockito.clearInvocations(publisher);
        FilesDeletionEvent item = new FilesDeletionEvent(FileDeletionDto.build(checksum,
                                                                               storage,
                                                                               owner,
                                                                               SESSION_OWNER_1,
                                                                               SESSION_1,
                                                                               false), UUID.randomUUID().toString());
        List<FilesDeletionEvent> items = new ArrayList<>();
        items.add(item);
        deletionFlowHandler.handleBatch(items);
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
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.DELETE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.DELETED_FILES,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(2),
                       SessionNotifierPropertyEnum.STORED_FILES,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
    }

    /**
     * Test deletion of a file with access to storage location for the last owner of the file.
     * Expected results :
     * - File should not belongs to the given owner
     * - File should be fully deleted
     */
    @Test
    public void deleteFlowItemStored() throws InterruptedException, ExecutionException {
        String checksum = UUID.randomUUID().toString();
        String owner = "owner";
        FileReference fileRef = this.generateStoredFileReference(checksum,
                                                                 owner,
                                                                 "file.test",
                                                                 ONLINE_CONF_LABEL,
                                                                 Optional.empty(),
                                                                 Optional.empty(),
                                                                 SESSION_OWNER_1,
                                                                 SESSION_1);
        String storage = fileRef.getLocation().getStorage();
        Mockito.clearInvocations(publisher);
        FilesDeletionEvent item = new FilesDeletionEvent(FileDeletionDto.build(checksum,
                                                                               storage,
                                                                               owner,
                                                                               SESSION_OWNER_1,
                                                                               SESSION_1,
                                                                               false), UUID.randomUUID().toString());
        List<FilesDeletionEvent> items = new ArrayList<>();
        items.add(item);
        deletionFlowHandler.handleBatch(items);
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
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.DELETE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");

        // A new File deletion request should be sent
        Assert.assertTrue("A file deletion request should be created",
                          fileDeletionRequestService.search(fileRef).isPresent());
        Assert.assertEquals("A file deletion request should be created in TO_DO state",
                            FileRequestStatus.TO_DO,
                            fileDeletionRequestService.search(fileRef).get().getStatus());
        Mockito.clearInvocations(publisher);

        // Now schedule deletion jobs
        Collection<JobInfo> jobs = fileDeletionRequestService.scheduleJobs(FileRequestStatus.TO_DO,
                                                                           Lists.newArrayList());
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
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.DELETED_FILES,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(2),
                       SessionNotifierPropertyEnum.STORED_FILES,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");

    }

    /**
     * Test deletion of a file with for the last owner of the file with  error occurs during file deletion on storage location.
     * <p>
     * Expected results :
     * - File should not belongs to the given owner
     * - Error is sent for the file deletion on storage
     */
    @Test
    public void deleteFlowItemStoredError() throws InterruptedException, ExecutionException {
        String checksum = UUID.randomUUID().toString();
        String owner = "owner";
        FileReference fileRef = this.generateStoredFileReference(checksum,
                                                                 owner,
                                                                 "delErr.file.test",
                                                                 ONLINE_CONF_LABEL,
                                                                 Optional.empty(),
                                                                 Optional.empty(),
                                                                 SESSION_OWNER_1,
                                                                 SESSION_1);
        String storage = fileRef.getLocation().getStorage();
        Mockito.clearInvocations(publisher);
        FilesDeletionEvent item = new FilesDeletionEvent(FileDeletionDto.build(checksum,
                                                                               storage,
                                                                               owner,
                                                                               SESSION_OWNER_1,
                                                                               SESSION_1,
                                                                               false), UUID.randomUUID().toString());
        List<FilesDeletionEvent> items = new ArrayList<>();
        items.add(item);
        deletionFlowHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should not belongs to owner anymore",
                            FileReferenceEventType.DELETED_FOR_OWNER,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
        // A new File deletion request should be sent
        Assert.assertTrue("A file deletion request should be created",
                          fileDeletionRequestService.search(fileRef).isPresent());
        Assert.assertEquals("A file deletion request should be created in TO_DO state",
                            FileRequestStatus.TO_DO,
                            fileDeletionRequestService.search(fileRef).get().getStatus());
        Mockito.clearInvocations(publisher);

        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 2, stepEventList.size());
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.DELETE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");

        // Now schedule deletion jobs
        Collection<JobInfo> jobs = fileDeletionRequestService.scheduleJobs(FileRequestStatus.TO_DO,
                                                                           Lists.newArrayList());
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
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.REQUESTS_ERRORS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
    }

    /**
     * Test deletion of a file with for the last owner of the file with error occurs during file deletion on storage location.
     * <p>
     * Expected results :
     * - File should not belongs to the given owner
     * - File should be fully deleted
     */
    @Test
    public void deleteFlowItemStoredErrorWithForce() throws InterruptedException, ExecutionException {
        String checksum = UUID.randomUUID().toString();
        String owner = "owner";
        FileReference fileRef = this.generateStoredFileReference(checksum,
                                                                 owner,
                                                                 "delErr.file.test",
                                                                 ONLINE_CONF_LABEL,
                                                                 Optional.empty(),
                                                                 Optional.empty(),
                                                                 SESSION_OWNER_1,
                                                                 SESSION_1);
        String storage = fileRef.getLocation().getStorage();
        Mockito.clearInvocations(publisher);
        FilesDeletionEvent item = new FilesDeletionEvent(FileDeletionDto.build(checksum,
                                                                               storage,
                                                                               owner,
                                                                               SESSION_OWNER_1,
                                                                               SESSION_1,
                                                                               true), UUID.randomUUID().toString());
        List<FilesDeletionEvent> items = new ArrayList<>();
        items.add(item);
        deletionFlowHandler.handleBatch(items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should not belongs to owner anymore",
                            FileReferenceEventType.DELETED_FOR_OWNER,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
        // A new File deletion request should be sent
        Assert.assertTrue("A file deletion request should be created",
                          fileDeletionRequestService.search(fileRef).isPresent());
        Assert.assertEquals("A file deletion request should be created in TO_DO state",
                            FileRequestStatus.TO_DO,
                            fileDeletionRequestService.search(fileRef).get().getStatus());
        Mockito.clearInvocations(publisher);

        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 2, stepEventList.size());
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.DELETE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");

        // Now schedule deletion jobs
        Collection<JobInfo> jobs = fileDeletionRequestService.scheduleJobs(FileRequestStatus.TO_DO,
                                                                           Lists.newArrayList());
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
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.DELETED_FILES,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(2),
                       SessionNotifierPropertyEnum.STORED_FILES,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
    }

    /**
     * Test deletion in case the file is not stored physically
     */
    @Test
    public void deleteFlowItemReferenced() {
        // PREPARATION
        // init parameters
        String checksum = UUID.randomUUID().toString();
        String storage = "local";
        String owner = "owner-test";

        // create reference flow item
        FilesReferenceEvent refItem = new FilesReferenceEvent(FileReferenceRequestDto.build("file.name",
                                                                                            checksum,
                                                                                            "MD5",
                                                                                            "application/octet-stream",
                                                                                            10L,
                                                                                            owner,
                                                                                            storage,
                                                                                            "file://storage/location/file.name",
                                                                                            SESSION_OWNER_1,
                                                                                            SESSION_1),
                                                              UUID.randomUUID().toString());
        referenceFlowItemHandler.handleBatch(Lists.newArrayList(refItem));
        Mockito.clearInvocations(publisher);

        // DELETE FILE REFERENCE
        FilesDeletionEvent delItem = new FilesDeletionEvent(FileDeletionDto.build(checksum,
                                                                                  storage,
                                                                                  owner,
                                                                                  SESSION_OWNER_1,
                                                                                  SESSION_1,
                                                                                  false), UUID.randomUUID().toString());
        deletionFlowHandler.handleBatch(Lists.newArrayList(delItem));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // CHECK RESULTS
        // check events sent
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Collection<FileReferenceEvent> events = getFileReferenceEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("There should be two events. One DELETED_FOR_WONER and one FULLY_DELETED",
                            Sets.newHashSet(FileReferenceEventType.DELETED_FOR_OWNER,
                                            FileReferenceEventType.FULLY_DELETED),
                            events.stream().map(FileReferenceEvent::getType).collect(Collectors.toSet()));
        // Check step events were correctly send
        List<StepPropertyUpdateRequestEvent> stepEventList = getStepPropertyEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("Unexpected number of StepPropertyUpdateRequestEvents", 5, stepEventList.size());
        checkStepEvent(stepEventList.get(0),
                       SessionNotifierPropertyEnum.DELETE_REQUESTS,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(1),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(2),
                       SessionNotifierPropertyEnum.REQUESTS_RUNNING,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(3),
                       SessionNotifierPropertyEnum.DELETED_FILES,
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
        checkStepEvent(stepEventList.get(4),
                       SessionNotifierPropertyEnum.REFERENCED_FILES,
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER_1,
                       SESSION_1,
                       "1");
    }
}
