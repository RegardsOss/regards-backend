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
package fr.cnes.regards.modules.ingest.service.chain;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Module-common handler for AMQP events.
 *
 * @author Sébastien Binda
 */
@Component
public class IngestProcessingChainEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestProcessingChainEventHandler.class);

    @Autowired
    private IIngestProcessingChainService ingestProcessingService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Tenant resolver to access all configured tenant
     */
    @Autowired
    private ITenantResolver tenantResolver;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        LOGGER.info("IngestProcessingChainEventHandler subscribing to new TenantConnectionReady events.");

        // Multitenant version of the microservice.
        for (final String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            try {
                LOGGER.debug("Trying to initialize {} for tenant {}",
                             IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                             tenant);
                ingestProcessingService.initDefaultServiceConfiguration();
            } catch (ModuleException e) {
                LOGGER.error("Error initializing ingest configuration for tenant {}. Error : {}",
                             tenant,
                             e.getMessage(),
                             e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @EventListener
    public void processEvent(TenantConnectionReady event) {
        try {
            LOGGER.info("New tenant \"{}\" ready, initializing ingest processing configuration.", event.getTenant());
            // Set working tenant
            runtimeTenantResolver.forceTenant(event.getTenant());
            ingestProcessingService.initDefaultServiceConfiguration();
            LOGGER.info("New tenant \"{}\" ready, ingest processing configuration initialized successfully",
                        event.getTenant());
        } catch (ModuleException e) {
            LOGGER.error("Error during default ingest chain initialization for tenant {}. error : {}",
                         event.getTenant(),
                         e.getMessage(),
                         e);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }
}
