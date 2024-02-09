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

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.fileaccess.dao.IStorageLocationConfigurationRepository;
import fr.cnes.regards.modules.fileaccess.domain.StorageLocationConfiguration;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IStorageLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service to handle configuration of storage locations.<br>
 * A storage location can be associated to a plugin configuration<br>
 * If there is no plugin configuration associated, so the storage location is not reachable and files are only referenced.<br>
 *
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 * @author Thibaud Michaudel
 */
@Service
@RegardsTransactional
public class StorageLocationConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageLocationConfigurationService.class);

    private final IPluginService pluginService;

    private final IStorageLocationConfigurationRepository storageLocConfRepo;

    public StorageLocationConfigurationService(IPluginService pluginService,
                                               IStorageLocationConfigurationRepository storageLocConfRepo) {
        this.pluginService = pluginService;
        this.storageLocConfRepo = storageLocConfRepo;
    }

    /**
     * Creates a new configuration for a storage location.
     */
    public StorageLocationConfiguration create(String name, PluginConfiguration toBeCreated, Long allocatedSizeInKo)
        throws ModuleException {
        PluginConfiguration pluginConf = null;
        if (storageLocConfRepo.existsByName(name)) {
            throw new EntityAlreadyExistsException(String.format("Storage location configuration %s, already exists.",
                                                                 name));
        }
        if (toBeCreated != null) {
            toBeCreated.setBusinessId(name);
            toBeCreated.setLabel(name);
            pluginConf = pluginService.savePluginConfiguration(toBeCreated);
        }

        StorageLocationConfiguration conf = new StorageLocationConfiguration(name, pluginConf, allocatedSizeInKo);
        // Calculate priority
        Optional<Long> actualLowestPriority = getLowestPriority(conf.getStorageType());
        conf.setPriority(actualLowestPriority.orElse(-1L) + 1);
        return storageLocConfRepo.save(conf);
    }

    /**
     * Search for all storage location configuration of the given storage type.
     */
    public List<StorageLocationConfiguration> searchAll() {
        return storageLocConfRepo.findAll();
    }

    /**
     * Search for the configuration of a given storage location.
     *
     * @param name storage name
     * @return {@link StorageLocationConfiguration}
     */
    public Optional<StorageLocationConfiguration> search(String name) {
        return storageLocConfRepo.findByName(name);
    }

    /**
     * Return the actual lowest priority value for the given storage type
     */
    public Optional<Long> getLowestPriority(StorageType storageType) {
        StorageLocationConfiguration lowestPrioritizedStorage = storageLocConfRepo.findFirstByStorageTypeOrderByPriorityDesc(
            storageType);
        if (lowestPrioritizedStorage == null) {
            // in case there is no one yet, lets give it the highest priority
            return Optional.empty();
        }
        return Optional.of((lowestPrioritizedStorage.getPriority()));
    }

    public boolean allowPhysicalDeletion(StorageLocationConfiguration conf) throws ModuleException {
        if (conf != null && conf.getPluginConfiguration() != null) {
            try {
                IStorageLocation location = pluginService.getPlugin(conf.getPluginConfiguration().getBusinessId());
                return location.allowPhysicalDeletion();
            } catch (NotAvailablePluginConfigurationException e) {
                LOGGER.debug(String.format(
                    "StorageLocationConfiguration %s is considered not allowing physical file delete because "
                    + "underlying PluginConfiguration is not available",
                    conf.getName()), e);
                return false;
            }
        } else {
            return false;
        }
    }
}
