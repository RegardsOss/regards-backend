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
package fr.cnes.regards.modules.workermanager.service.requests.scan;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.workermanager.dao.IWorkerConfigRepository;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerConfig;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.domain.request.SearchRequestParameters;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.JobsPriority;
import fr.cnes.regards.modules.workermanager.service.cache.WorkerCacheService;
import fr.cnes.regards.modules.workermanager.service.requests.RequestService;
import fr.cnes.regards.modules.workermanager.service.requests.job.DeleteRequestJob;
import fr.cnes.regards.modules.workermanager.service.requests.job.DispatchRequestJob;
import net.javacrumbs.shedlock.core.LockConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This service scans requests and changes their status {@link RequestStatus} as fast as possible
 *
 * @author Léo Mieulet
 */
@Service
public class RequestScanService {

    public static final String REQUEST_SCAN_LOCK = "scanRequests";

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestScanService.class);

    private static final Long MAX_TASK_WAIT_DURING_SCHEDULE = 30L; // In second

    private static final int DEFAULT_SCAN_PAGE_SIZE = 400;

    @Value("${regards.workermanager.scan.page.size:" + DEFAULT_SCAN_PAGE_SIZE + "}")
    public int scanPageSize;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private RequestService requestService;

    @Autowired
    private WorkerCacheService workerCacheService;

    @Autowired
    private IWorkerConfigRepository workerConfigRepo;

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    @Autowired
    private RequestScanService self;

    /**
     * Check if requests with state {@link RequestStatus#NO_WORKER_AVAILABLE} and corresponding worker alive exists
     * If there is, run a {@link RequestScanTask} to handle them
     *
     * @throws Throwable
     */
    public void scanNoWorkerAvailableRequests() throws Throwable {
        List<WorkerConfig> workerConfigs = workerConfigRepo.findAll();
        // Iterate over all worker configs
        for (WorkerConfig workerConfig : workerConfigs) {
            // Check if there is such type of workers alive
            // And check if there is some requests waiting for available worker
            Optional<String> firstContentType = workerConfig.getContentTypes().stream().findFirst();
            if (firstContentType.isPresent() && workerCacheService.getWorkerTypeByContentType(firstContentType.get())
                    .isPresent() && requestService.hasRequestsMatchingContentTypeAndNoWorkerAvailable(
                    workerConfig.getContentTypes())) {
                SearchRequestParameters filters = new SearchRequestParameters().withContentTypesIncluded(
                        workerConfig.getContentTypes()).withStatusesIncluded(RequestStatus.NO_WORKER_AVAILABLE);
                scanUsingFilters(filters, RequestStatus.TO_DISPATCH, MAX_TASK_WAIT_DURING_SCHEDULE);
            }
        }
    }

    public void scanUsingFilters(SearchRequestParameters filters, RequestStatus newStatus, Long lockAtMostUntilSec)
            throws Throwable {
        lockingTaskExecutors.executeWithLock(new RequestScanTask(this, filters, newStatus),
                                             new LockConfiguration(RequestScanService.REQUEST_SCAN_LOCK,
                                                                   Instant.now().plusSeconds(lockAtMostUntilSec)));
    }

    /**
     * Open a transaction that updates one page of requests matching provided filters
     * and updates their state to the provided one, then submit them on a job
     *
     * @param filters
     * @param newStatus
     * @return
     */
    public void updateRequestsToStatusAndScheduleJob(SearchRequestParameters filters, RequestStatus newStatus)
            throws ModuleException {
        LOGGER.debug("[REQUESTS SCAN] Start updating requests status ... ");
        long start = System.currentTimeMillis();

        int totalProducts = 0;
        int nbJob = 0;
        boolean hasNext;
        do {
            // Handle one page in another transaction
            int nbProductsHandled = self.updateOnePageOfRequestsToStatusAndScheduleJob(filters, newStatus);
            if (nbProductsHandled > 0) {
                nbJob += 1;
                totalProducts += nbProductsHandled;
                hasNext = true;
            } else {
                hasNext = false;
            }
        } while (hasNext);
        LOGGER.info("{} requests updated to status [{}] and {} jobs have been scheduled in {} ms", totalProducts,
                    newStatus, nbJob, System.currentTimeMillis() - start);
    }

    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    public int updateOnePageOfRequestsToStatusAndScheduleJob(SearchRequestParameters filters, RequestStatus newStatus)
            throws ModuleException {
        Pageable pageable = PageRequest.of(0, scanPageSize);
        Page<Request> requests = requestService.searchRequests(filters, pageable);
        if (requests.getNumberOfElements() > 0) {
            scheduleJob(newStatus, requests);
            requestService.updateRequestsStatusTo(requests, newStatus);
        }
        return requests.getNumberOfElements();
    }

    protected JobInfo scheduleJob(RequestStatus status, Page<Request> requests) throws ModuleException {

        Set<Long> requestsIds = requests.stream().map(Request::getId).collect(Collectors.toSet());
        int priority;
        HashSet<JobParameter> jobParameters;
        String className;

        switch (status) {
            case TO_DELETE:
                jobParameters = Sets.newHashSet(new JobParameter(DeleteRequestJob.REQUEST_DB_IDS, requestsIds));
                priority = getDeletionJobPriority();
                className = DeleteRequestJob.class.getName();
                break;
            case TO_DISPATCH:
                jobParameters = Sets.newHashSet(new JobParameter(DispatchRequestJob.REQUEST_DB_IDS, requestsIds));
                priority = getDispatchJobPriority();
                className = DispatchRequestJob.class.getName();
                break;
            default:
                String error = String.format("Unsupported status %s.", status);
                LOGGER.error(error);
                throw new ModuleException(error);
        }
        // create job
        return jobInfoService.createAsQueued(
                new JobInfo(false, priority, jobParameters, authResolver.getUser(), className));
    }

    private int getDeletionJobPriority() {
        return JobsPriority.REQUEST_DELETION_JOB.getPriority();
    }

    private int getDispatchJobPriority() {
        return JobsPriority.REQUEST_DISPATCH_JOB.getPriority();
    }
}
