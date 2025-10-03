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
package fr.cnes.regards.modules.crawler.service.metric;

import fr.cnes.regards.framework.metric.service.AbstractMetricService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

/**
 * Centralizes metrics related to main indexing operations.
 *
 * @author tguillou
 */
@Service
public class IndexerMetricService extends AbstractMetricService {

    // ----- Metric common tags -----

    private static final String CRAWLER_NAME_TAG = "crawlerName";

    private static final String TENANT_TAG = "tenant";

    // ----- Metric names -----

    public static final String INDEXER_FIND_ALL = "indexer_find_all";

    public static final String INDEXER_UPSERT = "indexer_upsert";

    public static final String INDEXER_REMOVE_DATA_OBJECTS_ASSOC = "indexer_remove_data_objects_assoc";

    /**
     * Manage access rights and also dataset association to data objects
     */
    public static final String INDEXER_MANAGE_ACCESS_RIGHTS = "indexer_manage_access_rights";

    public static final String INDEXER_MANAGE_ACCESS_RIGHT_WITH_FILTER = "indexer_manage_access_rights_with_filter";

    public static final String INDEXER_COMPUTED_ATTRIBUTES = "indexer_computed_attributes";

    /**
     * Value for crawlerName tag when the operation is not linked to a crawling. Can happen when
     * updating dataset, its access rights, or deleting data objects.
     */
    public static final String NOT_LINKED_TO_CRAWLING = "not_linked_to_crawling";

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public IndexerMetricService(MeterRegistry meterRegistry, IRuntimeTenantResolver runtimeTenantResolver) {
        super(meterRegistry);
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    /**
     * Start a timer sample
     */
    public Timer.Sample startNewTimer() {
        return Timer.start(super.registry);
    }

    /**
     * Stop a timer sample, and record the duration in a timer with given name and tags
     */
    public void stopTimer(Timer.Sample sample, String timerName, String crawlerName) {
        if (crawlerName == null) {
            crawlerName = NOT_LINKED_TO_CRAWLING;
        }
        sample.stop(Timer.builder(timerName)
                         .tags(CRAWLER_NAME_TAG, crawlerName, TENANT_TAG, runtimeTenantResolver.getTenant())
                         .register(super.registry));
    }
}
