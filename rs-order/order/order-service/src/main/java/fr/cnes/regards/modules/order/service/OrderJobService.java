/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Order jobs specific behavior, like priority computation or job enqueue user business rules management
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class OrderJobService
        implements IOrderJobService, IHandler<JobEvent> {

    /**
     * Number of concurrent storage files retrieval jobs per user
     */
    @Value("${regards.order.max.storage.files.jobs.per.user:2}")
    private int maxJobsPerUser;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IOrderJobService self;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        subscriber.subscribeTo(JobEvent.class, this);
    }

    @Override
    public int computePriority(String user, String role) {
        // Total running and future jobs
        long currentTotal = jobInfoRepository.countFutureAndRunningJobs();
        // Total running and future jobs of user
        long currentUser = jobInfoRepository.countUserFutureAndRunningJobs(user);
        // rate : current user jobs / current total jobs
        double rate = (currentTotal == 0l) ? 1. : (double) currentUser / (double) currentTotal;
        // a user PUBLIC cannot be here so there is two case : REGISTERED_USER and all ADMIN roles (near a thousand)
        if (role.equals(DefaultRole.REGISTERED_USER)) {
            // User : Priority between 0 and 80 depending on rate
            return (int) (80 * (1 - rate));
        }
        // Admin : Priotiry between 80 and 100 depending on rate
        return (int) (100 - 20 * (1 - rate));
    }

    @Override
    public void manageUserOrderJobInfos(String user) {
        int currentUserJobCount = (int) jobInfoRepository.countUserPlannedAndRunningJobs(user);
        // There is room for several jobs to be executed for this user
        if (currentUserJobCount < maxJobsPerUser) {
            List<JobInfo> jobInfos = jobInfoRepository
                    .findTopUserPendingJobs(user, maxJobsPerUser - currentUserJobCount);
            if (!jobInfos.isEmpty()) {
                for (JobInfo jobInfo : jobInfos) {
                    jobInfo.updateStatus(JobStatus.QUEUED);
                }
                jobInfoRepository.save(jobInfos);
            }
        }
    }

    /**
     * Each time something happens on a storage job, an event is thrown
     */
    @Override
    public void handle(TenantWrapper<JobEvent> wrapper) {
        JobEvent event = wrapper.getContent();
        switch (event.getJobEventType()) {
            // If job is ended
            case ABORTED:
            case FAILED:
            case SUCCEEDED:
                UUID jobId = event.getJobId();
                tenantResolver.forceTenant(wrapper.getTenant());
                JobInfo endedJobInfo = jobInfoRepository.findOne(jobId);
                if (endedJobInfo != null) {
                    self.manageUserOrderJobInfos(endedJobInfo.getOwner());
                }
        }
    }

}
