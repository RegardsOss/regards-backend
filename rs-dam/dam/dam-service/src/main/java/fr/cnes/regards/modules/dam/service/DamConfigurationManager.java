/*
 *
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 *
 */
package fr.cnes.regards.modules.dam.service;

import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.DynamicTenantSettingService;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IConnectionPlugin;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IDataSourcePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DAM configuration manager. Exports model & connection plugin configurations & datasource plugin configurations.
 *
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 */
@Component
public class DamConfigurationManager extends AbstractModuleManager<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DamConfigurationManager.class);

    private final IPluginService pluginService;
    private final DynamicTenantSettingService dynamicTenantSettingService;

    public DamConfigurationManager(IPluginService pluginService, DynamicTenantSettingService dynamicTenantSettingService) {
        this.pluginService = pluginService;
        this.dynamicTenantSettingService = dynamicTenantSettingService;
    }

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration) {

        Set<String> importErrors = new HashSet<>();

        // First create connections
        for (PluginConfiguration plgConf : getPluginConfigurations(configuration)) {
            try {
                pluginService.savePluginConfiguration(plgConf);
            } catch (EntityInvalidException | EncryptionException | EntityNotFoundException e) {
                importErrors.add(e.getMessage());
            }
        }

        for (DynamicTenantSetting setting : getSettings(configuration)) {
            try {
                dynamicTenantSettingService.update(setting.getName(), setting.getValue());
            } catch (ModuleException e) {
                importErrors.add(String.format("Configuration item not imported : Invalid Tenant Setting %s", setting));
                LOGGER.error("Configuration item not imported : Invalid Tenant Setting {}", setting);
            }
        }

        return importErrors;
    }

    @Override
    public ModuleConfiguration exportConfiguration() {

        List<ModuleConfigurationItem<?>> configurations = new ArrayList<>();

        // export connections
        for (PluginConfiguration connection : pluginService.getPluginConfigurationsByType(IConnectionPlugin.class)) {
            // All connection should be active
            PluginConfiguration exportedConnection = pluginService.prepareForExport(connection);
            exportedConnection.setIsActive(true);
            configurations.add(ModuleConfigurationItem.build(exportedConnection));
        }
        // export datasources
        for (PluginConfiguration dataSource : pluginService.getPluginConfigurationsByType(IDataSourcePlugin.class)) {
            configurations.add(ModuleConfigurationItem.build(pluginService.prepareForExport(dataSource)));
        }

        // export settings
        dynamicTenantSettingService.readAll().forEach(setting -> configurations.add(ModuleConfigurationItem.build(setting)));

        return ModuleConfiguration.build(info, configurations);
    }

    /**
     * Get all {@link PluginConfiguration}s of the {@link ModuleConfigurationItem}s
     * @param configuration {@link ModuleConfiguration}s
     * @return  {@link PluginConfiguration}s
     */
    private Set<PluginConfiguration> getPluginConfigurations(ModuleConfiguration configuration) {
        return configuration.getConfiguration().stream()
                .filter(i -> PluginConfiguration.class.isAssignableFrom(i.getKey()))
                .map(i -> (PluginConfiguration) i.getTypedValue()).collect(Collectors.toSet());
    }

    private Set<DynamicTenantSetting> getSettings(ModuleConfiguration configuration) {
        return configuration.getConfiguration().stream()
                .filter(i -> DynamicTenantSetting.class.isAssignableFrom(i.getKey()))
                .map(i -> (DynamicTenantSetting) i.getTypedValue()).collect(Collectors.toSet());
    }

}
