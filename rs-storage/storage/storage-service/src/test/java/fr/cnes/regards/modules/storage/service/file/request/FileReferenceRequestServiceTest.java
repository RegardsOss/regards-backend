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

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEventType;
import fr.cnes.regards.modules.storage.service.AbstractStorageTest;
import fr.cnes.regards.modules.storage.service.file.job.FileDeletionJobProgressManager;
import fr.cnes.regards.modules.storage.service.file.job.FileDeletionRequestJob;

/**
 * Test class
 *
 * @author Sébastien Binda
 *
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_reference_tests"},
        locations = { "classpath:application-test.properties" })
public class FileReferenceRequestServiceTest extends AbstractStorageTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceRequestServiceTest.class);

    private static final  String SESSION_OWNER_1 = "SOURCE 1";

    private static final  String SESSION_OWNER_2 = "SOURCE 2";

    private static final String SESSION_1 = "SESSION 1";

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
                                                                 ONLINE_CONF_LABEL, Optional.empty(),
                                                                 Optional.empty(), SESSION_OWNER_1, SESSION_1);
        String fileRefStorage = fileRef.getLocation().getStorage();

        // Remove all his owners
        String deletionReqId = UUID.randomUUID().toString();
        FileDeletionRequestDTO request = FileDeletionRequestDTO
                .build(fileRefChecksum, fileRefStorage, fileRefOwner, SESSION_OWNER_1, SESSION_1, false);
        fileDeletionRequestService.handle(Sets.newHashSet(request), deletionReqId);

        Optional<FileReference> oFileRef = fileRefService.search(fileRefStorage, fileRefChecksum);
        Assert.assertFalse("File reference should no have any owners anymore",
                           fileRefService.hasOwner(oFileRef.get().getId()));

        // Simulate FileDeletionRequest in PENDING state
        FileDeletionRequest fdr = fileDeletionRequestRepo.findByFileReferenceId(fileRef.getId()).get();
        fdr.setStatus(FileRequestStatus.PENDING);
        fileDeletionRequestRepo.save(fdr);

        // Reference the same file for a new owner
        String fileRefNewOwner = "new-owner";
        this.generateStoredFileReferenceAlreadyReferenced(fileRefChecksum, fileRefStorage, fileRefNewOwner,
                                                          SESSION_OWNER_2, SESSION_1);

        // check that there is always a deletion request in pending state
        Optional<FileDeletionRequest> ofdr = fileDeletionRequestRepo.findByFileReferenceId(fdr.getId());
        oFileRef = fileRefService.search(fileRef.getLocation().getStorage(), fileRef.getMetaInfo().getChecksum());
        Assert.assertTrue("File deletion request should always exists", ofdr.isPresent());
        Assert.assertEquals("File deletion request should always be running", FileRequestStatus.PENDING,
                            ofdr.get().getStatus());
        // check that a new reference request is made to store again the file after deletion request is done
        reqStatusService.checkDelayedStorageRequests();
        Collection<FileStorageRequest> storageReqs = stoReqService.search(fileRefStorage, fileRefChecksum);
        Assert.assertEquals("A new file reference request should exists", 1, storageReqs.size());
        Assert.assertEquals("A new file reference request should exists with DELAYED status", FileRequestStatus.DELAYED,
                            storageReqs.stream().findFirst().get().getStatus());

        // Check that the file reference is still not referenced as owned by the new owner and the request is still existing
        oFileRef = fileRefService.search(fileRefStorage, fileRefChecksum);
        Assert.assertTrue("File reference should still exists", oFileRef.isPresent());
        Assert.assertFalse("File reference should still have no owners",
                           fileRefService.hasOwner(oFileRef.get().getId()));

        // Simulate deletion request ends
        FileDeletionJobProgressManager manager = new FileDeletionJobProgressManager(fileDeletionRequestService,
                fileEventPublisher, new FileDeletionRequestJob());
        manager.deletionSucceed(fdr);
        fileRefEventHandler.handle(TenantWrapper.build(FileReferenceEvent
                .build(fileRefChecksum, fileRefStorage, FileReferenceEventType.FULLY_DELETED, null, "Deletion succeed",
                       oFileRef.get().getLocation(), oFileRef.get().getMetaInfo(), Sets.newHashSet(deletionReqId)),
                                                       runtimeTenantResolver.getTenant()));
        // Has the handler clear the tenant we have to force it here for tests.
        runtimeTenantResolver.forceTenant(tenant);
        storageReqs = stoReqService.search(fileRefStorage, fileRefChecksum);
        Assert.assertEquals("File storage request still exists", 1, storageReqs.size());
        Assert.assertEquals("File storage request should still exists with DELAYED status", FileRequestStatus.DELAYED,
                            storageReqs.stream().findFirst().get().getStatus());
        reqStatusService.checkDelayedStorageRequests();
        storageReqs = stoReqService.search(fileRefStorage, fileRefChecksum);
        Assert.assertEquals("File storage request still exists", 1, storageReqs.size());
        Assert.assertEquals("File storage request should exists with TO_DO status", FileRequestStatus.TO_DO,
                            storageReqs.stream().findFirst().get().getStatus());

        // Now the deletion job is ended, the file reference request is in {@link FileRequestStatus#TO_DO} state.
        Collection<JobInfo> jobs = stoReqService.scheduleJobs(FileRequestStatus.TO_DO,
                                                              Lists.newArrayList(fileRefStorage), Lists.newArrayList());
        runAndWaitJob(jobs);

        storageReqs = stoReqService.search(fileRefStorage, fileRefChecksum);
        oFileRef = fileRefService.search(fileRefStorage, fileRefChecksum);
        Assert.assertTrue("File storage request should not exists anymore", storageReqs.isEmpty());
        Assert.assertTrue("File reference should still exists", oFileRef.isPresent());
        FileReference fr = fileRefWithOwnersRepo.findOneById(oFileRef.get().getId());
        Assert.assertTrue("File reference should belongs to new owner", fr.getLazzyOwners().contains(fileRefNewOwner));
    }

    @Requirement("REGARDS_DSL_STOP_AIP_070")
    @Purpose("System can reference file without moving files and save the files checksum.")
    @Test
    public void referenceFileWithoutStorage() {
        String owner = "someone";
        Optional<FileReference> oFileRef = referenceRandomFile(owner, null, "file.test", ONLINE_CONF_LABEL,
                                                               SESSION_OWNER_1, SESSION_1);
        Assert.assertTrue("File reference should have been created", oFileRef.isPresent());
        Collection<FileStorageRequest> storageReqs = stoReqService.search(oFileRef.get().getLocation().getStorage(),
                                                                          oFileRef.get().getMetaInfo().getChecksum());
        Assert.assertTrue("File reference request should not exists anymore as file is well referenced",
                          storageReqs.isEmpty());
    }

    @Test
    public void referenceFileWithInvalidURL() throws ModuleException {
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(UUID.randomUUID().toString(), "MD5", "file.test",
                1024L, MediaType.APPLICATION_OCTET_STREAM);
        FileLocation location = new FileLocation(OFFLINE_CONF_LABEL, "anywhere://in/this/directory/file.test");
        try {
            fileReqService.reference("someone", fileMetaInfo, location, Sets.newHashSet(UUID.randomUUID().toString()),
                                     SESSION_OWNER_1, SESSION_1);
            Assert.fail("Module exception should be thrown here as url is not valid");
        } catch (ModuleException e) {
            // Expected exception
            LOGGER.error(e.getMessage(), e);
        }
    }

}
