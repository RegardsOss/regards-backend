/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.request.FileDeletionDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEventType;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileReferenceRequestAggregation;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceRequestArgs;
import fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceRequestCompanionService;
import fr.cnes.regards.modules.storage.service.file.job.FileDeletionJobProgressManager;
import fr.cnes.regards.modules.storage.service.file.job.FileDeletionRequestJob;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceConstants.*;
import static fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceRequestArgs.newFileReferenceRequestArgs1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_reference_tests" },
                    locations = { "classpath:application-test.properties" })
public class FileReferenceRequestServiceDuringDeletionIT extends AbstractFileReferenceRequestServiceIT {

    @Autowired
    FileReferenceRequestCompanionService companionService;

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.modules.storage.service.file.fixture" })
    public static class ScanningTestConfiguration {

    }

    /**
     * Same storage and checksum are used all along the test.
     * This test is similar with the one in {@link FileStorageRequestServiceIT}.
     * GIVEN:
     * <ul>
     *     <li>Create and handle a FileStorageRequestAggregation</li>
     *     <li>-> expect a stored and referenced file</li>
     *     <li>Create and handle a FileDeletionRequest</li>
     *     <li>-> expect FileReference still exists but no more owner and no more stored file</li>
     *     <li>Create a FileDeletionRequest in a PENDING status</li>
     *     <li>Create and handle a FileStorageRequestAggregation with a new owner</li>
     *     <li>-> expect FileStorageRequestAggregation in a DELAYED status</li>
     *     <li>-> expect FileDeletionRequest in a PENDING status</li>
     *     <li>Remove the FileDeletionRequest</li>
     *     <li>Check and update the status of FileStorageRequestAggregation</li>
     *     <li>-> expect status of FileStorageRequestAggregation to be updated from DELAYED to TO_DO</li>
     * <ul>
     * WHEN:
     * <ul>
     *     <li>Schedule job for handling the FileStorageRequestAggregation in TO_DO status</li>
     * </ul>
     * THEN:
     * <ul>
     *     <li>->expect the FileReference to have a new owner</li>
     *     <li>->expect the FileStorageRequestAggregation to be removed/li>
     * </ul>
     */
    @Test
    public void createStorageRequestDuringDeletion() {

        // GIVEN

        // Reference & store a file
        final FileReference fileRef = this.generateStoredFileReference(CHECKSUM1,
                                                                       OWNER1,
                                                                       FILE_REF_NAME,
                                                                       // STORAGE1,
                                                                       ONLINE_CONF_LABEL,
                                                                       Optional.empty(),
                                                                       Optional.empty(),
                                                                       SESSION1_OWNER,
                                                                       SESSION1);
        assumeThat(fileRef).isNotNull();

        final String fileRefStorage = fileRef.getLocation().getStorage();
        assumeThat(fileRefStorage).isEqualTo(ONLINE_CONF_LABEL);

        // Remove all his owners
        final String deletionReqId = UUID.randomUUID().toString();
        final FileDeletionDto request = FileDeletionDto.build(CHECKSUM1,
                                                              fileRefStorage,
                                                              OWNER1,
                                                              SESSION1_OWNER,
                                                              SESSION1,
                                                              false);
        deletionRequestService.handle(Set.of(request), deletionReqId);

        final FileReference createdFileRef = referenceService.search(fileRefStorage, CHECKSUM1).orElse(null);
        assumeThat(createdFileRef).as("File reference should have been created.").isNotNull();
        assumeThat(referenceService.hasOwner(createdFileRef.getId())).as(
            "File reference should no more have any owners.").isFalse();
        // Simulate FileDeletionRequest in PENDING state
        final FileDeletionRequest fdr1 = deletionRequestRepository.findByFileReferenceId(fileRef.getId()).orElse(null);
        assumeThat(fdr1).as("FileDeletionRequest should exists").isNotNull();
        fdr1.setStatus(FileRequestStatus.PENDING);
        deletionRequestRepository.save(fdr1);

        // Reference the same file for a new owner
        this.generateStoredFileReferenceAlreadyReferenced(CHECKSUM1, fileRefStorage, OWNER2, SESSION2_OWNER, SESSION2);

        // check that the FileDeletionRequest is still in a PENDING state
        final FileDeletionRequest fdr2 = deletionRequestRepository.findByFileReferenceId(fdr1.getId()).orElse(null);
        referenceService.search(fileRef.getLocation().getStorage(), fileRef.getMetaInfo().getChecksum());
        assumeThat(fdr2).as("File deletion request should still exists").isNotNull();
        assumeThat(fdr2.getStatus()).as("File deletion request should still be PENDING")
                                    .isEqualTo(FileRequestStatus.PENDING);

        // update the status of FileStorageRequestAggregation :
        // - DELAYED or TO_DO with no FileDeletionRequest in PENDING or TO_DO  -> TO_DO
        // - DELAYED or TO_DO with FileDeletionRequest in PENDING or TO_DO -> DELAYED
        statusService.checkDelayedStorageRequests(storageRequestService);

        // expect the FileStorageRequestAggregation to be in a DELAYED status
        final Collection<FileStorageRequestAggregation> storageRequests = storageRequestService.search(fileRefStorage,
                                                                                                       CHECKSUM1);
        assumeThat(storageRequests).as("A new file reference request should exists").hasSize(1);
        final FileStorageRequestAggregation storageRequest = storageRequests.iterator().next();
        assumeThat(storageRequest).as("A new file reference request should exists").isNotNull();
        assumeThat(storageRequest.getStatus()).as("A new file reference request should exists with DELAYED status")
                                              .isEqualTo(FileRequestStatus.DELAYED);

        // Check that the file reference is still not referenced as owned by the new owner and the request is still existing
        final FileReference foundFileRef = referenceService.search(fileRefStorage, CHECKSUM1).orElse(null);
        assumeThat(foundFileRef).as("File reference should still exists").isNotNull();
        assumeThat(referenceService.hasOwner(foundFileRef.getId())).as("File reference should still have no owners")
                                                                   .isFalse();

        // Simulate deletion request ends
        simulateDeletionRequestJob(fdr2, foundFileRef);

        // File storage request should still exists with DELAYED status
        final Collection<FileStorageRequestAggregation> delayedStorageReqs = storageRequestService.search(fileRefStorage,
                                                                                                          CHECKSUM1);
        assumeThat(delayedStorageReqs).as("File storage request still exists").hasSize(1);
        final FileStorageRequestAggregation delayedStorageRequest = delayedStorageReqs.iterator().next();
        assumeThat(delayedStorageRequest.getStatus()).as("File storage request should still exists with DELAYED status")
                                                     .isEqualTo(FileRequestStatus.DELAYED);

        // update the status of FileStorageRequestAggregation :
        // - DELAYED or TO_DO with no PENDING or TO_DO FileDeletionRequest -> TO_DO
        // - DELAYED or TO_DO with PENDING or TO_DO FileDeletionRequest -> DELAYED
        statusService.checkDelayedStorageRequests(null);

        // the status of FileStorageRequestAggregation is expected to be updated from DELAYED to TO_DO
        final Collection<FileStorageRequestAggregation> todoStorageReqs = storageRequestService.search(fileRefStorage,
                                                                                                       CHECKSUM1);
        assumeThat(todoStorageReqs).as("File storage request still exists").hasSize(1);
        assumeThat(todoStorageReqs.iterator().next().getStatus()).as(
            "File storage request should exists with TO_DO status").isEqualTo(FileRequestStatus.TO_DO);

        // WHEN
        // run job for handling FileStorageRequestAggregation in TO_DO status.
        final Collection<JobInfo> jobs = storageRequestService.scheduleJobs(FileRequestStatus.TO_DO,
                                                                            Set.of(fileRefStorage),
                                                                            Set.of());
        runAndWaitJob(jobs);

        // THEN
        // expect the FileStorageRequestAggregation has been consumed and removed
        final Collection<FileStorageRequestAggregation> noStorageRequests = storageRequestService.search(fileRefStorage,
                                                                                                         CHECKSUM1);
        assertThat(noStorageRequests).as("File storage request should not exists anymore").isEmpty();

        // expect the FileReference has FILE_REF_NEW_OWNER as the single new owner
        final FileReference fileReference = referenceService.search(fileRefStorage, CHECKSUM1).orElse(null);
        assertFileReference(fileReference, OWNER2);
    }

