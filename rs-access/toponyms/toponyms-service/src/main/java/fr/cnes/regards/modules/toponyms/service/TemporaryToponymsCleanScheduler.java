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

package fr.cnes.regards.modules.toponyms.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler to handle created {@link }
 *
 * @author Iliana Ghazali
 */

@Component
@Profile("!noschedule")
@EnableScheduling
public class TemporaryToponymsCleanScheduler {

    public static final Logger LOGGER = LoggerFactory.getLogger(TemporaryToponymsCleanScheduler.class);

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IJobInfoService jobService;

    @Autowired
    private IAuthenticationResolver authResolver;


    private final static String DEFAULT_INITIAL_DELAY = "10000";
    private final static String DEFAULT_SCHEDULING_DELAY = "86400000";
    private static final String CLEAR_TEMPORARY_TOPONYMS_LOCK = "scheduledClearTemporaryToponyms";
    private static final String CLEAR_TOPONYMS_TITLE = "CLEAR TEMPORARY TOPONYMS";
    private static final String CLEAR_TOPONYMS = "CLEAR TEMPORARY TOPONYMS";


    /**
     * Periodically check the cache total size and delete expired files or/and older files if needed.
     * Default : scheduled to be run every hour.
     */
    @Scheduled(initialDelayString = "${regards.toponyms.temporary.cleanup.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
            fixedDelayString = "${regards.toponyms.temporary.cleanup.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void cleanTemporaryToponyms() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            Set<JobParameter> parameters = Sets.newHashSet();
            if (jobService.retrieveJobsCount(TemporaryToponymsCleanJob.class.getName(), JobStatus.PENDING, JobStatus.RUNNING,
                    JobStatus.QUEUED, JobStatus.TO_BE_RUN) == 0) {
                jobService.createAsQueued(new JobInfo(false, 0, parameters,
                        authResolver.getUser(), TemporaryToponymsCleanJob.class.getName()));
            }
            runtimeTenantResolver.clearTenant();
        }
    }
}
