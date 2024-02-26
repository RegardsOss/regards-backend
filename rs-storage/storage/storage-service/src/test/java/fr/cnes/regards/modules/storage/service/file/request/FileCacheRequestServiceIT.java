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
package fr.cnes.regards.modules.storage.service.file.request;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import fr.cnes.regards.modules.storage.domain.database.CacheFile;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_cache_tests" },
                    locations = { "classpath:application-test.properties" })
public class FileCacheRequestServiceIT extends AbstractStorageIT {

    @Autowired
    private IDynamicTenantSettingService dynamicTenantSettingService;

    @SpyBean
    private RequestsGroupService requestsGroupService;

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();
        simulateApplicationStartedEvent();
        simulateApplicationReadyEvent();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // we override cache setting values for tests
        dynamicTenantSettingService.update(StorageSetting.CACHE_PATH_NAME,
                                           Paths.get("target", "cache", getDefaultTenant()));
        dynamicTenantSettingService.update(StorageSetting.CACHE_MAX_SIZE_NAME, 10L);
    }

    @Test
    public void makeAvailableOnlines() throws InterruptedException, ExecutionException {
        // Given
        FileReference fileRef = this.generateRandomStoredOnlineFileReference();
        Mockito.clearInvocations(fileEventPublisher);

        // When
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef.getMetaInfo().getChecksum()),
                                              24,
                                              UUID.randomUUID().toString());

        // Then
        Assert.assertEquals("No cache request should be created", 0, fileCacheRequestRepository.count());

        Mockito.verify(fileEventPublisher, Mockito.times(1))
               .available(Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.anyString(),
                          Mockito.any());

    }

    /**
     * Test to retrieve nearline files and add it in the internal system cache.
     */
    @Test
    @Requirement("REGARDS_DSL_STO_CMD_110")
    @Purpose("The system retrieve nearline files in an internal cache system")
    public void makeAvailableNearLine() throws InterruptedException, ExecutionException {
        // Given
        FileReference fileRef = this.generateRandomStoredNearlineFileReference("file-nl-1.test", Optional.empty());

        // When
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef.getMetaInfo().getChecksum()),
                                              24,
                                              UUID.randomUUID().toString());
        // Then
        Assert.assertEquals("A cache request should be created",
                            1,
                            fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).size());

        // When
        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);
        // Then
        Assert.assertTrue("file should be restored in cache",
                          Files.exists(Paths.get(cacheService.getFilePath(fileRef.getMetaInfo().getChecksum()))));
    }

    /**
     * Cache size limit is set to 10ko (regards.storage.cache.size.limit.ko.per.tenant=10) see class @TestPropertySource.
     */
    @Test
    public void makeAvailable_cacheFull() throws Exception {
        // Given : simulate cache full 80% 8 file * 1ko (cache size limit 10ko)
        createCacheFiles();
        // Reference 5 files of 1ko each
        FileReference fileRef = this.generateRandomStoredNearlineFileReference("file-nl-1.test", Optional.empty());
        FileReference fileRef2 = this.generateRandomStoredNearlineFileReference("file-nl-2.test", Optional.empty());
        FileReference fileRef3 = this.generateRandomStoredNearlineFileReference("file-nl-3.test", Optional.empty());
        FileReference fileRef4 = this.generateRandomStoredNearlineFileReference("file-nl-4.test", Optional.empty());
        FileReference fileRef5 = this.generateRandomStoredNearlineFileReference("file-nl-5.test", Optional.empty());
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef.getMetaInfo().getChecksum(),
                                                              fileRef2.getMetaInfo().getChecksum(),
                                                              fileRef3.getMetaInfo().getChecksum(),
                                                              fileRef4.getMetaInfo().getChecksum(),
                                                              fileRef5.getMetaInfo().getChecksum()),
                                              24,
                                              UUID.randomUUID().toString());
        Assert.assertEquals("A cache request should be created",
                            1,
                            fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).size());
        Assert.assertEquals("A cache request should be created",
                            1,
                            fileCacheRequestService.search(fileRef2.getMetaInfo().getChecksum()).size());
        Assert.assertEquals("A cache request should be created",
                            1,
                            fileCacheRequestService.search(fileRef3.getMetaInfo().getChecksum()).size());
        Assert.assertEquals("A cache request should be created",
                            1,
                            fileCacheRequestService.search(fileRef4.getMetaInfo().getChecksum()).size());
        Assert.assertEquals("A cache request should be created",
                            1,
                            fileCacheRequestService.search(fileRef5.getMetaInfo().getChecksum()).size());

        // When
        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);
        // Then : only 2 files can be restored in cache
        // There should remain 3 cache  request in {@link FileRequestStatus#TO_DO} state
        Assert.assertEquals("There should remain 3 cache request in TO_DO state",
                            3,
                            fileCacheRequestRepository.count());

        Assert.assertEquals("There should be 10 files in cache", 10, cacheFileRepository.count());

        // When: simulate cache  purge. One  file is expired so one file should be removed from cache
        cacheService.purge(false);

        jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        // Then : one new file can be restored
        // There should remain 2 cache request in {@link FileRequestStatus#TO_DO} state
        Assert.assertEquals("There should remain 2 cache request in TO_DO state",
                            2,
                            fileCacheRequestRepository.count());

        Assert.assertEquals("There should be 10 files in cache", 10, cacheFileRepository.count());
    }

    /**
     * Create the list of cache files in database :
     * <ul>
     *     <li>7 cache files with expiration date=now (+) one day</li>
     *     <li>1 cache file with expiration date=now (-) one day(expired file)</li>
     * </ul>
     */
    private void createCacheFiles() throws MalformedURLException {
        List<CacheFile> cacheFiles = new ArrayList<>();

        String cacheRequestsGroupId = UUID.randomUUID().toString();
        for (int index = 0; index < 7; index++) {
            cacheFiles.add(CacheFile.buildFileInternalCache(UUID.randomUUID().toString(),
                                                            1024L,
                                                            "file",
                                                            MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                                                            new URL("file", null, "/plop/file"),
                                                            OffsetDateTime.now().plusDays(1),
                                                            Set.of(cacheRequestsGroupId),
                                                            DataType.RAWDATA.name()));
        }
        cacheFiles.add(CacheFile.buildFileInternalCache(UUID.randomUUID().toString(),
                                                        1024L,
                                                        "file",
                                                        MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                                                        new URL("file", null, "/plop/file"),
                                                        OffsetDateTime.now().minusDays(1),
                                                        Set.of(cacheRequestsGroupId),
                                                        DataType.RAWDATA.name()));

        cacheFileRepository.saveAll(cacheFiles);
    }

    @Test
    public void makeAvailable_plugin_restoration_error() throws InterruptedException, ExecutionException {
        // Given
        FileReference fileRef = this.generateRandomStoredNearlineFileReference("restoError.file1.test",
                                                                               Optional.empty());

        // When
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef.getMetaInfo().getChecksum()),
                                              24,
                                              UUID.randomUUID().toString());
        // Then
        Assert.assertEquals("A cache request should be created",
                            1,
                            fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).size());

        // When
        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);
        // Then
        Assert.assertFalse("file should be restored in cache",
                           Files.exists(Paths.get(cacheService.getFilePath(fileRef.getMetaInfo().getChecksum()))));
        Optional<FileCacheRequest> request = fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum())
                                                                    .stream()
                                                                    .findFirst();
        Assert.assertTrue("A cache request should be created", request.isPresent());
        Assert.assertEquals("A cache request should be created", FileRequestStatus.ERROR, request.get().getStatus());
    }

    /**
     * Check a multiple files availability request.
     * If a file is requested many times it should be retrieve only one times in cache system to be available.
     */
    @Test
    @Requirement("REGARDS_DSL_STO_ARC_440")
    @Purpose("The system keeps only one copy of a file into its cache")
    public void restoreMultiple() throws InterruptedException, ExecutionException {
        // Given
        FileReference fileRef1 = this.generateRandomStoredNearlineFileReference("file-nl-1.test", Optional.empty());
        FileReference fileRef2 = this.generateRandomStoredNearlineFileReference("file-nl-2.test", Optional.empty());
        FileReference fileRef3 = this.generateRandomStoredNearlineFileReference("file-nl-3.test", Optional.empty());
        FileReference fileRef4 = this.generateRandomStoredNearlineFileReference("file-nl-4.test", Optional.empty());
        // When : create availability requests for 5 files (4 different and 2 times the same file)
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef1.getMetaInfo().getChecksum(),
                                                              fileRef1.getMetaInfo().getChecksum(),
                                                              fileRef2.getMetaInfo().getChecksum(),
                                                              fileRef3.getMetaInfo().getChecksum(),
                                                              fileRef4.getMetaInfo().getChecksum()),
                                              24,
                                              UUID.randomUUID().toString());
        // Then
        Assert.assertEquals("A cache request should be created",
                            1,
                            fileCacheRequestService.search(fileRef1.getMetaInfo().getChecksum()).size());
        Assert.assertEquals("A cache request should be created",
                            1,
                            fileCacheRequestService.search(fileRef2.getMetaInfo().getChecksum()).size());
        Assert.assertEquals("A cache request should be created",
                            1,
                            fileCacheRequestService.search(fileRef3.getMetaInfo().getChecksum()).size());
        Assert.assertEquals("A cache request should be created",
                            1,
                            fileCacheRequestService.search(fileRef4.getMetaInfo().getChecksum()).size());

        // When
        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);
        // Then
        Assert.assertTrue("file should be restored in cache",
                          Files.exists(Paths.get(cacheService.getFilePath(fileRef1.getMetaInfo().getChecksum()))));
        Assert.assertTrue("file should be restored in cache",
                          Files.exists(Paths.get(cacheService.getFilePath(fileRef2.getMetaInfo().getChecksum()))));
        Assert.assertTrue("file should be restored in cache",
                          Files.exists(Paths.get(cacheService.getFilePath(fileRef3.getMetaInfo().getChecksum()))));
        Assert.assertTrue("file should be restored in cache",
                          Files.exists(Paths.get(cacheService.getFilePath(fileRef4.getMetaInfo().getChecksum()))));
        Assert.assertEquals("No cache request should remain", 0, fileCacheRequestRepository.count());

    }

    @Test
    public void makeAvailable_without_cache() throws InterruptedException, ExecutionException {
        // Given
        FileReference fileRef = this.generateRandomStoredNearlineFileReference();
        Mockito.clearInvocations(fileEventPublisher);

        // When
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef), 24, UUID.randomUUID().toString());

        // Then
        Assert.assertEquals("A cache request should be done for the near line file to download",
                            1,
                            fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).size());
        Mockito.verify(fileEventPublisher, Mockito.never())
               .available(Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.anyString(),
                          Mockito.any());
    }

    @Test
    public void makeAvailable_with_cache() throws InterruptedException, ExecutionException {
        // Given
        FileReference fileRef = this.generateRandomStoredNearlineFileReference();
        Mockito.clearInvocations(fileEventPublisher);

        // When
        simulateFileInInternalCache(fileRef.getMetaInfo().getChecksum());
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef), 24, UUID.randomUUID().toString());

        // Then
        Assert.assertEquals(
            "No cache request should be created for the near line file to download as it is available in cache",
            0,
            fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).size());
        Mockito.verify(fileEventPublisher, Mockito.times(1))
               .available(Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.anyString(),
                          Mockito.any());
    }

    @Test
    public void test_handle_success_internal_cache()
        throws MalformedURLException, ExecutionException, InterruptedException {
        // Given
        FileCacheRequest fileCacheRequest = createFileCacheRequest();
        fileCacheRequestRepository.save(fileCacheRequest);

        Mockito.reset(publisher);
        Mockito.reset(requestsGroupService);

        OffsetDateTime expirationDate = OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();

        // When
        fileCacheRequestService.handleSuccessInternalCache(fileCacheRequest,
                                                           new URL("http", "s3", "file-nl.test"),
                                                           Collections.singletonList("owner"),
                                                           1024L,
                                                           "success_message");

        // Then
        Optional<CacheFile> cacheFileOptional = cacheFileRepository.findOneByChecksum(fileCacheRequest.getChecksum());

        Assert.assertTrue(cacheFileOptional.isPresent());
        // Check internal cache
        Assert.assertTrue(cacheFileOptional.get().isInternalCache());
        Assert.assertNull(cacheFileOptional.get().getExternalCachePlugin());

        Assert.assertTrue("Invalid expiration date, at least expiration date must be plus "
                          + fileCacheRequest.getAvailabilityHours(),
                          ChronoUnit.HOURS.between(expirationDate, cacheFileOptional.get().getExpirationDate())
                          >= fileCacheRequest.getAvailabilityHours());

        Assert.assertEquals(0, fileCacheRequestRepository.findByChecksum(fileCacheRequest.getChecksum()).size());

        verifyAfterStoringFileInternalExternalCache();
    }

    @Test
    public void test_handle_success_internal_cache_without_saved_fileCacheRequest()
        throws MalformedURLException, ExecutionException, InterruptedException {
        // Given : file cache request doesn't exist in database
        FileCacheRequest fileCacheRequest = createFileCacheRequest();
        fileCacheRequest.setId(999);

        Mockito.reset(publisher);
        Mockito.reset(requestsGroupService);

        // When
        fileCacheRequestService.handleSuccessInternalCache(fileCacheRequest,
                                                           new URL("http", "s3", "file-nl.test"),
                                                           Collections.singletonList("owner"),
                                                           1024L,
                                                           "success_message");

        // Then
        Optional<CacheFile> cacheFileOptional = cacheFileRepository.findOneByChecksum(fileCacheRequest.getChecksum());

        Assert.assertTrue(cacheFileOptional.isEmpty());

        Assert.assertTrue(fileCacheRequestRepository.findByChecksum(fileCacheRequest.getChecksum()).isEmpty());

        verifyAfterStoringFileInternalExternalCache();
    }

    @Test
    public void test_handle_success_external_cache()
        throws MalformedURLException, ExecutionException, InterruptedException {
        // Given
        String pluginBusinessid = "plugin Business identifier";

        FileCacheRequest fileCacheRequest = createFileCacheRequest();
        fileCacheRequestRepository.save(fileCacheRequest);

        Mockito.reset(publisher);
        Mockito.reset(requestsGroupService);

        OffsetDateTime expirationDate = OffsetDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MICROS);

        // When
        fileCacheRequestService.handleSuccessExternalCache(fileCacheRequest,
                                                           new URL("http", "s3", "file-nl.test"),
                                                           Collections.singletonList("owner"),
                                                           1024L,
                                                           pluginBusinessid,
                                                           expirationDate,
                                                           "success_message");

        // Then
        Optional<CacheFile> cacheFileOptional = cacheFileRepository.findOneByChecksum(fileCacheRequest.getChecksum());

        Assert.assertTrue(cacheFileOptional.isPresent());
        // Check external cache
        Assert.assertFalse(cacheFileOptional.get().isInternalCache());
        Assert.assertEquals(pluginBusinessid, cacheFileOptional.get().getExternalCachePlugin());

        Assert.assertTrue(expirationDate.isEqual(cacheFileOptional.get().getExpirationDate()));

        Assert.assertEquals(0, fileCacheRequestRepository.findByChecksum(fileCacheRequest.getChecksum()).size());

        verifyAfterStoringFileInternalExternalCache();
    }

    @Test
    public void test_handle_success_external_cache_without_saved_fileCacheRequest()
        throws MalformedURLException, ExecutionException, InterruptedException {
        // Given : file cache request doesn't exist in database
        FileCacheRequest fileCacheRequest = createFileCacheRequest();
        fileCacheRequest.setId(999L);

        Mockito.reset(publisher);
        Mockito.reset(requestsGroupService);

        // When
        fileCacheRequestService.handleSuccessExternalCache(fileCacheRequest,
                                                           new URL("http", "s3", "file-nl.test"),
                                                           Collections.singletonList("owner"),
                                                           1024L,
                                                           "plugin Business identifier",
                                                           OffsetDateTime.now().plusHours(10),
                                                           "success_message");

        // Then
        Optional<CacheFile> cacheFileOptional = cacheFileRepository.findOneByChecksum(fileCacheRequest.getChecksum());

        Assert.assertTrue(cacheFileOptional.isEmpty());

        Assert.assertTrue(fileCacheRequestRepository.findByChecksum(fileCacheRequest.getChecksum()).isEmpty());

        verifyAfterStoringFileInternalExternalCache();
    }

    @Test
    public void cacheRequestWithExistingToDo() throws InterruptedException, ExecutionException {
        // Given
        FileReference fileRef1 = this.generateRandomStoredNearlineFileReference("file-nl-1.test", Optional.empty());
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef1.getMetaInfo().getChecksum()),
                                              24,
                                              "originalGroupId");

        // When
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef1.getMetaInfo().getChecksum()), 24, "newGroupId");

        // Then
        Optional<FileCacheRequest> oFileCacheRequest = fileCacheRequestService.search(fileRef1.getMetaInfo()
                                                                                              .getChecksum())
                                                                              .stream()
                                                                              .findFirst();
        Assert.assertTrue("A cache request should be created", oFileCacheRequest.isPresent());
        FileCacheRequest fileCacheRequest = oFileCacheRequest.get();
        Assert.assertEquals("There should be two groupIds", 2, fileCacheRequest.getGroupIds().size());

        // When
        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        // Then
        Assert.assertTrue("file should be restored in cache",
                          Files.exists(Paths.get(cacheService.getFilePath(fileRef1.getMetaInfo().getChecksum()))));
        Assert.assertEquals("No cache request should remain", 0, fileCacheRequestRepository.count());

        ArgumentCaptor<Set<String>> groupIdsCaptor = ArgumentCaptor.forClass(Set.class);

        Mockito.verify(fileEventPublisher, Mockito.times(1))
               .available(Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          groupIdsCaptor.capture(),
                          Mockito.any());

        Set<String> groupIds = groupIdsCaptor.getValue();
        Assert.assertEquals("There should be 2 notified groupIds", 2, groupIds.size());
        Assert.assertTrue("The original groupId should have been notified", groupIds.contains("originalGroupId"));
        Assert.assertTrue("The new groupId should have been notified", groupIds.contains("newGroupId"));
    }

    @Test
    public void cacheRequestWithExistingDelayed() throws InterruptedException, ExecutionException {
        // Given
        FileReference fileRef1 = this.generateRandomStoredNearlineFileReference("file-nl-1.test", Optional.empty());
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef1.getMetaInfo().getChecksum()),
                                              24,
                                              "originalGroupId");

        FileCacheRequest fileCacheRequest = fileCacheRequestService.search(fileRef1.getMetaInfo().getChecksum())
                                                                   .stream()
                                                                   .findFirst()
                                                                   .get();
        fileCacheRequest.setStatus(FileRequestStatus.DELAYED);
        fileCacheRequestRepository.save(fileCacheRequest);

        // When
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef1.getMetaInfo().getChecksum()), 24, "newGroupId");

        // Then
        Optional<FileCacheRequest> oFileCacheRequest = fileCacheRequestService.search(fileRef1.getMetaInfo()
                                                                                              .getChecksum())
                                                                              .stream()
                                                                              .findFirst();
        Assert.assertTrue("A cache request should be created", oFileCacheRequest.isPresent());
        fileCacheRequest = oFileCacheRequest.get();
        Assert.assertEquals("There should be two groupIds", 2, fileCacheRequest.getGroupIds().size());

        // When
        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.DELAYED);
        runAndWaitJob(jobs);

        // Then
        Assert.assertTrue("file should be restored in cache",
                          Files.exists(Paths.get(cacheService.getFilePath(fileRef1.getMetaInfo().getChecksum()))));
        Assert.assertEquals("No cache request should remain", 0, fileCacheRequestRepository.count());

        ArgumentCaptor<Set<String>> groupIdsCaptor = ArgumentCaptor.forClass(Set.class);

        Mockito.verify(fileEventPublisher, Mockito.times(1))
               .available(Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          groupIdsCaptor.capture(),
                          Mockito.any());

        Set<String> groupIds = groupIdsCaptor.getValue();
        Assert.assertEquals("There should be 2 notified groupIds", 2, groupIds.size());
        Assert.assertTrue("The original groupId should have been notified", groupIds.contains("originalGroupId"));
        Assert.assertTrue("The new groupId should have been notified", groupIds.contains("newGroupId"));
    }

    @Test
    public void cacheRequestWithExistingPending() throws InterruptedException, ExecutionException {
        // Given
        FileReference fileRef1 = this.generateRandomStoredNearlineFileReference("file-nl-1.test", Optional.empty());
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef1.getMetaInfo().getChecksum()),
                                              24,
                                              "originalGroupId");

        FileCacheRequest fileCacheRequest = fileCacheRequestService.search(fileRef1.getMetaInfo().getChecksum())
                                                                   .stream()
                                                                   .findFirst()
                                                                   .get();
        // Artificially change the original request status to PENDING so it blocks the new request
        fileCacheRequest.setStatus(FileRequestStatus.PENDING);
        fileCacheRequestRepository.save(fileCacheRequest);

        // When
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef1.getMetaInfo().getChecksum()), 24, "newGroupId");

        // Then
        Set<FileCacheRequest> requests = fileCacheRequestService.search(fileRef1.getMetaInfo().getChecksum());
        Assert.assertEquals("There should be 2 file cache requests", 2, requests.size());

        Optional<FileCacheRequest> oOriginalRequest = requests.stream()
                                                              .filter(request -> request.getGroupIds()
                                                                                        .contains("originalGroupId"))
                                                              .findFirst();
        Assert.assertTrue("The original request should still be there", oOriginalRequest.isPresent());
        FileCacheRequest originalRequest = oOriginalRequest.get();
        Assert.assertEquals("The original request should still be in pending status",
                            FileRequestStatus.PENDING,
                            originalRequest.getStatus());
        Assert.assertEquals("No groupId should have been added to the original request",
                            1,
                            originalRequest.getGroupIds().size());

        Optional<FileCacheRequest> oNewRequest = requests.stream()
                                                         .filter(request -> request.getGroupIds()
                                                                                   .contains("newGroupId"))
                                                         .findFirst();
        Assert.assertTrue("The new request should have been created", oNewRequest.isPresent());
        FileCacheRequest newRequest = oNewRequest.get();
        Assert.assertEquals("The new request should be in delayed status",
                            FileRequestStatus.DELAYED,
                            newRequest.getStatus());
        Assert.assertEquals("There should be only the new groupId in the request", 1, newRequest.getGroupIds().size());

        // When
        // Change back the original request status to TO_DO so it can be run
        fileCacheRequest.setStatus(FileRequestStatus.TO_DO);
        fileCacheRequestRepository.save(fileCacheRequest);

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);
        reqStatusService.checkDelayedCacheRequests();

        // Then
        requests = fileCacheRequestService.search(fileRef1.getMetaInfo().getChecksum());
        oNewRequest = requests.stream().filter(request -> request.getGroupIds().contains("newGroupId")).findFirst();
        Assert.assertTrue("The new request should still exist", oNewRequest.isPresent());
        newRequest = oNewRequest.get();
        Assert.assertEquals("The new request should be in TO_DO status",
                            FileRequestStatus.TO_DO,
                            newRequest.getStatus());
        Assert.assertEquals("There should be only the new groupId in the request", 1, newRequest.getGroupIds().size());

        // When
        jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        // Then
        Assert.assertTrue("file should be restored in cache",
                          Files.exists(Paths.get(cacheService.getFilePath(fileRef1.getMetaInfo().getChecksum()))));

        Assert.assertEquals("No cache request should remain", 0, fileCacheRequestRepository.count());

        ArgumentCaptor<Set<String>> groupIdsCaptor = ArgumentCaptor.forClass(Set.class);

        Mockito.verify(fileEventPublisher, Mockito.times(2))
               .available(Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          groupIdsCaptor.capture(),
                          Mockito.any());

        Set<String> groupIds = groupIdsCaptor.getAllValues().get(0);
        Assert.assertEquals("There should be 1 notified groupIds", 1, groupIds.size());
        Assert.assertTrue("The originalGroupId groupId should have been notified first",
                          groupIds.contains("originalGroupId"));

        groupIds = groupIdsCaptor.getAllValues().get(1);
        Assert.assertEquals("There should be 1 notified groupIds", 1, groupIds.size());
        Assert.assertTrue("The newGroupId groupId should have been the second one notified",
                          groupIds.contains("newGroupId"));

    }

    @Test
    public void cacheRequestWithExistingError() throws InterruptedException, ExecutionException {
        // Given
        FileReference fileRef1 = this.generateRandomStoredNearlineFileReference("file-nl-1.test", Optional.empty());
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef1.getMetaInfo().getChecksum()),
                                              24,
                                              "originalGroupId");

        FileCacheRequest fileCacheRequest = fileCacheRequestService.search(fileRef1.getMetaInfo().getChecksum())
                                                                   .stream()
                                                                   .findFirst()
                                                                   .get();
        fileCacheRequest.setStatus(FileRequestStatus.ERROR);
        fileCacheRequestRepository.save(fileCacheRequest);

        // When
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileRef1.getMetaInfo().getChecksum()), 24, "newGroupId");

        // Then
        Set<FileCacheRequest> requests = fileCacheRequestService.search(fileRef1.getMetaInfo().getChecksum());
        Assert.assertEquals("There should be 2 file cache requests", 2, requests.size());

        Optional<FileCacheRequest> oOriginalRequest = requests.stream()
                                                              .filter(request -> request.getGroupIds()
                                                                                        .contains("originalGroupId"))
                                                              .findFirst();
        Assert.assertTrue("The original request should still be there", oOriginalRequest.isPresent());
        FileCacheRequest originalRequest = oOriginalRequest.get();
        Assert.assertEquals("The original request should still be in error status",
                            FileRequestStatus.ERROR,
                            originalRequest.getStatus());
        Assert.assertEquals("No groupId should have been added to the original request",
                            1,
                            originalRequest.getGroupIds().size());

        Optional<FileCacheRequest> oNewRequest = requests.stream()
                                                         .filter(request -> request.getGroupIds()
                                                                                   .contains("newGroupId"))
                                                         .findFirst();
        Assert.assertTrue("The new request should have been created", oNewRequest.isPresent());
        FileCacheRequest newRequest = oNewRequest.get();
        Assert.assertEquals("The new request should be in TO_DO status",
                            FileRequestStatus.TO_DO,
                            newRequest.getStatus());
        Assert.assertEquals("There should be only the new groupId in the request", 1, newRequest.getGroupIds().size());

        // When
        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        // Then
        Assert.assertTrue("file should be restored in cache",
                          Files.exists(Paths.get(cacheService.getFilePath(fileRef1.getMetaInfo().getChecksum()))));
        Assert.assertEquals("The cache request in error should remain", 1, fileCacheRequestRepository.count());

        ArgumentCaptor<Set<String>> groupIdsCaptor = ArgumentCaptor.forClass(Set.class);

        Mockito.verify(fileEventPublisher, Mockito.times(1))
               .available(Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          Mockito.any(),
                          groupIdsCaptor.capture(),
                          Mockito.any());

        Set<String> groupIds = groupIdsCaptor.getValue();
        Assert.assertEquals("There should be 1 notified groupIds", 1, groupIds.size());
        Assert.assertTrue("The newGroupId groupId should have been notified", groupIds.contains("newGroupId"));
        Assert.assertFalse("The original groupId should not have been notified as it is in error state",
                           groupIds.contains("originalGroupId"));
    }

    // ---------------------
    // -- UTILITY METHODS --
    // ---------------------

    /**
     * Create a file cache request with availability of 24 hours.
     */
    private FileCacheRequest createFileCacheRequest() throws ExecutionException, InterruptedException {
        FileReference fileRef = generateRandomStoredNearlineFileReference("file-nl.test", Optional.empty());

        return new FileCacheRequest(fileRef, "restoreDirectory", 24, "group id");
    }

    private void verifyAfterStoringFileInternalExternalCache() {
        Mockito.verify(publisher, Mockito.times(1)).publish(any(FileReferenceEvent.class));

        ArgumentCaptor<FileRequestType> fileRequestTypeCaptor = ArgumentCaptor.forClass(FileRequestType.class);
        Mockito.verify(requestsGroupService, Mockito.times(1))
               .requestSuccess(any(), fileRequestTypeCaptor.capture(), any(), any(), any(), any(), any());
        Assert.assertSame(FileRequestType.AVAILABILITY, fileRequestTypeCaptor.getValue());
    }

}
