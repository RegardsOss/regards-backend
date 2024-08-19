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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.AbstractPublisher;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesRestorationRequestEvent;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesRetryRequestEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileAvailableEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEventType;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_availability_file_ref_tests" },
                    locations = { "classpath:application-test.properties" })
public class AvailabilityFileReferenceIT extends AbstractStorageIT {

    private static final String SESSION_OWNER_1 = "SOURCE 1";

    private static final String SESSION_1 = "SESSION 1";

    @Autowired
    private FileRestorationRequestEventHandler handler;

    @Autowired
    private FilesRetryRequestEventHandler retryHandler;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Before
    public void initialize() throws ModuleException {
        Mockito.clearInvocations(publisher);
        super.init();
        simulateApplicationStartedEvent();
        simulateApplicationReadyEvent();
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_140")
    @Purpose("Check that a restoration request is well handled when a new bus message is received")
    public void test_files_restoration_request_event() throws InterruptedException, ExecutionException {
        // Given
        // Simulate storage of 3 files in a nearline location
        FileReference file1 = generateRandomStoredNearlineFileReference("file.nearline.1.test", Optional.empty());
        FileReference file2 = generateRandomStoredNearlineFileReference("file.nearline.2.test", Optional.empty());
        FileReference file3 = generateRandomStoredNearlineFileReference("file.nearline.3.test", Optional.empty());
        // Simulate storage of 2 files in an online location
        FileReference file4 = generateRandomStoredOnlineFileReference("file.online.1.test", Optional.empty());
        FileReference file5 = generateRandomStoredOnlineFileReference("file.online.2.test", Optional.empty());
        // Simulate reference of 2 files offline
        FileReference file6 = referenceRandomFile("owner",
                                                  "file",
                                                  "file.offline.1.test",
                                                  "somewhere",
                                                  SESSION_OWNER_1,
                                                  SESSION_1,
                                                  false).get();
        FileReference file7 = referenceRandomFile("owner",
                                                  "file",
                                                  "file.offline.2.test",
                                                  "somewhere-else",
                                                  SESSION_OWNER_1,
                                                  SESSION_1,
                                                  false).get();
        // Simulate storage of a file in two locations : 1 in nearline and 1 in online
        String checksum = RandomChecksumUtils.generateRandomChecksum();
        generateStoredFileReference(checksum,
                                    "owner",
                                    "file.online.nealine.test",
                                    ONLINE_CONF_LABEL,
                                    Optional.empty(),
                                    Optional.empty(),
                                    SESSION_OWNER_1,
                                    SESSION_1);
        generateStoredFileReference(checksum,
                                    "owner",
                                    "file.online.nealine.test",
                                    NEARLINE_CONF_LABEL,
                                    Optional.empty(),
                                    Optional.empty(),
                                    SESSION_OWNER_1,
                                    SESSION_1);

        Set<String> checksums = Sets.newHashSet(file1.getMetaInfo().getChecksum(),
                                                file2.getMetaInfo().getChecksum(),
                                                file3.getMetaInfo().getChecksum(),
                                                file4.getMetaInfo().getChecksum(),
                                                file5.getMetaInfo().getChecksum(),
                                                file6.getMetaInfo().getChecksum(),
                                                file7.getMetaInfo().getChecksum(),
                                                checksum);
        Mockito.clearInvocations(publisher);
        String groupId = UUID.randomUUID().toString();

        // When
        handler.handleBatch(Collections.singletonList(new FilesRestorationRequestEvent(checksums, 24, groupId)));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // There should be 5 cache request for the 3 files only in near line and 2 files offline.
        // The file stored in two locations online and near line does not need to be restored
        // as its available online.
        Assert.assertEquals("There should be 5 cache requests created",
                            5,
                            fileCacheRequestRepository.findByGroupIdsAndStatus(groupId, FileRequestStatus.TO_DO)
                                                      .size());
        Assert.assertTrue("A cache request should be done for near line file 1",
                          fileCacheRequestService.search(file1.getMetaInfo().getChecksum())
                                                 .stream()
                                                 .findFirst()
                                                 .isPresent());
        Assert.assertTrue("A cache request should be done for near line file 2",
                          fileCacheRequestService.search(file2.getMetaInfo().getChecksum())
                                                 .stream()
                                                 .findFirst()
                                                 .isPresent());
        Assert.assertTrue("A cache request should be done for near line file 3",
                          fileCacheRequestService.search(file3.getMetaInfo().getChecksum())
                                                 .stream()
                                                 .findFirst()
                                                 .isPresent());
        Assert.assertFalse("A cache request should not be done for near line file 4 as it is online too",
                           fileCacheRequestService.search(checksum).stream().findFirst().isPresent());

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        // Then
        // There should be 2 notification error for availability of offline files
        // There should be 3 notification for nearline files available
        // There should be 3 notification for online files available
        Mockito.verify(publisher, Mockito.times(8)).publish(Mockito.any(FileReferenceEvent.class));
        // Check if restoration events are sent for available files
        Mockito.verify(publisher, Mockito.times(8)) // 6 available files + 2 unavailable files
               .broadcast(Mockito.eq(FileAvailableEvent.EXCHANGE_NAME),
                          Mockito.eq(Optional.empty()),
                          Mockito.eq(Optional.of(FileAvailableEvent.ROUTING_KEY_AVAILABILITY_STATUS)),
                          Mockito.eq(Optional.empty()),
                          Mockito.eq(AbstractPublisher.DEFAULT_PRIORITY),
                          Mockito.any(FileAvailableEvent.class),
                          Mockito.eq(Maps.newHashMap()));

        ArgumentCaptor<ISubscribable> subscribableCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(subscribableCaptor.capture());

        Set<String> availables = Sets.newHashSet();
        Set<String> notAvailables = Sets.newHashSet();
        for (FileReferenceEvent evt : getFileReferenceEvents(subscribableCaptor.getAllValues())) {
            if (evt.getType() == FileReferenceEventType.AVAILABLE) {
                availables.add(evt.getChecksum());
            } else if (evt.getType() == FileReferenceEventType.AVAILABILITY_ERROR) {
                notAvailables.add(evt.getChecksum());
            }
        }
        // Available files
        Assert.assertEquals("There should be 6 files availables", 6, availables.size());
        Assert.assertTrue("File should be available as it is online",
                          availables.contains(file4.getMetaInfo().getChecksum()));
        Assert.assertTrue("File should be available as it is online",
                          availables.contains(file5.getMetaInfo().getChecksum()));
        Assert.assertTrue("File should be available as it is online", availables.contains(checksum));
        // Unavailable files
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
        // Given
        // Simulate file storage on a near line location
        FileReference file1 = generateRandomStoredNearlineFileReference("file.nearline.1.test", Optional.empty());
        // Simulate file in cache
        cacheService.addFile(file1.getMetaInfo().getChecksum(),
                             123L,
                             "file.nearline.1.test",
                             MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                             DataType.RAWDATA.name(),
                             new URL("file", null, "target/cache/test/file.nearline.1.test"),
                             OffsetDateTime.now().plusDays(1),
                             Set.of(UUID.randomUUID().toString()),
                             null);
        // Simulate availability request on this file
        Mockito.clearInvocations(publisher);

        // When
        handler.handleBatch(Collections.singletonList(new FilesRestorationRequestEvent(Sets.newHashSet(file1.getMetaInfo()
                                                                                                            .getChecksum()),
                                                                                       24,
                                                                                       UUID.randomUUID().toString())));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Then
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FileReferenceEvent.class));
        ArgumentCaptor<ISubscribable> subscribableCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(subscribableCaptor.capture());

