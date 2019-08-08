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
package fr.cnes.regards.modules.storagelight.service.file.reference;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.storagelight.dao.FileReferenceSpecification;
import fr.cnes.regards.modules.storagelight.domain.DownloadableFile;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.CacheFile;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEventState;
import fr.cnes.regards.modules.storagelight.service.file.reference.flow.FileReferenceEventHandler;
import fr.cnes.regards.modules.storagelight.service.file.reference.job.FileDeletionJobProgressManager;
import fr.cnes.regards.modules.storagelight.service.file.reference.job.FileDeletionRequestJob;

/**
 * @author sbinda
 *
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests",
        "regards.storage.cache.path=target/cache" })
public class FileReferenceServiceTest extends AbstractFileReferenceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceServiceTest.class);

    @Autowired
    private FileReferenceEventHandler fileRefHandler;

    @Before
    public void initialize() throws ModuleException {
        super.init();
    }

    @Test
    public void storeWithUnknownStorageLocation() throws MalformedURLException {
        String owner = "someone";
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo("invalid_checksum", "MD5", "file.test", 132L,
                MediaType.APPLICATION_OCTET_STREAM);
        URL originUrl = new URL("file://in/this/directory/file.test");
        FileLocation destination = new FileLocation("elsewhere", "elsewhere://in/this/directory/file.test");
        fileRefService.storeFile(owner, fileMetaInfo, originUrl, "elsewhere",
                                 Optional.of("elsewhere://in/this/directory/file.test"), UUID.randomUUID().toString());
        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        Optional<FileStorageRequest> oFileRefReq = fileStorageRequestService.search(destination.getStorage(),
                                                                                    fileMetaInfo.getChecksum());
        Assert.assertFalse("File reference should not have been created. As storage is not possible into an unkown storage location",
                           oFileRef.isPresent());
        Assert.assertTrue("File reference request should exists", oFileRefReq.isPresent());
        Assert.assertTrue("File reference request should be in STORE_ERROR status",
                          oFileRefReq.get().getStatus().equals(FileRequestStatus.ERROR));
    }

    @Test
    public void referenceFileWithoutStorage() {
        String owner = "someone";
        Optional<FileReference> oFileRef = referenceRandomFile(owner, null, "file.test", "anywhere");
        Assert.assertTrue("File reference should have been created", oFileRef.isPresent());
        Optional<FileStorageRequest> oFileRefReq = fileStorageRequestService
                .search(oFileRef.get().getLocation().getStorage(), oFileRef.get().getMetaInfo().getChecksum());
        Assert.assertTrue("File reference request should not exists anymore as file is well referenced",
                          !oFileRefReq.isPresent());
    }

    @Test
    public void search() throws InterruptedException {
        // 1. Add reference for search tests
        String owner = "someone";
        OffsetDateTime beforeDate = OffsetDateTime.now().minusSeconds(1);
        FileReference fileRef = referenceRandomFile(owner, null, "file1.test", "anywhere").get();
        OffsetDateTime afterFirstDate = OffsetDateTime.now();
        Thread.sleep(1);
        referenceRandomFile("someone-else", "Type1", "file2.test", "somewhere-else");
        referenceRandomFile("someone-else", "Type2", "file3.test", "somewhere-else");
        referenceRandomFile("someone-else", "Test", "data_4.nc", "somewhere-else");
        referenceRandomFile("someone-else", "Test", "data_5.nc", "void");
        OffsetDateTime afterEndDate = OffsetDateTime.now().plusSeconds(1);

        // Search all
        Assert.assertEquals("There should be 5 file references.", 5,
                            fileRefService.search(PageRequest.of(0, 100, Direction.ASC, "id")).getTotalElements());
        // Search by fileName
        Assert.assertEquals("There should be one file references named file1.test.", 1, fileRefService
                .search(FileReferenceSpecification.search("file1.test", null, null, null, null, null, null),
                        PageRequest.of(0, 100, Direction.ASC, "id"))
                .getTotalElements());
        Assert.assertEquals("There should be 3 file references with name containing file", 3, fileRefService
                .search(FileReferenceSpecification.search("file", null, null, null, null, null, null),
                        PageRequest.of(0, 100, Direction.ASC, "id"))
                .getTotalElements());
        // Search by checksum
        Assert.assertEquals("There should be one file references with checksum given", 1, fileRefService
                .search(FileReferenceSpecification.search(null, fileRef.getMetaInfo().getChecksum(), null, null, null,
                                                          null, null),
                        PageRequest.of(0, 100, Direction.ASC, "id"))
                .getTotalElements());
        // Search by storage
        Assert.assertEquals("There should be 5 file references in given storages", 5, fileRefService
                .search(FileReferenceSpecification.search(null, null, null,
                                                          Sets.newHashSet("anywhere", "somewhere-else", "void"), null,
                                                          null, null),
                        PageRequest.of(0, 100, Direction.ASC, "id"))
                .getTotalElements());
        Assert.assertEquals("There should be 3 file references in given storages", 3, fileRefService
                .search(FileReferenceSpecification.search(null, null, null, Sets.newHashSet("somewhere-else"), null,
                                                          null, null),
                        PageRequest.of(0, 100, Direction.ASC, "id"))
                .getTotalElements());
        // Search by type
        Assert.assertEquals("There should be 0 file references for given type", 0, fileRefService
                .search(FileReferenceSpecification.search(null, null, Lists.newArrayList("Type0"), null, null, null,
                                                          null),
                        PageRequest.of(0, 100, Direction.ASC, "id"))
                .getTotalElements());
        Assert.assertEquals("There should be 1 file references for given type", 1, fileRefService
                .search(FileReferenceSpecification.search(null, null, Sets.newHashSet("Type2"), null, null, null, null),
                        PageRequest.of(0, 100, Direction.ASC, "id"))
                .getTotalElements());
        // Search by date
        Assert.assertEquals("There should be 5 file references for given from date", 5, fileRefService
                .search(FileReferenceSpecification.search(null, null, null, null, null, beforeDate, null),
                        PageRequest.of(0, 100, Direction.ASC, "id"))
                .getTotalElements());
        Assert.assertEquals("There should be 4 file references for given from and to date", 4, fileRefService
                .search(FileReferenceSpecification.search(null, null, null, null, null, afterFirstDate, afterEndDate),
                        PageRequest.of(0, 100, Direction.ASC, "id"))
                .getTotalElements());

    }

    @Test
    public void referenceFileDuringDeletion() throws InterruptedException, ExecutionException, EntityNotFoundException {

        String tenant = runtimeTenantResolver.getTenant();
        // Reference & store a file
        String fileRefChecksum = "file-ref-1";
        String fileRefOwner = "first-owner";
        FileReference fileRef = this.generateStoredFileReference(fileRefChecksum, fileRefOwner, "file.test",
                                                                 ONLINE_CONF_LABEL);
        String fileRefStorage = fileRef.getLocation().getStorage();

        // Remove all his owners
        fileRefService.removeOwner(fileRefChecksum, fileRefStorage, fileRefOwner, false, UUID.randomUUID().toString());

        Optional<FileReference> oFileRef = fileRefService.search(fileRefStorage, fileRefChecksum);
        Assert.assertTrue("File reference should no have any owners anymore", oFileRef.get().getOwners().isEmpty());

        // Simulate FileDeletionRequest in PENDING state
        FileDeletionRequest fdr = fileDeletionRequestRepo.findByFileReferenceId(fileRef.getId()).get();
        fdr.setStatus(FileRequestStatus.PENDING);
        fileDeletionRequestRepo.save(fdr);

        // Reference the same file for a new owner
        String fileRefNewOwner = "new-owner";
        this.generateStoredFileReferenceAlreadyReferenced(fileRefChecksum, fileRefStorage, fileRefNewOwner);

        // check that there is always a deletion request in pending state
        Optional<FileDeletionRequest> ofdr = fileDeletionRequestRepo.findByFileReferenceId(fdr.getId());
        oFileRef = fileRefService.search(fileRef.getLocation().getStorage(), fileRef.getMetaInfo().getChecksum());
        Assert.assertTrue("File deletion request should elawys exists", ofdr.isPresent());
        Assert.assertEquals("File deletion request should always be running", FileRequestStatus.PENDING,
                            ofdr.get().getStatus());
        // check that a new reference request is made to store again the file after deletion request is done
        Optional<FileStorageRequest> frr = fileStorageRequestService.search(fileRefStorage, fileRefChecksum);
        Assert.assertTrue("A new file reference request should exists", frr.isPresent());
        Assert.assertEquals("A new file reference request should exists with DELAYED status", FileRequestStatus.DELAYED,
                            frr.get().getStatus());

        // Check that the file reference is still not referenced as owned by the new owner and the request is still existing
        oFileRef = fileRefService.search(fileRefStorage, fileRefChecksum);
        Assert.assertTrue("File reference should still exists", oFileRef.isPresent());
        Assert.assertTrue("File reference should still have no owners", oFileRef.get().getOwners().isEmpty());

        // Simulate deletion request ends
        FileDeletionJobProgressManager manager = new FileDeletionJobProgressManager(fileRefService,
                fileDeletionRequestService, publisher, new FileDeletionRequestJob());
        manager.deletionSucceed(fdr);
        fileRefHandler.handle(
                              new TenantWrapper<>(
                                      new FileReferenceEvent(fileRefChecksum, FileReferenceEventState.FULLY_DELETED,
                                              null, "Deletion succeed", oFileRef.get().getLocation()),
                                      runtimeTenantResolver.getTenant()));
        // Has the handler clear the tenant we have to force it here for tests.
        runtimeTenantResolver.forceTenant(tenant);
        frr = fileStorageRequestService.search(fileRefStorage, fileRefChecksum);
        Assert.assertTrue("File storage request still exists", frr.isPresent());
        Assert.assertEquals("File storage request still exists with TODO status", FileRequestStatus.TODO,
                            frr.get().getStatus());

        // Now the deletion job is ended, the file reference request is in todo state.
        Collection<JobInfo> jobs = fileStorageRequestService
                .scheduleJobs(FileRequestStatus.TODO, Lists.newArrayList(fileRefStorage), Lists.newArrayList());
        runAndWaitJob(jobs);

        frr = fileStorageRequestService.search(fileRefStorage, fileRefChecksum);
        oFileRef = fileRefService.search(fileRefStorage, fileRefChecksum);
        Assert.assertFalse("File storage request should not exists anymore", frr.isPresent());
        Assert.assertTrue("File reference should still exists", oFileRef.isPresent());
        Assert.assertTrue("File reference should belongs to new owner",
                          oFileRef.get().getOwners().contains(fileRefNewOwner));
    }

    @Test
    public void storeWithOnlinePlugin() throws InterruptedException, ExecutionException {
        this.generateRandomStoredOnlineFileReference();
    }

    @Test
    public void storeWithNearlinePlugin() throws InterruptedException, ExecutionException {
        this.generateRandomStoredNearlineFileReference();
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
        fileRefService.storeFile(owner, fileMetaInfo, origin, ONLINE_CONF_LABEL, Optional.of("/in/this/directory"),
                                 UUID.randomUUID().toString());
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = fileStorageRequestService.scheduleJobs(FileRequestStatus.TODO, null, null);
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
    public void storeWithOnlinePluginError() throws InterruptedException, ExecutionException {
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
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
        fileRefService.storeFile(owner, fileMetaInfo, originUrl, ONLINE_CONF_LABEL, Optional.of("/in/this/directory"),
                                 UUID.randomUUID().toString());
        // Add a valid one for storage
        String fileNameHandled = "file.test";
        String checksumHandled = UUID.randomUUID().toString();
        fileMetaInfo = new FileReferenceMetaInfo(checksumHandled, "MD5", fileNameHandled, 132L,
                MediaType.APPLICATION_OCTET_STREAM);
        fileRefService.storeFile(owner, fileMetaInfo, originUrl, ONLINE_CONF_LABEL, Optional.of("/in/this/directory"),
                                 UUID.randomUUID().toString());

        Collection<JobInfo> jobs = fileStorageRequestService.scheduleJobs(FileRequestStatus.TODO, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        runAndWaitJob(jobs);

        Optional<FileReference> fileRef = fileRefService.search(ONLINE_CONF_LABEL, checksumNotHandled);
        Optional<FileStorageRequest> req = fileStorageRequestService.search(ONLINE_CONF_LABEL, checksumNotHandled);
        Assert.assertFalse("File reference should not exists has the file to store has not been handled",
                           fileRef.isPresent());
        Assert.assertTrue("File reference request should still exists has the file to store has not been handled",
                          req.isPresent());
        fileRef = fileRefService.search(ONLINE_CONF_LABEL, checksumHandled);
        req = fileStorageRequestService.search(ONLINE_CONF_LABEL, checksumHandled);
        Assert.assertTrue("File reference should exists has the file to store has been handled", fileRef.isPresent());
        Assert.assertFalse("File reference request should not exists anymore has the file to store has been handled",
                           req.isPresent());
    }

    @Test
    public void retryStoreErrors() throws InterruptedException, ExecutionException, ModuleException {
        FileStorageRequest fileRefReq = this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        // Update plugin conf to now accept error files
        this.updatePluginConfForError("unknown.*");
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = fileStorageRequestService.scheduleJobs(FileRequestStatus.ERROR, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        runAndWaitJob(jobs);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        Optional<FileStorageRequest> oFileRefReq = fileStorageRequestService
                .search(fileRefReq.getStorage(), fileRefReq.getMetaInfo().getChecksum());
        Optional<FileReference> oFileRef = fileRefService.search(fileRefReq.getStorage(),
                                                                 fileRefReq.getMetaInfo().getChecksum());
        Assert.assertTrue("File reference should have been created.", oFileRef.isPresent());
        Assert.assertFalse("File reference request should not exists anymore", oFileRefReq.isPresent());
    }

    @Test
    public void retryMultipleStoreErrors() throws InterruptedException, ExecutionException, ModuleException {
        FileStorageRequest fileRefReq = this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL);
        // Update plugin conf to now accept error files
        this.updatePluginConfForError("unknown.*");
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = fileStorageRequestService.scheduleJobs(FileRequestStatus.ERROR, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        jobService.runJob(jobs.iterator().next(), tenant).get();
        runtimeTenantResolver.forceTenant(tenant);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        Page<FileStorageRequest> fileRefReqs = fileStorageRequestService
                .search(fileRefReq.getStorage(), PageRequest.of(0, 1000, Direction.ASC, "id"));
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
        Collection<JobInfo> jobs = fileStorageRequestService.scheduleJobs(FileRequestStatus.ERROR, null, owners);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        jobService.runJob(jobs.iterator().next(), tenant).get();
        runtimeTenantResolver.forceTenant(tenant);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        Page<FileStorageRequest> fileRefReqs = fileStorageRequestService
                .search(ONLINE_CONF_LABEL, PageRequest.of(0, 1000, Direction.ASC, "id"));
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
        Collection<JobInfo> jobs = fileStorageRequestService.scheduleJobs(FileRequestStatus.ERROR, storages, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        jobService.runJob(jobs.iterator().next(), tenant).get();
        runtimeTenantResolver.forceTenant(tenant);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        Page<FileStorageRequest> fileRefReqs = fileStorageRequestService
                .search(PageRequest.of(0, 1000, Direction.ASC, "id"));
        Page<FileReference> fileRefs = fileRefService.search(PageRequest.of(0, 1000, Direction.ASC, "id"));
        Assert.assertEquals("File references should have been created.", 2, fileRefs.getContent().size());
        Assert.assertEquals("File reference requests should not exists anymore for given storage", 1,
                            fileRefReqs.getContent().size());
        Assert.assertTrue("File references request should still exists for other storage.", fileRefReqs.getContent()
                .stream()
                .anyMatch(frr -> frr.getMetaInfo().getChecksum().equals(fileRefReqOther.getMetaInfo().getChecksum())));
    }

    @Test
    public void downloadFileReferenceOnline() throws ModuleException, InterruptedException, ExecutionException {
        fileRefService.downloadFile(this.generateRandomStoredOnlineFileReference().getMetaInfo().getChecksum());
    }

    @Test
    public void downloadFileReferenceOffLine() throws ModuleException, InterruptedException, ExecutionException {
        FileReference fileRef = this
                .referenceFile(UUID.randomUUID().toString(), "owner", null, "file.test", "somewhere").get();
        try {
            fileRefService.downloadFile(fileRef.getMetaInfo().getChecksum());
            Assert.fail("File should not be available for download as it is not handled by a known storage location plugin");
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Test
    public void downloadFileReferenceNearline()
            throws ModuleException, InterruptedException, ExecutionException, IOException {
        FileReference fileRef = this.generateRandomStoredNearlineFileReference();
        try {
            fileRefService.downloadFile(fileRef.getMetaInfo().getChecksum());
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
            DownloadableFile file = fileRefService.downloadFile(fileRef.getMetaInfo().getChecksum());
            Assert.assertNotNull("File should be downloadable", file);
            Assert.assertNotNull("File should be downloadable", file.getFileInputStream());
            Assert.assertEquals("File should be downloadable with a valid name", fileRef.getMetaInfo().getFileName(),
                                file.getFileName());
            Assert.assertEquals("File should be downloadable with a valid mime type",
                                fileRef.getMetaInfo().getMimeType(), file.getMimeType());
            file.getFileInputStream().close();
        }
    }

    @Test
    public void restore() throws InterruptedException, ExecutionException {
        FileReference fileRef = this.generateRandomStoredNearlineFileReference("file-nl-1.test");
        fileRefService.makeAvailable(Sets.newHashSet(fileRef.getMetaInfo().getChecksum()),
                                     OffsetDateTime.now().plusDays(1), UUID.randomUUID().toString());
        Assert.assertTrue("A cache request should be created",
                          fileCacheRequestService.search(fileRef.getMetaInfo().getChecksum()).isPresent());

        Collection<JobInfo> jobs = fileCacheRequestService.scheduleJobs(FileRequestStatus.TODO);
        runAndWaitJob(jobs);

        Assert.assertTrue("file should be restored in cache",
                          Files.exists(Paths.get(cacheService.getFilePath(fileRef.getMetaInfo().getChecksum()))));
    }
}
