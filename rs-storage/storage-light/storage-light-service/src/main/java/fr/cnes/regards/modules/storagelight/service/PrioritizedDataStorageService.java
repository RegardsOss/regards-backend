package fr.cnes.regards.modules.storagelight.service;

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
import fr.cnes.regards.modules.storagelight.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storagelight.domain.plugin.DataStorageType;
import fr.cnes.regards.modules.storagelight.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storagelight.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storagelight.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storagelight.domain.plugin.PluginConfUpdatable;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@RegardsTransactional
public class PrioritizedDataStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(PrioritizedDataStorageService.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

    @Autowired
    private IFileReferenceRepository fileRefereceRepository;

    @Autowired
    private EntityManager em;

    public PrioritizedDataStorage create(PluginConfiguration toBeCreated) throws ModuleException {
        PluginConfiguration dataStorageConf = pluginService.savePluginConfiguration(toBeCreated);
        DataStorageType dataStorageType;
        if (dataStorageConf.getInterfaceNames().contains(IOnlineDataStorage.class.getName())) {
            dataStorageType = DataStorageType.ONLINE;
        } else if (dataStorageConf.getInterfaceNames().contains(INearlineDataStorage.class.getName())) {
            dataStorageType = DataStorageType.NEARLINE;
        } else {
            throw new EntityInvalidException(String
                    .format("Given plugin configuration(label: %s) is not a configuration for an online or nearline data storage (respectfully %s or %s)!",
                            dataStorageConf.getLabel(), IOnlineDataStorage.class.getName(),
                            INearlineDataStorage.class.getName()));
        }
        Long actualLowestPriority = getLowestPriority(dataStorageType);
        return prioritizedDataStorageRepository.save(new PrioritizedDataStorage(dataStorageConf,
                actualLowestPriority == null ? 0 : actualLowestPriority + 1, dataStorageType));
    }

    public List<PrioritizedDataStorage> findAllByType(DataStorageType type) {
        return prioritizedDataStorageRepository.findAllByDataStorageTypeOrderByPriorityAsc(type);
    }

    public Long getLowestPriority(DataStorageType dataStorageType) {
        PrioritizedDataStorage lowestPrioritizedDataStorage = prioritizedDataStorageRepository
                .findFirstByDataStorageTypeOrderByPriorityDesc(dataStorageType);
        if (lowestPrioritizedDataStorage == null) {
            // in case there is no one yet, lets give it the highest priority
            return null;
        }
        return lowestPrioritizedDataStorage.getPriority();
    }

    @Nullable
    public PrioritizedDataStorage getFirstActiveByType(DataStorageType dataStorageType) {
        return prioritizedDataStorageRepository
                .findFirstByDataStorageTypeAndDataStorageConfigurationActiveOrderByPriorityAsc(dataStorageType, true);
    }

    public void increasePriority(Long prioritizedDataStorageId) throws EntityNotFoundException {
        PrioritizedDataStorage actual = retrieve(prioritizedDataStorageId);
        PrioritizedDataStorage other = prioritizedDataStorageRepository
                .findOneByDataStorageTypeAndPriority(actual.getDataStorageType(), actual.getPriority() - 1);
        // is there someone which has a greater priority?
        if (other != null) {
            Long actualPriority = actual.getPriority();
            actual.setPriority(null);
            other.setPriority(null);
            prioritizedDataStorageRepository.saveAndFlush(actual);
            prioritizedDataStorageRepository.saveAndFlush(other);
            other.setPriority(actualPriority);
            actual.setPriority(actualPriority - 1);
            prioritizedDataStorageRepository.saveAndFlush(other);
            prioritizedDataStorageRepository.saveAndFlush(actual);
        }
    }

    public void decreasePriority(Long prioritizedDataStorageId) throws EntityNotFoundException {
        PrioritizedDataStorage actual = retrieve(prioritizedDataStorageId);
        PrioritizedDataStorage other = prioritizedDataStorageRepository
                .findOneByDataStorageTypeAndPriority(actual.getDataStorageType(), actual.getPriority() + 1);
        // is there someone which has a lower priority?
        if (other != null) {
            Long actualPriority = actual.getPriority();
            actual.setPriority(null);
            other.setPriority(null);
            prioritizedDataStorageRepository.saveAndFlush(actual);
            prioritizedDataStorageRepository.saveAndFlush(other);
            other.setPriority(actualPriority);
            actual.setPriority(actualPriority + 1);
            prioritizedDataStorageRepository.saveAndFlush(other);
            prioritizedDataStorageRepository.saveAndFlush(actual);
        }
    }

    public PrioritizedDataStorage retrieve(Long id) throws EntityNotFoundException {
        Optional<PrioritizedDataStorage> actual = prioritizedDataStorageRepository.findById(id);
        if (!actual.isPresent()) {
            throw new EntityNotFoundException(id, PrioritizedDataStorage.class);
        }
        return actual.get();
    }

    public PrioritizedDataStorage update(Long id, PrioritizedDataStorage updated) throws ModuleException {
        PrioritizedDataStorage oldOne = retrieve(id);
        if (!id.equals(updated.getId())) {
            throw new EntityInconsistentIdentifierException(id, updated.getId(), PrioritizedDataStorage.class);
        }

        PluginConfUpdatable updatable = null;
        boolean oldConfActive = oldOne.getDataStorageConfiguration().isActive();
        String storageLabel = oldOne.getDataStorageConfiguration().getLabel();

        if (oldConfActive) {
            // Count number of files stored by the plugin configuration
            Long nbfilesAlreadyStored = fileRefereceRepository.countByLocationStorage(storageLabel);

            // Ask plugin if the update is allowed
            try {
                IDataStorage<?> plugin = pluginService.getPlugin(oldOne.getDataStorageConfiguration().getId());
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
            return prioritizedDataStorageRepository.save(oldOne);
        } else {
            throw new EntityOperationForbiddenException(oldOne.getDataStorageConfiguration().getLabel(),
                    PrioritizedDataStorage.class, updatable.getUpdateNotAllowedReason());
        }
    }

    public void delete(Long pluginConfId) throws ModuleException {
        Optional<PrioritizedDataStorage> toDeleteOpt = prioritizedDataStorageRepository.findById(pluginConfId);
        if (toDeleteOpt.isPresent()) {
            // first we need to increase all the priorities of those which are less prioritized than the one to delete
            PrioritizedDataStorage toDelete = toDeleteOpt.get();
            if (canDelete(toDelete)) {
                Set<PrioritizedDataStorage> lessPrioritizeds = prioritizedDataStorageRepository
                        .findAllByDataStorageTypeAndPriorityGreaterThanOrderByPriorityAsc(toDelete.getDataStorageType(),
                                                                                          toDelete.getPriority());
                prioritizedDataStorageRepository.delete(toDelete);
                pluginService.deletePluginConfiguration(toDelete.getDataStorageConfiguration().getId());
                em.flush();
                for (PrioritizedDataStorage lessPrioritized : lessPrioritizeds) {
                    lessPrioritized.setPriority(lessPrioritized.getPriority() - 1);
                }
                prioritizedDataStorageRepository.saveAll(lessPrioritizeds);
            } else {
                String msg = String.format("Data storage %s could not be deleted because it contains files",
                                           toDelete.getDataStorageConfiguration().getLabel());
                LOG.info(msg);
                throw new EntityOperationForbiddenException(msg);
            }
        }
    }

    public boolean canDelete(PrioritizedDataStorage prioritizedDataStorage) {
        return fileRefereceRepository
                .countByLocationStorage(prioritizedDataStorage.getDataStorageConfiguration().getLabel()) == 0L;
    }
}