        FileReferenceEvent event = getFileReferenceEvent(subscribableCaptor.getAllValues());
        Assert.assertEquals("File should be available", FileReferenceEventType.AVAILABLE, event.getType());
        Assert.assertEquals("File available is not the requested one",
                            file1.getMetaInfo().getChecksum(),
                            event.getChecksum());
        Assert.assertFalse("No cache request should be created as file is in cache",
                           fileCacheRequestService.search(file1.getMetaInfo().getChecksum())
                                                  .stream()
                                                  .findFirst()
                                                  .isPresent());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_140")
    @Purpose("Check that a availability request is well handled when a new bus message is received")
    public void availability() throws InterruptedException, ExecutionException {
        // Given
        // Simulate storage of 3 files in a near line location with restore error
        FileReference file1 = generateRandomStoredNearlineFileReference("restoError.file1.test", Optional.empty());
        FileReference file2 = generateRandomStoredNearlineFileReference("restoError.file1.test", Optional.empty());
        FileReference file3 = generateRandomStoredNearlineFileReference("restoError.file1.test", Optional.empty());
        FileReference file4 = generateRandomStoredNearlineFileReference("file4.test", Optional.empty());
        Mockito.clearInvocations(publisher);

        Set<String> checksums = Sets.newHashSet(file1.getMetaInfo().getChecksum(),
                                                file2.getMetaInfo().getChecksum(),
                                                file3.getMetaInfo().getChecksum(),
                                                file4.getMetaInfo().getChecksum());

        String groupId = UUID.randomUUID().toString();

        // When
        handler.handleBatch(Collections.singletonList(new FilesRestorationRequestEvent(checksums, 24, groupId)));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        Assert.assertEquals("There should be 4 cache requests created",
                            4,
                            fileCacheRequestRepository.findByGroupIdsAndStatus(groupId, FileRequestStatus.TO_DO)
                                                      .size());

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Then
        Assert.assertEquals("There should be 0 cache requests in TO_DO",
                            0,
                            fileCacheRequestRepository.findByGroupIdsAndStatus(groupId, FileRequestStatus.TO_DO)
                                                      .size());
        Assert.assertEquals("There should be 3 cache requests in ERROR",
                            3,
                            fileCacheRequestRepository.findByGroupIdsAndStatus(groupId, FileRequestStatus.ERROR)
                                                      .size());
        Assert.assertEquals("There should be 1 file cache requests",
                            1,
                            cacheFileRepository.findAllByChecksumIn(Sets.newHashSet(file4.getMetaInfo().getChecksum()))
                                               .size());

        // There should be 3 notification error for availability of offline files
        // There should be 1 notification for available file
        Mockito.verify(publisher, Mockito.times(4)).publish(Mockito.any(FileReferenceEvent.class));
        ArgumentCaptor<ISubscribable> subscribableCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(subscribableCaptor.capture());

        Set<String> availables = Sets.newHashSet();
        Set<String> notAvailables = Sets.newHashSet();
        for (FileReferenceEvent evt : getFileReferenceEvents(subscribableCaptor.getAllValues())) {
            if (evt.getType() == FileReferenceEventType.AVAILABLE) {
                availables.add(evt.getChecksum());
            } else if (evt.getType() == FileReferenceEventType.AVAILABILITY_ERROR) {
                notAvailables.add(evt.getChecksum());
            }
        }
        Assert.assertEquals("There should be 1 files available", 1, availables.size());
        Assert.assertEquals("There should be 3 files unavailable(=error)", 3, notAvailables.size());

        // When
        retryHandler.handle(TenantWrapper.build(FilesRetryRequestEvent.buildAvailabilityRetry(groupId),
                                                getDefaultTenant()));
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Then
        Assert.assertEquals("There should be 3 cache requests in TODO",
                            3,
                            fileCacheRequestRepository.findByGroupIdsAndStatus(groupId, FileRequestStatus.TO_DO)
                                                      .size());
        Assert.assertEquals("There should be 0 cache requests in ERROR",
                            0,
                            fileCacheRequestRepository.findByGroupIdsAndStatus(groupId, FileRequestStatus.ERROR)
                                                      .size());
    }

}
