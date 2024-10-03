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
package fr.cnes.regards.modules.search.service.engine;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.search.service.ISearchEngineConfigurationService;
import fr.cnes.regards.modules.search.service.engine.plugin.legacy.LegacySearchEngine;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Handler to manage new tenant for microservice initializations.
 *
 * @author Sébastien Binda
 */
@Component
public class NewTenantEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    public static final Logger LOGGER = LoggerFactory.getLogger(NewTenantEventHandler.class);

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(NewTenantEventHandler.class);

    /**
     * Name of the lock used in this service
     */
    public static final String SEARCH_ENGINE_LOCK_NAME = "initDefaultSearchEngine";

    @Autowired
    private ISearchEngineConfigurationService engineService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

    @Autowired
    private ITenantResolver resolver;

    private final Task initLegacySearchEngine = () -> {
        lockingTaskExecutors.assertLocked();
        engineService.initDefaultSearchEngine(LegacySearchEngine.class);
    };

    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        LOG.info(
            "search-service module subscribing to new TenantConnectionReady events and initializing already existing ones.");
        // If does not exists, initialize the default search engine
        for (String tenant : resolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                try {
                    lockingTaskExecutors.executeWithLock(initLegacySearchEngine,
                                                         new LockConfiguration(Instant.now(),
                                                                               SEARCH_ENGINE_LOCK_NAME,
                                                                               Duration.ofSeconds(30),
                                                                               Duration.ZERO));
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @EventListener
    public void processEvent(TenantConnectionReady event) {
        try {
            LOG.info("New tenant ready, initializing search-module for tenant {}.", event.getTenant());
            String tenant = event.getTenant();
            runtimeTenantResolver.forceTenant(tenant);
            try {
                lockingTaskExecutors.executeWithLock(initLegacySearchEngine,
                                                     new LockConfiguration(Instant.now(),
                                                                           SEARCH_ENGINE_LOCK_NAME,
                                                                           Duration.ofSeconds(30),
                                                                           Duration.ZERO));
            } catch (Throwable e) {
                LOGGER.error(e.getMessage(), e);
            }
            LOG.info("New tenant ready, search-module initialized");
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }
}
