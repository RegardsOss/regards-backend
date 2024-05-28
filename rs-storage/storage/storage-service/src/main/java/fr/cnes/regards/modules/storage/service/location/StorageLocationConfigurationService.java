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
package fr.cnes.regards.modules.storage.service.location;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.*;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IStorageLocation;
import fr.cnes.regards.modules.storage.dao.IStorageLocationConfigurationRepostory;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;
import java.util.*;

/**
 * Service to handle configuration of storge locations.<br>
 * A storage location can be associated to a plugin configuration<br>
 * If there is no plugin configuration associated, so the storage location is not reachable and files are only referenced.<br>
 *
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 */
@Service
@RegardsTransactional
public class StorageLocationConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageLocationConfigurationService.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IStorageLocationConfigurationRepostory storageLocConfRepo;

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
        Long actualLowestPriority = getLowestPriority(conf.getStorageType());
        conf.setPriority(actualLowestPriority == null ? 0 : actualLowestPriority + 1);
        return storageLocConfRepo.save(conf);
    }

    /**
     * Search for all storage location configuration of the given storage type.
     *
     * @param type {@link StorageType}
     */
    public List<StorageLocationConfiguration> search(StorageType type) {
        return storageLocConfRepo.findAllByStorageTypeOrderByPriorityAsc(type);
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
     * @param storageId storage id
     * @return {@link StorageLocationConfiguration}
     */
    public Optional<StorageLocationConfiguration> search(String storageId) {
        return storageLocConfRepo.findByName(storageId);
    }

    /**
     * Search for the configuration of given storages locations.
     *
     * @param storageNames storage ids
     * @return Set of {@link StorageLocationConfiguration}
     */
    public Set<StorageLocationConfiguration> searchByNames(Collection<String> storageNames) {
        return storageLocConfRepo.findByNameIn(storageNames);
    }

    /**
     * Return the highest prioritized storage location for the given storage location list and the given type.
     * This method can return an empty optional value in case no configuration match the given storage location list.
     *
     * @param storageIds storage ids
     * @param type       storage type
     * @return {@link StorageLocationConfiguration}
     */
    public Optional<StorageLocationConfiguration> searchActiveHigherPriority(Collection<String> storageIds,
                                                                             StorageType type) {
        Optional<StorageLocationConfiguration> storage = Optional.empty();
        Set<StorageLocationConfiguration> confs;
        if (type != null) {
            confs = storageLocConfRepo.findByStorageTypeAndNameIn(type, storageIds);
        } else {
            confs = storageLocConfRepo.findByNameIn(storageIds);
        }
        for (StorageLocationConfiguration c : confs) {
            if (c.getPluginConfiguration().isActive() && (!storage.isPresent() || c.getPriority() < storage.get()
                                                                                                           .getPriority())) {
                storage = Optional.of(c);
            }
        }
        return storage;
    }

    /**
     * Return the highest prioritized storage location for the given storage location list.
     * This method can return an empty optional value in case no configuration match the given storage location list.
     */
    public Optional<StorageLocationConfiguration> searchActiveHigherPriority(Set<String> storageIds) {
        return this.searchActiveHigherPriority(storageIds, null);
    }

    /**
     * Return the first active storage location of the given type.
     *
     * @param storageType storage type
     * @return {@link StorageLocationConfiguration}
     */
    @Nullable
    public StorageLocationConfiguration getFirstActive(StorageType storageType) {
        return storageLocConfRepo.findFirstByStorageTypeAndPluginConfigurationActiveOrderByPriorityAsc(storageType,
                                                                                                       true);
    }

    /**
     * Increase the priority of the given plugin configuration. To do so, it may change the priority of others plugin configuration.
     *
     * @param storageId storage id
     * @throws EntityNotFoundException if no {@link StorageLocationConfiguration} corresponding to {@code storageId} exists
     */
    public void increasePriority(String storageId) throws EntityNotFoundException {
        Optional<StorageLocationConfiguration> oActual = search(storageId);
        if (oActual.isPresent()) {
            StorageLocationConfiguration actual = oActual.get();
            StorageLocationConfiguration other = storageLocConfRepo.findOneByStorageTypeAndPriority(actual.getStorageType(),
                                                                                                    actual.getPriority()
                                                                                                    - 1);
            // is there someone which has a greater priority?
            if (other != null) {
                Long actualPriority = actual.getPriority();
                actual.setPriority(null);
                other.setPriority(null);
                storageLocConfRepo.saveAndFlush(actual);
                storageLocConfRepo.saveAndFlush(other);
                other.setPriority(actualPriority);
                actual.setPriority(actualPriority - 1);
                storageLocConfRepo.saveAndFlush(other);
                storageLocConfRepo.saveAndFlush(actual);
            }
        } else {
            throw new EntityNotFoundException(storageId, StorageLocationConfiguration.class);
        }
    }

    /**
     * Decrease the priority of the given plugin configuration. To do so, it may change the priority of others plugin configuration.
     *
     * @param storageId storage id
     * @throws EntityNotFoundException if no {@link StorageLocationConfiguration} corresponding to {@code storageId} exists
     */
    public void decreasePriority(String storageId) throws EntityNotFoundException {
        Optional<StorageLocationConfiguration> oActual = search(storageId);
        if (oActual.isPresent()) {
            StorageLocationConfiguration actual = oActual.get();
            StorageLocationConfiguration other = storageLocConfRepo.findOneByStorageTypeAndPriority(actual.getStorageType(),
                                                                                                    actual.getPriority()
                                                                                                    + 1);
            // is there someone which has a lower priority?
            if (other != null) {
                Long actualPriority = actual.getPriority();
                actual.setPriority(null);
                other.setPriority(null);
                storageLocConfRepo.saveAndFlush(actual);
                storageLocConfRepo.saveAndFlush(other);
                other.setPriority(actualPriority);
                actual.setPriority(actualPriority + 1);
                storageLocConfRepo.saveAndFlush(other);
                storageLocConfRepo.saveAndFlush(actual);
            }
        } else {
            throw new EntityNotFoundException(storageId, StorageLocationConfiguration.class);
        }
    }

    /**
     * Retrieve a {@link StorageLocationConfiguration} by id.
     *
     * @param id storage id
     * @return {@link StorageLocationConfiguration}
     * @throws EntityNotFoundException if no {@link StorageLocationConfiguration} corresponding to {@code id} exists
     */
    public StorageLocationConfiguration retrieve(Long id) throws EntityNotFoundException {
        Optional<StorageLocationConfiguration> actual = storageLocConfRepo.findById(id);
        if (!actual.isPresent()) {
            throw new EntityNotFoundException(id, StorageLocationConfiguration.class);
        }
        return actual.get();
    }

    /**
     * Update a {@link StorageLocationConfiguration} by id.
     *
     * @param storageId existing conf id
     * @param updated   {@link StorageLocationConfiguration} new conf
     * @return {@link StorageLocationConfiguration}
     * @throws EntityNotFoundException if no {@link StorageLocationConfiguration} corresponding to {@code storageId} exists
     */
    public StorageLocationConfiguration update(String storageId, StorageLocationConfiguration updated)
        throws ModuleException {

        if (!storageId.equals(updated.getName())) {
            throw new EntityInconsistentIdentifierException(storageId,
                                                            updated.getName(),
                                                            StorageLocationConfiguration.class);
        }
        Optional<StorageLocationConfiguration> oOldOne = search(storageId);
        if (!oOldOne.isPresent()) {
            throw new EntityNotFoundException(storageId, StorageLocationConfiguration.class);
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
        toUpdate.setPriority(oldOne.getPriority());
        return storageLocConfRepo.save(toUpdate);
    }

    /**
     * Delete a {@link StorageLocationConfiguration} by id.
     *
     * @param pluginConfId plugin conf id
     * @throws ModuleException in accordance with {@link fr.cnes.regards.framework.modules.plugins.service.PluginService#deletePluginConfiguration(String)}
     */
    public void delete(Long pluginConfId) throws ModuleException {
        Optional<StorageLocationConfiguration> toDeleteOpt = storageLocConfRepo.findById(pluginConfId);
        if (toDeleteOpt.isPresent()) {
            // Delete conf and plugin conf associated
            StorageLocationConfiguration toDelete = toDeleteOpt.get();
            storageLocConfRepo.delete(toDelete);
            if (toDelete.getPluginConfiguration() != null) {
                pluginService.deletePluginConfiguration(toDelete.getPluginConfiguration().getBusinessId());
            }
            // Increase all the priorities of those which are less prioritized than the one to delete
            Set<StorageLocationConfiguration> lessPrioritizeds = storageLocConfRepo.findAllByStorageTypeAndPriorityGreaterThanOrderByPriorityAsc(
                toDelete.getStorageType(),
                toDelete.getPriority());
            for (StorageLocationConfiguration lessPrioritized : lessPrioritizeds) {
                lessPrioritized.setPriority(lessPrioritized.getPriority() - 1);
            }
            storageLocConfRepo.saveAll(lessPrioritizeds);
        }
    }

    /**
     * Return the actual lowest priority value for the given storage type
     */
    public Long getLowestPriority(StorageType storageType) {
        StorageLocationConfiguration lowestPrioritizedStorage = storageLocConfRepo.findFirstByStorageTypeOrderByPriorityDesc(
            storageType);
        if (lowestPrioritizedStorage == null) {
            // in case there is no one yet, lets give it the highest priority
            return null;
        }
        return lowestPrioritizedStorage.getPriority();
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
