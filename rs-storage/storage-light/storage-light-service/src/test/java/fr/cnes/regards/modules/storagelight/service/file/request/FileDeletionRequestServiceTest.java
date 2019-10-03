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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storagelight.service.AbstractStorageTest;

/**
 * @author sbinda
 *
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_deletion_tests",
        "regards.storage.cache.path=target/cache" }, locations = { "classpath:application-test.properties" })
public class FileDeletionRequestServiceTest extends AbstractStorageTest {

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();
    }

    @Test
    public void deleteAll() throws InterruptedException, ExecutionException, ModuleException {
        String owner = "first-owner";
        Long nbFiles = 20L;
        for (int i = 0; i < nbFiles; i++) {
            generateStoredFileReference(UUID.randomUUID().toString(), owner, String.format("file-%d.test", i),
                                        ONLINE_CONF_LABEL, Optional.empty());
        }
        JobInfo ji = fileDeletionRequestService.scheduleJob(ONLINE_CONF_LABEL, false);
        Assert.assertNotNull("A job should be created", ji);
        Mockito.reset(publisher);
        jobService.runJob(ji, getDefaultTenant()).get();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // A deletion request should be created for each file
        Mockito.verify(publisher, Mockito.times(20)).publish(Mockito.any(DeletionFlowItem.class));
    }

    @Test
    public void deleteFileReference() throws EntityNotFoundException, InterruptedException, ExecutionException {

        // Add a new file reference without storage with two owners
        String storage = "anywhere";
        List<String> owners = Lists.newArrayList("someone", "someone-else");
        Optional<FileReference> oFileRef = Optional.empty();
        for (String owner : owners) {
            oFileRef = referenceRandomFile(owner, null, "file1.test", storage);
        }
        Assert.assertTrue("File reference should have been created", oFileRef.isPresent());
        Optional<FileStorageRequest> oFileRefReq = stoReqService.search(oFileRef.get().getLocation().getStorage(),
                                                                        oFileRef.get().getMetaInfo().getChecksum());
        Assert.assertTrue("File reference request should not exists anymore as file is well referenced",
                          !oFileRefReq.isPresent());
        FileReference fileRef = oFileRef.get();

        // Delete file reference for one owner
        FileDeletionRequestDTO request = FileDeletionRequestDTO
                .build(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(), owners.get(0), false);
        fileDeletionRequestService.handle(Sets.newHashSet(request), UUID.randomUUID().toString());

        // File reference should still exists for the remaining owner
        Optional<FileReference> afterDeletion = fileRefService.search(fileRef.getLocation().getStorage(),
                                                                      fileRef.getMetaInfo().getChecksum());
        Assert.assertTrue("File reference should be always existing", afterDeletion.isPresent());
        Assert.assertEquals("File reference should always be owned by one owner", 1,
                            afterDeletion.get().getOwners().size());
        Assert.assertTrue("File reference should always be owned by one owner",
                          afterDeletion.get().getOwners().contains(owners.get(1)));

        // Delete file reference for the remaining owner
        request = FileDeletionRequestDTO.build(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(),
                                               owners.get(1), false);
        fileDeletionRequestService.handle(Sets.newHashSet(request), UUID.randomUUID().toString());

        // File reference should be deleted
        afterDeletion = fileRefService.search(fileRef.getLocation().getStorage(), fileRef.getMetaInfo().getChecksum());
        Assert.assertFalse("File reference should not existing anymore", afterDeletion.isPresent());

    }

    @Test
    public void deleteFileReferenceError() throws EntityNotFoundException, InterruptedException, ExecutionException {

        String fileChecksum = "file-1";
        String firstOwner = "first-owner";
        FileReference fileRef = generateStoredFileReference(fileChecksum, firstOwner, "delErr.file1.test",
                                                            ONLINE_CONF_LABEL, Optional.empty());
        Assert.assertNotNull("File reference should have been created", fileRef);
        Assert.assertTrue("File reference should belongs to first owner", fileRef.getOwners().contains(firstOwner));

        // Delete file reference
        FileDeletionRequestDTO request = FileDeletionRequestDTO
                .build(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(), firstOwner, false);
        fileDeletionRequestService.handle(Sets.newHashSet(request), UUID.randomUUID().toString());

        // File reference should still exists with no owners
        Optional<FileReference> afterDeletion = fileRefService.search(fileRef.getLocation().getStorage(),
                                                                      fileRef.getMetaInfo().getChecksum());
        Assert.assertTrue("File reference should still exists", afterDeletion.isPresent());
        Assert.assertTrue("File reference should not belongs to anyone", afterDeletion.get().getOwners().isEmpty());
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
    public void deleteStoredFileReference() throws InterruptedException, ExecutionException, EntityNotFoundException {
        String fileChecksum = "file-1";
        String firstOwner = "first-owner";
        String secondOwner = "second-owner";
        FileReference fileRef = generateStoredFileReference(fileChecksum, firstOwner, "file.test", ONLINE_CONF_LABEL,
                                                            Optional.empty());
        Assert.assertNotNull("File reference should have been created", fileRef);
        Assert.assertTrue("File reference should belongs to first owner", fileRef.getOwners().contains(firstOwner));
        Optional<FileReference> oFileRef = generateStoredFileReferenceAlreadyReferenced(fileChecksum,
                                                                                        fileRef.getLocation()
                                                                                                .getStorage(),
                                                                                        secondOwner);
        Assert.assertTrue("File reference should be updated", oFileRef.isPresent());
        Assert.assertTrue("File reference should belongs to first owner",
                          oFileRef.get().getOwners().contains(firstOwner));
        Assert.assertTrue("File reference should belongs to second owner",
                          oFileRef.get().getOwners().contains(secondOwner));
        fileRef = oFileRef.get();

        // Delete file reference for one owner
        FileDeletionRequestDTO request = FileDeletionRequestDTO
                .build(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(), firstOwner, false);
        fileDeletionRequestService.handle(Sets.newHashSet(request), UUID.randomUUID().toString());

        // File reference should still exists for the remaining owner
        Optional<FileReference> afterDeletion = fileRefService.search(fileRef.getLocation().getStorage(),
                                                                      fileRef.getMetaInfo().getChecksum());
        Assert.assertTrue("File reference should be always existing", afterDeletion.isPresent());
        Assert.assertEquals("File reference should always be owned by one owner", 1,
                            afterDeletion.get().getOwners().size());
        Assert.assertTrue("File reference should always be owned by one owner",
                          afterDeletion.get().getOwners().contains(secondOwner));

        // Delete file reference for the remaining owner
        request = FileDeletionRequestDTO.build(fileRef.getMetaInfo().getChecksum(), fileRef.getLocation().getStorage(),
                                               secondOwner, false);
        fileDeletionRequestService.handle(Sets.newHashSet(request), UUID.randomUUID().toString());

        // File reference should still exists with no owners
        afterDeletion = fileRefService.search(fileRef.getLocation().getStorage(), fileRef.getMetaInfo().getChecksum());
        Assert.assertTrue("File reference should still exists", afterDeletion.isPresent());
        Assert.assertTrue("File reference should not belongs to anyone", afterDeletion.get().getOwners().isEmpty());
        Optional<FileDeletionRequest> oDeletionRequest = fileDeletionRequestService.search(fileRef);
        Assert.assertTrue("File deletion request should be created", oDeletionRequest.isPresent());

        // Now schedule deletion jobs
        Collection<JobInfo> jobs = fileDeletionRequestService.scheduleJobs(FileRequestStatus.TO_DO,
                                                                           Lists.newArrayList());
        runAndWaitJob(jobs);

        // File reference & request deletion should be deleted
        afterDeletion = fileRefService.search(fileRef.getLocation().getStorage(), fileRef.getMetaInfo().getChecksum());
        Assert.assertFalse("File reference should be deleted", afterDeletion.isPresent());
        oDeletionRequest = fileDeletionRequestService.search(fileRef);
        Assert.assertFalse("File reference request should be deleted", oDeletionRequest.isPresent());
    }

}
