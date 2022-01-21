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
package fr.cnes.regards.modules.configuration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * Module-common handler for AMQP events.
 *
 * @author Xavier-Alexandre Brochard
 */
@Component
public class UiConfigurationServiceEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(UiConfigurationServiceEventHandler.class);

    @Autowired
    private IUILayoutService layoutService;

    @Autowired
    private IThemeService themeService;

    @Autowired
    private IModuleService moduleService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOG.info("UiConfigurationServiceEventHandler subscribing to new TenantConnectionReady events.");
    }

    @EventListener
    public void processEvent(TenantConnectionReady event) {
        try {
            LOG.info("New tenant ready, initializing default layout, themes and modules for tenant {}.",
                     event.getTenant());
            String tenant = event.getTenant();
            runtimeTenantResolver.forceTenant(tenant);
            AbstractUiConfigurationService layoutServiceAsAbstract = (AbstractUiConfigurationService) layoutService;
            AbstractUiConfigurationService themeServiceAsAbstract = (AbstractUiConfigurationService) themeService;
            AbstractUiConfigurationService moduleServiceAsAbstract = (AbstractUiConfigurationService) moduleService;
            layoutServiceAsAbstract.initProjectUI(tenant);
            themeServiceAsAbstract.initProjectUI(tenant);
            moduleServiceAsAbstract.initProjectUI(tenant);
            LOG.info("New tenant ready, default layout, themes and modules initialized successfully");
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }
}
