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

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestAggregationDto;
import fr.cnes.regards.modules.fileaccess.plugin.domain.FileStorageWorkingSubset;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRequestRepository;
import fr.cnes.regards.modules.storage.domain.database.request.FileReferenceRequestAggregation;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.StorageJobsPriority;
import fr.cnes.regards.modules.storage.service.file.job.FileReferenceRequestJob;
import fr.cnes.regards.modules.storage.service.file.job.FileStorageRequestJob;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Service for creating and registering new {@link FileReferenceRequestJob}.
 * It is purposely not implemented in {@link FileReferenceRequestService}
 * following the principle of separation of concern.
 * This service is called by the {@link FileRequestScheduler}.
 *
 * @author Olivier Navarro
 **/
@Service
@MultitenantTransactional
@RequiredArgsConstructor
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class FileReferenceRequestJobSchedulingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceRequestJobSchedulingService.class);

    private final IFileReferenceRequestRepository fileRefRequestRepository;

    // TODO remove this dirty hack
    private final FileReferenceRequestJobSchedulingService self;

    private final IJobInfoService jobInfoService;

    private final IAuthenticationResolver authenticationResolver;

    @Value("${regards.storage.storage.requests.per.job:100}")
    private Integer nbRequestsPerJob;

    /**
     * Schedule {@link FileStorageRequestJob}s for all the {@link FileStorageRequestAggregation}s grouped by storage.
     * One JobInfo is created per group.
     *
     * @param status of the request to handle
     * @return {@link JobInfo}s scheduled
     */
    public Collection<JobInfo> scheduleJobs(FileRequestStatus status) {
        return this.scheduleJobs(status, Set.of(), Set.of());
    }

    /**
     * Schedule {@link FileStorageRequestJob}s for all {@link FileStorageRequestAggregation}s matching the given
     * parameters. FileStorageRequestAggregation are grouped by storage. One JobInfo is created per group.
     *
     * @param status   of the request to handle
     * @param storages of the request to handle
     * @param owners   of the request to handle
     * @return Collection of {@link JobInfo}s scheduled
     */
    public Collection<JobInfo> scheduleJobs(FileRequestStatus status,
                                            Collection<String> storages,
                                            Collection<String> owners) {

        // build the job list
        final Collection<JobInfo> jobInfos = Lists.newArrayList();

        final Set<String> allStorages = fileRefRequestRepository.findStoragesByStatus(status);
        final Set<String> storagesToSchedule = CollectionUtils.isEmpty(storages) ?
            allStorages :
            allStorages.stream().filter(storages::contains).collect(Collectors.toSet());

        final long start = System.currentTimeMillis();
        LOGGER.trace("[REFERENCE REQUESTS] Scheduling storage jobs ...");
        for (String storage : storagesToSchedule) {
            boolean productRemains;
            do {
                productRemains = self.scheduleJobsByStorage(jobInfos, storage, owners, status);
            } while (productRemains);
        }
        LOGGER.debug("[REFERENCE REQUESTS] {} jobs scheduled in {} ms",
                     jobInfos.size(),
                     System.currentTimeMillis() - start);
        return jobInfos;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean scheduleJobsByStorage(Collection<JobInfo> jobInfos,
                                         String storage,
                                         Collection<String> owners,
                                         FileRequestStatus status) {
        final Long maxId = 0L;
        // Always search the first page of requests until there is no requests anymore.
        // To do so, we order on id to ensure to not handle same requests multiple times.
        final Pageable page = PageRequest.of(0, nbRequestsPerJob, Sort.by("id"));
        // Always retrieve first page, as request status are updated during job scheduling method.
        Page<FileReferenceRequestAggregation> filesPage;
        if (CollectionUtils.isEmpty(owners)) {
            filesPage = fileRefRequestRepository.findAllByStorageAndStatusAndIdGreaterThan(storage,
                                                                                           status,
                                                                                           maxId,
                                                                                           page);
        } else {
            filesPage = fileRefRequestRepository.findAllByStorageAndStatusAndOwnersInAndIdGreaterThan(storage,
                                                                                                      status,
                                                                                                      owners,
                                                                                                      maxId,
                                                                                                      page);
        }
        if (filesPage.hasContent()) {
            final Optional<JobInfo> optJobInfo = scheduleJobsByStorage(storage, filesPage.getContent());
            optJobInfo.ifPresent(jobInfos::add);
        }
        return filesPage.hasContent();
    }

    /**
     * Schedule {@link FileStorageRequestJob}s for all given {@link FileReferenceRequestAggregation}s and a given storage location.
     *
     * @return {@link JobInfo}s scheduled
     */
    private Optional<JobInfo> scheduleJobsByStorage(final String storage,
                                                    final Collection<FileReferenceRequestAggregation> requests) {
        LOGGER.debug("Nb requests to schedule for storage {} = {}", storage, requests.size());

        final Predicate<FileReferenceRequestAggregation> isSameStorage = request -> Objects.equals(request.getStorage(),
                                                                                                   storage);
        final Collection<FileReferenceRequestAggregation> reqWithSameStorage = requests.stream()
                                                                                       .filter(isSameStorage)
                                                                                       .collect(Collectors.toSet());

        if (!reqWithSameStorage.isEmpty()) {
            final Set<FileStorageRequestAggregationDto> dtos = reqWithSameStorage.stream()
                                                                                 .map(FileReferenceRequestAggregation::toDto)
                                                                                 .collect(Collectors.toSet());
            final FileStorageWorkingSubset workingSubset = new FileStorageWorkingSubset(dtos);

            final JobParameter parameter = new JobParameter(FileStorageRequestJob.WORKING_SUB_SET, workingSubset);
            final Set<JobParameter> parameters = Set.of(parameter);
            final JobInfo jobInfo = new JobInfo(false,
                                                StorageJobsPriority.FILE_REFERENCE_JOB,
                                                parameters,
                                                authenticationResolver.getUser(),
                                                FileReferenceRequestJob.class.getName());

            final JobInfo queuedJobInfo = jobInfoService.createAsQueued(jobInfo);
            for (FileReferenceRequestAggregation request : reqWithSameStorage) {
                request.setStatus(FileRequestStatus.PENDING);
                request.setJobId(queuedJobInfo.getId().toString());
                fileRefRequestRepository.save(request);
            }

            for (FileStorageRequestAggregationDto dto : dtos) {
                final Optional<FileReferenceRequestAggregation> optFound = fileRefRequestRepository.findById(dto.getId());
                optFound.filter(req -> FileRequestStatus.PENDING.equals(req.getStatus())).orElseThrow();
            }
            LOGGER.debug("[REFERENCE REQUESTS] Job scheduled for {} requests on storage {}", dtos.size(), storage);
            return Optional.of(queuedJobInfo);
        }
        return Optional.empty();
    }
}
