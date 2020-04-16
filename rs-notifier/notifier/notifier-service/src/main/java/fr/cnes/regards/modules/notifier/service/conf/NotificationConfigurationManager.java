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
package fr.cnes.regards.modules.notifier.service.conf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;

/**
 * Configuration manager for current module
 * @author Sébastien Binda
 */
@Component
public class NotificationConfigurationManager extends AbstractModuleManager<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationConfigurationManager.class);

    @Autowired
    private IPluginService pluginService;

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration) {

        Set<String> importErrors = new HashSet<>();
        Set<PluginConfiguration> configurations = getPluginConfs(configuration.getConfiguration());

        // First create connections
        for (PluginConfiguration plgConf : configurations) {
            try {
                PluginConfiguration existingOne = null;
                try {
                    existingOne = pluginService.getPluginConfiguration(plgConf.getBusinessId());
                } catch (EntityNotFoundException e) { // NOSONAR
                    // Nothing to do plugin configuration does not exists.
                }
                if (existingOne != null) {
                    LOGGER.info("Updating existing plugin configuration {}", plgConf.getBusinessId());
                    existingOne.setLabel(plgConf.getLabel());
                    existingOne.setParameters(plgConf.getParameters());
                    pluginService.updatePluginConfiguration(existingOne);
                } else {
                    LOGGER.info("Creating new plugin configuration {}", plgConf.getBusinessId());
                    pluginService.savePluginConfiguration(plgConf);
                }
            } catch (ModuleException e) {
                importErrors.add(e.getMessage());
            }
        }

        return importErrors;
    }

    @Override
    public ModuleConfiguration exportConfiguration() throws ModuleException {
        List<ModuleConfigurationItem<?>> configurations = new ArrayList<>();
        // export connections
        for (PluginConfiguration factory : pluginService.getAllPluginConfigurations()) {
            // All connection should be active
            PluginConfiguration exportedConf = pluginService.prepareForExport(factory);
            exportedConf.setIsActive(true);
            configurations.add(ModuleConfigurationItem.build(exportedConf));
        }
        return ModuleConfiguration.build(info, configurations);
    }

    /**
     * Get all {@link PluginConfiguration}s of the {@link ModuleConfigurationItem}s
     * @param items {@link ModuleConfigurationItem}s
     * @return  {@link PluginConfiguration}s
     */
    private Set<PluginConfiguration> getPluginConfs(Collection<ModuleConfigurationItem<?>> items) {
        return items.stream().filter(i -> PluginConfiguration.class.isAssignableFrom(i.getKey()))
                .map(i -> (PluginConfiguration) i.getTypedValue()).collect(Collectors.toSet());
    }
}
