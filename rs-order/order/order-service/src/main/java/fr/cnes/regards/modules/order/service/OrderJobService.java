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
package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockService;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockServiceResponse;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.order.dao.IFilesTasksRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.dto.dto.OrderStatus;
import fr.cnes.regards.modules.order.service.job.OrderJobPriority;
import fr.cnes.regards.modules.order.service.job.StorageFilesJob;
import fr.cnes.regards.modules.order.service.job.parameters.FilesJobParameter;
import fr.cnes.regards.modules.order.service.request.CancelOrderJob;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Order jobs specific behavior, like priority computation or job enqueue user business rules management
 *
 * @author oroussel
 */
@Service
@MultitenantTransactional
@RefreshScope
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class OrderJobService implements IOrderJobService, IHandler<JobEvent>, DisposableBean {

    /**
     * Number of concurrent storage files retrieval jobs per user
     */
    @Value("${regards.order.max.storage.files.jobs.per.user:2}")
    private int maxJobsPerUser;

    private final IJobInfoRepository jobInfoRepository;

    private final IFilesTasksRepository filesTasksRepository;

    private final IOrderDataFileRepository orderDataFileRepository;

    private final IOrderRepository orderRepository;

    private final ISubscriber subscriber;

    private final IOrderJobService self;

    private final LockService lockService;

    private final IRuntimeTenantResolver tenantResolver;

    public OrderJobService(IJobInfoRepository jobInfoRepository,
                           IFilesTasksRepository filesTasksRepository,
                           IOrderDataFileRepository orderDataFileRepository,
                           IOrderRepository orderRepository,
                           ISubscriber subscriber,
                           IOrderJobService orderJobService,
                           IRuntimeTenantResolver tenantResolver,
                           LockService lockService) {
        this.jobInfoRepository = jobInfoRepository;
        this.filesTasksRepository = filesTasksRepository;
        this.orderDataFileRepository = orderDataFileRepository;
        this.orderRepository = orderRepository;
        this.subscriber = subscriber;
        this.self = orderJobService;
        this.lockService = lockService;
        this.tenantResolver = tenantResolver;
    }

    @Override
    @EventListener
    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    // Doesn't need a transaction (make Controller IT tests failed otherwise)
    public void handleApplicationReadyEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(JobEvent.class, this);
    }

    @Override
    public void destroy() {
        subscriber.unsubscribeFrom(JobEvent.class, false);
    }

    @Override
    @EventListener
    public void handleRefreshScopeRefreshedEvent(RefreshScopeRefreshedEvent event) {
        subscriber.subscribeTo(JobEvent.class, this);
    }

    @Override
    public int computePriority(String user, String role) {
        // Total running and future jobs
        long currentTotal = jobInfoRepository.countFutureAndRunningJobs();
        // Total running and future jobs of user
        long currentUser = jobInfoRepository.countUserFutureAndRunningJobs(user);
        // rate : current user jobs / current total jobs
        double rate = currentTotal == 0L ? 1. : (double) currentUser / (double) currentTotal;
        // a user PUBLIC cannot be here so there are two cases : REGISTERED_USER and all ADMIN roles (near a thousand)
        if (role.equals(DefaultRole.REGISTERED_USER.toString())) {
            // User : Priority between 0 and 80 depending on rate
            return (int) (OrderJobPriority.PROCESS_ORDER_MIN_JOB_PRIORITY * (1 - rate));
        }
        // Admin : Priority between 80 and 100 depending on rate
        return (int) (OrderJobPriority.PROCESS_ORDER_MAX_JOB_PRIORITY - 20 * (1 - rate));
    }

    /**
     * Each time something happens on a storage job, an event is thrown
     */
    @Override
    public void handle(TenantWrapper<JobEvent> wrapper) {
        JobEvent event = wrapper.getContent();
        if (event.getJobEventType().isFinalState()) {
            tenantResolver.forceTenant(wrapper.getTenant());

            JobInfo endedJobInfo = jobInfoRepository.findCompleteById(event.getJobId());
            if (endedJobInfo != null) {
                if (JobEventType.FAILED == event.getJobEventType()) {
                    if (CancelOrderJob.class.getName().equals(endedJobInfo.getClassName())) {
                        LOGGER.info("[{}] cancel order job is in failed status. Initialize in queued status again",
                                    endedJobInfo.getId());
                        endedJobInfo.updateStatus(JobStatus.QUEUED);
                        jobInfoRepository.save(endedJobInfo);
                    }
                    if (StorageFilesJob.class.getName().equals(endedJobInfo.getClassName())) {
                        LOGGER.info("[{}] storage file job is in failed status. set as failed status",
                                    endedJobInfo.getId());
                        // Update datafile_status to error
                        Long[] fileIds = endedJobInfo.getParametersAsMap().get(FilesJobParameter.NAME).getValue();
                        List<OrderDataFile> errorDataFiles =
                            orderDataFileRepository.findAllById(Arrays.asList(fileIds));
                        errorDataFiles.forEach(df -> df.setState(FileState.ERROR));
                        // Save update dataFiles with error status and launch next fileTasks if any (done in the
                        // saveAll method)
                        orderDataFileRepository.saveAll(errorDataFiles);
                    }
                }

                self.manageUserOrderStorageFilesJobInfos(endedJobInfo.getOwner());
            }
            tenantResolver.clearTenant();
        }
    }

    private Void doManageUserOrderStorageFilesJobInfos(String user) {
        int currentJobsCount = (int) jobInfoRepository.countUserPlannedAndRunningJobs(user);

        // Current Waiting for user jobs
        int finishedJobsWithFilesToBeDownloadedCount = (int) filesTasksRepository.countWaitingForUserFilesTasks(user);

        // There is room for several jobs to be executed for this user if sum of theses 2 values is less than maximum
        // defined one
        if (currentJobsCount + finishedJobsWithFilesToBeDownloadedCount < maxJobsPerUser) {
            int count = maxJobsPerUser - currentJobsCount - finishedJobsWithFilesToBeDownloadedCount;
            List<JobInfo> jobInfos = jobInfoRepository.findTopUserPendingJobs(user,
                                                                              StorageFilesJob.class.getName(),
                                                                              count);
            if (!jobInfos.isEmpty()) {
                for (JobInfo jobInfo : jobInfos) {
                    Long[] filesId = jobInfo.getParametersAsMap().get("files").getValue();
                    Long orderId = orderDataFileRepository.getById(filesId[0]).getOrderId();
                    if (!isOrderPaused(orderId)) {
                        jobInfo.updateStatus(JobStatus.QUEUED);
                    }
                }
                jobInfoRepository.saveAll(jobInfos);
            }
        }
        return null;
    }

    @Override
    public void manageUserOrderStorageFilesJobInfos(String user) {
        // Current count of user jobs running, planned or to be run
        if (user != null) {
            try {
                LockServiceResponse<Object> lockResponse = lockService.runWithLock(String.format("run-suborders-%s",
                                                                                                 user),
                                                                                   () -> doManageUserOrderStorageFilesJobInfos(
                                                                                       user));
                if (!lockResponse.isExecuted()) {
                    LOGGER.error(String.format("Wait too long for a lock to run suborders of user %s.", user));
                }
            } catch (InterruptedException e) {
                LOGGER.error(String.format("Thread interrupted while waiting for lock to run new suborders. Cause : %s",
                                           e.getMessage()), e);
            }
        }
    }

    @Override
    public boolean isOrderPaused(Long orderId) {
        Order order = orderRepository.getById(orderId);
        return order.getStatus().equals(OrderStatus.PAUSED);
    }

}
