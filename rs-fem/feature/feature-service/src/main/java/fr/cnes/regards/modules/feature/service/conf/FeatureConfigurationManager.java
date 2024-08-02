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
package fr.cnes.regards.modules.feature.service.conf;

import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractModuleManagerWithTenantSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Configuration manager for current module
 *
 * @author Marc Sordi
 */
@Component
public class FeatureConfigurationManager extends AbstractModuleManagerWithTenantSettings<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureConfigurationManager.class);

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration, Set<String> importErrors) {
        for (ModuleConfigurationItem<?> item : configuration.getConfiguration()) {
            if (!DynamicTenantSetting.class.isAssignableFrom(item.getKey())) {
                String message = String.format(
                    "Configuration item of type %s has been ignored while import because it cannot be handled by %s. Module %s",
                    item.getKey(),
                    this.getClass().getName(),
                    configuration.getModule().getName());
                importErrors.add(message);
                LOGGER.warn(message);
            }
        }
        return importErrors;
    }

    @Override
    public ModuleConfiguration exportConfiguration(List<ModuleConfigurationItem<?>> configuration) {
        return ModuleConfiguration.build(info, true, configuration);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
