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

package fr.cnes.regards.modules.storage.service.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test that the storage metric service increments the meter registry as expected
 *
 * @author mnguyen0
 */

public class StorageMetricServiceTest {

    private MeterRegistry meterRegistry;

    private StorageMetricService storageMetricService;

    private String NAME = "storage1";

    private String TENANT = "tenant1";

    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();  // Utilisation de SimpleMeterRegistry
        storageMetricService = new StorageMetricService(meterRegistry);  // Service avec SimpleMeterRegistry
    }

    @Test
    public void increment_storage_request_counter_test() {

        // When
        storageMetricService.incrementStorageRequests(NAME, TENANT);

        // Then
        Counter counter = meterRegistry.find("storage_request_counter").tags("name", NAME, "tenant", TENANT).counter();
        Assert.assertNotNull(counter);
        Assert.assertEquals(1, counter.count(), 0.0);

        // Asserts that done requests counters have been initialized
        Assert.assertNotNull(meterRegistry.find("storage_request_success_counter")
                                          .tags("name", NAME, "tenant", TENANT)
                                          .counter());

        Assert.assertNotNull(meterRegistry.find("storage_request_error_counter")
                                          .tags("name", NAME, "tenant", TENANT)
                                          .counter());
    }

    @Test
    public void increment_storage_request_success_counter_test() {
        // When
        storageMetricService.incrementStorageRequestSuccess(NAME, TENANT);

        // Then
        Counter counter = meterRegistry.find("storage_request_success_counter")
                                       .tags("name", NAME, "tenant", TENANT)
                                       .counter();
        Assert.assertNotNull(counter);
        Assert.assertEquals(1, counter.count(), 0.0);
    }

    @Test
    public void increment_storage_request_error_counter_test() {
        // When
        storageMetricService.incrementStorageRequestError(NAME, TENANT);

        // Then
        Counter counter = meterRegistry.find("storage_request_error_counter")
                                       .tags("name", NAME, "tenant", TENANT)
                                       .counter();
        Assert.assertNotNull(counter);
        Assert.assertEquals(1, counter.count(), 0.0);
    }

    @Test
    public void increment_restoration_request_counter_test() {
        // When
        storageMetricService.incrementRestorationRequests(NAME, TENANT);

        // Then
        Counter counter = meterRegistry.find("storage_restore_request_counter")
                                       .tags("name", NAME, "tenant", TENANT)
                                       .counter();
        Assert.assertNotNull(counter);
        Assert.assertEquals(1, counter.count(), 0.0);

        // Asserts that done requests counters have been initialized
        Assert.assertNotNull(meterRegistry.find("storage_restore_request_success_counter")
                                          .tags("name", NAME, "tenant", TENANT)
                                          .counter());

        Assert.assertNotNull(meterRegistry.find("storage_restore_request_error_counter")
                                          .tags("name", NAME, "tenant", TENANT)
                                          .counter());
    }

    @Test
    public void increment_restoration_request_success_counter_test() {
        // When
        storageMetricService.incrementRestorationRequestSuccess(NAME, TENANT);

        // Then
        Counter counter = meterRegistry.find("storage_restore_request_success_counter")
                                       .tags("name", NAME, "tenant", TENANT)
                                       .counter();
        Assert.assertNotNull(counter);
        Assert.assertEquals(1, counter.count(), 0.0);
    }

    @Test
    public void increment_restoration_request_error_counter_test() {
        // When
        storageMetricService.incrementRestorationRequestError(NAME, TENANT);

        // Then
        Counter counter = meterRegistry.find("storage_restore_request_error_counter")
                                       .tags("name", NAME, "tenant", TENANT)
                                       .counter();
        Assert.assertNotNull(counter);
        Assert.assertEquals(1, counter.count(), 0.0);
    }

}
