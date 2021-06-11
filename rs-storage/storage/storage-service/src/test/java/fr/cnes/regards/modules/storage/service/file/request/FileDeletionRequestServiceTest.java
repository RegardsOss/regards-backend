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
package fr.cnes.regards.modules.storage.service.file.request;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.commons.compress.utils.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

import com.netflix.discovery.converters.Auto;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storage.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storage.service.AbstractStorageTest;

/**
 * Test class
 *
 * @author Sébastien Binda
 *
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_deletion_tests" }, locations = { "classpath:application-test.properties" })
public class FileDeletionRequestServiceTest extends AbstractStorageTest {

    @Autowired
    private IDynamicTenantSettingService dynamicTenantSettingService;

    private static final  String SESSION_OWNER_1 = "SOURCE 1";

    private static final  String SESSION_OWNER_2 = "SOURCE 2";

    private static final  String SESSION_OWNER_3 = "SOURCE 3";

    private static final String SESSION_1 = "SESSION 1";

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();
        simulateApplicationStartedEvent();
        simulateApplicationReadyEvent();
        // we override cache setting values for tests
        dynamicTenantSettingService.update(StorageSetting.CACHE_PATH_NAME, Paths.get("target", "cache", getDefaultTenant()));
        dynamicTenantSettingService.update(StorageSetting.CACHE_MAX_SIZE_NAME, 5L);
    }

    @Test
    public void deleteAll() throws InterruptedException, ExecutionException, ModuleException {
        String owner = "first-owner";
        Long nbFiles = 20L;
        for (int i = 0; i < nbFiles; i++) {
            generateStoredFileReference(UUID.randomUUID().toString(), owner, String.format("file-%d.test", i),
                                        ONLINE_CONF_LABEL, Optional.empty(), Optional.empty(), SESSION_OWNER_1, SESSION_1);
        }
        JobInfo ji = fileDeletionRequestService.scheduleJob(ONLINE_CONF_LABEL, false, SESSION_OWNER_1, SESSION_1);
        Assert.assertNotNull("A job should be created", ji);
        Mockito.reset(publisher);
        jobService.runJob(ji, getDefaultTenant()).get();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // A deletion request should be created for a group request containing each file
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(DeletionFlowItem.class));
    }

    @Test
    public void deleteFileReference() {

        // Add a new file reference without storage with two owners
        String storage = "anywhere";
        List<String> owners = Lists.newArrayList("someone", "someone-else");
        Optional<FileReference> oFileRef = Optional.empty();
        for (String owner : owners) {
            oFileRef = referenceRandomFile(owner, null, "file1.test", storage, "source " + owner, "session " + owner);
        }
        Assert.assertTrue("File reference should have been created", oFileRef.isPresent());
        Collection<FileStorageRequest> storageReqs = stoReqService.search(oFileRef.get().getLocation().getStorage(),
                                                                          oFileRef.get().getMetaInfo().getChecksum());
        Assert.assertTrue("File reference request should not exists anymore as file is well referenced",
                          storageReqs.isEmpty());
        FileReference fileRef = oFileRef.get();

        // Delete file reference for one owner
        FileDeletionRequestDTO request = FileDeletionRequestDTO
                .build(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(), owners.get(0),
                       SESSION_OWNER_1, SESSION_1, false);
        fileDeletionRequestService.handle(Sets.newHashSet(request), UUID.randomUUID().toString());

        // File reference should still exists for the remaining owner
        Optional<FileReference> afterDeletion = fileRefService.search(fileRef.getLocation().getStorage(),
                                                                      fileRef.getMetaInfo().getChecksum());

        Assert.assertTrue("File reference should be always existing", afterDeletion.isPresent());
        FileReference fr = fileRefWithOwnersRepo.findOneById(afterDeletion.get().getId());
        Assert.assertEquals("File reference should always be owned by one owner", 1, fr.getLazzyOwners().size());
        Assert.assertTrue("File reference should always be owned by one owner",
                          fr.getLazzyOwners().contains(owners.get(1)));

        // To check that cache request are deleted with fileReference add a cache request for one stored file
        fileCacheRequestService.create(fileRef, OffsetDateTime.now().plusDays(1), UUID.randomUUID().toString());

        // Delete file reference for the remaining owner
        request = FileDeletionRequestDTO
                .build(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(), owners.get(1),
                       SESSION_OWNER_2, SESSION_1, false);
        fileDeletionRequestService.handle(Sets.newHashSet(request), UUID.randomUUID().toString());

        // File reference should be deleted
        afterDeletion = fileRefService.search(fileRef.getLocation().getStorage(), fileRef.getMetaInfo().getChecksum());
        Assert.assertFalse("File reference should not existing anymore", afterDeletion.isPresent());

    }

    @Test
    public void deleteFileStoredMultiple() throws InterruptedException, ExecutionException {

        String fileChecksum = "file-1";
        String firstOwner = "first-owner";
        String secondOwner = "second-owner";
        FileReference fileRef = generateStoredFileReference(fileChecksum, firstOwner, "file.test", ONLINE_CONF_LABEL,
                                                            Optional.empty(), Optional.empty(), SESSION_OWNER_1, SESSION_1);
        Assert.assertNotNull("File reference should have been created", fileRef);
        Assert.assertTrue("File reference should belongs to first owner",
                          fileRef.getLazzyOwners().contains(firstOwner));
        Optional<FileReference> oFileRef = generateStoredFileReferenceAlreadyReferenced(fileChecksum,
                                                                                        fileRef.getLocation()
                                                                                                .getStorage(),
                                                                                        secondOwner, SESSION_OWNER_2,
                                                                                        SESSION_1);
        Assert.assertTrue("File reference should be updated", oFileRef.isPresent());
        Assert.assertTrue("File reference should belongs to first owner",
                          oFileRef.get().getLazzyOwners().contains(firstOwner));
        Assert.assertTrue("File reference should belongs to second owner",
                          oFileRef.get().getLazzyOwners().contains(secondOwner));
        fileRef = oFileRef.get();

        // Create deletion request for each owner
        FileDeletionRequestDTO request = FileDeletionRequestDTO
                .build(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(), firstOwner,
                       SESSION_OWNER_1, SESSION_1, false);
        FileDeletionRequestDTO request2 = FileDeletionRequestDTO
                .build(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(), secondOwner,
                       SESSION_OWNER_2, SESSION_1, false);
        FileDeletionRequestDTO request3 = FileDeletionRequestDTO
                .build(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(), "other-owner",
                       SESSION_OWNER_3, SESSION_1, false);
        fileDeletionRequestService.handle(Sets.newHashSet(request, request2, request3), UUID.randomUUID().toString());

        // Re-submit same request for one owner
        fileDeletionRequestService.handle(Sets.newHashSet(request3), UUID.randomUUID().toString());

        // File reference should be deleted
        Optional<FileDeletionRequest> afterDeletion = fileDeletionRequestService.search(fileRef);
        Assert.assertTrue("File deletion request should exists", afterDeletion.isPresent());

    }

    @Test
    public void deleteFileReferenceError() throws InterruptedException, ExecutionException {

        String fileChecksum = "file-1";
        String firstOwner = "first-owner";
        FileReference fileRef = generateStoredFileReference(fileChecksum, firstOwner, "delErr.file1.test",
                                                            ONLINE_CONF_LABEL, Optional.empty(), Optional.empty(), SESSION_OWNER_1, SESSION_1);
        Assert.assertNotNull("File reference should have been created", fileRef);
        Assert.assertTrue("File reference should belongs to first owner",
                          fileRef.getLazzyOwners().contains(firstOwner));

        // Delete file reference
        FileDeletionRequestDTO request = FileDeletionRequestDTO
                .build(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(), firstOwner,
                       SESSION_OWNER_1, SESSION_1, false);
        fileDeletionRequestService.handle(Sets.newHashSet(request), UUID.randomUUID().toString());

        // File reference should still exists with no owners
        Optional<FileReference> afterDeletion = fileRefService.search(fileRef.getLocation().getStorage(),
                                                                      fileRef.getMetaInfo().getChecksum());
        Assert.assertTrue("File reference should still exists", afterDeletion.isPresent());
        FileReference fr = fileRefWithOwnersRepo.findOneById(afterDeletion.get().getId());
        Assert.assertTrue("File reference should not belongs to anyone", fr.getLazzyOwners().isEmpty());
        Optional<FileDeletionRequest> oDeletionRequest = fileDeletionRequestService.search(fileRef);
        Assert.assertTrue("File deletion request should be created", oDeletionRequest.isPresent());

        // Now schedule deletion jobs
        Collection<JobInfo> jobs = fileDeletionRequestService.scheduleJobs(FileRequestStatus.TO_DO,
                                                                           Lists.newArrayList());
        runAndWaitJob(jobs);

        // File reference & request deletion should be deleted
        afterDeletion = fileRefService.search(fileRef.getLocation().getStorage(), fileRef.getMetaInfo().getChecksum());
        Assert.assertTrue("File reference should be deleted", afterDeletion.isPresent());
        oDeletionRequest = fileDeletionRequestService.search(fileRef);
        Assert.assertTrue("File reference request should be still present", oDeletionRequest.isPresent());
        Assert.assertEquals("File reference request should be in ERROR state", FileRequestStatus.ERROR,
                            oDeletionRequest.get().getStatus());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_ARC_100")
    @Purpose("Check that a deletion request for file is well when physical deletion is allowed.")
    public void deleteStoredFile() {
        Path deletedFilePath = deleteStoredFile(ONLINE_CONF_LABEL);
        Assert.assertFalse("File should be deleted on disk", Files.exists(deletedFilePath));
    }

    @Test
    @Requirement("REGARDS_DSL_STO_ARC_100")
    @Purpose("Check that a deletion request for file is well when physical deletion is not allowed.")
    public void deleteStoredFileWithoutPhysicalDeletion() {
        Path deletedFilePath = deleteStoredFile(ONLINE_CONF_LABEL_WITHOUT_DELETE);
        Assert.assertTrue("File should not be deleted on disk", Files.exists(deletedFilePath));
    }

    private Path deleteStoredFile(String pluginConf) {
        try {
            String fileChecksum = "file-1";
            String firstOwner = "first-owner";
            String secondOwner = "second-owner";
            FileReference fileRef = generateStoredFileReference(fileChecksum, firstOwner, "file.test", pluginConf,
                                                                Optional.empty(), Optional.empty(), SESSION_OWNER_1, SESSION_1);
            Assert.assertNotNull("File reference should have been created", fileRef);
            Assert.assertTrue("File reference should belongs to first owner",
                              fileRef.getLazzyOwners().contains(firstOwner));
            Optional<FileReference> oFileRef = generateStoredFileReferenceAlreadyReferenced(fileChecksum,
                                                                                            fileRef.getLocation()
                                                                                                    .getStorage(),
                                                                                            secondOwner,
                                                                                            SESSION_OWNER_2, SESSION_1);
            Assert.assertTrue("File reference should be updated", oFileRef.isPresent());
            Assert.assertTrue("File reference should belongs to first owner",
                              oFileRef.get().getLazzyOwners().contains(firstOwner));
            Assert.assertTrue("File reference should belongs to second owner",
                              oFileRef.get().getLazzyOwners().contains(secondOwner));
            fileRef = oFileRef.get();
            Path filePathToDelete;
            filePathToDelete = Paths.get(new URL(fileRef.getLocation().getUrl()).getPath());

            // Delete file reference for one owner
            FileDeletionRequestDTO request = FileDeletionRequestDTO
                    .build(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(), firstOwner,
                           SESSION_OWNER_1, SESSION_1, false);
            fileDeletionRequestService.handle(Sets.newHashSet(request), UUID.randomUUID().toString());

            // File reference should still exists for the remaining owner
            Optional<FileReference> afterDeletion = fileRefService
                    .search(fileRef.getLocation().getStorage(), fileRef.getMetaInfo().getChecksum());
            Assert.assertTrue("File reference should be always existing", afterDeletion.isPresent());
            FileReference fr = fileRefWithOwnersRepo.findOneById(afterDeletion.get().getId());
            Assert.assertEquals("File reference should always be owned by one owner", 1, fr.getLazzyOwners().size());
            Assert.assertTrue("File reference should always be owned by one owner",
                              fr.getLazzyOwners().contains(secondOwner));

            // To check that cache request are deleted with fileReference add a cache request for one stored file
            fileCacheRequestService.create(fileRef, OffsetDateTime.now().plusDays(1), UUID.randomUUID().toString());

            // Delete file reference for the remaining owner
            request = FileDeletionRequestDTO
                    .build(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(), secondOwner,
                           SESSION_OWNER_2, SESSION_1, false);
            fileDeletionRequestService.handle(Sets.newHashSet(request), UUID.randomUUID().toString());

            // File reference should still exists with no owners
            afterDeletion = fileRefService
                    .search(fileRef.getLocation().getStorage(), fileRef.getMetaInfo().getChecksum());
            Assert.assertTrue("File reference should still exists", afterDeletion.isPresent());
            fr = fileRefWithOwnersRepo.findOneById(afterDeletion.get().getId());
            Assert.assertTrue("File reference should not belongs to anyone", fr.getLazzyOwners().isEmpty());
            Optional<FileDeletionRequest> oDeletionRequest = fileDeletionRequestService.search(fileRef);
            Assert.assertTrue("File deletion request should be created", oDeletionRequest.isPresent());
            Assert.assertTrue("File should exists on disk", Files.exists(filePathToDelete));

            // Now schedule deletion jobs
            Collection<JobInfo> jobs = fileDeletionRequestService.scheduleJobs(FileRequestStatus.TO_DO,
                                                                               Lists.newArrayList());
            runAndWaitJob(jobs);

            // File reference & request deletion should be deleted
            afterDeletion = fileRefService.search(fileRef.getLocation().getStorage(),
                                                  fileRef.getMetaInfo().getChecksum());
            Assert.assertFalse("File reference should be deleted", afterDeletion.isPresent());
            oDeletionRequest = fileDeletionRequestService.search(fileRef);
            Assert.assertFalse("File reference request should be deleted", oDeletionRequest.isPresent());
            return filePathToDelete;
        } catch (InterruptedException | ExecutionException | MalformedURLException e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }

}
