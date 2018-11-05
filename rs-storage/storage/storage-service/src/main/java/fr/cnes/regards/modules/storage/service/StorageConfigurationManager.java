/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.manager.ModuleReadinessReport;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.ISecurityDelegation;
import fr.cnes.regards.modules.storage.domain.ready.StorageReadySpecifications;

/**
 * Configuration manager for storage module.
 * {@link PrioritizedDataStorage} are exported as simple plugin configuration so priority will be lost.
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 */
@Component
public class StorageConfigurationManager extends AbstractModuleManager<StorageReadySpecifications> {

    public static final String PLUGIN_CONFIGURATION_ALREADY_EXISTS = "A plugin configuration already exists with same label, skipping import of %s.";

    public static final String VALIDATION_ISSUES = "Skipping import of %s for these reasons: %s";

    @Autowired
    private IPluginConfigurationRepository pluginConfigurationRepository;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IPrioritizedDataStorageService prioritizedDataStorageService;

    @Override
    public Set<String> importConfiguration(ModuleConfiguration configuration) {
        Set<String> importErrors = new HashSet<>();
        for (ModuleConfigurationItem<?> item : configuration.getConfiguration()) {
            // We only exported PluginConfiguration so lets handle validation issues first
            // and then check if it is a DataStorage configuration
            if (PluginConfiguration.class.isAssignableFrom(item.getKey())) {
                PluginConfiguration plgConf = item.getTypedValue();
                if (pluginService.findPluginConfigurationByLabel(plgConf.getLabel()).isPresent()) {
                    importErrors.add(String.format(PLUGIN_CONFIGURATION_ALREADY_EXISTS, plgConf.getLabel()));
                } else {
                    EntityInvalidException validationIssues = PluginUtils.validate(plgConf);
                    if (validationIssues == null) {
                        // Now that we are about to create the plugin configuration, lets check for IDataStorage
                        if (plgConf.getInterfaceNames().contains(IDataStorage.class.getName())) {
                            try {
                                prioritizedDataStorageService.create(plgConf);
                            } catch (ModuleException e) {
                                importErrors.add(String.format("Skipping import of Data Storage %s: %s",
                                        plgConf.getLabel(), e.getMessage()));
                                logger.error(e.getMessage(), e);
                            }
                        } else {
                            try {
                                pluginService.savePluginConfiguration(plgConf);
                            } catch (ModuleException e) {
                                // This should not occurs, but we never know
                                importErrors.add(String.format("Skipping import of PluginConfiguration %s: %s",
                                        plgConf.getLabel(), e.getMessage()));
                                logger.error(e.getMessage(), e);
                            }
                        }
                    } else {
                        importErrors.add(String.format(VALIDATION_ISSUES, plgConf.getLabel(), validationIssues
                                .getMessages().stream().collect(Collectors.joining(",", "", "."))));
                    }
                }
            }
        }
        return importErrors;
    }

    @Override
    public ModuleConfiguration exportConfiguration() throws ModuleException {
        List<ModuleConfigurationItem<?>> configurations = new ArrayList<>();
        // Fill list using ModuleConfigurationItem#build
        for (PrioritizedDataStorage pds : prioritizedDataStorageRepository.findAll()) {
            // Lets export pds as plugin configuration as we don't take into account the exported priority
            configurations.add(ModuleConfigurationItem.build(pds.getDataStorageConfiguration()));
        }
        for (PluginConfiguration plgConf : pluginConfigurationRepository.findAll()) {
            if (!plgConf.getInterfaceNames().contains(IDataStorage.class.getName())) {
                configurations.add(ModuleConfigurationItem.build(plgConf));
            }
        }
        return ModuleConfiguration.build(info, configurations);
    }

    @Override
    public ModuleReadinessReport<StorageReadySpecifications> isReady() {
        boolean ready = true;
        List<String> reasons = Lists.newArrayList();
        //lets check allocation strategy
        Set<PluginConfiguration> strategies = pluginService.getPluginConfigurationsByType(IAllocationStrategy.class)
                .stream().filter(pc -> pc.isActive()).collect(Collectors.toSet());
        if (strategies.size() != 1) {
            reasons.add("There should be one and only one Allocation Strategy configured and active in the system. There is currently: "
                    + strategies.size());
            ready = false;
        }

        // check data storage
        Set<PluginConfiguration> dataStorages = pluginService.getPluginConfigurationsByType(IDataStorage.class).stream()
                .filter(pc -> pc.isActive()).collect(Collectors.toSet());
        if (dataStorages.isEmpty()) {
            reasons.add("There should be at least one ONLINE DataStorage configured and active in the system.");
            ready = false;
        }
        // check security delegation
        long numberSecurityDelegation = pluginService.getPluginConfigurationsByType(ISecurityDelegation.class).stream()
                .filter(pc -> pc.isActive()).count();
        if (numberSecurityDelegation != 1) {
            reasons.add("There should be one and only one Security Delegation configured and active in the system. There is currently: "
                    + numberSecurityDelegation);
            ready = false;
        }

        String allocationStrategy = null;
        if (strategies.stream().findFirst().isPresent()) {
            allocationStrategy = strategies.stream().findFirst().get().getLabel();
        }
        List<String> storages = dataStorages.stream().map(ds -> ds.getLabel()).collect(Collectors.toList());

        return new ModuleReadinessReport<StorageReadySpecifications>(ready, reasons,
                new StorageReadySpecifications(allocationStrategy, storages));
    }

    @Override
    public boolean isReadyImplemented() {
        return true;
    }
}
