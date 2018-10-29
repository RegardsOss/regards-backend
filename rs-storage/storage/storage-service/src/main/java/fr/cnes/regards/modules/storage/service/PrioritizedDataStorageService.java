package fr.cnes.regards.modules.storage.service;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.dao.IStorageDataFileRepository;
import fr.cnes.regards.modules.storage.domain.database.DataStorageType;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.PluginConfUpdatable;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@RegardsTransactional
public class PrioritizedDataStorageService implements IPrioritizedDataStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(PrioritizedDataStorageService.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

    @Autowired
    private IStorageDataFileRepository storageDataFileRepository;

    @Autowired
    private EntityManager em;

    @Override
    public PrioritizedDataStorage create(PluginConfiguration toBeCreated) throws ModuleException {
        PluginConfiguration dataStorageConf = pluginService.savePluginConfiguration(toBeCreated);
        DataStorageType dataStorageType;
        if (dataStorageConf.getInterfaceNames().contains(IOnlineDataStorage.class.getName())) {
            dataStorageType = DataStorageType.ONLINE;
        } else if (dataStorageConf.getInterfaceNames().contains(INearlineDataStorage.class.getName())) {
            dataStorageType = DataStorageType.NEARLINE;
        } else {
            throw new EntityInvalidException(String.format(
                    "Given plugin configuration(label: %s) is not a configuration for an online or nearline data storage (respectfully %s or %s)!",
                    dataStorageConf.getLabel(),
                    IOnlineDataStorage.class.getName(),
                    INearlineDataStorage.class.getName()));
        }
        Long actualLowestPriority = getLowestPriority(dataStorageType);
        return prioritizedDataStorageRepository.save(new PrioritizedDataStorage(dataStorageConf,
                                                                                actualLowestPriority == null ?
                                                                                        0 :
                                                                                        actualLowestPriority + 1,
                                                                                dataStorageType));
    }

    @Override
    public List<PrioritizedDataStorage> findAllByType(DataStorageType type) {
        return prioritizedDataStorageRepository.findAllByDataStorageTypeOrderByPriorityAsc(type);
    }

    @Override
    public Long getLowestPriority(DataStorageType dataStorageType) {
        PrioritizedDataStorage lowestPrioritizedDataStorage = prioritizedDataStorageRepository
                .findFirstByDataStorageTypeOrderByPriorityDesc(dataStorageType);
        if (lowestPrioritizedDataStorage == null) {
            // in case there is no one yet, lets give it the highest priority
            return null;
        }
        return lowestPrioritizedDataStorage.getPriority();
    }

    @Override
    @Nullable
    public PrioritizedDataStorage getFirstActiveByType(DataStorageType dataStorageType) {
        return prioritizedDataStorageRepository
                .findFirstByDataStorageTypeAndDataStorageConfigurationActiveOrderByPriorityAsc(dataStorageType, true);
    }

    @Override
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

    @Override
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

    @Override
    public PrioritizedDataStorage retrieve(Long id) throws EntityNotFoundException {
        PrioritizedDataStorage actual = prioritizedDataStorageRepository.findOne(id);
        if (actual == null) {
            throw new EntityNotFoundException(id, PrioritizedDataStorage.class);
        }
        return actual;
    }

    @Override
    public PrioritizedDataStorage update(Long id, PrioritizedDataStorage updated) throws ModuleException {
        PrioritizedDataStorage oldOne = retrieve(id);
        if (!id.equals(updated.getId())) {
            throw new EntityInconsistentIdentifierException(id, updated.getId(), PrioritizedDataStorage.class);
        }

        PluginConfUpdatable updatable = null;

        if (oldOne.getDataStorageConfiguration().isActive()) {
            // Count number of files stored by the plugin configuration
            Long nbfilesAlreadyStored = storageDataFileRepository.countByPrioritizedDataStoragesId(id);

            // Ask plugin if the update is allowed
            IDataStorage<?> plugin = pluginService.getPlugin(oldOne.getDataStorageConfiguration().getId());
            updatable = plugin.allowConfigurationUpdate(updated.getDataStorageConfiguration(),
                                                        oldOne.getDataStorageConfiguration(),
                                                        nbfilesAlreadyStored > 0);
        }

        if (!oldOne.getDataStorageConfiguration().isActive() || updatable.isUpdateAllowed()) {
            PluginConfiguration updatedConf = pluginService
                    .updatePluginConfiguration(updated.getDataStorageConfiguration());
            oldOne.setDataStorageConfiguration(updatedConf);
            return prioritizedDataStorageRepository.save(oldOne);
        } else {
            throw new EntityOperationForbiddenException(oldOne.getDataStorageConfiguration().getLabel(),
                                                        PrioritizedDataStorage.class,
                                                        updatable.getUpdateNotAllowedReason());
        }
    }

    @Override
    public void delete(Long pluginConfId) throws ModuleException {
        Optional<PrioritizedDataStorage> toDeleteOpt = prioritizedDataStorageRepository.findOneById(pluginConfId);
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
                prioritizedDataStorageRepository.save(lessPrioritizeds);
            } else {
                String msg = String.format("Data storage %s could not be deleted because it contains files",
                                           toDelete.getDataStorageConfiguration().getLabel());
                LOG.info(msg);
                throw new EntityOperationForbiddenException(msg);
            }
        }
    }

    @Override
    public boolean canDelete(PrioritizedDataStorage prioritizedDataStorage) {
        return storageDataFileRepository.findTopByPrioritizedDataStoragesId(prioritizedDataStorage.getId()) == null;
    }
}
