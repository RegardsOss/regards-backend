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
package fr.cnes.regards.modules.fileaccess.service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.fileaccess.dao.IStorageLocationConfigurationRepository;
import fr.cnes.regards.modules.fileaccess.dto.IStoragePluginConfigurationDto;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IStorageLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.transaction.NotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service to manage plugin configurations to send to the storage workers
 *
 * @author Thibaud Michaudel
 **/
@Service
@MultitenantTransactional
public class StoragePluginConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoragePluginConfigurationService.class);

    private final PluginService pluginService;

    private final IStorageLocationConfigurationRepository storageLocationConfigurationRepository;

    public StoragePluginConfigurationService(PluginService pluginService,
                                             IStorageLocationConfigurationRepository storageLocationConfigurationRepository) {
        this.pluginService = pluginService;
        this.storageLocationConfigurationRepository = storageLocationConfigurationRepository;
    }

    public List<IStoragePluginConfigurationDto> getAllConfigurations() {
        List<IStoragePluginConfigurationDto> configurations = new ArrayList<>();
        List<String> existingBusinessIds = storageLocationConfigurationRepository.findAll()
                                                                                 .stream()
                                                                                 .map(configuration -> configuration.getPluginConfiguration()
                                                                                                                    .getBusinessId())
                                                                                 .toList();

        for (String id : existingBusinessIds) {
            try {
                IStorageLocation plugin = pluginService.getPlugin(id);
                configurations.add(plugin.createWorkerStoreConfiguration());
            } catch (ModuleException | NotAvailablePluginConfigurationException e) {
                LOGGER.error("Error while attempting to retrieve plugin {}", id, e);
            } catch (NotSupportedException e) {
                LOGGER.warn("The plugin {} has no shareable configuration defined", id);
            }
        }
        return configurations;
    }

    public Optional<IStoragePluginConfigurationDto> getByName(String storageName) {
        try {
            IStorageLocation plugin = pluginService.getPlugin(storageName);
            return Optional.of(plugin.createWorkerStoreConfiguration());
        } catch (ModuleException | NotAvailablePluginConfigurationException e) {
            LOGGER.error("Error while attempting to retrieve plugin {}", storageName, e);
        } catch (NotSupportedException e) {
            LOGGER.error("The plugin {} has no shareable configuration defined", storageName);
        }
        return Optional.empty();
    }
}
