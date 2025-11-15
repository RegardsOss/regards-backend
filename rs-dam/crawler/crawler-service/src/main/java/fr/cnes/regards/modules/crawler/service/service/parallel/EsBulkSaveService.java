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
package fr.cnes.regards.modules.crawler.service.service.parallel;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.service.service.DatasourceIngestionService;
import fr.cnes.regards.modules.crawler.service.service.DatasourceIngestionStatusService;
import fr.cnes.regards.modules.crawler.service.service.IngestionParameters;
import fr.cnes.regards.modules.indexer.dao.BulkSaveResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Service to instantiate and help {@link EsBulkParallelSaver}.
 *
 * @author tguillou
 */
@Service
public class EsBulkSaveService {

    private final ExecutorService saveThreadPoolExecutor;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final DatasourceIngestionStatusService datasourceIngestionStatusService;

    @Value("${regards.elasticsearch.thread.waiting.threshold:50}")
    private Integer threadWaitingThreshold;

    @Value("${regards.elasticsearch.thread.throttle.after.threshold:4}")
    private Integer threadThrottleAfterThreshold;

    @Value("${regards.elasticsearch.thread.intermediate.calculation.frequency:10}")
    private Integer intermediateCalculationFrequency;

    public EsBulkSaveService(@Qualifier("esThreadPool") ExecutorService saveThreadPoolExecutor,
                             IRuntimeTenantResolver runtimeTenantResolver,
                             DatasourceIngestionStatusService datasourceIngestionStatusService) {
        this.saveThreadPoolExecutor = saveThreadPoolExecutor;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.datasourceIngestionStatusService = datasourceIngestionStatusService;
    }

    public Future<BulkSaveResult> submitToSaveThreadPool(Callable<BulkSaveResult> runnable) {
        return saveThreadPoolExecutor.submit(runnable);
    }

    public void setTenant(String tenant) {
        runtimeTenantResolver.forceTenant(tenant);
    }

    public EsBulkParallelSaver createBulkParallelSaver(IngestionParameters ingestionParameters,
                                                       DatasourceIngestion dsi,
                                                       DatasourceIngestionService datasourceIngestionService) {
        return new EsBulkParallelSaver(ingestionParameters,
                                       dsi,
                                       this,
                                       datasourceIngestionService,
                                       datasourceIngestionStatusService);
    }

    /**
     * Number of done batches/thread/task after which an intermediate calculation of the crawling progress.
     */
    public Integer getIntermediateCalculationFrequency() {
        return intermediateCalculationFrequency;
    }

    /**
     * Number of waiting threads before waiting for some to finish.
     * If number of waiting threads is above this threshold, the EsBulkParallelSaver will wait for some threads to finish
     * This is to avoid too many threads waiting, which would lead to resource starvation.
     */
    public Integer getThreadWaitingThreshold() {
        return threadWaitingThreshold;
    }

    /**
     * Maximum number of threads/batches remaining after the threshold reached.
     */
    public Integer getThreadThrottleAfterThreshold() {
        return threadThrottleAfterThreshold;
    }
}
