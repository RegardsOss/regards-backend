/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.service.file.reference.flow;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEventState;
import fr.cnes.regards.modules.storagelight.domain.flow.DeleteFileRefFlowItem;
import fr.cnes.regards.modules.storagelight.service.file.reference.AbstractFileReferenceTest;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileStorageRequestService;

/**
 * @author Sébastien Binda
 *
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests",
        "regards.storage.cache.path=target/cache", "regards.storage.cache.minimum.time.to.live.hours=12" })
public class DeleteFileReferenceFlowItemTest extends AbstractFileReferenceTest {

    @Autowired
    private DeleteFileReferenceFlowHandler handler;

    @Autowired
    FileReferenceService fileRefService;

    @Autowired
    FileStorageRequestService fileRefRequestService;

    @SpyBean
    public IPublisher publisher;

    @Before
    public void initialize() throws ModuleException {
        Mockito.clearInvocations(publisher);
        super.init();
    }

    /**
     * Test deletion for a file not referenced
     * Expected results :
     *  - No change on files. (no fileReference event)
     */
    @Test
    public void deleteFlowItemNotExists() {
        DeleteFileRefFlowItem item = new DeleteFileRefFlowItem(UUID.randomUUID().toString(), "some-stprage", "owner");
        TenantWrapper<DeleteFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        handler.handle(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Mockito.verify(publisher, Mockito.never()).publish(Mockito.any(FileReferenceEvent.class));
    }

    /**
     * Test deletion of a file for only one of his owners.
     * Expected results :
     *  - File should not belongs to the given owners
     *  - File should not be fully deleted as it is owned by other owners.
     */
    @Test
    public void deleteFlowItemOnlyOneOwner() {
        String checksum = UUID.randomUUID().toString();
        String storage = "some-storage";
        String owner = "owner";
        this.referenceFile(checksum, Sets.newHashSet(owner), null, "file.test", storage);
        Mockito.clearInvocations(publisher);
        DeleteFileRefFlowItem item = new DeleteFileRefFlowItem(checksum, storage, owner);
        TenantWrapper<DeleteFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        handler.handle(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Collection<FileReferenceEvent> events = getFileReferenceEvents(argumentCaptor.getAllValues());
        Assert.assertEquals("There should be two events. One DELETED_FOR_WONER and one FULLY_DELETED",
                            Sets.newHashSet(FileReferenceEventState.DELETED_FOR_OWNER,
                                            FileReferenceEventState.FULLY_DELETED),
                            events.stream().map(r -> r.getState()).collect(Collectors.toSet()));
    }

    /**
     * Test deletion of a file with no access to storage location for the last owner of the file.
     * Expected results :
     *  - File should not belongs to the given owner
     *  - File should be fully deleted
     */
    @Test
    public void deleteFlowItemMultipleOwners() {
        String checksum = UUID.randomUUID().toString();
        String storage = "some-storage";
        String owner = "owner";
        this.referenceFile(checksum, Sets.newHashSet(owner, "other-owner"), null, "file.test", storage);
        Mockito.clearInvocations(publisher);
        DeleteFileRefFlowItem item = new DeleteFileRefFlowItem(checksum, storage, owner);
        TenantWrapper<DeleteFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        handler.handle(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should be deleted for the given owner",
                            FileReferenceEventState.DELETED_FOR_OWNER,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getState());
    }

    /**
     * Test deletion of a file with access to storage location for the last owner of the file.
     * Expected results :
     *  - File should not belongs to the given owner
     *  - File should be fully deleted
     */
    @Test
    public void deleteFlowItemStored() throws InterruptedException, ExecutionException {
        String checksum = UUID.randomUUID().toString();
        String owner = "owner";
        FileReference fileRef = this.generateStoredFileReference(checksum, owner, "file.test");
        String storage = fileRef.getLocation().getStorage();
        Mockito.clearInvocations(publisher);
        DeleteFileRefFlowItem item = new DeleteFileRefFlowItem(checksum, storage, owner);
        TenantWrapper<DeleteFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        handler.handle(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should not belongs to owner anymore",
                            FileReferenceEventState.DELETED_FOR_OWNER,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getState());
        // A new File deletion request should be sent
        Assert.assertTrue("A file deletion request should be created",
                          fileDeletionRequestService.search(fileRef).isPresent());
        Assert.assertEquals("A file deletion request should be created in TODO state", FileRequestStatus.TODO,
                            fileDeletionRequestService.search(fileRef).get().getStatus());
        Mockito.clearInvocations(publisher);

        // Now schedule deletion jobs
        Collection<JobInfo> jobs = fileDeletionRequestService.scheduleDeletionJobs(FileRequestStatus.TODO,
                                                                                   Lists.newArrayList());
        runAndWaitJob(jobs);

        argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should not belongs to owner anymore", FileReferenceEventState.FULLY_DELETED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getState());
    }

    /**
     * Test deletion of a file with for the last owner of the file with  error occurs during file deletion on storage location.
     *
     * Expected results :
     *  - File should not belongs to the given owner
     *  - Error is sent for the file deletion on storage
     */
    @Test
    public void deleteFlowItemStoredError() throws InterruptedException, ExecutionException {
        String checksum = UUID.randomUUID().toString();
        String owner = "owner";
        FileReference fileRef = this.generateStoredFileReference(checksum, owner, "delErr.file.test");
        String storage = fileRef.getLocation().getStorage();
        Mockito.clearInvocations(publisher);
        DeleteFileRefFlowItem item = new DeleteFileRefFlowItem(checksum, storage, owner);
        TenantWrapper<DeleteFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        handler.handle(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should not belongs to owner anymore",
                            FileReferenceEventState.DELETED_FOR_OWNER,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getState());
        // A new File deletion request should be sent
        Assert.assertTrue("A file deletion request should be created",
                          fileDeletionRequestService.search(fileRef).isPresent());
        Assert.assertEquals("A file deletion request should be created in TODO state", FileRequestStatus.TODO,
                            fileDeletionRequestService.search(fileRef).get().getStatus());
        Mockito.clearInvocations(publisher);

        // Now schedule deletion jobs
        Collection<JobInfo> jobs = fileDeletionRequestService.scheduleDeletionJobs(FileRequestStatus.TODO,
                                                                                   Lists.newArrayList());
        runAndWaitJob(jobs);

        argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should not belongs to owner anymore",
                            FileReferenceEventState.DELETION_ERROR,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getState());
    }

    /**
     * Test deletion of a file with for the last owner of the file with error occurs during file deletion on storage location.
     *
     * Expected results :
     *  - File should not belongs to the given owner
     *  - File should be fully deleted
     */
    @Test
    public void deleteFlowItemStoredErrorWithForce() throws InterruptedException, ExecutionException {
        String checksum = UUID.randomUUID().toString();
        String owner = "owner";
        FileReference fileRef = this.generateStoredFileReference(checksum, owner, "delErr.file.test");
        String storage = fileRef.getLocation().getStorage();
        Mockito.clearInvocations(publisher);
        DeleteFileRefFlowItem item = new DeleteFileRefFlowItem(checksum, storage, owner);
        TenantWrapper<DeleteFileRefFlowItem> wrapper = new TenantWrapper<>(item.withForceDelete(), getDefaultTenant());
        // Publish request
        handler.handle(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should not belongs to owner anymore",
                            FileReferenceEventState.DELETED_FOR_OWNER,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getState());
        // A new File deletion request should be sent
        Assert.assertTrue("A file deletion request should be created",
                          fileDeletionRequestService.search(fileRef).isPresent());
        Assert.assertEquals("A file deletion request should be created in TODO state", FileRequestStatus.TODO,
                            fileDeletionRequestService.search(fileRef).get().getStatus());
        Mockito.clearInvocations(publisher);

        // Now schedule deletion jobs
        Collection<JobInfo> jobs = fileDeletionRequestService.scheduleDeletionJobs(FileRequestStatus.TODO,
                                                                                   Lists.newArrayList());
        runAndWaitJob(jobs);

        argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference should not belongs to owner anymore", FileReferenceEventState.FULLY_DELETED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getState());
    }

}
