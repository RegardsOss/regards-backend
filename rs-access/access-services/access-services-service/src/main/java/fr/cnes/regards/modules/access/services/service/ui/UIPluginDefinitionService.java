/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.service.ui;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionReady;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginDefinitionRepository;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;

@Service(value = "pluginService")
public class UIPluginDefinitionService
        implements IUIPluginDefinitionService, ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private IUIPluginDefinitionRepository repository;

    @Value("${regards.access.multitenant:true}")
    private boolean isMultitenentMicroservice;

    /**
     * Runtime tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * AMQP Message subscriber
     */
    @Autowired
    private IInstanceSubscriber instanceSubscriber;

    /**
     * Tenant resolver to access all configured tenant
     */
    @Autowired
    private ITenantResolver tenantResolver;

    /**
     * Perform initialization only when the whole application is ready
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        // Initialize subscriber for new tenant connection and initialize database if not already done
        instanceSubscriber.subscribeTo(TenantConnectionReady.class, new TenantConnectionReadyEventHandler());
    }

    private class TenantConnectionReadyEventHandler implements IHandler<TenantConnectionReady> {

        @Override
        public void handle(final TenantWrapper<TenantConnectionReady> pWrapper) {
            runtimeTenantResolver.forceTenant(pWrapper.getContent().getTenant());
            initDefault();
            runtimeTenantResolver.clearTenant();
        }
    }

    /**
     * Init
     */
    @PostConstruct
    public void init() {
        if (isMultitenentMicroservice) {
            // Multitenant version of the microservice.
            for (final String tenant : tenantResolver.getAllActiveTenants()) {
                runtimeTenantResolver.forceTenant(tenant);
                initDefault();
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    protected void initDefault() {
        // Create default plugins if no plugin defined
        final List<UIPluginDefinition> plugins = repository.findAll();
        if (plugins.isEmpty()) {
            // Create string plugin
            UIPluginDefinition plugin = new UIPluginDefinition();
            plugin.setName("string-criteria");
            plugin.setSourcePath("/plugins/criterion/string/plugin.js");
            plugin.setType(UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = new UIPluginDefinition();
            plugin.setName("full-text-criteria");
            plugin.setSourcePath("/plugins/criterion/full-text/plugin.js");
            plugin.setType(UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = new UIPluginDefinition();
            plugin.setName("numerical-criteria");
            plugin.setSourcePath("/plugins/criterion/numerical/plugin.js");
            plugin.setType(UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = new UIPluginDefinition();
            plugin.setName("two-numerical-criteria");
            plugin.setSourcePath("/plugins/criterion/two-numerical/plugin.js");
            plugin.setType(UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = new UIPluginDefinition();
            plugin.setName("temporal-criteria");
            plugin.setSourcePath("/plugins/criterion/temporal/plugin.js");
            plugin.setType(UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = new UIPluginDefinition();
            plugin.setName("two-temporal-criteria");
            plugin.setSourcePath("/plugins/criterion/two-temporal/plugin.js");
            plugin.setType(UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);
        }
    }

    @Override
    public UIPluginDefinition retrievePlugin(final Long pPluginId) throws EntityNotFoundException {
        if (!repository.exists(pPluginId)) {
            throw new EntityNotFoundException(pPluginId, UIPluginDefinition.class);
        }
        return repository.findOne(pPluginId);
    }

    @Override
    public Page<UIPluginDefinition> retrievePlugins(final Pageable pPageable) {
        return repository.findAll(pPageable);
    }

    @Override
    public Page<UIPluginDefinition> retrievePlugins(final UIPluginTypesEnum pType, final Pageable pPageable) {
        return repository.findByType(pType, pPageable);
    }

    @Override
    public UIPluginDefinition savePlugin(final UIPluginDefinition pPlugin) throws EntityInvalidException {
        return repository.save(pPlugin);
    }

    @Override
    public UIPluginDefinition updatePlugin(final UIPluginDefinition pPlugin)
            throws EntityNotFoundException, EntityInvalidException {
        if (!repository.exists(pPlugin.getId())) {
            throw new EntityNotFoundException(pPlugin.getId(), UIPluginDefinition.class);
        }
        return repository.save(pPlugin);
    }

    @Override
    public void deletePlugin(final Long pPluginId) throws EntityNotFoundException {
        if (!repository.exists(pPluginId)) {
            throw new EntityNotFoundException(pPluginId, UIPluginDefinition.class);
        }
        repository.delete(pPluginId);
    }

}
