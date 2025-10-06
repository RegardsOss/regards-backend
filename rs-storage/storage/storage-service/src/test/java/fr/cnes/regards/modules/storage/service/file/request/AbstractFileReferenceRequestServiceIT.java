/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */

package fr.cnes.regards.modules.storage.service.file.request;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileReferenceRequestAggregation;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceRequestArgs;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.springframework.http.MediaType;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * @author Olivier Navarro
 **/
public abstract class AbstractFileReferenceRequestServiceIT extends AbstractStorageIT {

    protected String tenant;

    @Before
    public void retrieveTenant() {
        tenant = runtimeTenantResolver.getTenant();
    }

    protected void forceTenant() {
        runtimeTenantResolver.forceTenant(tenant);
    }

    @Before
    @Override
    public void init() throws ModuleException {
        super.init();
    }

    protected void assumeStatus(FileReferenceRequestAggregation request,
                                FileRequestStatus status,
                                String firstOrSecond) {
        assertThat(request).as("%sFileReferenceRequestAggregation exists", firstOrSecond).isNotNull();
        assertThat(request.getStatus()).as("%sFileReferenceRequestAggregation should be in a %s status",
                                           firstOrSecond,
                                           status.name()).isEqualTo(status);
    }

    protected void assertStatus(FileReferenceRequestAggregation request,
                                FileRequestStatus status,
                                String firstOrSecond) {
        assertThat(request).as("%sFileReferenceRequestAggregation should exists", firstOrSecond).isNotNull();
        assertThat(request.getStatus()).as("%sFileReferenceRequestAggregation should be in a %s status",
                                           firstOrSecond,
                                           status.name()).isEqualTo(status);
    }

    protected Set<FileRequestStatus> getStatuses(String checksum, String storage) {
        return referenceRequestRepository.findByMetaInfoChecksumAndStorage(checksum, storage)
                                         .stream()
                                         .map(FileReferenceRequestAggregation::getStatus)
                                         .collect(Collectors.toSet());
    }

    protected @Nullable FileReferenceRequestAggregation getReferenceRequest(String storage, String checksum) {
        return referenceRequestRepository.findByStorageAndMetaInfoChecksum(storage, checksum).orElse(null);
    }

    protected @Nullable FileReferenceRequestAggregation getReferenceRequest(FileReferenceRequestArgs args) {
        final String groupId = args.getGroupId();
        return referenceRequestRepository.findByMetaInfoChecksumAndStorage(args.getChecksum(), args.getStorage())
                                         .stream()
                                         .filter(request -> request.getGroupIds().contains(groupId))
                                         .findFirst()
                                         .orElse(null);
    }

    protected FileReference getFileReference(FileReferenceRequestArgs args) {
        return referenceService.search(args.getStorage(), args.getChecksum()).orElse(null);
    }

    protected void assertFileReference(FileReference fileReference, String... owners) {
        // assert file reference exists
        assertThat(fileReference).as("File reference should exists").isNotNull();

        // assert the owners
        assertFileReferenceOwners(fileReference, owners);
    }

    protected void assertFileReference(FileReference fileReference, FileReferenceRequestArgs args, String... owners) {
        // assert file reference exists
        assertThat(fileReference).as("File reference should exists").isNotNull();
        // assert checksum
        assertThat(fileReference.getMetaInfo()).isNotNull();
        assertThat(fileReference.getMetaInfo().getChecksum()).isEqualTo(args.getChecksum());
        // assert storage
        final FileLocation location = fileReference.getLocation();
        assertThat(location).isNotNull();
        assertThat(location.getStorage()).isEqualTo(args.getStorage());
        // asset url
        assertThat(location.getUrl()).isEqualTo(args.getUrl());

        // assert the owners
        assertFileReferenceOwners(fileReference, owners);
    }

