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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

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

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_storage_tests" },
    locations = { "classpath:application-test.properties" })
public class FileStorageServiceRequestIT extends AbstractStorageIT {

    private static final String SESSION_OWNER_1 = "SOURCE 1";

    private static final String SESSION_1 = "SESSION 1";

    private static final String SESSION_OWNER_2 = "SOURCE 2";

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();
    }

    @Test
    public void retryMultipleStoreErrors() throws InterruptedException, ExecutionException, ModuleException {
        FileStorageRequest fileRefReq = this.generateStoreFileError("someone",
                                                                    ONLINE_CONF_LABEL,
                                                                    SESSION_OWNER_1,
                                                                    SESSION_1);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL, SESSION_OWNER_1, SESSION_1);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL, SESSION_OWNER_1, SESSION_1);
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
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL, SESSION_OWNER_1, SESSION_1);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL, SESSION_OWNER_1, SESSION_1);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL, SESSION_OWNER_1, SESSION_1);
        FileStorageRequest fileRefReq1 = this.generateStoreFileError("someone-else",
                                                                     ONLINE_CONF_LABEL,
                                                                     SESSION_OWNER_2,
                                                                     SESSION_1);
        FileStorageRequest fileRefReq2 = this.generateStoreFileError("someone-else",
                                                                     ONLINE_CONF_LABEL,
                                                                     SESSION_OWNER_2,
                                                                     SESSION_1);
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
        Assert.assertEquals("File references should have been created for the given owner.",
                            2,
                            fileRefs.getContent().size());
        Assert.assertTrue("File references should have been created for the given owner.",
                          fileRefs.getContent()
                                  .stream()
                                  .anyMatch(fr -> fr.getMetaInfo()
                                                    .getChecksum()
                                                    .equals(fileRefReq1.getMetaInfo().getChecksum())));
        Assert.assertTrue("File references should have been created for the given owner.",
                          fileRefs.getContent()
                                  .stream()
                                  .anyMatch(fr -> fr.getMetaInfo()
                                                    .getChecksum()
                                                    .equals(fileRefReq2.getMetaInfo().getChecksum())));
        Assert.assertEquals("File reference requests should not exists anymore for the given owner",
                            3,
                            fileRefReqs.getContent().size());
    }

    @Test
    public void retryMultipleStoreErrorsByStorage() throws InterruptedException, ExecutionException, ModuleException {
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL, SESSION_OWNER_1, SESSION_1);
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL, SESSION_OWNER_1, SESSION_1);
        FileStorageRequest fileRefReqOther = this.generateStoreFileError("someone",
                                                                         "other-target",
                                                                         SESSION_OWNER_1,
                                                                         SESSION_1);
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
        Assert.assertEquals("File reference requests should not exists anymore for given storage",
                            1,
                            fileRefReqs.getContent().size());
        Assert.assertTrue("File references request should still exists for other storage.",
                          fileRefReqs.getContent()
                                     .stream()
                                     .anyMatch(frr -> frr.getMetaInfo()
                                                         .getChecksum()
                                                         .equals(fileRefReqOther.getMetaInfo().getChecksum())));
    }

    @Test
    public void retryStoreErrors() throws InterruptedException, ExecutionException, ModuleException {
        FileStorageRequest fileRefReq = this.generateStoreFileError("someone",
                                                                    ONLINE_CONF_LABEL,
                                                                    SESSION_OWNER_1,
                                                                    SESSION_1);
        // Update plugin conf to now accept error files
        this.updatePluginConfForError("unknown.*");
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.ERROR, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        runAndWaitJob(jobs);
        // After storage job is successfully done, the FileRefenrece should be created and the FileReferenceRequest should be removed.
        Collection<FileStorageRequest> storageReqs = stoReqService.search(fileRefReq.getStorage(),
                                                                          fileRefReq.getMetaInfo().getChecksum());
        Optional<FileReference> oFileRef = fileRefService.search(fileRefReq.getStorage(),
                                                                 fileRefReq.getMetaInfo().getChecksum());
        Assert.assertTrue("File reference should have been created.", oFileRef.isPresent());
        Assert.assertTrue("File reference request should not exists anymore", storageReqs.isEmpty());
    }

    @Test
    public void relaunchStoreErrorRequest() throws InterruptedException, ExecutionException, ModuleException {
        String checksum = UUID.randomUUID().toString();
        String storageDestination = "somewhere";
        String owner = "owner";
        String fileName = "error.file.test";
        String errorUrl = "file://somewhere/plop.test";
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum,
                                                                       "MD5",
                                                                       fileName,
                                                                       132L,
                                                                       MediaType.APPLICATION_OCTET_STREAM);
        FileLocation destination = new FileLocation(storageDestination, "/in/this/directory", false);
        // Run file reference creation.
        stoReqService.handleRequest(owner,
                                    SESSION_OWNER_1,
                                    SESSION_1,
                                    fileMetaInfo,
                                    errorUrl,
                                    storageDestination,
                                    Optional.of("/in/this/directory"),
                                    UUID.randomUUID().toString());
        // The file reference should exist yet cause a storage job is needed. Nevertheless a FileReferenceRequest should be created.
        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        Collection<FileStorageRequest> fileRefReqs = stoReqService.search(destination.getStorage(),
                                                                          fileMetaInfo.getChecksum());
        Assert.assertFalse("File reference should not have been created yet.", oFileRef.isPresent());
        Assert.assertEquals("File reference request should exists", 1, fileRefReqs.size());
        Assert.assertEquals("File reference request should be in STORE_ERROR status",
                            FileRequestStatus.ERROR,
                            fileRefReqs.stream().findFirst().get().getStatus());
        Assert.assertEquals("File reference request should be in STORE_ERROR status",
                            fileName,
                            fileRefReqs.stream().findFirst().get().getMetaInfo().getFileName());
        Assert.assertEquals("File reference request should be in STORE_ERROR status",
                            errorUrl,
                            fileRefReqs.stream().findFirst().get().getOriginUrl());

        String newFileName = "ok.file.test";
        fileMetaInfo = new FileReferenceMetaInfo(checksum,
                                                 "MD5",
                                                 "ok.file.test",
                                                 132L,
                                                 MediaType.APPLICATION_OCTET_STREAM);
        // Run file reference creation.
        stoReqService.handleRequest(owner,
                                    SESSION_OWNER_1,
                                    SESSION_1,
                                    fileMetaInfo,
                                    originUrl,
                                    storageDestination,
                                    Optional.of("/in/this/directory"),
                                    UUID.randomUUID().toString());

        fileRefReqs = stoReqService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        Assert.assertFalse("File reference should not have been created yet.", oFileRef.isPresent());
        Assert.assertEquals("File reference request should exists", 1, fileRefReqs.size());
        Assert.assertEquals("File reference request should be in STORE_ERROR status",
                            FileRequestStatus.TO_DO,
                            fileRefReqs.stream().findFirst().get().getStatus());
        Assert.assertEquals("File reference request should be in STORE_ERROR status",
                            newFileName,
                            fileRefReqs.stream().findFirst().get().getMetaInfo().getFileName());
        Assert.assertEquals("File reference request should be in STORE_ERROR status",
                            originUrl,
                            fileRefReqs.stream().findFirst().get().getOriginUrl());
    }

    @Test
    public void storeImageWithOnlinePlugin()
        throws InterruptedException, ExecutionException, IOException, NoSuchAlgorithmException {
        File inputImage = Paths.get("src/test/resources/input/cnes.png").toFile();
        InputStream stream = com.google.common.io.Files.asByteSource(inputImage).openStream();
        String checksum = ChecksumUtils.computeHexChecksum(stream, "MD5");
        stream.close();
        String owner = "someone";
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum,
                                                                       "MD5",
                                                                       inputImage.getName(),
                                                                       inputImage.getTotalSpace(),
                                                                       MediaType.IMAGE_PNG);
        URL origin = new URL("file", "localhost", inputImage.getAbsolutePath());
        FileLocation destination = new FileLocation(ONLINE_CONF_LABEL, "/in/this/directory", false);
        // Run file reference creation.
        stoReqService.handleRequest(owner,
                                    SESSION_OWNER_1,
                                    SESSION_1,
                                    fileMetaInfo,
                                    origin.toString(),
                                    ONLINE_CONF_LABEL,
                                    Optional.of("/in/this/directory"),
                                    UUID.randomUUID().toString());
        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        runAndWaitJob(jobs);

        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), checksum);
        Assert.assertTrue("File reference should have been created.", oFileRef.isPresent());
        Assert.assertEquals("File reference should have been created.",
                            499,
                            oFileRef.get().getMetaInfo().getWidth().intValue());
        Assert.assertEquals("File reference should have been created.",
                            362,
                            oFileRef.get().getMetaInfo().getHeight().intValue());
    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_AIP_010"), @Requirement("REGARDS_DSL_STOP_AIP_070") })
    @Purpose("System should be able to store files nearline.")
    public void storeWithNearlinePlugin() throws InterruptedException, ExecutionException {
        this.generateRandomStoredNearlineFileReference();
    }

    @Test
    public void storeWithNearlinePluginError() throws InterruptedException, ExecutionException {
        this.generateStoreFileError("someone", NEARLINE_CONF_LABEL, SESSION_OWNER_1, SESSION_1);
    }

    @Test
    public void storeWithNotHandledFiles() throws InterruptedException, ExecutionException, MalformedURLException {

        String owner = "someone";
        // Add a file reference request for a file that will not be handled by the storage plugin (ignored by his name in the test plugin)
        String checksumNotHandled = UUID.randomUUID().toString();
        String fileNameNotHandled = "doNotHandle.file.test";
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksumNotHandled,
                                                                       "MD5",
                                                                       fileNameNotHandled,
                                                                       132L,
                                                                       MediaType.APPLICATION_OCTET_STREAM);
        stoReqService.handleRequest(owner,
                                    SESSION_OWNER_1,
                                    SESSION_1,
                                    fileMetaInfo,
                                    originUrl,
                                    ONLINE_CONF_LABEL,
                                    Optional.of("/in/this/directory"),
                                    UUID.randomUUID().toString());
        // Add a valid one for storage
        String fileNameHandled = "file.test";
        String checksumHandled = UUID.randomUUID().toString();
        fileMetaInfo = new FileReferenceMetaInfo(checksumHandled,
                                                 "MD5",
                                                 fileNameHandled,
                                                 132L,
                                                 MediaType.APPLICATION_OCTET_STREAM);
        stoReqService.handleRequest(owner,
                                    SESSION_OWNER_1,
                                    SESSION_1,
                                    fileMetaInfo,
                                    originUrl,
                                    ONLINE_CONF_LABEL,
                                    Optional.of("/in/this/directory"),
                                    UUID.randomUUID().toString());

        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO, null, null);
        Assert.assertEquals("One storage job should scheduled", 1, jobs.size());
        // Run Job and wait for end
        runAndWaitJob(jobs);

        Optional<FileReference> fileRef = fileRefService.search(ONLINE_CONF_LABEL, checksumNotHandled);
        Collection<FileStorageRequest> storageReqs = stoReqService.search(ONLINE_CONF_LABEL, checksumNotHandled);
        Assert.assertFalse("File reference should not exists has the file to store has not been handled",
                           fileRef.isPresent());
        Assert.assertEquals("File reference request should still exists has the file to store has not been handled",
                            1,
                            storageReqs.size());
        fileRef = fileRefService.search(ONLINE_CONF_LABEL, checksumHandled);
        storageReqs = stoReqService.search(ONLINE_CONF_LABEL, checksumHandled);
        Assert.assertTrue("File reference should exists has the file to store has been handled", fileRef.isPresent());
        Assert.assertTrue("File reference request should not exists anymore has the file to store has been handled",
                          storageReqs.isEmpty());
    }

    @Requirements({ @Requirement("REGARDS_DSL_STO_AIP_010"), @Requirement("REGARDS_DSL_STOP_AIP_070") })
    @Purpose("System should be able to store files online.")
    @Test
    public void storeWithOnlinePlugin() throws InterruptedException, ExecutionException {
        this.generateRandomStoredOnlineFileReference();
    }

    @Requirement("REGARDS_DSL_STO_AIP_030")
    @Purpose("Check that an invalid URL is not accepted during a storage request.")
    @Test
    public void storeWithInvalidUrl() throws MalformedURLException {
        String checksum = UUID.randomUUID().toString();
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum,
                                                                       "MD5",
                                                                       "invalid.test",
                                                                       1024L,
                                                                       MediaType.APPLICATION_OCTET_STREAM);
        // Run file reference creation.
        stoReqService.handleRequest("someone",
                                    SESSION_OWNER_1,
                                    SESSION_1,
                                    fileMetaInfo,
                                    "invalid:/plop/file@.file",
                                    ONLINE_CONF_LABEL,
                                    Optional.empty(),
                                    UUID.randomUUID().toString());
        Collection<FileStorageRequest> storageReqs = stoReqService.search(ONLINE_CONF_LABEL,
                                                                          fileMetaInfo.getChecksum());
        Assert.assertEquals("Request sould be in error status as file url is not valid",
                            storageReqs.stream().findFirst().get().getStatus(),
                            FileRequestStatus.ERROR);
    }

    @Test
    public void storeWithOnlinePluginError() throws InterruptedException, ExecutionException {
        this.generateStoreFileError("someone", ONLINE_CONF_LABEL, SESSION_OWNER_1, SESSION_1);
    }

    @Test
    public void storeFileNearlineWithPendingActionRemaining() {
        String owner = "someone";
        // Add a file reference request for a file that will be stored with action pending remaining
        String checksum = UUID.randomUUID().toString();
        String fileName = "pending.file.test";
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum,
                                                                       "MD5",
                                                                       fileName,
                                                                       132L,
                                                                       MediaType.APPLICATION_OCTET_STREAM);
        stoReqService.handleRequest(owner,
                                    SESSION_OWNER_1,
                                    SESSION_1,
                                    fileMetaInfo,
                                    originUrl,
                                    NEARLINE_CONF_LABEL,
                                    Optional.of("/in/this/directory"),
                                    UUID.randomUUID().toString());
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO,
                                                              Lists.newArrayList(NEARLINE_CONF_LABEL),
                                                              Lists.newArrayList(owner));
        runAndWaitJob(jobs);
        Optional<FileReference> fileRef = fileRefService.search(NEARLINE_CONF_LABEL, checksum);
        Assert.assertTrue("File should be referenced", fileRef.isPresent());
        Assert.assertFalse("File should in stored state", fileRef.get().isReferenced());
        Assert.assertTrue("File should in stored state", fileRef.get().getLocation().isPendingActionRemaining());
    }

    @Test
    public void storeWithUnknownStorageLocation() throws MalformedURLException {
        String owner = "someone";
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo("invalid_checksum",
                                                                       "MD5",
                                                                       "file.test",
                                                                       132L,
                                                                       MediaType.APPLICATION_OCTET_STREAM);
        URL originUrl = new URL("file://in/this/directory/file.test");
        FileLocation destination = new FileLocation("elsewhere", "elsewhere://in/this/directory/file.test", false);
        stoReqService.handleRequest(owner,
                                    SESSION_OWNER_1,
                                    SESSION_1,
                                    fileMetaInfo,
                                    originUrl.toString(),
                                    "elsewhere",
                                    Optional.of("elsewhere://in/this/directory/file.test"),
                                    UUID.randomUUID().toString());
        Optional<FileReference> oFileRef = fileRefService.search(destination.getStorage(), fileMetaInfo.getChecksum());
        Collection<FileStorageRequest> storageReqs = stoReqService.search(destination.getStorage(),
                                                                          fileMetaInfo.getChecksum());
        Assert.assertFalse(
            "File reference should not have been created. As storage is not possible into an unkown storage location",
            oFileRef.isPresent());
        Assert.assertEquals("File reference request should exists", 1, storageReqs.size());
        Assert.assertTrue("File reference request should be in STORE_ERROR status",
                          storageReqs.stream().findFirst().get().getStatus().equals(FileRequestStatus.ERROR));
    }

}
