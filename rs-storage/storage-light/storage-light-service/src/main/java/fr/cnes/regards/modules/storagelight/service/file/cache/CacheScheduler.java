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
package fr.cnes.regards.modules.storagelight.service.file.cache;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storagelight.service.JobsPriority;
import fr.cnes.regards.modules.storagelight.service.file.cache.job.CacheCleanJob;
import fr.cnes.regards.modules.storagelight.service.file.cache.job.CacheVerificationJob;

/**
 * Enable cache clean action scheduler.
 *
 * @author Sébastien Binda
 *
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class CacheScheduler {

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IJobInfoService jobService;

    @Autowired
    private IAuthenticationResolver authResolver;

    /**
     * Periodically check the cache total size and delete expired files or/and older files if needed.
     * Default : scheduled to be run every hour.
     */
    @Scheduled(fixedRateString = "${regards.cache.cleanup.rate.ms:3600000}", initialDelay = 3_600_000)
    public void cleanCache() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            Set<JobParameter> parameters = Sets.newHashSet();
            if (jobService.retrieveJobsCount(CacheCleanJob.class.getName(), JobStatus.PENDING, JobStatus.RUNNING,
                                             JobStatus.QUEUED, JobStatus.TO_BE_RUN) == 0) {
                jobService.createAsQueued(new JobInfo(false, JobsPriority.CACHE_PURGE.getPriority(), parameters,
                        authResolver.getUser(), CacheCleanJob.class.getName()));
            }
            runtimeTenantResolver.clearTenant();
        }
    }

    /**
     * Default cron value : Every day at 5am.
     */
    @Scheduled(cron = "${regards.cache.verification.cron:0 0 5 * * *}")
    public void checkCacheCoherence() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            Set<JobParameter> parameters = Sets.newHashSet();
            if (jobService.retrieveJobsCount(CacheVerificationJob.class.getName(), JobStatus.PENDING, JobStatus.RUNNING,
                                             JobStatus.QUEUED, JobStatus.TO_BE_RUN) == 0) {
                jobService.createAsQueued(new JobInfo(false, JobsPriority.CACHE_VERIFICATION.getPriority(), parameters,
                        authResolver.getUser(), CacheVerificationJob.class.getName()));
            }
            runtimeTenantResolver.clearTenant();
        }
    }

}
