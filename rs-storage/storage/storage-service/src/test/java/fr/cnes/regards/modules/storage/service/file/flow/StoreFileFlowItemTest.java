/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEventType;
import fr.cnes.regards.modules.storage.domain.flow.RetryFlowItem;
import fr.cnes.regards.modules.storage.domain.flow.StorageFlowItem;
import fr.cnes.regards.modules.storage.service.AbstractStorageTest;

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noschedule" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests",
        "regards.storage.cache.path=target/cache" }, locations = { "classpath:application-test.properties" })
public class StoreFileFlowItemTest extends AbstractStorageTest {

    @Autowired
    private StorageFlowItemHandler storeHandler;

    @Autowired
    private RetryFlowItemHandler retryHandler;

    @Before
    public void initialize() throws ModuleException {
        Mockito.clearInvocations(publisher);
        super.init();
    }

    @Test(expected = IllegalArgumentException.class)
    @Requirement("REGARDS_DSL_STO_AIP_080")
    @Purpose("Check that a storage request without checksum is denied")
    public void storeFileWithoutChecksum() {
        // Create a new bus message File reference request
        StorageFlowItem.build(
                              FileStorageRequestDTO.build("file.name", null, "MD5", "application/octet-stream", "owner",
                                                          originUrl, ONLINE_CONF_LABEL, Optional.empty()),
                              UUID.randomUUID().toString());
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
        StorageFlowItem item = StorageFlowItem
                .build(FileStorageRequestDTO.build("file.name", checksum, "MD5", "application/octet-stream", owner,
                                                   originUrl, ONLINE_CONF_LABEL, Optional.empty()),
                       UUID.randomUUID().toString());
        List<StorageFlowItem> items = new ArrayList<>();
        items.add(item);
        storeHandler.handleBatch(getDefaultTenant(), items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is not referenced yet
        Assert.assertFalse("File should not be referenced yet", fileRefService.search(storage, checksum).isPresent());
        // Check a file reference request is created
        Assert.assertEquals("File request should be created", 1,
                            stoReqService.search(ONLINE_CONF_LABEL, checksum).size());
        // Now check for event published
        Mockito.verify(this.publisher, Mockito.times(0)).publish(Mockito.any(FileReferenceEvent.class));

        // SImulate job schedule
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO,
                                                              Lists.newArrayList(ONLINE_CONF_LABEL),
                                                              Lists.newArrayList(owner));
        runAndWaitJob(jobs);
        Assert.assertTrue("File should be referenced", fileRefService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        Assert.assertTrue("File request should be deleted",
                          stoReqService.search(ONLINE_CONF_LABEL, checksum).isEmpty());
        // Now check for event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published", FileReferenceEventType.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
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
        StorageFlowItem item = StorageFlowItem.build(requests, UUID.randomUUID().toString());

        List<StorageFlowItem> items = new ArrayList<>();
        items.add(item);
        storeHandler.handleBatch(getDefaultTenant(), items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Check file is not referenced yet
        Assert.assertFalse("File should not be referenced yet",
                           fileRefService.search(ONLINE_CONF_LABEL, cs1).isPresent());
        Assert.assertFalse("File should not be referenced yet",
                           fileRefService.search(ONLINE_CONF_LABEL, cs2).isPresent());
        // Check a file reference request is created
        Collection<FileStorageRequest> storageReqs = stoReqService.search(ONLINE_CONF_LABEL, cs1);
        Collection<FileStorageRequest> storageReqs2 = stoReqService.search(ONLINE_CONF_LABEL, cs2);
        Assert.assertEquals("File request should be created", 1, storageReqs.size());
        Assert.assertEquals("File request should be created", 1, storageReqs2.size());
        Assert.assertEquals("", storageReqs.stream().findFirst().get().getGroupIds().stream().findFirst().get(),
                            storageReqs2.stream().findFirst().get().getGroupIds().stream().findFirst().get());

        // Now check for event published
        Mockito.verify(this.publisher, Mockito.times(0)).publish(Mockito.any(FileReferenceEvent.class));

        // Simulate job schedule
        Collection<JobInfo> jobs = stoReqService
                .scheduleJobs(FileRequestStatus.TO_DO, Lists.newArrayList(ONLINE_CONF_LABEL), Lists.newArrayList());
        runAndWaitJob(jobs);
        Assert.assertTrue("File should be referenced", fileRefService.search(ONLINE_CONF_LABEL, cs1).isPresent());
        Assert.assertTrue("File should be referenced", fileRefService.search(ONLINE_CONF_LABEL, cs2).isPresent());
        Assert.assertTrue("File request should be deleted", stoReqService.search(ONLINE_CONF_LABEL, cs1).isEmpty());
        Assert.assertTrue("File request should be deleted", stoReqService.search(ONLINE_CONF_LABEL, cs2).isEmpty());
        // Now check for event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(2)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published", FileReferenceEventType.STORED,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());

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
        StorageFlowItem item = StorageFlowItem
                .build(FileStorageRequestDTO.build("file.name", checksum, "MD5", "application/octet-stream",
                                                   "owner-test", originUrl, storageDestination, Optional.empty()),
                       UUID.randomUUID().toString());
        List<StorageFlowItem> items = new ArrayList<>();
        items.add(item);
        storeHandler.handleBatch(getDefaultTenant(), items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertFalse("File should not be referenced",
                           fileRefService.search(storageDestination, checksum).isPresent());
        // Now check for event published
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published", FileReferenceEventType.STORE_ERROR,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());
    }

    /**
     * Test request to reference and store a file. An error should be thrown during storage by plugin
     */
    @Test
    public void storeFileFlowItem_storeError() {
        String checksum = UUID.randomUUID().toString();
        // Create a new bus message File reference request
        StorageFlowItem item = StorageFlowItem
                .build(FileStorageRequestDTO.build("error.file.name", checksum, "MD5", "application/octet-stream",
                                                   "owner-test", originUrl, ONLINE_CONF_LABEL, Optional.empty()),
                       UUID.randomUUID().toString());
        List<StorageFlowItem> items = new ArrayList<>();
        items.add(item);
        storeHandler.handleBatch(getDefaultTenant(), items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check file is well referenced
        Assert.assertFalse("File should not be referenced",
                           fileRefService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        // Now check for event published
        Mockito.verify(publisher, Mockito.times(0)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.clearInvocations(publisher);

        // Simulate job schedule
        Collection<JobInfo> jobs = stoReqService
                .scheduleJobs(FileRequestStatus.TO_DO, Lists.newArrayList(ONLINE_CONF_LABEL), Lists.newArrayList());
        runAndWaitJob(jobs);

        Assert.assertFalse("File should not be referenced",
                           fileRefService.search(ONLINE_CONF_LABEL, checksum).isPresent());
        Assert.assertEquals("File request should be still present", 1,
                            stoReqService.search(ONLINE_CONF_LABEL, checksum).size());
        Assert.assertEquals("File request should be in ERROR state", FileRequestStatus.ERROR,
                            stoReqService.search(ONLINE_CONF_LABEL, checksum).stream().findFirst().get().getStatus());
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Assert.assertEquals("File reference event STORED should be published", FileReferenceEventType.STORE_ERROR,
                            getFileReferenceEvent(argumentCaptor.getAllValues()).getType());

        Assert.assertEquals("File request still present", 1, stoReqService.search(ONLINE_CONF_LABEL, checksum).size());
        Assert.assertEquals("File request in ERROR state", FileRequestStatus.ERROR,
                            stoReqService.search(ONLINE_CONF_LABEL, checksum).stream().findFirst().get().getStatus());

        // Retry same storage request
        storeHandler.handleBatch(getDefaultTenant(), items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // There should be one storage request. Same as previous error one but updated to to_do thanks to new request
        Collection<FileStorageRequest> storeRequests = stoReqService.search(ONLINE_CONF_LABEL, checksum);
        Assert.assertEquals("File request still present", 1, storeRequests.size());
        // One in TO_DO state
        Assert.assertEquals("There should be one request in TO_DO state", 1L,
                            storeRequests.stream().filter(r -> r.getStatus() == FileRequestStatus.TO_DO).count());
    }

    @Test
    public void retry_byGroupId() {
        String storageDestination = "somewheere";
        String owner = "retry-test";
        Set<FileStorageRequestDTO> files = Sets.newHashSet();
        // Create a new bus message File reference request
        files.add(FileStorageRequestDTO.build("file1.test", UUID.randomUUID().toString(), "MD5",
                                              "application/octet-stream", owner, originUrl, storageDestination,
                                              Optional.empty()));
        files.add(FileStorageRequestDTO.build("file2.test", UUID.randomUUID().toString(), "MD5",
                                              "application/octet-stream", owner, originUrl, storageDestination,
                                              Optional.empty()));
        files.add(FileStorageRequestDTO.build("file3.test", UUID.randomUUID().toString(), "MD5",
                                              "application/octet-stream", owner, originUrl, storageDestination,
                                              Optional.empty()));
        StorageFlowItem item = StorageFlowItem.build(files, UUID.randomUUID().toString());
        List<StorageFlowItem> items = new ArrayList<>();
        items.add(item);
        storeHandler.handleBatch(getDefaultTenant(), items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check request in error
        Page<FileStorageRequest> requests = fileStorageRequestRepo
                .findByOwnersInAndStatus(Lists.newArrayList(owner), FileRequestStatus.ERROR, PageRequest.of(0, 1_000));
        Assert.assertEquals("The 3 requests should be in error", 3, requests.getTotalElements());

        RetryFlowItem retry = RetryFlowItem.buildStorageRetry(Lists.newArrayList(owner));
        TenantWrapper<RetryFlowItem> retryWrapper = TenantWrapper.build(retry, getDefaultTenant());
        retryHandler.handle(retryWrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Check request in {@link FileRequestStatus#TO_DO}
        requests = fileStorageRequestRepo.findByOwnersInAndStatus(Lists.newArrayList(owner), FileRequestStatus.TO_DO,
                                                                  PageRequest.of(0, 1_000));
        Assert.assertEquals("The 3 requests should be in TO_DO", 3, requests.getTotalElements());

        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO, Lists.newArrayList(),
                                                              Lists.newArrayList());
        runAndWaitJob(jobs);

        requests = fileStorageRequestRepo.findByOwnersInAndStatus(Lists.newArrayList(owner), FileRequestStatus.ERROR,
                                                                  PageRequest.of(0, 1_000));
        Assert.assertEquals("The 3 requests should be in error again", 3, requests.getTotalElements());

    }

    @Test
    public void retry_byOwners() {
        String storageDestination = "somewheere";
        List<String> owners = Lists.newArrayList("retry-test", "retry-test-2", "retry-test-3");
        Set<FileStorageRequestDTO> files = Sets.newHashSet();
        // Create a new bus message File reference request
        files.add(FileStorageRequestDTO.build("file1.test", UUID.randomUUID().toString(), "MD5",
                                              "application/octet-stream", owners.get(0), originUrl, storageDestination,
                                              Optional.empty()));
        files.add(FileStorageRequestDTO.build("file2.test", UUID.randomUUID().toString(), "MD5",
                                              "application/octet-stream", owners.get(1), originUrl, storageDestination,
                                              Optional.empty()));
        files.add(FileStorageRequestDTO.build("file3.test", UUID.randomUUID().toString(), "MD5",
                                              "application/octet-stream", owners.get(2), originUrl, storageDestination,
                                              Optional.empty()));
        StorageFlowItem item = StorageFlowItem.build(files, UUID.randomUUID().toString());
        List<StorageFlowItem> items = new ArrayList<>();
        items.add(item);
        storeHandler.handleBatch(getDefaultTenant(), items);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Check request in error
        Page<FileStorageRequest> requests = fileStorageRequestRepo
                .findByOwnersInAndStatus(owners, FileRequestStatus.ERROR, PageRequest.of(0, 1_000));
        Assert.assertEquals("The 3 requests should be in error", 3, requests.getTotalElements());

        RetryFlowItem retry = RetryFlowItem.buildStorageRetry(owners);
        TenantWrapper<RetryFlowItem> retryWrapper = TenantWrapper.build(retry, getDefaultTenant());
        retryHandler.handle(retryWrapper);
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Check request in {@link FileRequestStatus#TO_DO}
        requests = fileStorageRequestRepo.findByOwnersInAndStatus(owners, FileRequestStatus.TO_DO,
                                                                  PageRequest.of(0, 1_000));
        Assert.assertEquals("The 3 requests should be in TO_DO", 3, requests.getTotalElements());

        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO, Lists.newArrayList(),
                                                              Lists.newArrayList());
        runAndWaitJob(jobs);

        requests = fileStorageRequestRepo.findByOwnersInAndStatus(owners, FileRequestStatus.ERROR,
                                                                  PageRequest.of(0, 1_000));
        Assert.assertEquals("The 3 requests should be in error again", 3, requests.getTotalElements());

    }

}
