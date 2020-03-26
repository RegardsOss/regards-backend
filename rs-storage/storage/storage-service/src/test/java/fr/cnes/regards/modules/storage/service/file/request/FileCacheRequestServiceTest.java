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
package fr.cnes.regards.modules.storage.service.file.request;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.service.AbstractStorageTest;

/**
 * Test class
 *
 * @author Sébastien Binda
 *
 */
@ActiveProfiles({ "noschedule" })
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=storage_cache_tests",
                "regards.storage.cache.path=target/cache", "regards.storage.cache.size.limit.ko.per.tenant=10" },
        locations = { "classpath:application-test.properties" })
public class FileCacheRequestServiceTest extends AbstractStorageTest {

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();
    }

    @Test
    public void makeAvailableOnlines() throws InterruptedException, ExecutionException {
        FileReference fileRef = this.generateRandomStoredOnlineFileReference();
        Mockito.clearInvocations(fileEventPublisher);
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef.getMetaInfo().getChecksum()),
                                              OffsetDateTime.now().plusDays(1), UUID.randomUUID().toString());
        Assert.assertFalse("No cache request should be created",
                           fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).isPresent());

        Mockito.verify(fileEventPublisher, Mockito.times(1)).available(Mockito.any(), Mockito.any(), Mockito.any(),
                                                                       Mockito.any(), Mockito.any(), Mockito.any(),
                                                                       Mockito.any());

    }

    /**
     * Test to retrieve nearline files and add it in the internal system cache.
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    @Requirement("REGARDS_DSL_STO_CMD_110")
    @Purpose("The system retrieve nearline files in an internal cache system")
    public void makeAvailable() throws InterruptedException, ExecutionException {
        FileReference fileRef = this.generateRandomStoredNearlineFileReference("file-nl-1.test", Optional.empty());
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef.getMetaInfo().getChecksum()),
                                              OffsetDateTime.now().plusDays(1), UUID.randomUUID().toString());
        Assert.assertTrue("A cache request should be created",
                          fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).isPresent());

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        Assert.assertTrue("file should be restored in cache",
                          Files.exists(Paths.get(cacheService.getFilePath(fileRef.getMetaInfo().getChecksum()))));
    }

    /**
     * Cache size limit is set to 10ko (regards.storage.cache.size.limit.ko.per.tenant=10) see class @TestPropertySource.
     * @throws Exception
     */
    @Test
    public void makeAvailable_cacheFull() throws Exception {
        // Simulate cache full 80% 8 file * 1ko (cache size limit 10ko)
        String cacheRequestsGroupId = UUID.randomUUID().toString();
        cacheService.addFile(UUID.randomUUID().toString(), 1024L, "file",
                             MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                             new URL("file", null, "/plop/file"), OffsetDateTime.now().plusDays(1),
                             cacheRequestsGroupId);
        cacheService.addFile(UUID.randomUUID().toString(), 1024L, "file",
                             MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                             new URL("file", null, "/plop/file"), OffsetDateTime.now().plusDays(1),
                             cacheRequestsGroupId);
        cacheService.addFile(UUID.randomUUID().toString(), 1024L, "file",
                             MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                             new URL("file", null, "/plop/file"), OffsetDateTime.now().plusDays(1),
                             cacheRequestsGroupId);
        cacheService.addFile(UUID.randomUUID().toString(), 1024L, "file",
                             MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                             new URL("file", null, "/plop/file"), OffsetDateTime.now().plusDays(1),
                             cacheRequestsGroupId);
        cacheService.addFile(UUID.randomUUID().toString(), 1024L, "file",
                             MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                             new URL("file", null, "/plop/file"), OffsetDateTime.now().plusDays(1),
                             cacheRequestsGroupId);
        cacheService.addFile(UUID.randomUUID().toString(), 1024L, "file",
                             MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                             new URL("file", null, "/plop/file"), OffsetDateTime.now().plusDays(1),
                             cacheRequestsGroupId);
        cacheService.addFile(UUID.randomUUID().toString(), 1024L, "file",
                             MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                             new URL("file", null, "/plop/file"), OffsetDateTime.now().plusDays(1),
                             cacheRequestsGroupId);
        cacheService.addFile(UUID.randomUUID().toString(), 1024L, "file",
                             MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                             new URL("file", null, "/plop/file"), OffsetDateTime.now().minusDays(1),
                             cacheRequestsGroupId);

        // Reference 5 files of 1ko each
        FileReference fileRef = this.generateRandomStoredNearlineFileReference("file-nl-1.test", Optional.empty());
        FileReference fileRef2 = this.generateRandomStoredNearlineFileReference("file-nl-2.test", Optional.empty());
        FileReference fileRef3 = this.generateRandomStoredNearlineFileReference("file-nl-3.test", Optional.empty());
        FileReference fileRef4 = this.generateRandomStoredNearlineFileReference("file-nl-4.test", Optional.empty());
        FileReference fileRef5 = this.generateRandomStoredNearlineFileReference("file-nl-5.test", Optional.empty());
        fileCacheRequestService.makeAvailable(Sets
                .newHashSet(fileRef.getMetaInfo().getChecksum(), fileRef2.getMetaInfo().getChecksum(),
                            fileRef3.getMetaInfo().getChecksum(), fileRef4.getMetaInfo().getChecksum(),
                            fileRef5.getMetaInfo().getChecksum()), OffsetDateTime.now().plusDays(1),
                                              UUID.randomUUID().toString());
        Assert.assertTrue("A cache request should be created",
                          fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).isPresent());
        Assert.assertTrue("A cache request should be created",
                          fileCacheRequestService.search(fileRef2.getMetaInfo().getChecksum()).isPresent());
        Assert.assertTrue("A cache request should be created",
                          fileCacheRequestService.search(fileRef3.getMetaInfo().getChecksum()).isPresent());
        Assert.assertTrue("A cache request should be created",
                          fileCacheRequestService.search(fileRef4.getMetaInfo().getChecksum()).isPresent());
        Assert.assertTrue("A cache request should be created",
                          fileCacheRequestService.search(fileRef5.getMetaInfo().getChecksum()).isPresent());

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        // Only 2 files can be restored in cache
        // There should remains 3 cache  request in {@link FileRequestStatus#TO_DO} state
        Assert.assertEquals("There should remains 3 cache  request in TO_DO state", 3, fileCacheReqRepo.count());

        Assert.assertEquals("There should be 10 files in cache", 10, cacheFileRepo.count());

        // Simulate cache  purge. One  file is expired so one  file should be  removed from cache
        cacheService.purge();

        jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        // One new file can be restored
        // There should remains 2 cache  request in {@link FileRequestStatus#TO_DO} state
        Assert.assertEquals("There should remains 2 cache  request in TO_DO state", 2, fileCacheReqRepo.count());

        Assert.assertEquals("There should be 10 files in cache", 10, cacheFileRepo.count());

    }

    @Test
    public void makeAvailable_plugin_restoration_error() throws InterruptedException, ExecutionException {
        FileReference fileRef = this.generateRandomStoredNearlineFileReference("restoError.file1.test",
                                                                               Optional.empty());
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef.getMetaInfo().getChecksum()),
                                              OffsetDateTime.now().plusDays(1), UUID.randomUUID().toString());
        Assert.assertTrue("A cache request should be created",
                          fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).isPresent());

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        Assert.assertFalse("file should be restored in cache",
                           Files.exists(Paths.get(cacheService.getFilePath(fileRef.getMetaInfo().getChecksum()))));
        Optional<FileCacheRequest> request = fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum());
        Assert.assertTrue("A cache request should be created", request.isPresent());
        Assert.assertTrue("A cache request should be created", request.get().getStatus() == FileRequestStatus.ERROR);
    }

    /**
     * Check a multiple files availability request.
     * If a file is requested many times it should be retrieve only one times in cache system to be available.
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    @Requirement("REGARDS_DSL_STO_ARC_440")
    @Purpose("The system keeps only one copy of a file into its cache")
    public void restoreMultiple() throws InterruptedException, ExecutionException {
        FileReference fileRef = this.generateRandomStoredNearlineFileReference("file-nl-1.test", Optional.empty());
        FileReference fileRef2 = this.generateRandomStoredNearlineFileReference("file-nl-2.test", Optional.empty());
        FileReference fileRef3 = this.generateRandomStoredNearlineFileReference("file-nl-3.test", Optional.empty());
        FileReference fileRef4 = this.generateRandomStoredNearlineFileReference("file-nl-4.test", Optional.empty());
        // Create availability requests for 5 files (4 different and 2 times the same file)
        fileCacheRequestService.makeAvailable(Sets
                .newHashSet(fileRef.getMetaInfo().getChecksum(), fileRef.getMetaInfo().getChecksum(),
                            fileRef2.getMetaInfo().getChecksum(), fileRef3.getMetaInfo().getChecksum(),
                            fileRef4.getMetaInfo().getChecksum()), OffsetDateTime.now().plusDays(1),
                                              UUID.randomUUID().toString());
        Assert.assertTrue("A cache request should be created",
                          fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).isPresent());
        Assert.assertTrue("A cache request should be created",
                          fileCacheRequestService.search(fileRef2.getMetaInfo().getChecksum()).isPresent());
        Assert.assertTrue("A cache request should be created",
                          fileCacheRequestService.search(fileRef3.getMetaInfo().getChecksum()).isPresent());
        Assert.assertTrue("A cache request should be created",
                          fileCacheRequestService.search(fileRef4.getMetaInfo().getChecksum()).isPresent());

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        Assert.assertTrue("file should be restored in cache",
                          Files.exists(Paths.get(cacheService.getFilePath(fileRef.getMetaInfo().getChecksum()))));
        Assert.assertTrue("file should be restored in cache",
                          Files.exists(Paths.get(cacheService.getFilePath(fileRef2.getMetaInfo().getChecksum()))));
        Assert.assertTrue("file should be restored in cache",
                          Files.exists(Paths.get(cacheService.getFilePath(fileRef3.getMetaInfo().getChecksum()))));
        Assert.assertTrue("file should be restored in cache",
                          Files.exists(Paths.get(cacheService.getFilePath(fileRef4.getMetaInfo().getChecksum()))));
        Assert.assertTrue("No cache request should remains", fileCacheReqRepo.count() == 0);

    }

    @Test
    public void makeAvailable_without_cache() throws InterruptedException, ExecutionException {
        FileReference fileRef = this.generateRandomStoredNearlineFileReference();
        Mockito.clearInvocations(fileEventPublisher);
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef), OffsetDateTime.now().plusDays(1),
                                              UUID.randomUUID().toString());
        Assert.assertTrue("A cache request should be done for the near line file to download",
                          fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).isPresent());
        Mockito.verify(fileEventPublisher, Mockito.never()).available(Mockito.any(), Mockito.any(), Mockito.any(),
                                                                      Mockito.any(), Mockito.any(), Mockito.any(),
                                                                      Mockito.any());
    }

    @Test
    public void makeAvailable_with_cache() throws InterruptedException, ExecutionException {
        FileReference fileRef = this.generateRandomStoredNearlineFileReference();
        Mockito.clearInvocations(fileEventPublisher);
        this.simulateFileInCache(fileRef.getMetaInfo().getChecksum());
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef), OffsetDateTime.now().plusDays(1),
                                              UUID.randomUUID().toString());
        Assert.assertFalse("No cache request should be created for the near line file to download as it is available in cache",
                           fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).isPresent());
        Mockito.verify(fileEventPublisher, Mockito.times(1)).available(Mockito.any(), Mockito.any(), Mockito.any(),
                                                                       Mockito.any(), Mockito.any(), Mockito.any(),
                                                                       Mockito.any());
    }

}
