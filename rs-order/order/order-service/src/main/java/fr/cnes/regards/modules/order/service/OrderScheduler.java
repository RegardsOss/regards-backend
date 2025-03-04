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
package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Thomas GUILLOU
 **/

@EnableScheduling
@Component
@Profile("!noscheduler")
public class OrderScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderScheduler.class);

    private final OrderMaintenanceService orderMaintenanceService;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ITenantResolver tenantResolver;

    private final OrderService orderService;

    private final OrderJobService orderJobService;

    public OrderScheduler(OrderMaintenanceService orderMaintenanceService,
                          IRuntimeTenantResolver runtimeTenantResolver,
                          ITenantResolver tenantResolver,
                          OrderService orderService,
                          OrderJobService orderJobService) {
        this.orderMaintenanceService = orderMaintenanceService;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.tenantResolver = tenantResolver;
        this.orderService = orderService;
        this.orderJobService = orderJobService;
    }

    /**
     * Scheduled method to update all current running orders completions values and all order available files count
     * values into database
     */
    @Scheduled(initialDelayString = "${regards.order.computation.update.initial.delay.ms:60000}",
               fixedDelayString = "${regards.order.computation.update.rate.ms:10000}")
    public void updateCurrentOrdersComputations() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            try {
                orderMaintenanceService.updateTenantOrdersComputations();
            } catch (Exception e) {
                LOGGER.warn("Failed to update orders as the database returned us a serialisation anomaly", e);
            }
            // Check if there is user orders that can be updated.
            // This can happen if a jobEvent have not been successfully handled.
            orderService.getUsersWithRunningOrders().forEach(orderJobService::manageUserOrderStorageFilesJobInfos);
        }
    }
}
