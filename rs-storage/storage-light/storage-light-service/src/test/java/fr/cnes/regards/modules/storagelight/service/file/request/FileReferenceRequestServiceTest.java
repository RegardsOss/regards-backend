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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEventType;
import fr.cnes.regards.modules.storagelight.service.AbstractStorageTest;
import fr.cnes.regards.modules.storagelight.service.file.job.FileDeletionJobProgressManager;
import fr.cnes.regards.modules.storagelight.service.file.job.FileDeletionRequestJob;

/**
 * @author sbinda
 *
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_reference_tests",
        "regards.storage.cache.path=target/cache" }, locations = { "classpath:application-test.properties" })
public class FileReferenceRequestServiceTest extends AbstractStorageTest {

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();
    }

    @Test
    public void referenceFileDuringDeletion() throws InterruptedException, ExecutionException, EntityNotFoundException {

        String tenant = runtimeTenantResolver.getTenant();
        // Reference & store a file
        String fileRefChecksum = "file-ref-1";
        String fileRefOwner = "first-owner";
        FileReference fileRef = this.generateStoredFileReference(fileRefChecksum, fileRefOwner, "file.test",
                                                                 ONLINE_CONF_LABEL, Optional.empty());
        String fileRefStorage = fileRef.getLocation().getStorage();

        // Remove all his owners
        String deletionReqId = UUID.randomUUID().toString();
        FileDeletionRequestDTO request = FileDeletionRequestDTO.build(fileRefChecksum, fileRefStorage, fileRefOwner,
                                                                      false);
        fileDeletionRequestService.handle(Sets.newHashSet(request), deletionReqId);

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
        Optional<FileStorageRequest> frr = stoReqService.search(fileRefStorage, fileRefChecksum);
        Assert.assertTrue("A new file reference request should exists", frr.isPresent());
        Assert.assertEquals("A new file reference request should exists with DELAYED status", FileRequestStatus.DELAYED,
                            frr.get().getStatus());

        // Check that the file reference is still not referenced as owned by the new owner and the request is still existing
        oFileRef = fileRefService.search(fileRefStorage, fileRefChecksum);
        Assert.assertTrue("File reference should still exists", oFileRef.isPresent());
        Assert.assertTrue("File reference should still have no owners", oFileRef.get().getOwners().isEmpty());

        // Simulate deletion request ends
        FileDeletionJobProgressManager manager = new FileDeletionJobProgressManager(fileDeletionRequestService,
                fileEventPublisher, new FileDeletionRequestJob());
        manager.deletionSucceed(fdr);
        fileRefEventHandler.handle(TenantWrapper.build(FileReferenceEvent
                .build(fileRefChecksum, FileReferenceEventType.FULLY_DELETED, null, "Deletion succeed",
                       oFileRef.get().getLocation(), oFileRef.get().getMetaInfo(), Sets.newHashSet(deletionReqId)),
                                                       runtimeTenantResolver.getTenant()));
        // Has the handler clear the tenant we have to force it here for tests.
        runtimeTenantResolver.forceTenant(tenant);
        frr = stoReqService.search(fileRefStorage, fileRefChecksum);
        Assert.assertTrue("File storage request still exists", frr.isPresent());
        Assert.assertEquals("File storage request still exists with TO_DO status", FileRequestStatus.TO_DO,
                            frr.get().getStatus());

        // Now the deletion job is ended, the file reference request is in {@link FileRequestStatus#TO_DO} state.
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO,
                                                              Lists.newArrayList(fileRefStorage), Lists.newArrayList());
        runAndWaitJob(jobs);

        frr = stoReqService.search(fileRefStorage, fileRefChecksum);
        oFileRef = fileRefService.search(fileRefStorage, fileRefChecksum);
        Assert.assertFalse("File storage request should not exists anymore", frr.isPresent());
        Assert.assertTrue("File reference should still exists", oFileRef.isPresent());
        Assert.assertTrue("File reference should belongs to new owner",
                          oFileRef.get().getOwners().contains(fileRefNewOwner));
    }

    @Test
    public void referenceFileWithoutStorage() {
        String owner = "someone";
        Optional<FileReference> oFileRef = referenceRandomFile(owner, null, "file.test", "anywhere");
        Assert.assertTrue("File reference should have been created", oFileRef.isPresent());
        Optional<FileStorageRequest> oFileRefReq = stoReqService.search(oFileRef.get().getLocation().getStorage(),
                                                                        oFileRef.get().getMetaInfo().getChecksum());
        Assert.assertTrue("File reference request should not exists anymore as file is well referenced",
                          !oFileRefReq.isPresent());
    }

}
