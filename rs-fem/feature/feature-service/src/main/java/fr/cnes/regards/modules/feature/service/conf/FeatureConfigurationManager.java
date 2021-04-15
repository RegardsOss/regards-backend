/*
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
 */
package fr.cnes.regards.modules.feature.service.conf;

import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.DynamicTenantSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration manager for current module
 *
 * @author Marc Sordi
 */
@Component
public class FeatureConfigurationManager extends AbstractModuleManager<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureConfigurationManager.class);

    private final DynamicTenantSettingService dynamicTenantSettingService;

    public FeatureConfigurationManager(DynamicTenantSettingService dynamicTenantSettingService) {
        this.dynamicTenantSettingService = dynamicTenantSettingService;
    }

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration) {

        Set<String> importErrors = new HashSet<>();

        for (ModuleConfigurationItem<?> item : configuration.getConfiguration()) {
            if (DynamicTenantSetting.class.isAssignableFrom(item.getKey())) {
                DynamicTenantSetting setting = item.getTypedValue();
                try {
                    dynamicTenantSettingService.update(setting.getName(), setting.getValue());
                } catch (ModuleException e) {
                    importErrors.add(String.format("Configuration item not imported : Invalid Tenant Setting %s", setting));
                    LOGGER.error("Configuration item not imported : Invalid Tenant Setting {}", setting);
                }
            } else {
                String message = String.format(
                        "Configuration item of type %s has been ignored while import because it cannot be handled by %s. Module %s",
                        item.getKey(),
                        this.getClass().getName(),
                        configuration.getModule().getName()
                );
                importErrors.add(message);
                LOGGER.warn(message);
            }
        }
        return importErrors;
    }

    @Override
    public ModuleConfiguration exportConfiguration() {
        List<ModuleConfigurationItem<?>> configuration = new ArrayList<>();
        dynamicTenantSettingService.readAll()
                .forEach(setting -> configuration.add(ModuleConfigurationItem.build(setting)));
        return ModuleConfiguration.build(info, true, configuration);
    }

    @Override
    public Set<String> resetConfiguration() {
        Set<String> errors = new HashSet<>();
        try {
            for (DynamicTenantSetting dynamicTenantSetting : dynamicTenantSettingService.readAll()) {
                dynamicTenantSettingService.reset(dynamicTenantSetting.getName());
            }
        } catch (Exception e) {
            String error = "Error occurred while resetting settings.";
            LOGGER.error(error, e);
            errors.add(error);
        }
        return errors;
    }

}
