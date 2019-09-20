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
package fr.cnes.regards.modules.storagelight.service.file.request;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.storagelight.domain.database.CacheFile;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileCopyRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.service.file.AbstractStorageTest;

/**
 * @author sbinda
 *
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_copy_tests",
        "regards.storage.cache.path=target/cache" })
public class FileCopyRequestServiceTest extends AbstractStorageTest {

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();
    }

    @Test
    public void copyFile() throws InterruptedException, ExecutionException {
        FileReference fileRef = this.generateRandomStoredNearlineFileReference("file1.test");
        Set<FileCopyRequestDTO> requests = Sets
                .newHashSet(FileCopyRequestDTO.build(fileRef.getMetaInfo().getChecksum(), ONLINE_CONF_LABEL));
        fileCopyRequestService.handle(requests, UUID.randomUUID().toString());
        // A new copy request should be created
        Optional<FileCopyRequest> oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(),
                                                                       ONLINE_CONF_LABEL);
        Assert.assertTrue("There should be a copy request created", oReq.isPresent());

        // Now run copy schedule
        fileCopyRequestService.scheduleCopyRequests(FileRequestStatus.TODO);

        // There should be one availability request created
        Optional<FileCacheRequest> oCacheReq = fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum());
        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), ONLINE_CONF_LABEL);
        Assert.assertTrue("There should be a cache request created", oCacheReq.isPresent());
        Assert.assertTrue("No storage request should be created yet", fileStorageRequestRepo.count() == 0);
        Assert.assertTrue("There should be a copy request", oReq.isPresent());
        Assert.assertTrue("There should be a copy request in pending state",
                          oReq.get().getStatus() == FileRequestStatus.PENDING);

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TODO);
        runAndWaitJob(jobs);

        // Cache file should be restored
        oCacheReq = fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum());
        Optional<CacheFile> oCachedFile = cacheService.search(fileRef.getMetaInfo().getChecksum());
        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), ONLINE_CONF_LABEL);
        Assert.assertFalse("There should not be a cache request anymore", oCacheReq.isPresent());
        Assert.assertTrue("The file should be restored in  cache", oCachedFile.isPresent());
        Assert.assertTrue("There should be a copy request", oReq.isPresent());
        Assert.assertTrue("There should be a copy request in pending state",
                          oReq.get().getStatus() == FileRequestStatus.PENDING);

        // Simulate send of the event
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        FileReferenceEvent event = getFileReferenceEvent(argumentCaptor.getAllValues());

        fileRefEventHandler.handle(new TenantWrapper<>(event, getDefaultTenant()));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // A new storage request should be created
        Optional<FileStorageRequest> oStorageReq = stoReqService.search(ONLINE_CONF_LABEL,
                                                                        fileRef.getMetaInfo().getChecksum());
        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), ONLINE_CONF_LABEL);
        Assert.assertTrue("There should be a storage request created", oStorageReq.isPresent());
        Assert.assertTrue("There should be a copy request", oReq.isPresent());
        Assert.assertTrue("There should be a copy request in pending state",
                          oReq.get().getStatus() == FileRequestStatus.PENDING);

        // Run storage job
        Mockito.reset(publisher);
        jobs = stoReqService.scheduleJobs(FileRequestStatus.TODO, Lists.newArrayList(), Lists.newArrayList());
        runAndWaitJob(jobs);
        Optional<FileStorageRequest> oFileRefReq = stoReqService.search(ONLINE_CONF_LABEL,
                                                                        fileRef.getMetaInfo().getChecksum());
        Optional<FileReference> oFileRef = fileRefService.search(ONLINE_CONF_LABEL,
                                                                 fileRef.getMetaInfo().getChecksum());
        Assert.assertTrue("File reference should have been created.", oFileRef.isPresent());
        Assert.assertFalse("File reference request should not exists anymore", oFileRefReq.isPresent());
        Assert.assertTrue("There should be a copy request", oReq.isPresent());
        Assert.assertTrue("There should be a copy request in pending state",
                          oReq.get().getStatus() == FileRequestStatus.PENDING);

        // Simulate file  stored event
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        event = getFileReferenceEvent(argumentCaptor.getAllValues());
        fileRefEventHandler.handle(new TenantWrapper<>(event, getDefaultTenant()));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), ONLINE_CONF_LABEL);
        Assert.assertFalse("There should not be a copy request anymore", oReq.isPresent());

        // File should not be in cache anymore
        oCachedFile = cacheService.search(fileRef.getMetaInfo().getChecksum());
        Assert.assertFalse("The cache file should be deleted after copy", oCachedFile.isPresent());
    }

    @Test
    public void copyFile_error_offlineFile() {
        String storage = "somewhere";
        FileReference fileRef = referenceRandomFile("owner", "type", "file1.test", storage).get();
        Set<FileCopyRequestDTO> requests = Sets
                .newHashSet(FileCopyRequestDTO.build(fileRef.getMetaInfo().getChecksum(), storage));
        fileCopyRequestService.handle(requests, UUID.randomUUID().toString());
        Optional<FileCopyRequest> oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), storage);
        Assert.assertTrue("There should be a copy request created", oReq.isPresent());

        // Now run copy schedule
        fileCopyRequestService.scheduleCopyRequests(FileRequestStatus.TODO);

        // There should be one availability request created
        Optional<FileCacheRequest> oCacheReq = fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum());
        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), storage);
        Assert.assertTrue("There should be a cache request created", oCacheReq.isPresent());
        Assert.assertTrue("No storage request should be created yet", fileStorageRequestRepo.count() == 0);
        Assert.assertTrue("There should be a copy request", oReq.isPresent());
        Assert.assertTrue("There should be a copy request in pending state",
                          oReq.get().getStatus() == FileRequestStatus.PENDING);

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TODO);
        runAndWaitJob(jobs);

        oCacheReq = fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum());
        Assert.assertTrue("There should be a cache request in error state",
                          oCacheReq.get().getStatus() == FileRequestStatus.ERROR);

        // Simulate send of the event
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        FileReferenceEvent event = getFileReferenceEvent(argumentCaptor.getAllValues());
        fileRefEventHandler.handle(new TenantWrapper<>(event, getDefaultTenant()));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Copy request should be updated in ERROR
        oReq = fileCopyRequestService.search(fileRef.getMetaInfo().getChecksum(), storage);
        Assert.assertTrue("There should be a copy request in error state",
                          oReq.get().getStatus() == FileRequestStatus.ERROR);
    }

    @Test
    public void copyFile_error_unknownFile() {
        String storage = "somewhere";
        String unknownChecksum = UUID.randomUUID().toString();
        Set<FileCopyRequestDTO> requests = Sets.newHashSet(FileCopyRequestDTO.build(unknownChecksum, storage));
        fileCopyRequestService.handle(requests, UUID.randomUUID().toString());
        Optional<FileCopyRequest> oReq = fileCopyRequestService.search(unknownChecksum, storage);
        Assert.assertFalse("There should not be a copy request created", oReq.isPresent());
    }

}
