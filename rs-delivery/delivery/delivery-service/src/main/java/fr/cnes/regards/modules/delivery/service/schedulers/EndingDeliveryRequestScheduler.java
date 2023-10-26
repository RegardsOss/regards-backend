/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.schedulers;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.service.order.manager.EndingDeliveryService;
import fr.cnes.regards.modules.delivery.service.order.manager.EndingDeliveryTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Handle tasks to perform when a {@link DeliveryRequest} is in a final state.
 *
 * @author Iliana Ghazali
 **/
@Component
@Profile({ "!noscheduler" })
@EnableScheduling
public class EndingDeliveryRequestScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndingDeliveryRequestScheduler.class);

    private static final String DEFAULT_INITIAL_DELAY_IN_SEC = "10";

    private static final String DEFAULT_SCHEDULING_DELAY_IN_SEC = "5";

    public static final String LOCK_NAME = "ending-delivery-requests";

    // SERVICES

    private final ITenantResolver tenantResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final LockService lockService;

    private final EndingDeliveryService endingDeliveryService;

    private final int finishedRequestsPageSize;

    public EndingDeliveryRequestScheduler(ITenantResolver tenantResolver,
                                          IRuntimeTenantResolver runtimeTenantResolver,
                                          LockService lockService,
                                          EndingDeliveryService endingDeliveryService,
                                          @Value("${regards.delivery.request.finished.bulk.size:100}")
                                          int finishedRequestsPageSize) {
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.lockService = lockService;
        this.endingDeliveryService = endingDeliveryService;
        this.finishedRequestsPageSize = finishedRequestsPageSize;
    }

    @Scheduled(initialDelayString = "${regards.delivery.schedule.ending.requests.initial.delay:"
                                    + DEFAULT_INITIAL_DELAY_IN_SEC
                                    + "}",
               fixedDelayString = "${regards.delivery.schedule.ending.requests.delay:"
                                  + DEFAULT_SCHEDULING_DELAY_IN_SEC
                                  + "}",
               timeUnit = TimeUnit.SECONDS)
    public void scheduleDeliveryRequestsEndingTask() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, LOCK_NAME);
                lockService.runWithLock(LOCK_NAME,
                                        new EndingDeliveryTask(endingDeliveryService, finishedRequestsPageSize));
            } catch (InterruptedException e) {
                handleSchedulingError(LOCK_NAME, LOCK_NAME, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}