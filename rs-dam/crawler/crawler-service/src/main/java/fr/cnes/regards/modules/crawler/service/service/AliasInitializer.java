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

import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.indexer.dao.EsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * listens for the {@link ApplicationReadyEvent} and ensures at application startup that all indices from tenants have a
 * corresponding Elasticsearch alias.
 *
 * @author mnguyen
 */
@Component
public class AliasInitializer implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private EsRepository esRepos;

    @Autowired
    private IndexService indexService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationListener.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOGGER.info("Index service started, check if aliases exist");
        for (String tenant : tenantResolver.getAllTenants()) {
            if (esRepos.indexExists(tenant) && !esRepos.aliasExists(tenant)) {
                LOGGER.info("Creating alias for tenant [{}]", tenant);
                indexService.createOrUpdateAlias(tenant);
            }
        }
    }

}
