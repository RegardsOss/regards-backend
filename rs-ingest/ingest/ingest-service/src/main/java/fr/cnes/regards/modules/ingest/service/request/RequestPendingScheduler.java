/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.request;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.locks.service.ILockService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.locks.service.ILockService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;

/**
 * Scheduler to periodically check if there is some pending request that can be scheduled
 *
 * NOTE : Number of parallel schedule execution is defined by spring configuration property regards.scheduler.pool.size.
 *
 * @author Léo Mieulet
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class RequestPendingScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestPendingScheduler.class);

    private static final String FILE_SCHEDULER_LOCK = "request-pending-scheduler-lock";

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IRequestService requestService;

    @Autowired
    private ILockService lockService;

    private final Semaphore semaphore = new Semaphore(1, true);

    @Scheduled(fixedDelayString = "${regards.ingest.request.schedule.delay:3000}", initialDelay = 1_000)
    public void handleUpdatesCreatorRequests() throws ModuleException {
        schedule("handleUpdatesCreatorRequests", () -> {
            requestService.unblockRequests(RequestTypeEnum.AIP_UPDATES_CREATOR);
            return null;
        });
    }

    @Scheduled(fixedDelayString = "${regards.ingest.request.schedule.delay:3000}", initialDelay = 1_100)
    public void handleOAISDeletionRequests() throws ModuleException {
        schedule("handleOAISDeletionRequests", () -> {
            requestService.unblockRequests(RequestTypeEnum.OAIS_DELETION);
            return null;
        });
    }

    @Scheduled(fixedDelayString = "${regards.ingest.request.schedule.delay:3000}", initialDelay = 1_200)
    public void handleOAISDeletionCreator() throws ModuleException {
        schedule("handleOAISDeletionCreator", () -> {
            requestService.unblockRequests(RequestTypeEnum.OAIS_DELETION_CREATOR);
            return null;
        });
    }

    @Scheduled(fixedDelayString = "${regards.ingest.request.schedule.delay:3000}", initialDelay = 1_300)
    public void handleStorageMetaDataRequests() throws ModuleException {
        schedule("handleStorageMetaDataRequests", () -> {
            requestService.unblockRequests(RequestTypeEnum.STORE_METADATA);
            return null;
        });
    }

    @Scheduled(fixedDelayString = "${regards.ingest.request.schedule.delay:1000}", initialDelay = 1_400)
    public void handleUpdateRequests() throws ModuleException {
        schedule("handleUpdateRequests", () -> {
            requestService.unblockRequests(RequestTypeEnum.UPDATE);
            return null;
        });
    }

    private void schedule(String taskName, Callable<Void> func) {
        try {
            semaphore.acquire();
            for (String tenant : tenantResolver.getAllActiveTenants()) {
                runtimeTenantResolver.forceTenant(tenant);
                if (obtainLock()) {
                    try {
                        func.call();
                    } finally {
                        releaseLock();
                        runtimeTenantResolver.clearTenant();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error runing scheduling task {}. Cause : {}", taskName, e.getMessage());
        } finally {
            semaphore.release();
        }
    }

    /**
     * Get lock to ensure schedulers are not started at the same time by many instance of this microservice
     * @return
     */
    private boolean obtainLock() {
        return lockService.obtainLockOrSkip(FILE_SCHEDULER_LOCK, this, 60L);
    }

    /**
     * Release lock
     */
    private void releaseLock() {
        lockService.releaseLock(FILE_SCHEDULER_LOCK, this);
    }
}
