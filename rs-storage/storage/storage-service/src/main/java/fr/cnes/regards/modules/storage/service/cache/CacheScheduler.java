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
package fr.cnes.regards.modules.storage.service.cache;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storage.service.StorageJobsPriority;
import fr.cnes.regards.modules.storage.service.cache.job.CacheVerificationJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Enable cache clean action scheduler.
 *
 * @author Sébastien Binda
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class CacheScheduler {

    private static final String DEFAULT_INITIAL_DELAY = "60000";

    private static final String DEFAULT_DELAY = "7200000";

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IJobInfoService jobService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private CacheService cacheService;

    /**
     * Periodically check the cache total size and delete expired files or/and older files if needed.
     * Default : scheduled to be run every hour.
     */
    @Scheduled(initialDelayString = "${regards.cache.cleanup.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
               fixedDelayString = "${regards.cache.cleanup.delay:" + DEFAULT_DELAY + "}")
    public void cleanCache() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            cacheService.scheduleCacheCleanUp(CacheScheduler.class.getName(), false);
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
            if (jobService.retrieveJobsCount(CacheVerificationJob.class.getName(),
                                             JobStatus.PENDING,
                                             JobStatus.RUNNING,
                                             JobStatus.QUEUED,
                                             JobStatus.TO_BE_RUN) == 0) {
                jobService.createAsQueued(new JobInfo(false,
                                                      StorageJobsPriority.CACHE_VERIFICATION,
                                                      parameters,
                                                      CacheScheduler.class.getName(),
                                                      CacheVerificationJob.class.getName()));
            }
            runtimeTenantResolver.clearTenant();
        }
    }

}