    /**
     * Same storage and checksum are used all along the test.
     * <ol>
     *     <li>GIVEN: it is matching test {@link #createReferenceRequestDuringDeletion}</li>
     *     <li>Create and handle a FileReferenceRequestAggregation</li>
     *     <li>-> expect referenced file</li>
     *     <li>Create a FileDeletionRequest in a PENDING status</li>
     *     <li>Create and handle a FileReferenceRequestAggregation with a new owner</li>
     *     <li>-> expect FileReferenceRequestAggregation in a DELAYED status</li>
     *     <li>-> expect FileDeletionRequest remains in PENDING status</li>
     * </ol>
     * <ol>
     *     <li>WHEN:
     *     <li>Remove the FileDeletionRequest</li>
     *     <li>Check and update the status of FileReferenceRequestAggregation</li>
     *     <li>-> expect status of FileReferenceRequestAggregation to be updated from DELAYED to TO_DO</li>
     *     <li>Schedule job for handling the FileReferenceRequestAggregation in TO_DO status</li>
     * </ol>
     * <ol>
     *     <li>THEN:</li>
     *     <li>->expect the FileReference to have a new owner</li>
     *     <li>->expect the FileReferenceRequestAggregation to be in SUCCESS status/li>
     * </ol>
     */
    @Test
    public void createReferenceRequestDuringDeletionAndExecuteDeletion() {

        // GIVEN
        final FileReferenceRequestArgs args = newFileReferenceRequestArgs1(); //.withStorage(ONLINE_CONF_LABEL);
        final String storage = args.getStorage();
        final String checksum = args.getChecksum();

        // Step 1: Creation of FileReference with a single owner OWNER1
        final FileReference fileReference = companionService.createFileReference(args);

        // Step 2: create a FileDeletionRequest in a PENDING status
        final FileDeletionRequest deletionRequest = companionService.createDeletionRequestOnFileReference(args,
                                                                                                          FileRequestStatus.PENDING);

        // Step 3: Create and schedule file reference request FileReferenceRequestAggregation with a new Owner
        // Reference the same file for a new owner
        final FilesReferenceEvent filesRefEvent = this.createReferenceRequest(args.withOwner(OWNER2));

        // FileReferenceRequestAggregation status is changed from TO_DO to DELAYED
        // because of the presence of a FileDeletionRequest in a PENDING status
        // so no job are scheduled
        this.checkAndScheduleReferenceRequestJob(0);

        // WHEN

        // simulate scheduling and executing the deletion request
        simulateDeletionRequestJob(deletionRequest, fileReference);

        // schedule the reference request
        runtimeTenantResolver.forceTenant(tenant);
        statusService.checkDelayedReferenceRequests();
        this.scheduleReferenceRequestJob(1);

        // THEN

        // CHECK 1: The FileReferenceRequestAggregation are in SUCCESS status
        final FileReferenceRequestAggregation foundReferenceRequest = referenceRequestRepository.findByStorageAndMetaInfoChecksum(
            storage,
            checksum).orElse(null);
        assertThat(foundReferenceRequest).as("FileReferenceRequestAggregation should exists").isNotNull();
        assertThat(foundReferenceRequest.getStatus()).as("FileReferenceRequestAggregation must be in SUCCESS status")
                                                     .isEqualTo(FileRequestStatus.SUCCESS);

        // no other FileReferenceRequestAggregation
        final Collection<FileReferenceRequestAggregation> allReferenceRequests = referenceRequestRepository.findByMetaInfoChecksumAndStorage(
            checksum,
            storage);
        assertThat(allReferenceRequests).as("FileReferenceRequestAggregation should be unique").hasSize(1);

        // CHECK 2: The deletion request still exists in
        FileDeletionRequest notfoundDeletion = deletionRequestRepository.findById(deletionRequest.getId()).orElse(null);
        assertThat(notfoundDeletion).as("File deletion request should no more exists").isNull();
        notfoundDeletion = deletionRequestRepository.findByFileReferenceId(fileReference.getId()).orElse(null);
        assertThat(notfoundDeletion).as("File deletion request should no more exists").isNull();

        // CHECK3 :
        // a new FileReference exists with a single owner OWNER2
        final FileReference foundFileReference = getFileReference(args);
        // Check that the FileReference has OWNER2 as single owner
        assertFileReference(foundFileReference, args, OWNER2);
    }

