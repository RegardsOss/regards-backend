/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

    public OrderScheduler(OrderMaintenanceService orderMaintenanceService,
                          IRuntimeTenantResolver runtimeTenantResolver,
                          ITenantResolver tenantResolver) {
        this.orderMaintenanceService = orderMaintenanceService;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.tenantResolver = tenantResolver;
    }

    /**
     * Scheduled method to update all current running orders completions values and all order available files count
     * values into database
     */
    @Scheduled(fixedDelayString = "${regards.order.computation.update.rate.ms:1000}")
    public void updateCurrentOrdersComputations() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            try {
                orderMaintenanceService.updateTenantOrdersComputations();
            } catch (Exception e) {
                // FIXME - The Spring type of exception is not stable yet
                // So the catch can be more specific once Spring will be updated 5.3.0
                // @see https://github.com/spring-projects/spring-framework/issues/24064
                LOGGER.warn("Failed to update orders as the database returned us a serialisation anomaly", e);
            }
        }
    }

}
