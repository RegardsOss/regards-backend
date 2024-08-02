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
package fr.cnes.regards.framework.modules.tenant.settings.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * An abstract module to support {@link DynamicTenantSetting} import / export and reset automatically
 *
 * @author Léo Mieulet
 */
public abstract class AbstractModuleManagerWithTenantSettings<S> extends AbstractModuleManager<S> {

    @Autowired
    @Qualifier("dynamicTenantSettingServiceWithMask")
    private IDynamicTenantSettingService dynamicTenantSettingService;

    @Override
    public ModuleConfiguration exportConfiguration() {
        List<ModuleConfigurationItem<?>> configuration = new ArrayList<>();
        exportDynamicTenantSettings(configuration);
        return exportConfiguration(configuration);
    }

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration) {
        Set<DynamicTenantSetting> dynamicTenantSettings = getConfigurationSettingsByClass(configuration,
                                                                                          DynamicTenantSetting.class);
        return importConfiguration(configuration, importDynamicTenantSettingsConfiguration(dynamicTenantSettings));
    }

    @Override
    public Set<String> resetConfiguration() {
        Set<String> errors = Sets.newHashSet();
        resetDynamicTenantSettingService(errors);
        return resetConfiguration(errors);
    }

    /**
     * Reset dynamic tenant settings
     */
    private void resetDynamicTenantSettingService(Set<String> errors) {
        for (DynamicTenantSetting dynamicTenantSetting : dynamicTenantSettingService.readAll()) {
            String settingName = dynamicTenantSetting.getName();
            try {
                dynamicTenantSettingService.reset(settingName);
            } catch (ModuleException e) {
                String errorMsg = String.format("Could not reset dynamic setting with name '%s'. Cause: %s",
                                                settingName,
                                                e.getMessage());
                getLogger().error(errorMsg, e);
                errors.add(errorMsg);
            }
        }
    }

    /**
     * Export dynamic tenant settings
     */
    private void exportDynamicTenantSettings(List<ModuleConfigurationItem<?>> configuration) {
        dynamicTenantSettingService.readAll()
                                   .forEach(setting -> configuration.add(ModuleConfigurationItem.build(setting)));
    }

    /**
     * Import dynamic tenant settings
     */
    private Set<String> importDynamicTenantSettingsConfiguration(Set<DynamicTenantSetting> dynamicTenantSettings) {
        Set<String> errors = Sets.newHashSet();
        for (DynamicTenantSetting dynamicTenantSetting : dynamicTenantSettings) {
            String settingName = dynamicTenantSetting.getName();
            try {
                dynamicTenantSettingService.update(settingName, dynamicTenantSetting.getValue());
            } catch (ModuleException e) {
                String errorMsg = String.format("Failed to import dynamic tenant setting with name '%s'. Cause: %s",
                                                settingName,
                                                e.getMessage());
                getLogger().error(errorMsg, e);
                errors.add(errorMsg);
            }
        }
        return errors;
    }

    /**
     * Import configuration
     */
    protected abstract Set<String> importConfiguration(ModuleConfiguration configuration, Set<String> errors);

    /**
     * Export configuration
     */
    protected abstract ModuleConfiguration exportConfiguration(List<ModuleConfigurationItem<?>> configuration);

    /**
     * Reset current module configuration
     * All {@link DynamicTenantSetting} have been already resetted when this method is called
     *
     * @param errors current errors set
     */
    protected Set<String> resetConfiguration(Set<String> errors) {
        return errors;
    }

    abstract protected Logger getLogger();

}
