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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.dto.FileStorageRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEventState;
import fr.cnes.regards.modules.storagelight.domain.flow.FileStorageFlowItem;
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
public class StoreFileFlowItemTest extends AbstractFileReferenceTest {

    @Autowired
    private StoreFileFlowItemHandler storeHandler;

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
     */
    @Test
    public void storeFileFlowItem() {
        String owner = "new-owner";
        String checksum = UUID.randomUUID().toString();
        String storage = "storage";
        // Create a new bus message File reference request
        FileStorageFlowItem item = FileStorageFlowItem
                .build(FileStorageRequestDTO.build("file.name", checksum, "MD5", "application/octet-stream", owner,
                                                   originUrl, ONLINE_CONF_LABEL, Optional.empty()),
                       UUID.randomUUID().toString());
        TenantWrapper<FileStorageFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        storeHandler.handleSync(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is not referenced yet
        Assert.assertFalse("File should not be referenced yet", fileRefService.search(storage, checksum).isPresent());
        // Check a file reference request is created
        Assert.assertTrue("File request should be created",
                          fileStorageRequestService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        // Now check for event published
        Mockito.verify(this.publisher, Mockito.times(0)).publish(Mockito.any(FileReferenceEvent.class));

        // SImulate job schedule
        Collection<JobInfo> jobs = fileStorageRequestService
                .scheduleJobs(FileRequestStatus.TODO, Lists.newArrayList(ONLINE_CONF_LABEL), Lists.newArrayList(owner));
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

    @Test
    public void storeFilesFlowItem() {
        // Create a new bus message File reference request
        Set<FileStorageRequestDTO> requests = Sets.newHashSet();
        String cs1 = UUID.randomUUID().toString();
        String cs2 = UUID.randomUUID().toString();
        requests.add(FileStorageRequestDTO.build("file.name", cs1, "MD5", "application/octet-stream", "owner",
                                                 originUrl, ONLINE_CONF_LABEL, Optional.empty()));
        requests.add(FileStorageRequestDTO.build("file.name", cs2, "MD5", "application/octet-stream", "owner",
                                                 originUrl, ONLINE_CONF_LABEL, Optional.empty()));
        FileStorageFlowItem item = FileStorageFlowItem.build(requests, UUID.randomUUID().toString());

        TenantWrapper<FileStorageFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        storeHandler.handleSync(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Check file is not referenced yet
        Assert.assertFalse("File should not be referenced yet",
                           fileRefService.search(ONLINE_CONF_LABEL, cs1).isPresent());
        Assert.assertFalse("File should not be referenced yet",
                           fileRefService.search(ONLINE_CONF_LABEL, cs2).isPresent());
        // Check a file reference request is created
        Optional<FileStorageRequest> req1 = fileStorageRequestService.search(ONLINE_CONF_LABEL, cs1);
        Optional<FileStorageRequest> req2 = fileStorageRequestService.search(ONLINE_CONF_LABEL, cs2);
        Assert.assertTrue("File request should be created", req1.isPresent());
        Assert.assertTrue("File request should be created", req2.isPresent());
        Assert.assertEquals("", req1.get().getRequestIds().stream().findFirst().get(),
                            req2.get().getRequestIds().stream().findFirst().get());

        // Now check for event published
        Mockito.verify(this.publisher, Mockito.times(0)).publish(Mockito.any(FileReferenceEvent.class));

        // Simulate job schedule
        Collection<JobInfo> jobs = fileStorageRequestService
                .scheduleJobs(FileRequestStatus.TODO, Lists.newArrayList(ONLINE_CONF_LABEL), Lists.newArrayList());
        runAndWaitJob(jobs);
        Assert.assertTrue("File should be referenced", fileRefService.search(ONLINE_CONF_LABEL, cs1).isPresent());
        Assert.assertTrue("File should be referenced", fileRefService.search(ONLINE_CONF_LABEL, cs2).isPresent());
        Assert.assertFalse("File request should be deleted",
                           fileStorageRequestService.search(ONLINE_CONF_LABEL, cs1).isPresent());
        Assert.assertFalse("File request should be deleted",
                           fileStorageRequestService.search(ONLINE_CONF_LABEL, cs2).isPresent());
        // Now check for event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published", FileReferenceEventState.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getState());

    }

    /**
     * Test request to reference and store a file. An error should be thrown as the destination storage is unknown
     * The file is not stored by the service as the origin storage and the destination storage are identical
     */
    @Test
    public void storeFileFlowItem_unknownStorage() {
        String checksum = UUID.randomUUID().toString();
        String storageDestination = "somewheere";
        // Create a new bus message File reference request
        FileStorageFlowItem item = FileStorageFlowItem
                .build(FileStorageRequestDTO.build("file.name", checksum, "MD5", "application/octet-stream",
                                                   "owner-test", originUrl, storageDestination, Optional.empty()),
                       UUID.randomUUID().toString());
        TenantWrapper<FileStorageFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        storeHandler.handleSync(wrapper);
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
     * Test request to reference and store a file. An error should be thrown during storage by plugin
     */
    @Test
    public void storeFileFlowItem_storeError() {
        String checksum = UUID.randomUUID().toString();
        // Create a new bus message File reference request
        FileStorageFlowItem item = FileStorageFlowItem
                .build(FileStorageRequestDTO.build("error.file.name", checksum, "MD5", "application/octet-stream",
                                                   "owner-test", originUrl, ONLINE_CONF_LABEL, Optional.empty()),
                       UUID.randomUUID().toString());
        TenantWrapper<FileStorageFlowItem> wrapper = new TenantWrapper<>(item, getDefaultTenant());
        // Publish request
        storeHandler.handleSync(wrapper);
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
        storeHandler.handleSync(wrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Only one file reference request in db, with status in todo, to allow retry
        Assert.assertTrue("File request still present",
                          fileStorageRequestService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        Assert.assertEquals("File request in TODO state", FileRequestStatus.TODO,
                            fileStorageRequestService.search(ONLINE_CONF_LABEL, checksum).get().getStatus());
    }

}
