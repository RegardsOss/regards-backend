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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test that JobMetricServiceTest increments the meterRegistry as expected
 *
 * @author mnguyen0
 */
public class JobMetricServiceTest {

    @Test
    public void test_increment_job_creation_counter() {
        //Given
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        JobMetricService jobMetricService = new JobMetricService(meterRegistry);

        // When
        jobMetricService.incrementJobCreation("type1", "tenant1", "service1");

        // Then
        Counter counter = meterRegistry.get("regards_job_creation_count")
                                       .tags("type", "type1", "tenant", "tenant1", "service", "service1")
                                       .counter();
        Assert.assertNotNull(counter);
        Assert.assertEquals(1.0, counter.count(), 0.0);
    }

    @Test
    public void test_increment_running_job_counter() {
        //Given
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        JobMetricService jobMetricService = new JobMetricService(meterRegistry);

        // When
        jobMetricService.incrementRunningJob("type1", "tenant1", "service1");

        // Then
        Counter counter = meterRegistry.get("regards_running_job_count")
                                       .tags("type", "type1", "tenant", "tenant1", "service", "service1")
                                       .counter();
        Assert.assertNotNull(counter);
        Assert.assertEquals(1.0, counter.count(), 0.0);

        // Ensure the jobs_done_count counter exists, incremented by 0
        counter = meterRegistry.get("regards_job_done_count")
                               .tags("type", "type1", "tenant", "tenant1", "service", "service1", "status", "SUCCEEDED")
                               .counter();
        Assert.assertNotNull(counter);
        Assert.assertEquals(0.0, counter.count(), 0.0);
    }

    @Test
    public void test_increment_job_done_counter() {
        //Given
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        JobMetricService jobMetricService = new JobMetricService(meterRegistry);

        // When
        jobMetricService.incrementJobDone("type1", "tenant1", "service1", "SUCCEEDED");

        // Then
        Counter counter = meterRegistry.get("regards_job_done_count")
                                       .tags("type",
                                             "type1",
                                             "tenant",
                                             "tenant1",
                                             "service",
                                             "service1",
                                             "status",
                                             "SUCCEEDED")
                                       .counter();
        Assert.assertNotNull(counter);
        Assert.assertEquals(1.0, counter.count(), 0.0);
    }

}
