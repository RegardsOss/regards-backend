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
package fr.cnes.regards.modules.workermanager.task;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.requests.scan.RequestScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scan requests having {@link RequestStatus#NO_WORKER_AVAILABLE} that should be restarted
 * and schedule a job
 *
 * @author Léo Mieulet
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class NoWorkerAvailableScanRequestTaskScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoWorkerAvailableScanRequestTaskScheduler.class);

    private static final String NOTIFICATION_TITLE = "Scan NO_WORKER_AVAILABLE requests scheduler";

    private static final String SCAN_NO_WORKER_AVAILABLE_REQUESTS = "SCAN NO_WORKER_AVAILABLE REQUESTS";

    private static final String DEFAULT_INITIAL_DELAY = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY = "3000";

    @Autowired
    private RequestScanService requestScanService;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Scheduled(initialDelayString = "${regards.feature.request.scheduling.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
               fixedDelayString = "${regards.feature.request.reference.scheduling.delay:"
                                  + DEFAULT_SCHEDULING_DELAY
                                  + "}")
    public void scheduleScanNoWorkerAvailableRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, SCAN_NO_WORKER_AVAILABLE_REQUESTS);
                requestScanService.scanNoWorkerAvailableRequests();
            } catch (Throwable e) {
                handleSchedulingError(SCAN_NO_WORKER_AVAILABLE_REQUESTS, NOTIFICATION_TITLE, e);
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
