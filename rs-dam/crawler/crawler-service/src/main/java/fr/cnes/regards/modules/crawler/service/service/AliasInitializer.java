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

/**
 *
 */
package fr.cnes.regards.modules.crawler.service.service;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.indexer.service.EsRepositoryFacade;
import fr.cnes.regards.modules.indexer.service.IndexAliasResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * listens for the {@link ApplicationReadyEvent} and ensures at application startup that all indices from tenants have a
 * corresponding Elasticsearch alias.
 *
 * @author mnguyen
 */
@Component
public class AliasInitializer implements ApplicationListener<ApplicationStartedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationListener.class);

    private final ITenantResolver tenantResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final EsRepositoryFacade esRepositoryFacade;

    private final IndexService indexService;

    public AliasInitializer(ITenantResolver tenantResolver,
                            IRuntimeTenantResolver runtimeTenantResolver,
                            EsRepositoryFacade esRepositoryFacade,
                            IndexService indexService) {
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.esRepositoryFacade = esRepositoryFacade;
        this.indexService = indexService;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        LOGGER.info("Index service started, check if aliases exist");
        for (String tenant : tenantResolver.getAllTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            LOGGER.info("Tenant is forced to: [{}]", tenant);
            if (esRepositoryFacade.indexExists(tenant)
                && !esRepositoryFacade.aliasExists(IndexAliasResolver.resolveAliasName(tenant))) {
                LOGGER.info("Creating alias for tenant [{}]", tenant);
                indexService.createOrUpdateAlias(tenant);
            }
        }
    }

}
