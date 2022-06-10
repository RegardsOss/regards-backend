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
package fr.cnes.regards.framework.integration.test.job;

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.modules.jobs.service.JobInitializer;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Léo Mieulet
 */
@Component
public class JobTestCleaner {

    protected static final Logger LOGGER = LoggerFactory.getLogger(JobTestCleaner.class);

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IJobService jobService;

    @Autowired
    private JobInitializer jobInitializer;

    /**
     * This methods will suspend all running jobs in the ThreadPool
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanJob() {
        abortQueuedJobs();
        // kill the async task that pulls jobs and run them
        jobInitializer.killJobManager();
        // Clean and restart the job scheduler
        jobService.cleanAndRestart();
    }

    /**
     * Stop QUEUED job before they are launched
     */
    private void abortQueuedJobs() {
        List<JobInfo> jobToStops = jobInfoRepository.findAllByStatusStatus(JobStatus.QUEUED);
        for (JobInfo jobToStop : jobToStops) {
            LOGGER.info("Aborting one job of type {} before launching it", jobToStop.getClassName());
            jobToStop.getStatus().setStatus(JobStatus.ABORTED);
            jobInfoService.save(jobToStop);
        }
    }
}
