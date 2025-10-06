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

package fr.cnes.regards.framework.modules.jobs.metric;

import fr.cnes.regards.framework.metric.service.AbstractMetricService;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Service;

/**
 * Initializes and increments the prometheus counter metrics concerning jobs in order to show in grafana
 *
 * @author mnguyen0
 */
@Service
public class JobMetricService extends AbstractMetricService {

    private static final String JOBS_DONE_COUNTER_NAME = "regards_job_done_count";

    private static final String JOBS_RUNNING_COUNTER_NAME = "regards_running_job_count";

    private static final String JOBS_CREATION_COUNTER_NAME = "regards_job_creation_count";

    public static final String TYPE = "type";

    public static final String TENANT = "tenant";

    public static final String SERVICE = "service";

    public JobMetricService(MeterRegistry registry) {
        super(registry);
    }

    /**
     * Increment the counter of created jobs
     */
    public void incrementJobCreation(String type, String tenant, String service) {
        incrementCounter(JOBS_CREATION_COUNTER_NAME, Tags.of(TYPE, type, TENANT, tenant, SERVICE, service), 1);
    }

    /**
     * Increment the counter of running jobs
     */
    public void incrementRunningJob(String type, String tenant, String service) {
        incrementCounter(JOBS_RUNNING_COUNTER_NAME, Tags.of(TYPE, type, TENANT, tenant, SERVICE, service), 1);
        // The metric regards_job_done_count must be initialized in order to have results in the grafana dashboard
        // when checking the job currently running
        // So, if this counter does not exist, this initializes the counter with a 0 increment
        Counter.builder(JOBS_DONE_COUNTER_NAME)
               .tags(Tags.of(TYPE, type, TENANT, tenant, SERVICE, service, "status", JobStatus.SUCCEEDED.toString()))
               .register(registry);
    }

    /**
     * Increment the counter of done jobs
     */
    public void incrementJobDone(String type, String tenant, String service, String status) {
        incrementCounter(JOBS_DONE_COUNTER_NAME,
                         Tags.of(TYPE, type, TENANT, tenant, SERVICE, service, "status", status),
                         1);
    }
}
