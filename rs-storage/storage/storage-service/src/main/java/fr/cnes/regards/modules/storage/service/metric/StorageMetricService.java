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

import fr.cnes.regards.framework.metric.service.AbstractMetricService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Initializes and increments the prometheus counter metrics concerning storage in order to show in grafana
 *
 * @author mnguyen0
 */
@Service
public class StorageMetricService extends AbstractMetricService {

    private static final String STORAGE_REQUEST_COUNTER_NAME = "storage_request_counter";

    private static final String STORAGE_REQUEST_SUCCESS_COUNTER_NAME = "storage_request_success_counter";

    private static final String STORAGE_REQUEST_ERROR_COUNTER_NAME = "storage_request_error_counter";

    private static final String STORAGE_RESTORE_REQUEST_COUNTER_NAME = "storage_restore_request_counter";

    private static final String STORAGE_RESTORE_SUCCESS_COUNTER_NAME = "storage_restore_request_success_counter";

    private static final String STORAGE_RESTORE_ERROR_COUNTER_NAME = "storage_restore_request_error_counter";

    public StorageMetricService(MeterRegistry registry) {
        super(registry);
    }

    /**
     * Increment the counter of received storage requests
     */
    public void incrementStorageRequests(String storageName, String tenant) {
        incrementCounter(STORAGE_REQUEST_COUNTER_NAME, Map.of("name", storageName, "tenant", tenant), 1);
        // Initialize success and error counters to 0 for Grafana visualization
        //If the counters already exists, meterRegistry does not reinitialize it
        initializeCounterIfAbsent(STORAGE_REQUEST_SUCCESS_COUNTER_NAME, Map.of("name", storageName, "tenant", tenant));
        initializeCounterIfAbsent(STORAGE_REQUEST_ERROR_COUNTER_NAME, Map.of("name", storageName, "tenant", tenant));
    }

    /**
     * Increment the counter of finished storage requests in success
     */
    public void incrementStorageRequestSuccess(String storageName, String tenant) {
        incrementCounter(STORAGE_REQUEST_SUCCESS_COUNTER_NAME, Map.of("name", storageName, "tenant", tenant), 1);
    }

    /**
     * Increment the counter of finished storage requests in error
     */
    public void incrementStorageRequestError(String storageName, String tenant) {
        incrementCounter(STORAGE_REQUEST_ERROR_COUNTER_NAME, Map.of("name", storageName, "tenant", tenant), 1);
    }

    /**
     * Increment the counter of received storage restoration requests
     */
    public void incrementRestorationRequests(String storageName, String tenant) {
        incrementCounter(STORAGE_RESTORE_REQUEST_COUNTER_NAME, Map.of("name", storageName, "tenant", tenant), 1);
        // Initialize success and error counters to 0 for Grafana visualization
        //If the counters already exists, meterRegistry does not reinitialize it
        initializeCounterIfAbsent(STORAGE_RESTORE_SUCCESS_COUNTER_NAME, Map.of("name", storageName, "tenant", tenant));
        initializeCounterIfAbsent(STORAGE_RESTORE_ERROR_COUNTER_NAME, Map.of("name", storageName, "tenant", tenant));
    }

    /**
     * Increment the counter of finished storage restoration requests in success
     */
    public void incrementRestorationRequestSuccess(String storageName, String tenant) {
        incrementCounter(STORAGE_RESTORE_SUCCESS_COUNTER_NAME, Map.of("name", storageName, "tenant", tenant), 1);
    }

    /**
     * Increment the counter of finished storage restoration requests in error
     */
    public void incrementRestorationRequestError(String storageName, String tenant) {
        incrementCounter(STORAGE_RESTORE_ERROR_COUNTER_NAME, Map.of("name", storageName, "tenant", tenant), 1);
    }

}
