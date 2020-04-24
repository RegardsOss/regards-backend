/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.service.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.modules.locks.service.ILockService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.feature.service.IFeatureCopyService;
import fr.cnes.regards.modules.feature.service.IFeatureCreationService;
import fr.cnes.regards.modules.feature.service.IFeatureDeletionService;
import fr.cnes.regards.modules.feature.service.IFeatureNotificationService;
import fr.cnes.regards.modules.feature.service.IFeatureReferenceService;
import fr.cnes.regards.modules.feature.service.IFeatureUpdateService;

/**
 * Enable feature task scheduling
 *
 * @author Marc SORDI
 *
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class FeatureTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTaskScheduler.class);

    private static final String LOCK_REQUEST_UPDATE = "Update_Request";

    private static final String LOCK_REQUEST_INSERT = "Insert_Request";

    private static final String LOCK_REQUEST_DELETE = "Delete_Request";

    private static final String LOCK_REQUEST_COPY = "Copy_Request";

    private static final String LOCK_REQUEST_REFERENCE = "Reference_Request";

    private static final String LOCK_REQUEST_NOTIFICATION = "Notification_Request";

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ILockService lockService;

    @Autowired
    private IFeatureCreationService featureService;

    @Autowired
    private IFeatureUpdateService featureUpdateService;

    @Autowired
    private IFeatureDeletionService featureDeletionService;

    @Autowired
    private IFeatureReferenceService featureReferenceService;

    @Autowired
    private IFeatureCopyService featureCopyService;

    @Autowired
    private IFeatureNotificationService featureNotificationService;

    @Scheduled(initialDelayString = "${regards.feature.request.scheduling.initial.delay:30000}",
            fixedDelayString = "${regards.feature.request.update.scheduling.delay:1000}")
    public void scheduleUpdateRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                LOGGER.trace("LOCKING FOR TENANT {} IN SCHEDULE UPDATE REQUESTS", tenant);
                if (lockService.obtainLockOrSkip(LOCK_REQUEST_UPDATE, this, 60)) {
                    LOGGER.trace("LOCK OBTAINED FOR TENANT {} IN SCHEDULE UPDATE REQUESTS", tenant);
                    try {
                        long start = System.currentTimeMillis();
                        int nb = this.featureUpdateService.scheduleRequests();
                        if (nb != 0) {
                            LOGGER.info("{} update request(s) scheduled in {} ms", nb,
                                        System.currentTimeMillis() - start);
                        }
                    } finally {
                        LOGGER.trace("RELEASING OBTAINED LOCK FOR TENANT {} IN SCHEDULE UPDATE REQUESTS", tenant);
                        lockService.releaseLock(LOCK_REQUEST_UPDATE, this);
                        LOGGER.trace("LOCK RELEASED FOR TENANT {} IN SCHEDULE UPDATE REQUESTS", tenant);
                    }
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Scheduled(initialDelayString = "${regards.feature.request.scheduling.initial.delay:30000}",
            fixedDelayString = "${regards.feature.request.insert.scheduling.delay:1000}")
    public void scheduleInsertRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                LOGGER.trace("LOCKING FOR TENANT {} IN SCHEDULE INSERT REQUESTS", tenant);
                if (lockService.obtainLockOrSkip(LOCK_REQUEST_INSERT, this, 60)) {
                    LOGGER.trace("LOCK OBTAINED FOR TENANT {} IN SCHEDULE INSERT REQUESTS", tenant);
                    try {
                        long start = System.currentTimeMillis();
                        int nb = this.featureService.scheduleRequests();
                        if (nb != 0) {
                            LOGGER.info("{} creation request(s) scheduled in {} ms", nb,
                                        System.currentTimeMillis() - start);
                        }
                    } finally {
                        LOGGER.trace("RELEASING OBTAINED LOCK FOR TENANT {} IN SCHEDULE INSERT REQUESTS", tenant);
                        lockService.releaseLock(LOCK_REQUEST_INSERT, this);
                        LOGGER.trace("LOCK RELEASED FOR TENANT {} IN SCHEDULE INSERT REQUESTS", tenant);
                    }
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Scheduled(initialDelayString = "${regards.feature.request.scheduling.initial.delay:30000}",
            fixedDelayString = "${regards.feature.request.delete.scheduling.delay:1000}")
    public void scheduleDeleteRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                LOGGER.trace("LOCKING FOR TENANT {} IN SCHEDULE DELETE REQUESTS", tenant);
                if (lockService.obtainLockOrSkip(LOCK_REQUEST_DELETE, this, 60)) {
                    LOGGER.trace("LOCK OBTAINED FOR TENANT {} IN SCHEDULE DELETE REQUESTS", tenant);
                    try {
                        this.featureDeletionService.scheduleRequests();
                    } finally {
                        LOGGER.trace("RELEASING OBTAINED LOCK FOR TENANT {} IN SCHEDULE DELETE REQUESTS", tenant);
                        lockService.releaseLock(LOCK_REQUEST_DELETE, this);
                        LOGGER.trace("LOCK RELEASED FOR TENANT {} IN SCHEDULE DELETE REQUESTS", tenant);
                    }
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Scheduled(initialDelayString = "${regards.feature.request.scheduling.initial.delay:30000}",
            fixedDelayString = "${regards.feature.request.reference.scheduling.delay:1000}")
    public void scheduleReferenceRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                LOGGER.trace("LOCKING FOR TENANT {} IN SCHEDULE REFERENCE REQUESTS", tenant);
                if (lockService.obtainLockOrSkip(LOCK_REQUEST_REFERENCE, this, 60)) {
                    LOGGER.trace("LOCK OBTAINED FOR TENANT {} IN SCHEDULE REFERENCE REQUESTS", tenant);
                    try {
                        long start = System.currentTimeMillis();
                        int nb = this.featureReferenceService.scheduleRequests();
                        if (nb != 0) {
                            LOGGER.info("{} reference request(s) scheduled in {} ms", nb,
                                        System.currentTimeMillis() - start);
                        }
                    } finally {
                        LOGGER.trace("RELEASING OBTAINED LOCK FOR TENANT {} IN SCHEDULE REFERENCE REQUESTS", tenant);
                        lockService.releaseLock(LOCK_REQUEST_REFERENCE, this);
                        LOGGER.trace("LOCK RELEASED FOR TENANT {} IN SCHEDULE REFERENCE REQUESTS", tenant);
                    }
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Scheduled(initialDelayString = "${regards.feature.request.scheduling.initial.delay:30000}",
            fixedDelayString = "${regards.feature.request.copy.scheduling.delay:1000}")
    public void scheduleCopyRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                LOGGER.trace("LOCKING FOR TENANT {} IN SCHEDULE COPY REQUESTS", tenant);
                if (lockService.obtainLockOrSkip(LOCK_REQUEST_COPY, this, 60)) {
                    LOGGER.trace("LOCK OBTAINED FOR TENANT {} IN SCHEDULE COPY REQUESTS", tenant);
                    try {
                        long start = System.currentTimeMillis();
                        int nb = this.featureCopyService.scheduleRequests();
                        if (nb != 0) {
                            LOGGER.info("{} copy request(s) scheduled in {} ms", nb,
                                        System.currentTimeMillis() - start);
                        }
                    } finally {
                        LOGGER.trace("RELEASING OBTAINED LOCK FOR TENANT {} IN SCHEDULE COPY REQUESTS", tenant);
                        lockService.releaseLock(LOCK_REQUEST_COPY, this);
                        LOGGER.trace("LOCK RELEASED FOR TENANT {} IN SCHEDULE COPY REQUESTS", tenant);
                    }
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Scheduled(initialDelayString = "${regards.feature.request.scheduling.initial.delay:30000}",
            fixedDelayString = "${regards.feature.request.notification.scheduling.delay:1000}")
    public void scheduleNotificationRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                LOGGER.trace("LOCKING FOR TENANT {} IN SCHEDULE NOTIFICATION REQUESTS", tenant);
                if (lockService.obtainLockOrSkip(LOCK_REQUEST_NOTIFICATION, this, 60)) {
                    LOGGER.trace("LOCK OBTAINED FOR TENANT {} IN SCHEDULE NOTIFICATION REQUESTS", tenant);
                    try {
                        long start = System.currentTimeMillis();
                        int nb = this.featureNotificationService.scheduleRequests();
                        if (nb != 0) {
                            LOGGER.info("{} copy request(s) scheduled in {} ms", nb,
                                        System.currentTimeMillis() - start);
                        }
                    } finally {
                        LOGGER.trace("RELEASING OBTAINED LOCK FOR TENANT {} IN SCHEDULE NOTIFICATION REQUESTS", tenant);
                        lockService.releaseLock(LOCK_REQUEST_NOTIFICATION, this);
                        LOGGER.trace("LOCK RELEASED FOR TENANT {} IN SCHEDULE NOTIFICATION REQUESTS", tenant);
                    }
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }
}
