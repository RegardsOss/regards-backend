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

import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEventType;
import fr.cnes.regards.modules.storagelight.domain.flow.AvailabilityFileRefFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.RetryFlowItem;
import fr.cnes.regards.modules.storagelight.service.file.reference.AbstractFileReferenceTest;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileStorageRequestService;

/**
 * @author sbinda
 *
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_availability_tests",
        "regards.storage.cache.path=target/cache" })
public class AvailabilityFileReferenceFlowItemTest extends AbstractFileReferenceTest {

    @Autowired
    private AvailabilityFileFlowItemHandler handler;

    @Autowired
    private RetryFlowItemHandler retryHandler;

    @Autowired
    FileReferenceService fileRefService;

    @Autowired
    FileStorageRequestService fileStorageRequestService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Before
    public void initialize() throws ModuleException {
        Mockito.clearInvocations(publisher);
        super.init();
    }

    @Test
    public void availabilityFlowItem() throws InterruptedException, ExecutionException {

        // Simulate storage of 3 files in a near line location
        FileReference file1 = this.generateRandomStoredNearlineFileReference("file.nearline.1.test");
        FileReference file2 = this.generateRandomStoredNearlineFileReference("file.nearline.2.test");
        FileReference file3 = this.generateRandomStoredNearlineFileReference("file.nearline.3.test");
        // Simulate storage of 2 files in an online location
        FileReference file4 = this.generateRandomStoredOnlineFileReference("file.online.1.test");
        FileReference file5 = this.generateRandomStoredOnlineFileReference("file.online.2.test");
        // Simulate reference of 2 files offline
        FileReference file6 = this.referenceRandomFile("owner", "file", "file.offline.1.test", "somewhere").get();
        FileReference file7 = this.referenceRandomFile("owner", "file", "file.offline.2.test", "somewhere-else").get();
        // Simulate storage of a file in two locations near line and online
        String checksum = UUID.randomUUID().toString();
        this.generateStoredFileReference(checksum, "owner", "file.online.nealine.test", ONLINE_CONF_LABEL);
        this.generateStoredFileReference(checksum, "owner", "file.online.nealine.test", NEARLINE_CONF_LABEL);

        Set<String> checksums = Sets.newHashSet(file1.getMetaInfo().getChecksum(), file2.getMetaInfo().getChecksum(),
                                                file3.getMetaInfo().getChecksum(), file4.getMetaInfo().getChecksum(),
                                                file5.getMetaInfo().getChecksum(), file6.getMetaInfo().getChecksum(),
                                                file7.getMetaInfo().getChecksum(), checksum);
        Mockito.clearInvocations(publisher);
        AvailabilityFileRefFlowItem request = AvailabilityFileRefFlowItem
                .build(checksums, OffsetDateTime.now().plusDays(1), UUID.randomUUID().toString());
        handler.handle(new TenantWrapper<>(request, this.getDefaultTenant()));
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());

        // there should be 2 notification error for availability of offline files
        // There should be 3 notification for online files available
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(5)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Set<String> availables = Sets.newHashSet();
        Set<String> notAvailables = Sets.newHashSet();
        for (FileReferenceEvent evt : getFileReferenceEvents(argumentCaptor.getAllValues())) {
            if (evt.getType() == FileReferenceEventType.AVAILABLE) {
                availables.add(evt.getChecksum());
            } else if (evt.getType() == FileReferenceEventType.AVAILABILITY_ERROR) {
                notAvailables.add(evt.getChecksum());
            }
        }
        Assert.assertEquals("There should be 3 files availables", 3, availables.size());
        Assert.assertTrue("File should be available as it is online",
                          availables.contains(file4.getMetaInfo().getChecksum()));
        Assert.assertTrue("File should be available as it is online",
                          availables.contains(file5.getMetaInfo().getChecksum()));
        Assert.assertTrue("File should be available as it is online", availables.contains(checksum));
        Assert.assertEquals("There should be 2 files not availables", 2, notAvailables.size());
        Assert.assertTrue("File should be unavailable as it is offline",
                          notAvailables.contains(file6.getMetaInfo().getChecksum()));
        Assert.assertTrue("File should be unavailable as it is offline",
                          notAvailables.contains(file7.getMetaInfo().getChecksum()));

        // There should be 3 cache request for the 3 files only in near line.
        // The file stored in two locations online and near line does not need to be restored
        // as its available online.
        Assert.assertTrue("A cache request should be done for near line file 1",
                          fileCacheRequestService.search(file1.getMetaInfo().getChecksum()).isPresent());
        Assert.assertTrue("A cache request should be done for near line file 2",
                          fileCacheRequestService.search(file2.getMetaInfo().getChecksum()).isPresent());
        Assert.assertTrue("A cache request should be done for near line file 3",
                          fileCacheRequestService.search(file3.getMetaInfo().getChecksum()).isPresent());
        Assert.assertFalse("A cache request should not be done for near line file 4 as it is online too",
                           fileCacheRequestService.search(checksum).isPresent());
    }

    @Test
    public void availabilityWithCacheFile() throws InterruptedException, ExecutionException, MalformedURLException {
        // Simulate file storage on a near line location
        FileReference file1 = this.generateRandomStoredNearlineFileReference("file.nearline.1.test");
        // Simulate file in cache
        cacheService.addFile(file1.getMetaInfo().getChecksum(), 123L,
                             new URL("file", null, "target/cache/test/file.nearline.1.test"),
                             OffsetDateTime.now().plusDays(1), UUID.randomUUID().toString());
        // Simulate availability request on this file
        Mockito.clearInvocations(publisher);
        AvailabilityFileRefFlowItem request = AvailabilityFileRefFlowItem
                .build(Sets.newHashSet(file1.getMetaInfo().getChecksum()), OffsetDateTime.now().plusDays(2),
                       UUID.randomUUID().toString());
        handler.handle(new TenantWrapper<>(request, this.getDefaultTenant()));
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());

        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());

        FileReferenceEvent event = this.getFileReferenceEvent(argumentCaptor.getAllValues());
        Assert.assertEquals("File should be available", FileReferenceEventType.AVAILABLE, event.getType());
        Assert.assertEquals("File available is not the requested one", file1.getMetaInfo().getChecksum(),
                            event.getChecksum());
        Assert.assertFalse("No cache request should be created as file is in cache",
                           fileCacheRequestService.search(file1.getMetaInfo().getChecksum()).isPresent());
    }

    @Test
    public void availability() throws InterruptedException, ExecutionException {

        // Simulate storage of 3 files in a near line location with restore error
        FileReference file1 = this.generateRandomStoredNearlineFileReference("restoError.file1.test");
        FileReference file2 = this.generateRandomStoredNearlineFileReference("restoError.file1.test");
        FileReference file3 = this.generateRandomStoredNearlineFileReference("restoError.file1.test");
        FileReference file4 = this.generateRandomStoredNearlineFileReference("file4.test");
        Mockito.clearInvocations(publisher);

        Set<String> checksums = Sets.newHashSet(file1.getMetaInfo().getChecksum(), file2.getMetaInfo().getChecksum(),
                                                file3.getMetaInfo().getChecksum(), file4.getMetaInfo().getChecksum());

        String requestId = UUID.randomUUID().toString();
        AvailabilityFileRefFlowItem request = AvailabilityFileRefFlowItem
                .build(checksums, OffsetDateTime.now().plusDays(1), requestId);
        handler.handle(new TenantWrapper<>(request, this.getDefaultTenant()));
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());

        Assert.assertEquals("There should be 4 cache requests created", 4,
                            fileCacheReqRepo.findByRequestIdAndStatus(requestId, FileRequestStatus.TODO).size());

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TODO);
        runAndWaitJob(jobs);

        runtimeTenantResolver.forceTenant(this.getDefaultTenant());
        Assert.assertEquals("There should be 0 cache requests in TODO", 0,
                            fileCacheReqRepo.findByRequestIdAndStatus(requestId, FileRequestStatus.TODO).size());
        Assert.assertEquals("There should be 3 cache requests in ERROR", 3,
                            fileCacheReqRepo.findByRequestIdAndStatus(requestId, FileRequestStatus.ERROR).size());
        Assert.assertEquals("There should be 1 file cache requests", 1, cacheFileRepo
                .findAllByChecksumIn(Sets.newHashSet(file4.getMetaInfo().getChecksum())).size());

        // there should be 3 notification error for availability of offline files
        // There should be 1 notification for available file
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(4)).publish(Mockito.any(FileReferenceEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        Set<String> availables = Sets.newHashSet();
        Set<String> notAvailables = Sets.newHashSet();
        for (FileReferenceEvent evt : getFileReferenceEvents(argumentCaptor.getAllValues())) {
            if (evt.getType() == FileReferenceEventType.AVAILABLE) {
                availables.add(evt.getChecksum());
            } else if (evt.getType() == FileReferenceEventType.AVAILABILITY_ERROR) {
                notAvailables.add(evt.getChecksum());
            }
        }
        Assert.assertEquals("There should be 1 files available", 1, availables.size());
        Assert.assertEquals("There should be 3 files error", 3, notAvailables.size());

        retryHandler.handle(new TenantWrapper<RetryFlowItem>(RetryFlowItem.buildAvailabilityRetry(requestId),
                getDefaultTenant()));

        runtimeTenantResolver.forceTenant(this.getDefaultTenant());
        Assert.assertEquals("There should be 3 cache requests in TODO", 3,
                            fileCacheReqRepo.findByRequestIdAndStatus(requestId, FileRequestStatus.TODO).size());
        Assert.assertEquals("There should be 0 cache requests in ERROR", 0,
                            fileCacheReqRepo.findByRequestIdAndStatus(requestId, FileRequestStatus.ERROR).size());
    }

}
