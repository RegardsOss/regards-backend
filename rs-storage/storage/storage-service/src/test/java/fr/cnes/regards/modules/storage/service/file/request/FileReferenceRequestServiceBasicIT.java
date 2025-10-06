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

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileReferenceRequestAggregation;
import fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceRequestArgs;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.*;

import static fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceConstants.*;
import static fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceRequestArgs.newFileReferenceRequestArgs1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Test class
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_reference_tests" },
                    locations = { "classpath:application-test.properties" })
public class FileReferenceRequestServiceBasicIT extends AbstractFileReferenceRequestServiceIT {

    public static final String FIRST = "1st ";

    public static final String SECOND = "2nd ";

    /**
     * Same storage and checksum are used all along the test.
     * <ol>
     *     <li>Create a FileReferenceRequestAggregation</li>
     *     <li>Check and update the status of FileReferenceRequestAggregation, expected to be in TO_DO status</li>
     *     <li>Schedule a FileReferenceRequestJob to process the FileReferenceRequestAggregation in TO_DO status</li>
     *     <li>-> expect referenced file</li>
     *     <li>-> expect the FileReferenceRequestAggregation to be in SUCCESS status</li>
     *     <li>Check and update the status of FileReferenceRequestAggregation</li>
     *     <li>Remove completed FileReferenceRequestAggregation</li>
     *     <li>->expect the FileReferenceRequestAggregation to be removed</li>
     * </ol>
     */
    @Test
    public void createSingleReferenceRequest() {

        // GIVEN
        // Create a file reference request FileReferenceRequestAggregation
        final FileReferenceRequestArgs args = newFileReferenceRequestArgs1();
        final FilesReferenceEvent filesRefEvent1 = this.createReferenceRequest(args);
        final String checksum = filesRefEvent1.getFiles().iterator().next().getChecksum();
        final String storage = filesRefEvent1.getFiles().iterator().next().getStorage();

        // assert that a FileReferenceRequestAggregation has been created
        final FileReferenceRequestAggregation createdRequest = getReferenceRequest(storage, checksum);
        assumeThat(createdRequest).as("FileReferenceRequestAggregation should have been created").isNotNull();
        assumeThat(createdRequest.getId()).as("FileReferenceRequestAggregation should have been created")
                                          .isNotNull()
                                          .isPositive();

        // but no FileReference created yet
        final FileReference noFileRef = referenceService.search(storage, checksum).orElse(null);
        assumeThat(noFileRef).as("FileReference not yet created").isNull();

        // check and update status
        statusService.checkDelayedReferenceRequests();

        // assert that status is TO_DO
        final FileReferenceRequestAggregation updatedRequest = getReferenceRequest(storage, checksum);
        assumeStatus(updatedRequest, FileRequestStatus.TO_DO, "");

        // WHEN
        // schedule FileReferenceRequestJob
        this.scheduleReferenceRequestJob(1);

        // THEN
        // assert that a FileReference has been created
        // assertFileReferenceWithOwners(storage, checksum, FILE_REF_OWNER);

        // assert that a FileReferenceRequestAggregation has been handled and is in a SUCCESS status
        final FileReferenceRequestAggregation successfulRequest = getReferenceRequest(storage, checksum);
        assertStatus(successfulRequest, FileRequestStatus.SUCCESS, "");

        // remove all the FileReferenceRequestAggregation in SUCCESS or ERROR status
        referenceRequestService.deleteAllTerminatedRequestOfGroups(Collections.singleton(filesRefEvent1.getGroupId()));

        final Collection<FileReferenceRequestAggregation> noRequests = referenceRequestRepository.findByMetaInfoChecksumAndStorage(
            checksum,
            storage);
        assertThat(noRequests).as("FileReferenceRequestAggregation should have been removed").isEmpty();

        // assert that the ReferenceFile has been created
        final FileReference reference = referenceService.search(storage, checksum).orElse(null);
        assertThat(reference).as("FileReference should have been created").isNotNull();
    }

