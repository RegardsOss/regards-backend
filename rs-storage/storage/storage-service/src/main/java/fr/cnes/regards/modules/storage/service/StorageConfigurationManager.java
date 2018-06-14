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
import java.util.List;
import java.util.stream.Collectors;

import org.bouncycastle.openssl.EncryptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.manager.AbstractModuleConfigurationManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;

/**
 * Configuration manager for current module
 * @author Marc Sordi
 */
@Component
public class StorageConfigurationManager extends AbstractModuleConfigurationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConfigurationManager.class);

    @Autowired
    private IPluginConfigurationRepository pluginConfigurationRepository;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

    @Autowired
    private IPluginService pluginService;

    private IPrioritizedDataStorageService prioritizedDataStorageService;

    @Override
    public void importConfiguration(ModuleConfiguration configuration) throws ModuleException {
        for (ModuleConfigurationItem<?> item : configuration.getConfiguration()) {
            // first, is it a PrioritizedDataStorage?
            if (PrioritizedDataStorage.class.isAssignableFrom(item.getKey())) {
                importPrioritizedDataStorage(item);
            }
            if (PluginConfiguration.class.isAssignableFrom(item.getKey())) {
                importPluginConf(item);
            }
        }
    }

    protected void importPluginConf(ModuleConfigurationItem<?> item)
            throws EntityInvalidException {
        PluginConfiguration plgConf = item.getTypedValue();
        if (pluginService.findPluginConfigurationByLabel(plgConf.getLabel()).isPresent()) {
            LOGGER.warn("A plugin configuration already exists with same label, skipping import of {}.",
                        plgConf.getLabel());
        } else {
            EntityInvalidException validationIssues = PluginUtils.validate(plgConf);
            if (validationIssues == null) {
                pluginService.savePluginConfiguration(plgConf);
            } else {
                LOGGER.warn("Skipping import of {} for these reasons: {}",
                            plgConf.getLabel(),
                            validationIssues.getMessages().stream().collect(Collectors.joining(",", "", ".")));
            }
        }
    }

    protected void importPrioritizedDataStorage(ModuleConfigurationItem<?> item) throws ModuleException {
        PrioritizedDataStorage pds = item.getTypedValue();
        PluginConfiguration dataStorageConf = pds.getDataStorageConfiguration();
        if (pluginService.findPluginConfigurationByLabel(dataStorageConf.getLabel()).isPresent()) {
            LOGGER.warn("A plugin configuration already exists with same label, skipping import of {}.",
                        dataStorageConf.getLabel());
        } else {
            EntityInvalidException validationIssues = PluginUtils.validate(dataStorageConf);
            if (validationIssues != null) {
                LOGGER.warn("Skipping import of {} for these reasons: {}",
                            dataStorageConf.getLabel(),
                            validationIssues.getMessages().stream().collect(Collectors.joining(",", "", ".")));
            } else {
                if (prioritizedDataStorageRepository
                        .findOneByDataStorageTypeAndPriority(pds.getDataStorageType(), pds.getPriority())
                        != null) {
                    LOGGER.warn(
                            "A prioritized data storage with same priority already exists, skipping import of {}.",
                            dataStorageConf.getLabel());
                } else {
                    Long oldPriority = pds.getPriority();
                    PrioritizedDataStorage imported = prioritizedDataStorageService.create(dataStorageConf);
                    imported.setPriority(oldPriority);
                    prioritizedDataStorageRepository.save(imported);
                }
            }
        }
    }

    @Override
    public ModuleConfiguration exportConfiguration() throws ModuleException {
        List<ModuleConfigurationItem<?>> configurations = new ArrayList<>();
        // Fill list using ModuleConfigurationItem#build
        for (PrioritizedDataStorage pds : prioritizedDataStorageRepository.findAll()) {
            configurations.add(ModuleConfigurationItem.build(pds));
        }
        for (PluginConfiguration plgConf : pluginConfigurationRepository.findAll()) {
            if (!plgConf.getInterfaceNames().contains(IDataStorage.class.getName())) {
                configurations.add(ModuleConfigurationItem.build(plgConf));
            }
        }
        return ModuleConfiguration.build(info, configurations);
    }
}
