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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.service.file.AbstractStorageTest;

/**
 * @author sbinda
 *
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_storage_tests",
        "regards.storage.cache.path=target/cache" })
public class FileSstorageServiceRequestTest extends AbstractStorageTest {

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();
    }

    @Test
    public void retryMultipleStoreErrors() throws InterruptedException, ExecutionException, ModuleException {
        FileStorageRequest fileRefReq = this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        // Update plugin conf to now accept error files
        this.updatePluginConfForError("unknown.*");
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.ERROR, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        jobService.runJob(jobs.iterator().next(), tenant).get();
        runtimeTenantResolver.forceTenant(tenant);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        Page<FileStorageRequest> fileRefReqs = stoReqService.search(fileRefReq.getStorage(),
                                                                    PageRequest.of(0, 1000, Direction.ASC, "id"));
        Page<FileReference> fileRefs = fileRefService.search(fileRefReq.getStorage(),
                                                             PageRequest.of(0, 1000, Direction.ASC, "id"));
        Assert.assertEquals("File references should have been created.", 3, fileRefs.getContent().size());
        Assert.assertTrue("File reference requests should not exists anymore", fileRefReqs.getContent().isEmpty());
    }

    @Test
    public void retryMultipleStoreErrorsByOwner() throws InterruptedException, ExecutionException, ModuleException {
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        FileStorageRequest fileRefReq1 = this.generateStoreFileError("someone-else", ONLINE_CONF_LABEL);
        FileStorageRequest fileRefReq2 = this.generateStoreFileError("someone-else", ONLINE_CONF_LABEL);
        // Update plugin conf to now accept error files
        this.updatePluginConfForError("unknown.*");
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Set<String> owners = Sets.newHashSet("someone-else");
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.ERROR, null, owners);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        jobService.runJob(jobs.iterator().next(), tenant).get();
        runtimeTenantResolver.forceTenant(tenant);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        Page<FileStorageRequest> fileRefReqs = stoReqService.search(ONLINE_CONF_LABEL,
                                                                    PageRequest.of(0, 1000, Direction.ASC, "id"));
        Page<FileReference> fileRefs = fileRefService.search(ONLINE_CONF_LABEL,
                                                             PageRequest.of(0, 1000, Direction.ASC, "id"));
        Assert.assertEquals("File references should have been created for the given owner.", 2,
                            fileRefs.getContent().size());
        Assert.assertTrue("File references should have been created for the given owner.", fileRefs.getContent()
                .stream()
                .anyMatch(fr -> fr.getMetaInfo().getChecksum().equals(fileRefReq1.getMetaInfo().getChecksum())));
        Assert.assertTrue("File references should have been created for the given owner.", fileRefs.getContent()
                .stream()
                .anyMatch(fr -> fr.getMetaInfo().getChecksum().equals(fileRefReq2.getMetaInfo().getChecksum())));
        Assert.assertEquals("File reference requests should not exists anymore for the given owner", 3,
                            fileRefReqs.getContent().size());
    }

    @Test
    public void retryMultipleStoreErrorsByStorage() throws InterruptedException, ExecutionException, ModuleException {
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        FileStorageRequest fileRefReqOther = this.generateStoreFileError("someone", "other-target");
        // Update plugin conf to now accept error files
        this.updatePluginConfForError("unknown.*");
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Set<String> storages = Sets.newHashSet(ONLINE_CONF_LABEL);
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.ERROR, storages, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        jobService.runJob(jobs.iterator().next(), tenant).get();
        runtimeTenantResolver.forceTenant(tenant);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        Page<FileStorageRequest> fileRefReqs = stoReqService.search(PageRequest.of(0, 1000, Direction.ASC, "id"));
        Page<FileReference> fileRefs = fileRefService.search(PageRequest.of(0, 1000, Direction.ASC, "id"));
        Assert.assertEquals("File references should have been created.", 2, fileRefs.getContent().size());
        Assert.assertEquals("File reference requests should not exists anymore for given storage", 1,
                            fileRefReqs.getContent().size());
        Assert.assertTrue("File references request should still exists for other storage.", fileRefReqs.getContent()
                .stream()
                .anyMatch(frr -> frr.getMetaInfo().getChecksum().equals(fileRefReqOther.getMetaInfo().getChecksum())));
    }

    @Test
    public void retryStoreErrors() throws InterruptedException, ExecutionException, ModuleException {
        FileStorageRequest fileRefReq = this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        // Update plugin conf to now accept error files
        this.updatePluginConfForError("unknown.*");
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.ERROR, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        runAndWaitJob(jobs);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        Optional<FileStorageRequest> oFileRefReq = stoReqService.search(fileRefReq.getStorage(),
                                                                        fileRefReq.getMetaInfo().getChecksum());
        Optional<FileReference> oFileRef = fileRefService.search(fileRefReq.getStorage(),
                                                                 fileRefReq.getMetaInfo().getChecksum());
        Assert.assertTrue("File reference should have been created.", oFileRef.isPresent());
        Assert.assertFalse("File reference request should not exists anymore", oFileRefReq.isPresent());
    }

    @Test
    public void storeImageWithOnlinePlugin()
            throws InterruptedException, ExecutionException, IOException, NoSuchAlgorithmException {
        File inputImage = Paths.get("src/test/resources/input/cnes.png").toFile();
        InputStream stream = com.google.common.io.Files.asByteSource(inputImage).openStream();
        String checksum = ChecksumUtils.computeHexChecksum(stream, "MD5");
        stream.close();
        String owner = "someone";
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum, "MD5", inputImage.getName(),
                inputImage.getTotalSpace(), MediaType.IMAGE_PNG);
        URL origin = new URL("file", "localhost", inputImage.getAbsolutePath());
        FileLocation destination = new FileLocation(ONLINE_CONF_LABEL, "/in/this/directory");
        // Run file reference creation.
        stoReqService.handleRequest(owner, fileMetaInfo, origin, ONLINE_CONF_LABEL, Optional.of("/in/this/directory"),
                                    UUID.randomUUID().toString());
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TODO, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        runAndWaitJob(jobs);

        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), checksum);
        Assert.assertTrue("File reference should have been created.", oFileRef.isPresent());
        Assert.assertEquals("File reference should have been created.", 499,
                            oFileRef.get().getMetaInfo().getWidth().intValue());
        Assert.assertEquals("File reference should have been created.", 362,
                            oFileRef.get().getMetaInfo().getHeight().intValue());
    }

    @Test
    public void storeWithNearlinePlugin() throws InterruptedException, ExecutionException {
        this.generateRandomStoredNearlineFileReference();
    }

    @Test
    public void storeWithNearlinePluginError() throws InterruptedException, ExecutionException {
        this.generateStoreFileError("someone", NEARLINE_CONF_LABEL);
    }

    @Test
    public void storeWithNotHandledFiles() throws InterruptedException, ExecutionException {

        String owner = "someone";
        // Add a file reference request for a file that will not be handled by the storage plugin (ignored by his name in the test plugin)
        String checksumNotHandled = UUID.randomUUID().toString();
        String fileNameNotHandled = "doNotHandle.file.test";
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksumNotHandled, "MD5", fileNameNotHandled,
                132L, MediaType.APPLICATION_OCTET_STREAM);
        stoReqService.handleRequest(owner, fileMetaInfo, originUrl, ONLINE_CONF_LABEL,
                                    Optional.of("/in/this/directory"), UUID.randomUUID().toString());
        // Add a valid one for storage
        String fileNameHandled = "file.test";
        String checksumHandled = UUID.randomUUID().toString();
        fileMetaInfo = new FileReferenceMetaInfo(checksumHandled, "MD5", fileNameHandled, 132L,
                MediaType.APPLICATION_OCTET_STREAM);
        stoReqService.handleRequest(owner, fileMetaInfo, originUrl, ONLINE_CONF_LABEL,
                                    Optional.of("/in/this/directory"), UUID.randomUUID().toString());

        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TODO, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        runAndWaitJob(jobs);

        Optional<FileReference> fileRef = fileRefService.search(ONLINE_CONF_LABEL, checksumNotHandled);
        Optional<FileStorageRequest> req = stoReqService.search(ONLINE_CONF_LABEL, checksumNotHandled);
        Assert.assertFalse("File reference should not exists has the file to store has not been handled",
                           fileRef.isPresent());
        Assert.assertTrue("File reference request should still exists has the file to store has not been handled",
                          req.isPresent());
        fileRef = fileRefService.search(ONLINE_CONF_LABEL, checksumHandled);
        req = stoReqService.search(ONLINE_CONF_LABEL, checksumHandled);
        Assert.assertTrue("File reference should exists has the file to store has been handled", fileRef.isPresent());
        Assert.assertFalse("File reference request should not exists anymore has the file to store has been handled",
                           req.isPresent());
    }

    @Test
    public void storeWithOnlinePlugin() throws InterruptedException, ExecutionException {
        this.generateRandomStoredOnlineFileReference();
    }

    @Test
    public void storeWithOnlinePluginError() throws InterruptedException, ExecutionException {
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
    }

    @Test
    public void storeWithUnknownStorageLocation() throws MalformedURLException {
        String owner = "someone";
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo("invalid_checksum", "MD5", "file.test", 132L,
                MediaType.APPLICATION_OCTET_STREAM);
        URL originUrl = new URL("file://in/this/directory/file.test");
        FileLocation destination = new FileLocation("elsewhere", "elsewhere://in/this/directory/file.test");
        stoReqService.handleRequest(owner, fileMetaInfo, originUrl, "elsewhere",
                                    Optional.of("elsewhere://in/this/directory/file.test"),
                                    UUID.randomUUID().toString());
        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        Optional<FileStorageRequest> oFileRefReq = stoReqService.search(destination.getStorage(),
                                                                        fileMetaInfo.getChecksum());
        Assert.assertFalse("File reference should not have been created. As storage is not possible into an unkown storage location",
                           oFileRef.isPresent());
        Assert.assertTrue("File reference request should exists", oFileRefReq.isPresent());
        Assert.assertTrue("File reference request should be in STORE_ERROR status",
                          oFileRefReq.get().getStatus().equals(FileRequestStatus.ERROR));
    }

}
