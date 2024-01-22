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
package fr.cnes.regards.modules.storage.service.file;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.plugin.domain.NearlineFileNotAvailableException;
import fr.cnes.regards.modules.storage.domain.DownloadableFile;
import fr.cnes.regards.modules.storage.domain.database.CacheFile;
import fr.cnes.regards.modules.storage.domain.database.DownloadToken;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import io.vavr.control.Try;
import org.apache.commons.compress.utils.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_download_tests" },
                    locations = { "classpath:application-test.properties" })
public class FileDownloadServiceIT extends AbstractStorageIT {

    private static final String SESSION_OWNER = "SOURCE 1";

    private static final String SESSION = "SESSION 1";

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();
        simulateApplicationStartedEvent();
        simulateApplicationReadyEvent();
    }

    @Test
    public void downloadFileOnlineAndNotNearline() throws ExecutionException, InterruptedException, ModuleException {
        // Given
        FileReference fileRef = generateRandomStoredNearlineFileReference();
        fileRef = generateStoredFileReference(fileRef.getMetaInfo().getChecksum(),
                                              fileRef.getLazzyOwners().stream().findFirst().get(),
                                              fileRef.getMetaInfo().getFileName(),
                                              ONLINE_CONF_LABEL_WITHOUT_DELETE,
                                              Optional.empty(),
                                              Optional.empty(),
                                              SESSION_OWNER,
                                              SESSION);
        // When
        fileDownloadService.downloadFile(fileRef.getMetaInfo().getChecksum());

        // Then : there should not be any exception as the file is at the same time online and nearline
    }

    @Test
    public void downloadFileReferenceOnline() throws ModuleException, InterruptedException, ExecutionException {
        fileDownloadService.downloadFile(generateRandomStoredOnlineFileReference().getMetaInfo().getChecksum());
    }

    @Test
    public void downloadFileReferenceOffLine() {
        // Given
        FileReference fileRef = referenceFile(UUID.randomUUID().toString(),
                                              "owner",
                                              null,
                                              "file.test",
                                              "somewhere",
                                              "source1",
                                              "session1",
                                              false).get();
        // When
        Try<Callable<DownloadableFile>> result = Try.of(() -> fileDownloadService.downloadFile(fileRef.getMetaInfo()
                                                                                                      .getChecksum()));
        // Then
        assertTrue("File should not be available for download as it is not handled by a known storage location plugin",
                   result.isFailure());
        assertTrue(result.getCause() instanceof ModuleException);
    }

    @Test
    public void test_downloadFileReference_nearline() throws InterruptedException, ExecutionException, IOException {
        // Given
        FileReference fileReference = generateRandomStoredNearlineFileReference();
        // Update the real url of file(without the protocol) in order to download the file
        String url = fileReference.getLocation().getUrl();
        fileReference.getLocation().setUrl(url.substring(url.indexOf(":") + 1));
        fileRefService.store(fileReference);

        // When
        Try<DownloadableFile> downloadableFile = Try.of(() -> fileDownloadService.downloadFile(fileReference.getMetaInfo()
                                                                                                            .getChecksum()))
                                                    .mapTry(Callable::call);

        // Then
        assertTrue(downloadableFile.isSuccess());

        DownloadableFile file = downloadableFile.get();
        assertNotNull(file.getFileInputStream());
        assertEquals(fileReference.getMetaInfo().getFileName(), file.getFileName());
    }

    @Test
    public void test_downloadFileReference_nearline_without_cache_then_with_cache()
        throws InterruptedException, ExecutionException, IOException {
        // Given
        FileReference fileReference = generateRandomStoredNearlineFileReference();
        assertFalse(fileReference.isNearlineConfirmed());

        // When
        Try<DownloadableFile> downloadableFile = Try.of(() -> fileDownloadService.downloadFile(fileReference.getMetaInfo()
                                                                                                            .getChecksum()))
                                                    .mapTry(Callable::call);

        // Then
        assertTrue("File should not be available for download as it is not online", downloadableFile.isFailure());
        assertTrue(downloadableFile.getCause() instanceof NearlineFileNotAvailableException);

        List<FileReference> fileReferences = new ArrayList<>(fileRefService.search(fileReference.getMetaInfo()
                                                                                                .getChecksum()));
        assertEquals(1, fileReferences.size());
        assertTrue(fileReferences.get(0).isNearlineConfirmed());

        // Given : ask the file is available in cache
        fileCacheRequestService.makeAvailable(Sets.newHashSet(fileReference), 24, UUID.randomUUID().toString());

        // A cache request should be created
        Optional<FileCacheRequest> fileCacheRequestOpt = fileCacheRequestService.search(fileReference.getMetaInfo()
                                                                                                     .getChecksum());
        Assert.assertTrue("FileCacheRequest should be created", fileCacheRequestOpt.isPresent());
        assertEquals("FileCacheRequest should be created to retrieve file from nearline storage",
                     NEARLINE_CONF_LABEL,
                     fileCacheRequestOpt.get().getStorage());
        assertEquals("FileCacheRequest should be created to retrieve file from nearline storage",
                     FileRequestStatus.TO_DO,
                     fileCacheRequestOpt.get().getStatus());

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
        runAndWaitJob(jobs);

        Optional<CacheFile> cacheFileOpt = cacheService.findByChecksum(fileReference.getMetaInfo().getChecksum());
        Assert.assertTrue("File should be present in cache", cacheFileOpt.isPresent());
        assertEquals("File should be present in cache",
                     cacheService.getFilePath(fileReference.getMetaInfo().getChecksum()),
                     cacheFileOpt.get().getLocation().getPath());

        // When : now the file is available in cache try to download it again.
        downloadableFile = Try.of(() -> fileDownloadService.downloadFile(fileReference.getMetaInfo().getChecksum()))
                              .mapTry(Callable::call);

        // Then
        assertTrue(downloadableFile.isSuccess());

        DownloadableFile file = downloadableFile.get();
        Assert.assertNotNull("File should be downloadable", file);
        Assert.assertNotNull("File should be downloadable", file.getFileInputStream());
        assertEquals("File should be downloadable with a valid name",
                     fileReference.getMetaInfo().getFileName(),
                     file.getFileName());
        assertEquals("File should be downloadable with a valid mime type",
                     fileReference.getMetaInfo().getMimeType(),
                     file.getMimeType());
    }

    @Test
    public void test_downloadFileReference_confirmed_nearline() throws InterruptedException, ExecutionException {
        // Given
        FileReference fileReference = generateRandomStoredNearlineFileReference();
        fileReference.setNearlineConfirmed(true);
        fileRefService.store(fileReference);

        // When
        Try<DownloadableFile> result = Try.of(() -> fileDownloadService.downloadFile(fileReference.getMetaInfo()
                                                                                                  .getChecksum()))
                                          .mapTry(Callable::call);

        // Then
        assertTrue("File should not be available for download as it is confirmed in nearline", result.isFailure());
        assertTrue(result.getCause() instanceof NearlineFileNotAvailableException);
        assertTrue(result.getCause()
                         .getMessage()
                         .contains("because the file is located and confirmed in nearline storage"));
    }

    @Test
    public void testGenerateDownloadUrl() throws ModuleException {
        // Given
        Assert.assertTrue(downloadTokenRepo.findAll().isEmpty());
        // When
        downloadTokenService.generateDownloadUrl(UUID.randomUUID().toString());
        // Then
        assertEquals(1, downloadTokenRepo.findAll().size());

        // Given, when
        downloadTokenRepo.save(DownloadToken.build("plop", "pllip", OffsetDateTime.now().minusHours(2)));
        // Then
        assertEquals(2, downloadTokenRepo.findAll().size());
        // When
        downloadTokenService.purgeTokens();
        // Then
        assertEquals(1, downloadTokenRepo.findAll().size());
    }

    @Test
    public void downloadFileTypeDependsOnFileReferenceType() {
        // Given
        Random random = new Random();
        DataType[] typesCache = DataType.values();

        IntStream.range(0, 50).forEach(i -> Try.run(() -> {
            DataType type = typesCache[random.nextInt(typesCache.length)];
            FileReference fileRef = generateStoredFileReference(UUID.randomUUID().toString(),
                                                                "someone",
                                                                "file.test",
                                                                ONLINE_CONF_LABEL,
                                                                Optional.empty(),
                                                                Optional.of(type.name()),
                                                                SESSION_OWNER,
                                                                SESSION);

            // When
            DownloadableFile dlFile = Try.of(() -> fileDownloadService.downloadFile(fileRef.getMetaInfo()
                                                                                           .getChecksum()))
                                         .mapTry(Callable::call)
                                         .get();

            // Then
            assertTrue(type.equals(DataType.RAWDATA) ?
                           dlFile instanceof FileDownloadService.QuotaLimitedDownloadableFile :
                           dlFile instanceof FileDownloadService.StandardDownloadableFile);
        }));
    }
}