    /**
     * Creation of two FileReferenceRequestAggregation referencing the same file
     * but owned by two distinct owner.
     * Expected: creation of a single FileReference with both owners.
     * Also Note: <ul>
     * <li>1st FileReferenceRequestAggregation: TO_DO -> TO_DO</li>
     * <li>2nd FileReferenceRequestAggregation: TO_DO -> DELAYED</li>
     * </ul>FileReferenceRequestAggregation  ->
     */
    @Test
    public void createTwoReferenceRequestsWithDistinctOwnersAndSameGroup() {
        final FileReferenceRequestArgs args1 = newFileReferenceRequestArgs1();
        final FileReferenceRequestArgs args2 = args1.withOwner(OWNER2);
        assumeThat(args1.getGroupId()).isNotNull().isEqualTo(args2.getGroupId());
        assumeThat(args1.getOwner()).isNotNull().isNotEqualTo(args2.getOwner());

        // GIVEN ... WHEN ... THEN ...
        createTwoReferenceRequests(args1, args2, 1, 1);
        // THEN assert that a FileReference has been created with both owners
        final FileReference fileReference = getFileReference(args1);
        assertFileReference(fileReference, args1, args1.getOwner(), args2.getOwner());
    }

    /**
     * Creation of two FileReferenceRequestAggregation referencing the same file
     * but owned by two distinct owner in two belongings to a distinct group.
     * Expected: creation of a single FileReference with both owners.
     */
    @Test
    public void createTwoReferenceRequestsWithDistinctOwnersAndDistinctGroups() {
        final FileReferenceRequestArgs args1 = newFileReferenceRequestArgs1();
        final FileReferenceRequestArgs args2 = args1.withOwner(OWNER2).withGroupId(UUID.randomUUID().toString());
        // distinct groups
        assumeThat(args1.getGroupId()).isNotNull().isNotEqualTo(args2.getGroupId());
        // distinct owners
        assumeThat(args1.getOwner()).isNotNull().isNotEqualTo(args2.getOwner());

        // GIVEN ... WHEN ... THEN ...
        createTwoReferenceRequests(args1, args2, 1, 1);
        // THEN assert that a FileReference has been created with both owners
        final FileReference fileReference = getFileReference(args1);
        assertFileReference(fileReference, args1, args1.getOwner(), args2.getOwner());
    }

    /**
     * Creation of two FileReferenceRequestAggregation referencing the same file.
     * Both have the same owner but they are not belongings to the same group.
     * Expected: creation of a single FileReference with a unique owner.
     */
    @Test
    public void createTwoReferenceRequestsWithSameOwnerDistinctGroups() {
        final FileReferenceRequestArgs args1 = newFileReferenceRequestArgs1();
        final FileReferenceRequestArgs args2 = args1.withGroupId(UUID.randomUUID().toString());
        // distinct groups
        assumeThat(args1.getGroupId()).isNotNull().isNotEqualTo(args2.getGroupId());
        // same owner
        assumeThat(args1.getOwner()).isNotNull().isEqualTo(args2.getOwner());

        // GIVEN ... WHEN ... THEN ...
        createTwoReferenceRequests(args1, args2, 1, 1);

        // THEN assert that a FileReference has been created with the single owner
        final FileReference fileReference = getFileReference(args1);
        assertFileReference(fileReference, args1, args1.getOwner());
    }

    /**
     * The second request is terminated in ERROR.
     */
    @Test
    public void createTwoReferenceRequestWithSameUrlAndDistinctChecksum() {
        final FileReferenceRequestArgs args1 = newFileReferenceRequestArgs1();
        final FileReferenceRequestArgs args2 = args1.withChecksum(CHECKSUM2)
                                                    .withExpectedStatus(FileRequestStatus.ERROR);
        // distinct checksum
        assumeThat(args1.getChecksum()).isNotNull().isNotEqualTo(args2.getChecksum());
        // same storage
        assumeThat(args1.getStorage()).isNotNull().isEqualTo(args2.getStorage());
        // same url
        assumeThat(args1.getUrl()).isNotNull().isEqualTo(args2.getUrl());

        // GIVEN ... WHEN ... THEN ...
        createTwoReferenceRequests(args1, args2, 1, 0);

        // THEN assert that a FileReference has been created with the single owner
        final FileReference fileReference = getFileReference(args1);
        assertFileReference(fileReference, args1, args1.getOwner());
    }