    protected void assertFileReferenceOwners(FileReference fileReference, String... owners) {
        final Set<String> expectedOwners = Sets.newHashSet(owners);
        final FileReference fileReferenceWithOwners = referenceWithOwnersRepository.findOneById(fileReference.getId());
        assertThat(fileReferenceWithOwners).isNotNull();
        assertThat(fileReferenceWithOwners.getLazzyOwners()).as("File reference should have owners %s", expectedOwners)
                                                            .containsExactlyInAnyOrderElementsOf(expectedOwners);
    }

    protected FilesReferenceEvent createReferenceRequest(FileReferenceRequestArgs args) {

        // count the number of request before adding a new one.
        final Set<FileReferenceRequestAggregation> requestsBefore = referenceRequestRepository.findByMetaInfoChecksumAndStorage(
            args.getChecksum(),
            args.getStorage());

        // instantiate a file reference request event
        final FileReferenceRequestDto fileReferenceRequestDto = FileReferenceRequestDto.build(args.getFileName(),
                                                                                              args.getChecksum(),
                                                                                              "MD5",
                                                                                              MediaType.APPLICATION_OCTET_STREAM.toString(),
                                                                                              1024L,

                                                                                              args.getOwner(),
                                                                                              args.getStorage(),
                                                                                              args.getUrl(),

                                                                                              args.getSessionOwner(),
                                                                                              args.getSession());
        final String groupId = UUID.randomUUID().toString();
        final FilesReferenceEvent event = new FilesReferenceEvent(fileReferenceRequestDto, args.getGroupId());

        // create the async request into the IFileReferenceRequestRepository
        referenceRequestService.createReferenceRequests(List.of(event));

        // expect the new reference request to be created by comparing the difference between the two sets of
        // requests before and after the creation.
        final Set<FileReferenceRequestAggregation> requestsAfter = referenceRequestRepository.findByMetaInfoChecksumAndStorage(
            args.getChecksum(),
            args.getStorage());
        assumeThat(requestsAfter).hasSize(requestsBefore.size() + 1).containsAll(requestsBefore);
        final boolean allRemoved = requestsAfter.removeAll(requestsBefore);
        assumeThat(requestsBefore.isEmpty() || allRemoved).isTrue();
        assumeThat(requestsAfter).as("New FileReferenceRequestAggregation should have been created").hasSize(1);

        final FileReferenceRequestAggregation newRequest = requestsAfter.iterator().next();
        assumeThat(newRequest).as("New FileReferenceRequestAggregation should have been created").isNotNull();

        final boolean anyDeletion = anyDeletionRequest(args.getStorage(), args.getChecksum());
        final FileRequestStatus expectedStatus = requestsBefore.isEmpty() && !anyDeletion ?
            FileRequestStatus.TO_DO :
            FileRequestStatus.DELAYED;
        assumeThat(newRequest.getStatus()).as("FileReferenceRequestAggregation should be in %s status", expectedStatus)
                                          .isEqualTo(expectedStatus);
        return event;
    }

    protected Collection<JobInfo> checkAndScheduleReferenceRequestJob(int expectedJobCount) {
        statusService.checkDelayedReferenceRequests();
        return this.scheduleReferenceRequestJob(expectedJobCount);
    }

    protected Collection<JobInfo> scheduleReferenceRequestJob(int expectedJobCount) {
        runtimeTenantResolver.forceTenant(tenant);
        // Schedule job to initiate the FileReferenceRequestJob associated to the FileReferenceRequestAggregation
        // created earlier. Run the job till completion.
        final Collection<JobInfo> jobs = referenceRequestSchedulingService.scheduleJobs(FileRequestStatus.TO_DO);
        assertThat(jobs).as("Reference job should be scheduled").hasSize(expectedJobCount);
        // Run Job and wait for the end
        runAndWaitJob(jobs);
        return jobs;
    }

    protected boolean anyDeletionRequest(String storage, String checksum) {
        return deletionRequestRepository.existsByStorageAndFileReferenceMetaInfoChecksum(storage, checksum);
    }

}
