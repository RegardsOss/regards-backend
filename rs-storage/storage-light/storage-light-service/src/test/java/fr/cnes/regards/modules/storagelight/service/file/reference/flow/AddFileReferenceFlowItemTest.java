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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import fr.cnes.regards.modules.storagelight.domain.flow.AddFileRefFlowItem;
import fr.cnes.regards.modules.storagelight.service.file.reference.AbstractFileReferenceTest;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileStorageRequestService;

/**
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests",
        "regards.storage.cache.path=target/cache" })
public class AddFileReferenceFlowItemTest extends AbstractFileReferenceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddFileReferenceFlowItemTest.class);

    @Autowired
    private AddFileReferenceFlowItemHandler handler;

    @Autowired
    FileReferenceService fileRefService;

    @Autowired
    FileStorageRequestService fileStorageRequestService;

    @SpyBean
    public IPublisher publisher;

    @Before
    public void initialize() throws ModuleException {
        Mockito.clearInvocations(publisher);
        super.init();
    }

    /**
     * Test request to reference a file already stored.
     * The file is not stored by the service as the origin storage and the destination storage are identical
     * @throws InterruptedException
     */
    @Test
    public void addFileRefFlowItem() throws InterruptedException {
        String checksum = UUID.randomUUID().toString();
        String storage = "storage";
        // Create a new bus message File reference request
        AddFileRefFlowItem item = new AddFileRefFlowItem("file.name", checksum, "MD5", "application/octet-stream", 10L,
                "owner-test", storage, "file://storage/location/file.name", storage,
                "file://storage/location/file.name");
        TenantWrapper<AddFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        long start = System.currentTimeMillis();
        // Publish request
        handler.handleSync(wrapper);
        long finish = System.currentTimeMillis();
        LOGGER.info("Add file reference duration {}ms", finish - start);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertTrue("File should be referenced", fileRefService.search(storage, checksum).isPresent());
        // Now check for event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published", FileReferenceEventState.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getState());
    }

    /**
     * Test request to reference a file already stored.
     * The file is not stored by the service as the origin storage and the destination storage are identical
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void addFileRefFlowItemAlreadyExists() throws InterruptedException, ExecutionException {
        String checksum = UUID.randomUUID().toString();
        String owner = "new-owner";
        FileReference fileRef = this.generateStoredFileReference(checksum, owner, "file.test", ONLINE_CONF_LABEL);
        String storage = fileRef.getLocation().getStorage();
        // Create a new bus message File reference request
        AddFileRefFlowItem item = new AddFileRefFlowItem("file.name", checksum, "MD5", "application/octet-stream", 10L,
                "owner-test", storage, "file://storage/location/file.name", storage,
                "file://storage/location/file.name");
        TenantWrapper<AddFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        handler.handleSync(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertTrue("File should be referenced", fileRefService.search(storage, checksum).isPresent());
        // Now check for event published. One for each referenced file
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published", FileReferenceEventState.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getState());
    }

    /**
     * Test request to reference a file already stored.
     * The file is not stored by the service as the origin storage and the destination storage are identical
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void addFileRefFlowItemWithSameChecksum() throws InterruptedException, ExecutionException {
        String checksum = UUID.randomUUID().toString();
        String owner = "new-owner";
        String storage = "aStorage";
        this.generateStoredFileReference(checksum, owner, "file.test", ONLINE_CONF_LABEL);
        // Create a new bus message File reference request
        AddFileRefFlowItem item = new AddFileRefFlowItem("file.name", checksum, "MD5", "application/octet-stream", 10L,
                "owner-test", storage, "file://storage/location/file.name", storage,
                "file://storage/location/file.name");
        TenantWrapper<AddFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        handler.handleSync(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertTrue("File should be referenced", fileRefService.search(storage, checksum).isPresent());
        // Now check for event published. One for each referenced file
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published", FileReferenceEventState.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getState());
    }

    /**
     * Test request to reference a file already stored.
     * The file is not stored by the service as the origin storage and the destination storage are identical
     */
    @Test
    public void addFileRefFlowItemWithStorage() {
        String owner = "new-owner";
        String checksum = UUID.randomUUID().toString();
        String storage = "storage";
        // Create a new bus message File reference request
        AddFileRefFlowItem item = new AddFileRefFlowItem("file.name", checksum, "MD5", "application/octet-stream", 10L,
                owner, storage, "file://storage/location/file.name", ONLINE_CONF_LABEL);
        TenantWrapper<AddFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        handler.handleSync(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is not referenced yet
        Assert.assertFalse("File should not be referenced yet", fileRefService.search(storage, checksum).isPresent());
        // Check a file reference request is created
        Assert.assertTrue("File request should be created",
                          fileStorageRequestService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        // Now check for event published
        Mockito.verify(this.publisher, Mockito.times(0)).publish(Mockito.any(FileReferenceEvent.class));

        // SImulate job schedule
        Collection<JobInfo> jobs = fileStorageRequestService.scheduleJobs(FileRequestStatus.TODO,
                                                                               Lists.newArrayList(ONLINE_CONF_LABEL),
                                                                               Lists.newArrayList(owner));
        runAndWaitJob(jobs);
        Assert.assertTrue("File should be referenced", fileRefService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        Assert.assertFalse("File request should be deleted",
                           fileStorageRequestService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        // Now check for event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published", FileReferenceEventState.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getState());
    }

    /**
     * Test request to reference and store a file. An error should be thrown as the destination storage is unknown
     * The file is not stored by the service as the origin storage and the destination storage are identical
     */
    @Test
    public void addFileRefFlowItemError() {
        String checksum = UUID.randomUUID().toString();
        String storage = "storage";
        String storageDestination = "somewheere";
        // Create a new bus message File reference request
        AddFileRefFlowItem item = new AddFileRefFlowItem("file.name", checksum, "MD5", "application/octet-stream", 10L,
                "owner-test", storage, "file://storage/location/file.name", storageDestination);
        TenantWrapper<AddFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        handler.handleSync(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertFalse("File should not be referenced",
                           fileRefService.search(storageDestination, checksum).isPresent());
        // Now check for event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published", FileReferenceEventState.STORE_ERROR,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getState());
    }

    /**
     * Test request to reference and store a file. An error should be thrown as the destination storage is unknown
     * The file is not stored by the service as the origin storage and the destination storage are identical
     */
    @Test
    public void addFileRefFlowItemStoreError() {
        String checksum = UUID.randomUUID().toString();
        String storage = "storage";
        // Create a new bus message File reference request
        AddFileRefFlowItem item = new AddFileRefFlowItem("error.file.name", checksum, "MD5", "application/octet-stream",
                10L, "owner-test", storage, "file://storage/location/file.name", ONLINE_CONF_LABEL);
        TenantWrapper<AddFileRefFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        handler.handleSync(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertFalse("File should not be referenced",
                           fileRefService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        // Now check for event published
        Mockito.verify(publisher, Mockito.times(0)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.clearInvocations(publisher);

        // Simulate job schedule
        Collection<JobInfo> jobs = fileStorageRequestService
                .scheduleJobs(FileRequestStatus.TODO, Lists.newArrayList(ONLINE_CONF_LABEL), Lists.newArrayList());
        runAndWaitJob(jobs);

        Assert.assertFalse("File should not be referenced",
                           fileRefService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        Assert.assertTrue("File request should be still present",
                          fileStorageRequestService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        Assert.assertEquals("File request should be in ERROR state", FileRequestStatus.ERROR,
                            fileStorageRequestService.search(ONLINE_CONF_LABEL, checksum).get().getStatus());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published", FileReferenceEventState.STORE_ERROR,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getState());

        Assert.assertTrue("File request still present",
                          fileStorageRequestService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        Assert.assertEquals("File request in ERROR state", FileRequestStatus.ERROR,
                            fileStorageRequestService.search(ONLINE_CONF_LABEL, checksum).get().getStatus());

        // Retry same storage request
        handler.handleSync(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Only one file reference request in db, with status in todo, to allow retry
        Assert.assertTrue("File request still present",
                          fileStorageRequestService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        Assert.assertEquals("File request in TODO state", FileRequestStatus.TODO,
                            fileStorageRequestService.search(ONLINE_CONF_LABEL, checksum).get().getStatus());
    }

}
