/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.service.location;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storagelight.dao.IStorageLocationConfigurationRepostory;
import fr.cnes.regards.modules.storagelight.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storagelight.domain.plugin.INearlineStorageLocation;
import fr.cnes.regards.modules.storagelight.domain.plugin.IOnlineStorageLocation;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storagelight.domain.plugin.PluginConfUpdatable;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;

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

    private static final Logger LOG = LoggerFactory.getLogger(StorageLocationConfigurationService.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IStorageLocationConfigurationRepostory storageLocConfRepo;

    @Autowired
    private IFileReferenceRepository fileRefereceRepository;

    /**
     * Creates a new configuration for a storage location.
     * @param toBeCreated
     * @param allocatedSizeInKo
     * @return
     * @throws ModuleException
     */
    public StorageLocationConfiguration create(PluginConfiguration toBeCreated, Long allocatedSizeInKo)
            throws ModuleException {
        PluginConfiguration storageConf = pluginService.savePluginConfiguration(toBeCreated);
        StorageType storageType;
        if (storageConf.getInterfaceNames().contains(IOnlineStorageLocation.class.getName())) {
            storageType = StorageType.ONLINE;
        } else if (storageConf.getInterfaceNames().contains(INearlineStorageLocation.class.getName())) {
            storageType = StorageType.NEARLINE;
        } else {
            throw new EntityInvalidException(String
                    .format("Given plugin configuration(label: %s) is not a configuration for an online or nearline data storage (respectfully %s or %s)!",
                            storageConf.getLabel(), IOnlineStorageLocation.class.getName(),
                            INearlineStorageLocation.class.getName()));
        }
        Long actualLowestPriority = getLowestPriority(storageType);
        return storageLocConfRepo.save(new StorageLocationConfiguration(storageConf,
                actualLowestPriority == null ? 0 : actualLowestPriority + 1, allocatedSizeInKo, storageType));
    }

    /**
     * Search for all storage location configuration of the given storage type.
     * @param type {@link StorageType}
     * @return
     */
    public List<StorageLocationConfiguration> search(StorageType type) {
        return storageLocConfRepo.findAllByStorageTypeOrderByPriorityAsc(type);
    }

    /**
     * Search for the configuration of a given storage location.
     * @param storageId
     * @return {@link StorageLocationConfiguration}
     */
    public Optional<StorageLocationConfiguration> search(String storageId) {
        return storageLocConfRepo.findByStorageConfigurationBusinessId(storageId);
    }

    /**
     * Return the highest prioritized storage location for the given storage location list and the given type.
     * This method can return an empty optional value in case no configuration match the given storage location list.
     * @param storageIds
     * @param type
     * @return {@link StorageLocationConfiguration}
     */
    public Optional<StorageLocationConfiguration> searchActiveHigherPriority(Collection<String> storageIds,
            StorageType type) {
        Optional<StorageLocationConfiguration> storage = Optional.empty();
        Set<StorageLocationConfiguration> confs;
        if (type != null) {
            confs = storageLocConfRepo.findByStorageTypeAndStorageConfigurationLabelIn(type, storageIds);
        } else {
            confs = storageLocConfRepo.findByStorageConfigurationLabelIn(storageIds);
        }
        for (StorageLocationConfiguration c : confs) {
            if (c.getStorageConfiguration().isActive()
                    && (!storage.isPresent() || (c.getPriority() < storage.get().getPriority()))) {
                storage = Optional.of(c);
            }
        }
        return storage;
    }

    /**
     *
     * Return the highest prioritized storage location for the given storage location list.
     * This method can return an empty optional value in case no configuration match the given storage location list.
     *
     * @param storageIds
     * @return
     */
    public Optional<StorageLocationConfiguration> searchActiveHigherPriority(Set<String> storageIds) {
        return this.searchActiveHigherPriority(storageIds, null);
    }

    /**
     * Return the first active storage location of the given type.
     * @param storageType
     * @return {@link StorageLocationConfiguration}
     */
    @Nullable
    public StorageLocationConfiguration getFirstActive(StorageType storageType) {
        return storageLocConfRepo.findFirstByStorageTypeAndStorageConfigurationActiveOrderByPriorityAsc(storageType,
                                                                                                            true);
    }

    /**
     * Increase the priority of the given plugin configuration. To do so, it may change the priority of others plugin configuration.
     * @param storageId
     * @throws EntityNotFoundException
     */
    public void increasePriority(String storageId) throws EntityNotFoundException {
        Optional<StorageLocationConfiguration> oActual = search(storageId);
        if (oActual.isPresent()) {
            StorageLocationConfiguration actual = oActual.get();
            StorageLocationConfiguration other = storageLocConfRepo
                    .findOneByStorageTypeAndPriority(actual.getStorageType(), actual.getPriority() - 1);
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
     * @param storageId
     * @throws EntityNotFoundException
     */
    public void decreasePriority(String storageId) throws EntityNotFoundException {
        Optional<StorageLocationConfiguration> oActual = search(storageId);
        if (oActual.isPresent()) {
            StorageLocationConfiguration actual = oActual.get();
            StorageLocationConfiguration other = storageLocConfRepo
                    .findOneByStorageTypeAndPriority(actual.getStorageType(), actual.getPriority() + 1);
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
     * @param id
     * @return {@link StorageLocationConfiguration}
     * @throws EntityNotFoundException
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
     * @param id existing conf id
     * @param {@link StorageLocationConfiguration} new conf
     * @return {@link StorageLocationConfiguration}
     * @throws EntityNotFoundException
     */
    public StorageLocationConfiguration update(Long id, StorageLocationConfiguration updated) throws ModuleException {
        StorageLocationConfiguration oldOne = retrieve(id);
        if (!id.equals(updated.getId())) {
            throw new EntityInconsistentIdentifierException(id, updated.getId(), StorageLocationConfiguration.class);
        }

        PluginConfUpdatable updatable = null;
        boolean oldConfActive = oldOne.getStorageConfiguration().isActive();
        String storageLabel = oldOne.getStorageConfiguration().getLabel();

        if (oldConfActive) {
            // Count number of files stored by the plugin configuration
            Long nbfilesAlreadyStored = fileRefereceRepository.countByLocationStorage(storageLabel);

            // Ask plugin if the update is allowed
            try {
                IStorageLocation plugin = pluginService.getPlugin(oldOne.getStorageConfiguration().getBusinessId());
                updatable = plugin.allowConfigurationUpdate(updated.getStorageConfiguration(),
                                                            oldOne.getStorageConfiguration(), nbfilesAlreadyStored > 0);
            } catch (NotAvailablePluginConfigurationException e) {
                LOG.error(e.getMessage(), e);
                throw new EntityOperationForbiddenException(e.getMessage());
            }
        }

        // if oldConfActive is true, updatable cannot be null
        if (!oldConfActive || updatable.isUpdateAllowed()) {
            PluginConfiguration updatedConf = pluginService
                    .updatePluginConfiguration(updated.getStorageConfiguration());
            oldOne.setStorageConfiguration(updatedConf);
            return storageLocConfRepo.save(oldOne);
        } else {
            throw new EntityOperationForbiddenException(oldOne.getStorageConfiguration().getLabel(),
                    StorageLocationConfiguration.class, updatable.getUpdateNotAllowedReason());
        }
    }

    /**
     * Delete a {@link StorageLocationConfiguration} by id.
     * @param id
     * @throws ModuleException
     */
    public void delete(Long pluginConfId) throws ModuleException {
        Optional<StorageLocationConfiguration> toDeleteOpt = storageLocConfRepo.findById(pluginConfId);
        if (toDeleteOpt.isPresent()) {
            // first we need to increase all the priorities of those which are less prioritized than the one to delete
            StorageLocationConfiguration toDelete = toDeleteOpt.get();
            Set<StorageLocationConfiguration> lessPrioritizeds = storageLocConfRepo
                    .findAllByStorageTypeAndPriorityGreaterThanOrderByPriorityAsc(toDelete.getStorageType(),
                                                                                  toDelete.getPriority());
            storageLocConfRepo.delete(toDelete);
            pluginService.deletePluginConfiguration(toDelete.getStorageConfiguration().getBusinessId());
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
        StorageLocationConfiguration lowestPrioritizedStorage = storageLocConfRepo
                .findFirstByStorageTypeOrderByPriorityDesc(storageType);
        if (lowestPrioritizedStorage == null) {
            // in case there is no one yet, lets give it the highest priority
            return null;
        }
        return lowestPrioritizedStorage.getPriority();
    }
}
