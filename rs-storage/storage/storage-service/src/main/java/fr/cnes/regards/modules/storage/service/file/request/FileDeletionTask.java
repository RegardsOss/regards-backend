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
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.storage.dao.IFileDeletetionRequestRepository;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.TaskWithResult;

/**
 *
 * @author Sébastien Binda
 *
 */
public class FileDeletionTask implements TaskWithResult<Collection<JobInfo>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDeletionTask.class);

    private final FileRequestStatus status;

    private final Collection<String> storages;

    private final int nbRequestsPerJob;

    private final Collection<JobInfo> jobList;

    private final IFileDeletetionRequestRepository fileDeletionRequestRepo;

    private final FileDeletionRequestService deletionRequestService;

    public FileDeletionTask(FileRequestStatus status, Collection<String> storages, int nbRequestsPerJob,
            Collection<JobInfo> jobList, IFileDeletetionRequestRepository fileDeletionRequestRepo,
            FileDeletionRequestService deletionRequestService) {
        super();
        this.status = status;
        this.storages = storages;
        this.nbRequestsPerJob = nbRequestsPerJob;
        this.jobList = jobList;
        this.fileDeletionRequestRepo = fileDeletionRequestRepo;
        this.deletionRequestService = deletionRequestService;
    }

    @Override
    public Collection<JobInfo> call() throws Throwable {
        LOGGER.trace("[DELETION REQUESTS] Scheduling deletion jobs ...");
        long start = System.currentTimeMillis();
        Set<String> allStorages = fileDeletionRequestRepo.findStoragesByStatus(status);
        Set<String> deletionToSchedule = (storages != null) && !storages.isEmpty()
                ? allStorages.stream().filter(storages::contains).collect(Collectors.toSet())
                : allStorages;
        int loop = 0;
        for (String storage : deletionToSchedule) {
            Page<FileDeletionRequest> deletionRequestPage;
            Long maxId = 0L;
            // Always search the first page of requests until there is no requests anymore.
            // To do so, we order on id to ensure to not handle same requests multiple times.
            Pageable page = PageRequest.of(0, nbRequestsPerJob, Direction.ASC, "id");
            do {
                deletionRequestPage = fileDeletionRequestRepo.findByStorageAndStatusAndIdGreaterThan(storage, status,
                                                                                                     maxId, page);
                if (deletionRequestPage.hasContent()) {
                    maxId = deletionRequestPage.stream().max(Comparator.comparing(FileDeletionRequest::getId)).get()
                            .getId();
                    jobList.addAll(deletionRequestService.scheduleDeletionJobsByStorage(storage, deletionRequestPage));
                }
                loop++;
            } while (deletionRequestPage.hasContent() && (loop < 10));
        }
        LOGGER.debug("[DELETION REQUESTS] {} jobs scheduled in {} ms", jobList.size(),
                     System.currentTimeMillis() - start);
        return jobList;
    }

}
