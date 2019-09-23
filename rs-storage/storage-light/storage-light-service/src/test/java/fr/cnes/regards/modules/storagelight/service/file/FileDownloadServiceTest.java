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
package fr.cnes.regards.modules.storagelight.service.file;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.storagelight.domain.DownloadableFile;
import fr.cnes.regards.modules.storagelight.domain.database.CacheFile;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.service.AbstractStorageTest;

/**
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_download_tests",
        "regards.storage.cache.path=target/cache" })
public class FileDownloadServiceTest extends AbstractStorageTest {

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();
    }

    @Test
    public void downloadFileReferenceOnline()
            throws ModuleException, InterruptedException, ExecutionException, FileNotFoundException {
        downloadService.downloadFile(this.generateRandomStoredOnlineFileReference().getMetaInfo().getChecksum());
    }

    @Test
    public void downloadFileReferenceOffLine()
            throws ModuleException, InterruptedException, ExecutionException, FileNotFoundException {
        FileReference fileRef = this
                .referenceFile(UUID.randomUUID().toString(), "owner", null, "file.test", "somewhere").get();
        try {
            downloadService.downloadFile(fileRef.getMetaInfo().getChecksum());
            Assert.fail("File should not be available for download as it is not handled by a known storage location plugin");
        } catch (ModuleException e) {
            // Nothing to do
        }
    }

    @Test
    public void downloadFileReferenceNearline()
            throws ModuleException, InterruptedException, ExecutionException, IOException {
        FileReference fileRef = this.generateRandomStoredNearlineFileReference();
        try {
            downloadService.downloadFile(fileRef.getMetaInfo().getChecksum());
            Assert.fail("File should not be available for download as it is not online");
        } catch (EntityNotFoundException e) {
            // A cache request should be created
            Optional<FileCacheRequest> oReq = fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum());
            Assert.assertTrue("FileCacheRequest should be createdd", oReq.isPresent());
            Assert.assertEquals("FileCacheRequest should be created to retrieve file from nearline storage",
                                NEARLINE_CONF_LABEL, oReq.get().getStorage());
            Assert.assertEquals("FileCacheRequest should be created to retrieve file from nearline storage",
                                FileRequestStatus.TODO, oReq.get().getStatus());
            Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TODO);
            runAndWaitJob(jobs);

            Optional<CacheFile> oCf = cacheService.search(fileRef.getMetaInfo().getChecksum());
            Assert.assertTrue("File should be present in cache", oCf.isPresent());
            Assert.assertEquals("File should be present in cache",
                                cacheService.getFilePath(fileRef.getMetaInfo().getChecksum()),
                                oCf.get().getLocation().getPath().toString());

            // Now the file is available in cache try to download it again.
            DownloadableFile file = downloadService.downloadFile(fileRef.getMetaInfo().getChecksum());
            Assert.assertNotNull("File should be downloadable", file);
            Assert.assertNotNull("File should be downloadable", file.getFileInputStream());
            Assert.assertEquals("File should be downloadable with a valid name", fileRef.getMetaInfo().getFileName(),
                                file.getFileName());
            Assert.assertEquals("File should be downloadable with a valid mime type",
                                fileRef.getMetaInfo().getMimeType(), file.getMimeType());
            file.getFileInputStream().close();
        }
    }

    @Test(expected = EntityNotFoundException.class)
    public void download_without_cache() throws InterruptedException, ExecutionException, EntityNotFoundException {
        FileReference fileRef = this.generateRandomStoredNearlineFileReference();
        try {
            downloadService.download(fileRef);
        } finally {
            Assert.assertTrue("A cache request should be done for the near line file to download",
                              fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).isPresent());
        }
    }

    @Test
    public void download_with_cache()
            throws InterruptedException, ExecutionException, EntityNotFoundException, IOException {
        FileReference fileRef = this.generateRandomStoredNearlineFileReference();
        this.simulateFileInCache(fileRef.getMetaInfo().getChecksum());
        InputStream stream = downloadService.download(fileRef);
        Assert.assertNotNull(stream);
        stream.close();
    }

}
