/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.urn.DataType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEventType;
import fr.cnes.regards.modules.storage.domain.flow.AvailabilityFlowItem;
import fr.cnes.regards.modules.storage.domain.flow.RetryFlowItem;
import fr.cnes.regards.modules.storage.service.AbstractStorageTest;
import fr.cnes.regards.modules.storage.service.file.request.FileReferenceRequestService;
import fr.cnes.regards.modules.storage.service.file.request.FileStorageRequestService;

/**
 * Test class
 *
 * @author Sébastien Binda
 *
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_availability_tests" }, locations = { "classpath:application-test.properties" })
public class AvailabilityFileReferenceFlowItemTest extends AbstractStorageTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvailabilityFileReferenceFlowItemTest.class);

    private static final  String SESSION_OWNER_1 = "SOURCE 1";

    private static final String SESSION_1 = "SESSION 1";

    @Autowired
    private AvailabilityFlowItemHandler handler;

    @Autowired
    private RetryFlowItemHandler retryHandler;

    @Autowired
    FileReferenceRequestService fileRefService;

    @Autowired
    FileStorageRequestService fileStorageRequestService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IDynamicTenantSettingService dynamicTenantSettingService;

    @Before
    public void initialize() throws ModuleException {
        Mockito.clearInvocations(publisher);
        super.init();
        simulateApplicationStartedEvent();
        simulateApplicationReadyEvent();
        // we override cache setting values for tests
        dynamicTenantSettingService.update(StorageSetting.CACHE_PATH_NAME, Paths.get("target", "cache", getDefaultTenant()));
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_140")
    @Purpose("Check that a availability request is well handled when a new bus message is received")
    public void availabilityFlowItem() throws InterruptedException, ExecutionException {

        LOGGER.info("--> availabilityFlowItem");
        // Simulate storage of 3 files in a near line location
        FileReference file1 = this.generateRandomStoredNearlineFileReference("file.nearline.1.test", Optional.empty());
        FileReference file2 = this.generateRandomStoredNearlineFileReference("file.nearline.2.test", Optional.empty());
        FileReference file3 = this.generateRandomStoredNearlineFileReference("file.nearline.3.test", Optional.empty());
        // Simulate storage of 2 files in an online location
        FileReference file4 = this.generateRandomStoredOnlineFileReference("file.online.1.test", Optional.empty());
        FileReference file5 = this.generateRandomStoredOnlineFileReference("file.online.2.test", Optional.empty());
        // Simulate reference of 2 files offline
        FileReference file6 = this.referenceRandomFile("owner", "file", "file.offline.1.test", "somewhere",
                                                       SESSION_OWNER_1, SESSION_1).get();
        FileReference file7 = this.referenceRandomFile("owner", "file", "file.offline.2.test", "somewhere-else",
                                                       SESSION_OWNER_1, SESSION_1).get();
        // Simulate storage of a file in two locations near line and online
        String checksum = UUID.randomUUID().toString();
        this.generateStoredFileReference(checksum, "owner", "file.online.nealine.test", ONLINE_CONF_LABEL,
                                         Optional.empty(), Optional.empty(), SESSION_OWNER_1, SESSION_1);
        this.generateStoredFileReference(checksum, "owner", "file.online.nealine.test", NEARLINE_CONF_LABEL,
                                         Optional.empty(), Optional.empty(), SESSION_OWNER_1, SESSION_1);

        Set<String> checksums = Sets.newHashSet(file1.getMetaInfo().getChecksum(), file2.getMetaInfo().getChecksum(),
                                                file3.getMetaInfo().getChecksum(), file4.getMetaInfo().getChecksum(),
                                                file5.getMetaInfo().getChecksum(), file6.getMetaInfo().getChecksum(),
                                                file7.getMetaInfo().getChecksum(), checksum);
        Mockito.clearInvocations(publisher);
        String groupId = UUID.randomUUID().toString();
        AvailabilityFlowItem request = AvailabilityFlowItem.build(checksums, OffsetDateTime.now().plusDays(1), groupId);
        List<AvailabilityFlowItem> items = new ArrayList<>();
        items.add(request);
        handler.handleBatch(getDefaultTenant(), items);
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());

        // There should be 5 cache request for the 3 files only in near line and 2 files offline.
        // The file stored in two locations online and near line does not need to be restored
        // as its available online.
        Assert.assertEquals("There should be 5 cache requests created", 5,
                            fileCacheReqRepo.findByGroupIdAndStatus(groupId, FileRequestStatus.TO_DO).size());
        Assert.assertTrue("A cache request should be done for near line file 1",
                          fileCacheRequestService.search(file1.getMetaInfo().getChecksum()).isPresent());
        Assert.assertTrue("A cache request should be done for near line file 2",
                          fileCacheRequestService.search(file2.getMetaInfo().getChecksum()).isPresent());
        Assert.assertTrue("A cache request should be done for near line file 3",
                          fileCacheRequestService.search(file3.getMetaInfo().getChecksum()).isPresent());
        Assert.assertFalse("A cache request should not be done for near line file 4 as it is online too",
                           fileCacheRequestService.search(checksum).isPresent());

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        // there should be 2 notification error for availability of offline files
        // There should be 3 notification for near line files available
        // There should be 3 notification for online files available
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(8)).publish(Mockito.any(FileReferenceEvent.class));
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
        Assert.assertEquals("There should be 6 files availables", 6, availables.size());
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
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_140")
    @Purpose("Check that a availability request is well handled when a new bus message is received")
    public void availabilityWithCacheFile() throws InterruptedException, ExecutionException, MalformedURLException {
        LOGGER.info("--> availabilityWithCacheFile");
        // Simulate file storage on a near line location
        FileReference file1 = this.generateRandomStoredNearlineFileReference("file.nearline.1.test", Optional.empty());
        // Simulate file in cache
        cacheService.addFile(file1.getMetaInfo().getChecksum(), 123L, "file.nearline.1.test",
                             MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE), DataType.RAWDATA.name(),
                             new URL("file", null, "target/cache/test/file.nearline.1.test"),
                             OffsetDateTime.now().plusDays(1), UUID.randomUUID().toString());
        // Simulate availability request on this file
        Mockito.clearInvocations(publisher);
        AvailabilityFlowItem request = AvailabilityFlowItem.build(Sets.newHashSet(file1.getMetaInfo().getChecksum()),
                                                                  OffsetDateTime.now().plusDays(2),
                                                                  UUID.randomUUID().toString());
        List<AvailabilityFlowItem> items = new ArrayList<>();
        items.add(request);
        handler.handleBatch(getDefaultTenant(), items);
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
    @Requirement("REGARDS_DSL_STO_AIP_140")
    @Purpose("Check that a availability request is well handled when a new bus message is received")
    public void availability() throws InterruptedException, ExecutionException {
        LOGGER.info("--> availability");
        // Simulate storage of 3 files in a near line location with restore error
        FileReference file1 = this.generateRandomStoredNearlineFileReference("restoError.file1.test", Optional.empty());
        FileReference file2 = this.generateRandomStoredNearlineFileReference("restoError.file1.test", Optional.empty());
        FileReference file3 = this.generateRandomStoredNearlineFileReference("restoError.file1.test", Optional.empty());
        FileReference file4 = this.generateRandomStoredNearlineFileReference("file4.test", Optional.empty());
        Mockito.clearInvocations(publisher);

        Set<String> checksums = Sets.newHashSet(file1.getMetaInfo().getChecksum(), file2.getMetaInfo().getChecksum(),
                                                file3.getMetaInfo().getChecksum(), file4.getMetaInfo().getChecksum());

        String groupId = UUID.randomUUID().toString();
        AvailabilityFlowItem request = AvailabilityFlowItem.build(checksums, OffsetDateTime.now().plusDays(1), groupId);
        List<AvailabilityFlowItem> items = new ArrayList<>();
        items.add(request);
        handler.handleBatch(getDefaultTenant(), items);
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());

        Assert.assertEquals("There should be 4 cache requests created", 4,
                            fileCacheReqRepo.findByGroupIdAndStatus(groupId, FileRequestStatus.TO_DO).size());

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        runtimeTenantResolver.forceTenant(this.getDefaultTenant());
        Assert.assertEquals("There should be 0 cache requests in TO_DO", 0,
                            fileCacheReqRepo.findByGroupIdAndStatus(groupId, FileRequestStatus.TO_DO).size());
        Assert.assertEquals("There should be 3 cache requests in ERROR", 3,
                            fileCacheReqRepo.findByGroupIdAndStatus(groupId, FileRequestStatus.ERROR).size());
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

        retryHandler.handle(TenantWrapper.build(RetryFlowItem.buildAvailabilityRetry(groupId), getDefaultTenant()));

        runtimeTenantResolver.forceTenant(this.getDefaultTenant());
        Assert.assertEquals("There should be 3 cache requests in TODO", 3,
                            fileCacheReqRepo.findByGroupIdAndStatus(groupId, FileRequestStatus.TO_DO).size());
        Assert.assertEquals("There should be 0 cache requests in ERROR", 0,
                            fileCacheReqRepo.findByGroupIdAndStatus(groupId, FileRequestStatus.ERROR).size());
    }

}