    /**
     * Same storage and checksum are used all along the test.
     * <ol>
     *     <li>Create and handle a FileReferenceRequestAggregation</li>
     *     <li>-> expect referenced file</li>
     *     <li>Create a FileDeletionRequest in a PENDING status</li>
     *     <li>Create and handle a FileReferenceRequestAggregation with a new owner</li>
     *     <li>-> expect new FileReferenceRequestAggregation in a DELAYED status</li>
     *     <li>-> expect FileDeletionRequest remains in PENDING status</li>
     *     <li>-> expect FileReference unchanged</li>
     * </ol>
     */
    @Test
    public void createReferenceRequestDuringDeletion() {

        // GIVEN

        // Step 1: Creation of FileReference by handling a FileReferenceRequestAggregation
        // Create new file reference request FileReferenceRequestAggregation
        final FileReferenceRequestArgs args = newFileReferenceRequestArgs1(); //.withStorage(ONLINE_CONF_LABEL);
        final FilesReferenceEvent filesRefEvent1 = this.createReferenceRequest(args);
        final String checksum = filesRefEvent1.getFiles().iterator().next().getChecksum();
        final String storage = filesRefEvent1.getFiles().iterator().next().getStorage();

        // schedule FileReferenceRequestJob in order to have the FileReferenceRequestAggregation handled.
        this.scheduleReferenceRequestJob(1);

        // assert that a FileReferenceRequestAggregation has been handled and is in a SUCCESS status
        final FileReferenceRequestAggregation successfulRequest = getReferenceRequest(storage, checksum);
        assumeThat(successfulRequest).as("FileReferenceRequestAggregation should have been created").isNotNull();
        assumeThat(successfulRequest.getStatus()).as("FileReferenceRequestAggregation should be in SUCCESS status")
                                                 .isEqualTo(FileRequestStatus.SUCCESS);

        // assert that a FileReference has been created with single OWNER1
        final FileReference fileReference = this.getFileReference(args);
        assertFileReference(fileReference, args, OWNER1);
        // assert no other FileReference with same checksum exists
        final Set<FileReference> references = referenceService.search(Set.of(checksum));
        assumeThat(references).hasSize(1);

        // Step 2: prepare  the removal of the owner of the FileReference by handling a FileDeletionRequest
        // create FileDeletionRequest in a PENDING status
        FileDeletionRequest deletionRequest = companionService.createDeletionRequestOnFileReference(args,
                                                                                                    FileRequestStatus.PENDING);
        final Long deletionRequestId = deletionRequest.getId();

        // Step 3: Create new file reference request FileReferenceRequestAggregation with a new Owner
        // Reference the same file for a new owner
        final FilesReferenceEvent filesRefEvent2 = this.createReferenceRequest(args.withOwner(OWNER2));

        // WHEN

        // execute the FileReferenceRequestAggregation
        // the request status is changed from TO_DO to DELAYED because of the PENDING deletion request.
        statusService.checkDelayedReferenceRequests();
        // since no request in TO_DO no job scheduled
        this.scheduleReferenceRequestJob(0);

        // THEN

        // CHECK1:
        // the FileReferenceRequestAggregation with the new owner should be DELAYED
        final Collection<FileReferenceRequestAggregation> referenceRequests = this.referenceRequestRepository.findByMetaInfoChecksumAndStorage(
            checksum,
            storage);
        assertThat(referenceRequests).as("A new FileReferenceRequestAggregation should exists").hasSize(2);

        final FileReferenceRequestAggregation delayed = referenceRequests.stream()
                                                                         .filter(req -> FileRequestStatus.DELAYED.equals(
                                                                             req.getStatus()))
                                                                         .findFirst()
                                                                         .orElse(null);
        assertThat(delayed).as("2nd FileReferenceRequestAggregation is in a DELAYED status"
                               + " because of the PENDING FileDeletionRequest").isNotNull();
        assertThat(delayed.getOwners()).hasSize(1).contains(OWNER2);

        // the FileReferenceRequestAggregation with the 1st owner is still in SUCCESS
        final FileReferenceRequestAggregation success = referenceRequests.stream()
                                                                         .filter(req -> FileRequestStatus.SUCCESS.equals(
                                                                             req.getStatus()))
                                                                         .findFirst()
                                                                         .orElse(null);
        assertThat(success).as("1st FileReferenceRequestAggregation still exists in SUCCESS status").isNotNull();
        assertThat(success.getOwners()).hasSize(1).contains(OWNER1);

        // CHECK2: The deletion request still exists in PENDING
        deletionRequest = deletionRequestRepository.findById(deletionRequestId).orElse(null);
        assertThat(deletionRequest).as("File deletion request should still exists").isNotNull();
        assertThat(deletionRequest.getStatus()).as("File deletion request should still be PENDING")
                                               .isEqualTo(FileRequestStatus.PENDING);

        deletionRequest = deletionRequestRepository.findByFileReferenceId(fileReference.getId()).orElse(null);
        assertThat(deletionRequest).as("File deletion request still reference the same file").isNotNull();

        // CHECK3: the FileReference still exists with its single owner OWNER1
        final FileReference foundFileReference = referenceService.search(storage, checksum).orElse(null);
        assertFileReference(foundFileReference, args, OWNER1);
        assertThat(foundFileReference.getId()).isEqualTo(fileReference.getId());
    }