    /**
     * The second request is just ignored and terminated in SUCCESS
     */
    @Test
    public void createTwoReferenceRequestWithDistinctUrlAndSameChecksum() {
        final FileReferenceRequestArgs args1 = newFileReferenceRequestArgs1();
        final FileReferenceRequestArgs args2 = args1.withUrl(URL2);
        // same checksum
        assumeThat(args1.getChecksum()).isNotNull().isEqualTo(args2.getChecksum());
        // distinct urls
        assumeThat(args1.getUrl()).isNotNull().isNotEqualTo(args2.getUrl());

        // GIVEN ... WHEN ... THEN ...
        createTwoReferenceRequests(args1, args2, 1, 1);

        // THEN assert that a FileReference has been created with the single owner
        final FileReference fileReference = getFileReference(args1);
        assertFileReference(fileReference, args1, args1.getOwner());
    }

    @Test
    public void createTwoReferenceRequestWithDistinctStorageAndSameChecksum() {
        final FileReferenceRequestArgs args1 = newFileReferenceRequestArgs1();
        final FileReferenceRequestArgs args2 = args1.withStorage(STORAGE2);
        // same checksum
        assumeThat(args1.getChecksum()).isNotNull().isEqualTo(args2.getChecksum());
        // distinct storage
        assumeThat(args1.getStorage()).isNotNull().isNotEqualTo(args2.getStorage());

        // GIVEN ... WHEN ... THEN ...
        createTwoReferenceRequests(args1, args2, 2, 0);

        // THEN assert that 2 distinct FileReference have been created
        final FileReference fileReference1 = getFileReference(args1);
        assertFileReference(fileReference1, args1, args1.getOwner());
        final FileReference fileReference2 = getFileReference(args2);
        assertFileReference(fileReference2, args2, args2.getOwner());
        assertThat(fileReference1.getId()).as("Both reference are distinct").isNotEqualTo(fileReference2.getId());
    }

    @Test
    public void createTwoReferenceRequestWithDistinctStorageAndDistinctChecksum() {
        final FileReferenceRequestArgs args1 = newFileReferenceRequestArgs1();
        final FileReferenceRequestArgs args2 = args1.withStorage(STORAGE2).withChecksum(CHECKSUM2);
        // distinct checksum
        assumeThat(args1.getChecksum()).isNotNull().isNotEqualTo(args2.getChecksum());
        // distinct storage
        assumeThat(args1.getStorage()).isNotNull().isNotEqualTo(args2.getStorage());

        // GIVEN ... WHEN ... THEN ...
        createTwoReferenceRequests(args1, args2, 2, 0);

        // THEN assert that 2 distinct FileReference have been created
        final FileReference fileReference1 = getFileReference(args1);
        assertFileReference(fileReference1, args1, args1.getOwner());
        final FileReference fileReference2 = getFileReference(args2);
        assertFileReference(fileReference2, args2, args2.getOwner());
        assertThat(fileReference1.getId()).as("Both reference are distinct").isNotEqualTo(fileReference2.getId());
    }

