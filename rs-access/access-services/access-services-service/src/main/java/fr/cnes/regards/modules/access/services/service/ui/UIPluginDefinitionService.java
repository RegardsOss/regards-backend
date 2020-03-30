/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotEmptyException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginConfigurationRepository;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginDefinitionRepository;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;

@Service(value = "pluginService")
public class UIPluginDefinitionService
        implements IUIPluginDefinitionService, ApplicationListener<ApplicationReadyEvent> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(UIPluginDefinitionService.class);

    @Autowired
    private IUIPluginDefinitionRepository repository;

    @Autowired
    private IUIPluginConfigurationRepository pluginConfigurationRepository;

    @Value("${regards.access.multitenant:true}")
    private boolean isMultitenentMicroservice;

    /**
     * Runtime tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Tenant resolver to access all configured tenant
     */
    @Autowired
    private ITenantResolver tenantResolver;

    /**
     * Perform initialization only when the whole application is ready
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOG.info("UIPluginDefinitionService subscribing to new TenantConnectionReady events.");
    }

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processEvent(TenantConnectionReady event) {
        LOG.info("New tenant ready, initializing default plugins for tenant {}.", event.getTenant());
        try {
            runtimeTenantResolver.forceTenant(event.getTenant());
            initDefault();
        } finally {
            runtimeTenantResolver.clearTenant();
        }

        LOG.info("New tenant ready, default plugins initialized successfully.");
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
            UIPluginDefinition plugin = UIPluginDefinition.build("string-criteria",
                    "/plugins/criterion/string/plugin.js", UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = UIPluginDefinition.build("full-text-criteria",
                    "/plugins/criterion/full-text/plugin.js", UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = UIPluginDefinition.build("numerical-criteria",
                    "/plugins/criterion/numerical/plugin.js", UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = UIPluginDefinition.build("two-numerical-criteria",
                    "/plugins/criterion/two-numerical/plugin.js", UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = UIPluginDefinition.build("temporal-criteria",
                    "/plugins/criterion/temporal/plugin.js", UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = UIPluginDefinition.build("two-temporal-criteria",
                    "/plugins/criterion/two-temporal/plugin.js", UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);

            plugin = UIPluginDefinition.build("enumerated-criteria",
                    "/plugins/criterion/enumerated/plugin.js", UIPluginTypesEnum.CRITERIA);
            repository.save(plugin);
        }
    }

    @Override
    public UIPluginDefinition retrievePlugin(final Long pPluginId) throws EntityNotFoundException {
        if (!repository.existsById(pPluginId)) {
            throw new EntityNotFoundException(pPluginId, UIPluginDefinition.class);
        }
        return repository.findById(pPluginId).orElse(null);
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
        if (!repository.existsById(pPlugin.getId())) {
            throw new EntityNotFoundException(pPlugin.getId(), UIPluginDefinition.class);
        }
        return repository.save(pPlugin);
    }

    @Override
    public void deletePlugin(Long pluginId) throws ModuleException {

        Optional<UIPluginDefinition> oPluginDef = repository.findById(pluginId);
        if (!oPluginDef.isPresent()) {
            throw new EntityNotFoundException(pluginId, UIPluginDefinition.class);
        }
        if (pluginConfigurationRepository.hasPluginConfigurations(oPluginDef.get())) {
            throw new EntityNotEmptyException(pluginId, UIPluginDefinition.class);
        }
        repository.deleteById(pluginId);
    }

}
