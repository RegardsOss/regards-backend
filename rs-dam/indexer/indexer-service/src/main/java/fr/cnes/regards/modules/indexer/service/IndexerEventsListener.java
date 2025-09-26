/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.indexer.service;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.IInstanceNotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.indexer.dao.CreateIndexConfiguration;
import fr.cnes.regards.modules.indexer.domain.EsIndexAlias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listening for tenant creation and application ready to manage Elasticsearch index creation
 * <p>
 * The {@link ConditionalOnProperty} annotation prevents this component from being
 * activated in other microservices where it is not relevant.
 *
 * @author oroussel
 */
@Component
@ConditionalOnProperty(prefix = "regards.indexer.listener", name = "enabled", havingValue = "true")
public class IndexerEventsListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexerEventsListener.class);

    @Autowired
    private EsRepositoryFacade esRepositoryFacade;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IndexAliasService indexAliasService;

    @Autowired
    private IInstanceNotificationClient instanceNotificationClient;

    @EventListener
    public void handleApplicationReadyEvent(ApplicationReadyEvent event) {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            this.checkIndex(tenant);
        }
    }

    /**
     * A tenant (or project) has been created and is ready to be used => create ElasticSearch index (if id doesn't
     * already exist)
     */
    @EventListener
    public void handleTenantConnectionReady(TenantConnectionReady event) {
        checkIndex(event.getTenant());
    }

    private void checkIndex(String tenant) {

        LOGGER.info("Listener Checking index for {}", tenant);
        EsIndexAlias esIndexAlias = indexAliasService.getByAlias(IndexAliasResolver.resolveAliasName(tenant));
        if (esIndexAlias == null && !esRepositoryFacade.indexExists(tenant)) {
            // Creating the index with the default configuration if needed.
            // The user cannot use custom shard settings during the initial creation of the index as it happens before
            // the management of microservices is available. A catalog reset is required to use custom settings.
            boolean acknowledged = esRepositoryFacade.createIndex(tenant, CreateIndexConfiguration.DEFAULT);
            if (acknowledged) {
                instanceNotificationClient.notify(String.format(
                    "Elasticsearch index %s successfully created for tenant %s.",
                    tenant,
                    tenant), "Index creation success", NotificationLevel.INFO, DefaultRole.INSTANCE_ADMIN);
            } else {
                instanceNotificationClient.notify(String.format("Elasticsearch index creation for tenant %s has failed.",
                                                                tenant,
                                                                tenant),
                                                  "Index creation failure",
                                                  NotificationLevel.INFO,
                                                  DefaultRole.INSTANCE_ADMIN);
            }
        }
    }
}