    @Override
    protected FileReference generateStoredFileReference(String checksum,
                                                        String owner,
                                                        String fileName,
                                                        String storage,
                                                        Optional<String> subDir,
                                                        Optional<String> type,
                                                        String sessionOwner,
                                                        String session) {
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum,
                                                                       "MD5",
                                                                       fileName,
                                                                       1024L,
                                                                       MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE));
        fileMetaInfo.withType(type.orElse(null));
        // Run file reference creation.
        storageRequestService.handleRequest(owner,
                                            sessionOwner,
                                            session,
                                            fileMetaInfo,
                                            ORIGIN_URL,
                                            storage,
                                            subDir,
                                            UUID.randomUUID().toString());
        // The file reference should not exist yet cause a storage job is needed.
        // But a FileStorageRequestAggregation should have been created.
        final Optional<FileReference> oFileRef = referenceService.search(storage, checksum);
        assertThat(oFileRef).as("File reference should not have been created yet.").isEmpty();

        Collection<FileStorageRequestAggregation> fileRefReqs = storageRequestService.search(storage, checksum);
        assertThat(fileRefReqs).as("File reference request should exists").hasSize(1);
        final FileStorageRequestAggregation fileRefReq = fileRefReqs.iterator().next();
        assertThat(fileRefReq.getStatus()).as("File reference request should be in TO_DO status")
                                          .isEqualTo(FileRequestStatus.TO_DO);

        // Run Job schedule to initiate the storage job associated to the FileReferenceRequest created before
        Collection<JobInfo> jobs = storageRequestService.scheduleJobs(FileRequestStatus.TO_DO, null, null);
        assertThat(jobs).as("One storage job should scheduled").hasSize(1);
        // Run Job and wait for end
        runAndWaitJob(jobs);

        // Once storage job is successfully done,
        // the FileReference should have been created
        // and the FileReferenceRequest should have been removed.

        // FileReference successfully created
        FileReference fileRef = referenceService.search(storage, checksum).orElse(null);
        assertThat(fileRef).as("File reference should have been created.").isNotNull();
        assertThat(fileRef.getLocation().isPendingActionRemaining()).as(
            "File reference should be fully stored without remaining action.").isFalse();

        try {
            final Path path = Paths.get(new URL(fileRef.getLocation().getUrl()).getPath());
            assertThat(path).as("File should be created on disk").exists();
        } catch (MalformedURLException e) {
            fail(e.getMessage());
        }

        // no more file storage request (FileStorageReferenceAggregation).
        fileRefReqs = storageRequestService.search(storage, checksum);
        assertThat(fileRefReqs).as("File reference request should not exists anymore").isEmpty();
        return referenceWithOwnersRepository.findOneById(fileRef.getId());
    }

    /**
     * Simple test to ensure that a FileDeletionRequest pointing on a FileReference can be created.
     * That's what it is going to be used in the other test.
     */
    @Test
    public void createDeletionRequestOnFileReference() {
        final FileReferenceRequestArgs args = newFileReferenceRequestArgs1();
        companionService.createFileReference(args);
        companionService.createDeletionRequestOnFileReference(args, FileRequestStatus.DELAYED);
        // scheduling deletion request does not seem to work what ever is the status.
        // {@link #simulateDeletionRequestJob} seems to be the way to go.
        scheduleDeletionRequestJob(0);
    }

    /**
     * This code is purposely kept.
     * Scheduling FileDeletionRequestJob does not seems to work that way.
     * {@link #simulateDeletionRequestJob} seems to be the way to go.
     */
    private Collection<JobInfo> scheduleDeletionRequestJob(int expectedJobCount) {
        forceTenant();
        statusService.checkDelayedDeleteRequests();
        // Schedule job to initiate the FileReferenceRequestJob associated to the FileReferenceRequestAggregation
        // created earlier. Run the job till completion.
        final Collection<JobInfo> jobs = deletionRequestService.scheduleJobs(FileRequestStatus.TO_DO, Set.of());
        assertThat(jobs).as("Deletion job should be scheduled").hasSize(expectedJobCount);
        // Run Job and wait for the end
        runAndWaitJob(jobs);
        return jobs;
    }

    /**
     * Simulate the end of the deletion request.
     *
     * @param deletionRequest the FileDeletionRequest to terminate.
     * @param reference       the FileReference targeted by the FileDeletionRequest.
     */
    private void simulateDeletionRequestJob(FileDeletionRequest deletionRequest, FileReference reference) {
        forceTenant();
        statusService.checkDelayedDeleteRequests();
        // Simulate deletion request ends
        final FileDeletionJobProgressManager manager = new FileDeletionJobProgressManager(deletionRequestService,
                                                                                          storageLocationService,
                                                                                          new FileDeletionRequestJob());
        manager.deletionSucceed(deletionRequest.toDto());
        final FileReferenceEvent event = new FileReferenceEvent(CHECKSUM1,
                                                                deletionRequest.getStorage(),
                                                                FileReferenceEventType.FULLY_DELETED,
                                                                null,
                                                                "Deletion succeed",
                                                                reference.getLocation().toDto(),
                                                                reference.getMetaInfo().toDto(),
                                                                Set.of(deletionRequest.getGroupId()));

        referenceEventHandler.handleBatch(List.of(event));
        // Since the handler clear the tenant, we have to force it here when testing.
        forceTenant();
    }
}
