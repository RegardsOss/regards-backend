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
package fr.cnes.regards.modules.storage.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractModuleManagerWithTenantSettings;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.service.location.StorageLocationConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Module to allow import/export storage microservice configuration
 *
 * @author Sébastien Binda
 */
@Component
public class StorageModuleManager extends AbstractModuleManagerWithTenantSettings<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageModuleManager.class);

    @Autowired
    private StorageLocationConfigurationService storageConfService;

    @Override
    public ModuleConfiguration exportConfiguration(List<ModuleConfigurationItem<?>> configuration) {
        for (StorageLocationConfiguration conf : storageConfService.searchAll()) {
            configuration.add(ModuleConfigurationItem.build(conf));
        }
        return ModuleConfiguration.build(info, configuration);
    }

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration, Set<String> importErrors) {
        Set<StorageLocationConfiguration> storageLocationConfigs = getStorageLocationConfigs(configuration.getConfiguration());

        importErrors.addAll(importStorageLocationConfigs(storageLocationConfigs));
        return importErrors;
    }

    private Set<String> importStorageLocationConfigs(Set<StorageLocationConfiguration> storageLocationConfigs) {
        Set<String> importErrors = Sets.newHashSet();
        for (StorageLocationConfiguration conf : storageLocationConfigs) {
            if (storageConfService.search(conf.getName()).isPresent()) {
                importErrors.add(String.format("Storage location %s is already defined.", conf.getName()));
            } else {
                try {
                    storageConfService.create(conf.getName(),
                                              conf.getPluginConfiguration(),
                                              conf.getAllocatedSizeInKo());
                } catch (ModuleException e) {
                    importErrors.add(String.format("Skipping import of StorageLocationConfiguration %s: %s",
                                                   conf.getName(),
                                                   e.getMessage()));
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
        return importErrors;
    }

    private Set<StorageLocationConfiguration> getStorageLocationConfigs(Collection<ModuleConfigurationItem<?>> items) {
        return items.stream()
                    .filter(i -> StorageLocationConfiguration.class.isAssignableFrom(i.getKey()))
                    .map(i -> (StorageLocationConfiguration) i.getTypedValue())
                    .collect(Collectors.toSet());
    }
}