    private void createTwoReferenceRequests(FileReferenceRequestArgs args1,
                                            FileReferenceRequestArgs args2,
                                            int expectedJobCount1,
                                            int expectedJobCount2) {
        assumeThat(anyDeletionRequest(args1.getStorage(), args1.getChecksum())).isFalse();
        // Create a 1st file reference request FileReferenceRequestAggregation
        final FilesReferenceEvent filesRefEvent1 = this.createReferenceRequest(args1);
        assumeThat(anyDeletionRequest(args1.getStorage(), args1.getChecksum())).isFalse();

        final String checksum = filesRefEvent1.getFiles().iterator().next().getChecksum();
        final String storage = filesRefEvent1.getFiles().iterator().next().getStorage();

        // Create a 2nd file reference request FileReferenceRequestAggregation
        final FilesReferenceEvent filesRefEvent2 = this.createReferenceRequest(args2);
        assumeThat(anyDeletionRequest(args2.getStorage(), args2.getChecksum())).isFalse();

        final boolean sameGroup = Objects.equals(filesRefEvent1.getGroupId(), filesRefEvent2.getGroupId());
        final boolean sameChecksum = Objects.equals(args1.getChecksum(), args2.getChecksum());
        final boolean sameStorage = Objects.equals(args1.getStorage(), args2.getStorage());
        final boolean same = sameGroup && sameChecksum && sameStorage;
        if (same) {
            final Set<FileRequestStatus> statuses = getStatuses(checksum, storage);
            assumeThat(statuses).containsExactlyInAnyOrder(FileRequestStatus.TO_DO, FileRequestStatus.DELAYED);
        } else {
            // assert that both FileReferenceRequestAggregation have been created
            final FileReferenceRequestAggregation createdRequest1 = getReferenceRequest(args1);
            assumeThat(createdRequest1).as("FileReferenceRequestAggregation should have been created").isNotNull();
            assumeStatus(createdRequest1, FileRequestStatus.TO_DO, FIRST);
            final FileRequestStatus expectedStatus = (sameChecksum && sameStorage) ?
                FileRequestStatus.DELAYED :
                FileRequestStatus.TO_DO;
            final FileReferenceRequestAggregation createdRequest2 = getReferenceRequest(args2);
            assumeThat(createdRequest2).as("FileReferenceRequestAggregation should have been created").isNotNull();
            assumeStatus(createdRequest2, expectedStatus, SECOND);
        }
        // but no FileReference created yet
        final FileReference noFileRef1 = referenceService.search(args1.getStorage(), args1.getChecksum()).orElse(null);
        assumeThat(noFileRef1).as("FileReference not yet created").isNull();
        final FileReference noFileRef2 = referenceService.search(args2.getStorage(), args2.getChecksum()).orElse(null);
        assumeThat(noFileRef2).as("FileReference not yet created").isNull();

        // check and update status
        statusService.checkDelayedReferenceRequests();

        if (same) {
            final Set<FileRequestStatus> statuses = getStatuses(checksum, storage);
            assumeThat(statuses).containsExactlyInAnyOrder(FileRequestStatus.TO_DO, FileRequestStatus.DELAYED);
        } else {
            // assert that status is TO_DO
            final FileReferenceRequestAggregation updatedRequest1 = getReferenceRequest(args1);
            assumeStatus(updatedRequest1, FileRequestStatus.TO_DO, FIRST);

            final FileReferenceRequestAggregation updatedRequest2 = getReferenceRequest(args2);
            final FileRequestStatus expectedStatus = (sameChecksum && sameStorage) ?
                FileRequestStatus.DELAYED :
                FileRequestStatus.TO_DO;
            assumeStatus(updatedRequest2, expectedStatus, SECOND);
        }
        // WHEN
        // schedule FileReferenceRequestJob
        // The TO_DO FileReferenceRequestAggregation is scheduled
        this.scheduleReferenceRequestJob(expectedJobCount1);
        if (expectedJobCount1 == 1) {
            // The DELAYED FileReferenceRequestAggregation is re-scheduled
            this.checkAndScheduleReferenceRequestJob(expectedJobCount2);
        }
        // and schedule again
        this.checkAndScheduleReferenceRequestJob(0);

        // THEN
        // assert that all FileReferenceRequestAggregation have been handled and are in a SUCCESS status
        if (same) {
            assumeThat(getStatuses(checksum, storage)).containsExactlyInAnyOrder(FileRequestStatus.SUCCESS);
        } else {
            final FileReferenceRequestAggregation successfulRequest1 = getReferenceRequest(args1);
            assertStatus(successfulRequest1, args1.getExpectedStatus(), FIRST);
            final FileReferenceRequestAggregation successfulRequest2 = getReferenceRequest(args2);
            assertStatus(successfulRequest2, args2.getExpectedStatus(), SECOND);
        }
        // remove all the FileReferenceRequestAggregation in SUCCESS or ERROR status
        referenceRequestService.deleteAllTerminatedRequestOfGroups(Sets.newHashSet(filesRefEvent1.getGroupId(),
                                                                                   filesRefEvent2.getGroupId()));

        final Collection<FileReferenceRequestAggregation> noRequests = referenceRequestRepository.findByMetaInfoChecksumAndStorage(
            checksum,
            storage);
        assertThat(noRequests).as("FileReferenceRequestAggregation should have been removed").isEmpty();
    }

}
