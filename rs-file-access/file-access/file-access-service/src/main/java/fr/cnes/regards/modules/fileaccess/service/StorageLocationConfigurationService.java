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
import fr.cnes.regards.framework.module.rest.exception.*;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.fileaccess.dao.IStorageLocationConfigurationRepository;
import fr.cnes.regards.modules.fileaccess.domain.StorageLocationConfiguration;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IStorageLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
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

    private final IStorageLocationConfigurationRepository storageLocationConfigRepository;

    public StorageLocationConfigurationService(IPluginService pluginService,
                                               IStorageLocationConfigurationRepository storageLocationConfigRepository) {
        this.pluginService = pluginService;
        this.storageLocationConfigRepository = storageLocationConfigRepository;
    }

    /**
     * Creates a new configuration for a storage location.
     */
    public StorageLocationConfiguration create(String name, PluginConfiguration toBeCreated, Long allocatedSizeInKo)
        throws ModuleException {
        PluginConfiguration pluginConf = null;
        if (storageLocationConfigRepository.existsByName(name)) {
            throw new EntityAlreadyExistsException(String.format("Storage location configuration %s, already exists.",
                                                                 name));
        }
        if (toBeCreated != null) {
            toBeCreated.setBusinessId(name);
            toBeCreated.setLabel(name);
            pluginConf = pluginService.savePluginConfiguration(toBeCreated);
        }

        return storageLocationConfigRepository.save(new StorageLocationConfiguration(name,
                                                                                     pluginConf,
                                                                                     allocatedSizeInKo));
    }

    /**
     * Search for all storage location configuration of the given storage type.
     */
    public List<StorageLocationConfiguration> searchAll() {
        return storageLocationConfigRepository.findAll();
    }

    /**
     * Search for the configuration of a given storage location.
     *
     * @param name storage name
     * @return {@link StorageLocationConfiguration}
     */
    public Optional<StorageLocationConfiguration> search(String name) {
        return storageLocationConfigRepository.findByName(name);
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

    /**
     * Update a {@link StorageLocationConfiguration} by id.
     *
     * @param name    existing conf id
     * @param updated {@link StorageLocationConfiguration} new conf
     * @return {@link StorageLocationConfiguration}
     * @throws EntityNotFoundException if no {@link StorageLocationConfiguration} corresponding to {@code name} exists
     */
    public StorageLocationConfiguration update(String name, StorageLocationConfiguration updated)
        throws ModuleException {
        if (!name.equals(updated.getName())) {
            throw new EntityInconsistentIdentifierException(name,
                                                            updated.getName(),
                                                            StorageLocationConfiguration.class);
        }
        Optional<StorageLocationConfiguration> oOldOne = search(name);
        if (oOldOne.isEmpty()) {
            throw new EntityNotFoundException(name, StorageLocationConfiguration.class);
        }

        StorageLocationConfiguration oldOne = oOldOne.get();
        if (oldOne.getPluginConfiguration() != null) {
            if (updated.getPluginConfiguration() == null) {
                pluginService.deletePluginConfiguration(oldOne.getPluginConfiguration().getBusinessId());
            } else {
                if (Objects.equals(updated.getPluginConfiguration().getPluginId(),
                                   oldOne.getPluginConfiguration().getPluginId())) {
                    pluginService.updatePluginConfiguration(updated.getPluginConfiguration());
                } else {
                    throw new EntityInvalidException("Storage location plugin cannot be updated! "
                                                     + "If you made a mistake you have to delete the old one and create a new one.");
                }
            }
        } else if (updated.getPluginConfiguration() != null) {
            pluginService.savePluginConfiguration(updated.getPluginConfiguration());
        }

        // Recalculate storage information by creating a new one and setting the previous id to update.
        StorageLocationConfiguration toUpdate = new StorageLocationConfiguration(updated.getName(),
                                                                                 updated.getPluginConfiguration(),
                                                                                 updated.getAllocatedSizeInKo());
        toUpdate.setId(oldOne.getId());
        return storageLocationConfigRepository.save(toUpdate);
    }

    public void delete(String name) throws ModuleException {
        Optional<StorageLocationConfiguration> toDeleteOpt = storageLocationConfigRepository.findByName(name);
        if (toDeleteOpt.isPresent()) {
            // Delete conf and plugin conf associated
            StorageLocationConfiguration toDelete = toDeleteOpt.get();
            storageLocationConfigRepository.delete(toDelete);
            if (toDelete.getPluginConfiguration() != null) {
                pluginService.deletePluginConfiguration(toDelete.getPluginConfiguration().getBusinessId());
            }
        }
    }
}
