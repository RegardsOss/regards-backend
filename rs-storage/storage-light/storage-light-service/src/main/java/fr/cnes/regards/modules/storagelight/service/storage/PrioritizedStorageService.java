package fr.cnes.regards.modules.storagelight.service.storage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;

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
import fr.cnes.regards.modules.storagelight.dao.IPrioritizedStorageRepository;
import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedStorage;
import fr.cnes.regards.modules.storagelight.domain.plugin.INearlineStorageLocation;
import fr.cnes.regards.modules.storagelight.domain.plugin.IOnlineStorageLocation;
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storagelight.domain.plugin.PluginConfUpdatable;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@RegardsTransactional
public class PrioritizedStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(PrioritizedStorageService.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IPrioritizedStorageRepository prioritizedStorageRepo;

    @Autowired
    private IFileReferenceRepository fileRefereceRepository;

    @Autowired
    private EntityManager em;

    public PrioritizedStorage create(PluginConfiguration toBeCreated) throws ModuleException {
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
        return prioritizedStorageRepo.save(new PrioritizedStorage(storageConf,
                actualLowestPriority == null ? 0 : actualLowestPriority + 1, storageType));
    }

    public List<PrioritizedStorage> search(StorageType type) {
        return prioritizedStorageRepo.findAllByStorageTypeOrderByPriorityAsc(type);
    }

    /**
     * Return the highest priotized storage location for the given storage location list.
     * This method can return an empty optional value in case no configuration match the given storage location list.
     */
    public Optional<PrioritizedStorage> searchActiveHigherPriority(Collection<String> confLabels, StorageType type) {
        Optional<PrioritizedStorage> storage = Optional.empty();
        for (PrioritizedStorage c : prioritizedStorageRepo
                .findByStorageTypeAndStorageConfigurationLabelIn(type, confLabels)) {
            if (c.getDataStorageConfiguration().isActive()
                    && (!storage.isPresent() || (c.getPriority() < storage.get().getPriority()))) {
                storage = Optional.of(c);
            }
        }
        return storage;
    }

    @Nullable
    public PrioritizedStorage getFirstActive(StorageType storageType) {
        return prioritizedStorageRepo.findFirstByStorageTypeAndStorageConfigurationActiveOrderByPriorityAsc(storageType,
                                                                                                            true);
    }

    /**
     * Increase the priority of the given plugin configuration. To do so, it may change the priority of others plugin configuration.
     * @param prioritizedDataStorageId
     * @throws EntityNotFoundException
     */
    public void increasePriority(Long prioritizedDataStorageId) throws EntityNotFoundException {
        PrioritizedStorage actual = retrieve(prioritizedDataStorageId);
        PrioritizedStorage other = prioritizedStorageRepo.findOneByStorageTypeAndPriority(actual.getStorageType(),
                                                                                          actual.getPriority() - 1);
        // is there someone which has a greater priority?
        if (other != null) {
            Long actualPriority = actual.getPriority();
            actual.setPriority(null);
            other.setPriority(null);
            prioritizedStorageRepo.saveAndFlush(actual);
            prioritizedStorageRepo.saveAndFlush(other);
            other.setPriority(actualPriority);
            actual.setPriority(actualPriority - 1);
            prioritizedStorageRepo.saveAndFlush(other);
            prioritizedStorageRepo.saveAndFlush(actual);
        }
    }

    /**
     * Decrease the priority of the given plugin configuration. To do so, it may change the priority of others plugin configuration.
     * @param prioritizedDataStorageId
     * @throws EntityNotFoundException
     */
    public void decreasePriority(Long prioritizedDataStorageId) throws EntityNotFoundException {
        PrioritizedStorage actual = retrieve(prioritizedDataStorageId);
        PrioritizedStorage other = prioritizedStorageRepo.findOneByStorageTypeAndPriority(actual.getStorageType(),
                                                                                          actual.getPriority() + 1);
        // is there someone which has a lower priority?
        if (other != null) {
            Long actualPriority = actual.getPriority();
            actual.setPriority(null);
            other.setPriority(null);
            prioritizedStorageRepo.saveAndFlush(actual);
            prioritizedStorageRepo.saveAndFlush(other);
            other.setPriority(actualPriority);
            actual.setPriority(actualPriority + 1);
            prioritizedStorageRepo.saveAndFlush(other);
            prioritizedStorageRepo.saveAndFlush(actual);
        }
    }

    /**
     * Retrieve a {@link PrioritizedStorage} by id.
     * @param id
     * @return {@link PrioritizedStorage}
     * @throws EntityNotFoundException
     */
    public PrioritizedStorage retrieve(Long id) throws EntityNotFoundException {
        Optional<PrioritizedStorage> actual = prioritizedStorageRepo.findById(id);
        if (!actual.isPresent()) {
            throw new EntityNotFoundException(id, PrioritizedStorage.class);
        }
        return actual.get();
    }

    /**
     * Update a {@link PrioritizedStorage} by id.
     * @param id existing conf id
     * @param {@link PrioritizedStorage} new conf
     * @return {@link PrioritizedStorage}
     * @throws EntityNotFoundException
     */
    public PrioritizedStorage update(Long id, PrioritizedStorage updated) throws ModuleException {
        PrioritizedStorage oldOne = retrieve(id);
        if (!id.equals(updated.getId())) {
            throw new EntityInconsistentIdentifierException(id, updated.getId(), PrioritizedStorage.class);
        }

        PluginConfUpdatable updatable = null;
        boolean oldConfActive = oldOne.getDataStorageConfiguration().isActive();
        String storageLabel = oldOne.getDataStorageConfiguration().getLabel();

        if (oldConfActive) {
            // Count number of files stored by the plugin configuration
            Long nbfilesAlreadyStored = fileRefereceRepository.countByLocationStorage(storageLabel);

            // Ask plugin if the update is allowed
            try {
                IStorageLocation plugin = pluginService.getPlugin(oldOne.getDataStorageConfiguration().getId());
                updatable = plugin.allowConfigurationUpdate(updated.getDataStorageConfiguration(),
                                                            oldOne.getDataStorageConfiguration(),
                                                            nbfilesAlreadyStored > 0);
            } catch (NotAvailablePluginConfigurationException e) {
                throw new EntityOperationForbiddenException(e.getMessage());
            }
        }

        // if oldConfActive is true, updatable cannot be null
        if (!oldConfActive || updatable.isUpdateAllowed()) {
            PluginConfiguration updatedConf = pluginService
                    .updatePluginConfiguration(updated.getDataStorageConfiguration());
            oldOne.setDataStorageConfiguration(updatedConf);
            return prioritizedStorageRepo.save(oldOne);
        } else {
            throw new EntityOperationForbiddenException(oldOne.getDataStorageConfiguration().getLabel(),
                    PrioritizedStorage.class, updatable.getUpdateNotAllowedReason());
        }
    }

    /**
     * Delete a {@link PrioritizedStorage} by id.
     * @param id
     * @throws ModuleException
     */
    public void delete(Long pluginConfId) throws ModuleException {
        Optional<PrioritizedStorage> toDeleteOpt = prioritizedStorageRepo.findById(pluginConfId);
        if (toDeleteOpt.isPresent()) {
            // first we need to increase all the priorities of those which are less prioritized than the one to delete
            PrioritizedStorage toDelete = toDeleteOpt.get();
            if (canDelete(toDelete)) {
                Set<PrioritizedStorage> lessPrioritizeds = prioritizedStorageRepo
                        .findAllByStorageTypeAndPriorityGreaterThanOrderByPriorityAsc(toDelete.getStorageType(),
                                                                                      toDelete.getPriority());
                prioritizedStorageRepo.delete(toDelete);
                pluginService.deletePluginConfiguration(toDelete.getDataStorageConfiguration().getId());
                em.flush();
                for (PrioritizedStorage lessPrioritized : lessPrioritizeds) {
                    lessPrioritized.setPriority(lessPrioritized.getPriority() - 1);
                }
                prioritizedStorageRepo.saveAll(lessPrioritizeds);
            } else {
                String msg = String.format("Data storage %s could not be deleted because it contains files",
                                           toDelete.getDataStorageConfiguration().getLabel());
                LOG.info(msg);
                throw new EntityOperationForbiddenException(msg);
            }
        }
    }

    /**
     * Does the given {@link PrioritizedStorage} can be deleted ?
     * @return boolean
     */
    public boolean canDelete(PrioritizedStorage prioritizedDataStorage) {
        return fileRefereceRepository
                .countByLocationStorage(prioritizedDataStorage.getDataStorageConfiguration().getLabel()) == 0L;
    }

    /**
     * Return the actual lowest priority value for the given storage type
     */
    private Long getLowestPriority(StorageType storageType) {
        PrioritizedStorage lowestPrioritizedStorage = prioritizedStorageRepo
                .findFirstByStorageTypeOrderByPriorityDesc(storageType);
        if (lowestPrioritizedStorage == null) {
            // in case there is no one yet, lets give it the highest priority
            return null;
        }
        return lowestPrioritizedStorage.getPriority();
    }
}
