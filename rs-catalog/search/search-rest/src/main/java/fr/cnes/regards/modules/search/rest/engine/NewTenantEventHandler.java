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
package fr.cnes.regards.modules.search.rest.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.search.rest.engine.plugin.legacy.LegacySearchEngine;
import fr.cnes.regards.modules.search.service.ISearchEngineConfigurationService;

/**
 * Handler to manage new tenant for microservice initializations.
 * @author Sébastien Binda
 */
@Component
public class NewTenantEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(NewTenantEventHandler.class);

    @Autowired
    private ISearchEngineConfigurationService engineService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        LOG.info("search-service module subscribing to new TenantConnectionReady events.");
    }

    @EventListener
    public void processEvent(TenantConnectionReady event) {
        try {
            LOG.info("New tenant ready, initializing search-module for tenant {}.", event.getTenant());
            String tenant = event.getTenant();
            runtimeTenantResolver.forceTenant(tenant);
            engineService.initDefaultSearchEngine(LegacySearchEngine.class);
            LOG.info("New tenant ready, search-module initialized");
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }
}
