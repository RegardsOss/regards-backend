/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.jobs.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.modules.locks.service.ILockService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 * Enable feature task scheduling
 *
 * @author Marc SORDI
 *
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class JobSchedulerCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobSchedulerCleaner.class);

    private static final String LOCK_JOB_CLEAN = "Job_Clean";

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ILockService lockService;

    @Autowired
    private IJobService jobService;

    @Scheduled(initialDelayString = "${regards.job.cleaner.scheduling.initial.delay:10000}",
            fixedDelayString = "${regards.job.cleaner.scheduling.delay:1000}")
    public void scheduleUpdateRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                LOGGER.trace("LOCKING FOR TENANT {} IN SCHEDULE JOB CLEANING", tenant);
                if (lockService.obtainLockOrSkip(LOCK_JOB_CLEAN, this, 60)) {
                    LOGGER.trace("LOCK OBTAINED FOR TENANT {} IN SCHEDULE JOB CLEANING", tenant);
                    try {
                        jobService.cleanDeadJobs();
                    } finally {
                        LOGGER.trace("RELEASING OBTAINED LOCK FOR TENANT {} IN SCHEDULE JOB CLEANING", tenant);
                        lockService.releaseLock(LOCK_JOB_CLEAN, this);
                        LOGGER.trace("LOCK RELEASED FOR TENANT {} IN SCHEDULE JOB CLEANING", tenant);
                    }
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

}
