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

package fr.cnes.regards.modules.crawler.service.scheduler;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.crawler.service.service.DatasourceIngestionBuildingIndexService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that detects when the building index is finished, ie when two indexing has been done on the building index
 *
 * @author mnguyen0
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class CompleteBuildingIndexScheduler {

    private final ITenantResolver tenantResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final DatasourceIngestionBuildingIndexService dsiBuildingIndexService;

    public CompleteBuildingIndexScheduler(ITenantResolver tenantResolver,
                                          IRuntimeTenantResolver runtimeTenantResolver,
                                          DatasourceIngestionBuildingIndexService dsiBuildingIndexService) {
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.dsiBuildingIndexService = dsiBuildingIndexService;
    }

    /**
     * For all tenants, manage the building datasource ingestions
     */
    @Scheduled(initialDelayString = "${regards.building.datasources.rate.init.ms:120000}",
               fixedDelayString = "${regards.building.datasources.rate.ms:30000}")
    public void manageBuildingDataSourceIngestions() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                dsiBuildingIndexService.manageBuildingDatasourceIngestions(tenant);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

}
