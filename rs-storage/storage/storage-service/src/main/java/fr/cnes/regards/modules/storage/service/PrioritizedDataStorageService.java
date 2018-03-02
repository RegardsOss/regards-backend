package fr.cnes.regards.modules.storage.service;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.domain.database.DataStorageType;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@RegardsTransactional
public class PrioritizedDataStorageService implements IProritizedDataStorageService {

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

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
        return prioritizedDataStorageRepository
                .save(new PrioritizedDataStorage(dataStorageConf, actualLowestPriority + 1, dataStorageType));
    }

    @Override
    public Multimap<DataStorageType, PrioritizedDataStorage> findAllByTypes() {
        Multimap<DataStorageType, PrioritizedDataStorage> result = ArrayListMultimap.create();
        for (DataStorageType dataStorageType : DataStorageType.values()) {
            result.putAll(dataStorageType,
                          prioritizedDataStorageRepository.findAllByDataStorageTypeOrderByPriorityAsc(dataStorageType));
        }
        return result;
    }

    @Override
    public Long getLowestPriority(DataStorageType dataStorageType) {
        PrioritizedDataStorage lowestPrioritizedDataStorage = prioritizedDataStorageRepository
                .findFirstByDataStorageTypeOrderByPriorityDesc(dataStorageType);
        if (lowestPrioritizedDataStorage == null) {
            //in case there is no one yet, lets give it the highest priority
            return PrioritizedDataStorage.HIGHEST_PRIORITY;
        }
        return lowestPrioritizedDataStorage.getPriority();
    }

    @Override
    @Nullable
    public PrioritizedDataStorage getFirstActiveByType(DataStorageType dataStorageType) {
        PrioritizedDataStorage prioritizedDataStorage = prioritizedDataStorageRepository
                .findFirstByDataStorageTypeAndDataStorageConfigurationActiveOrderByPriorityAsc(dataStorageType, true);
        return prioritizedDataStorage;
    }

    @Override
    public void delete(Long pluginConfId) {
        Optional<PrioritizedDataStorage> toDeleteOpt = prioritizedDataStorageRepository
                .findOneByDataStorageConfigurationId(pluginConfId);
        if (toDeleteOpt.isPresent()) {
            //first we need to increase all the priorities of those which are less prioritized than the one to delete
            PrioritizedDataStorage toDelete = toDeleteOpt.get();
            Set<PrioritizedDataStorage> lessPrioritizeds = prioritizedDataStorageRepository
                    .findAllByDataStorageTypeAndPriorityGreaterThanOrderByPriorityAsc(toDelete.getDataStorageType(),
                                                                                      toDelete.getPriority());
            prioritizedDataStorageRepository.delete(toDelete);
            for (PrioritizedDataStorage lessPrioritized : lessPrioritizeds) {
                lessPrioritized.setPriority(lessPrioritized.getPriority() - 1);
            }
            prioritizedDataStorageRepository.save(lessPrioritizeds);
        }
    }

    @Override
    public void increasePriority(Long prioritizedDataStorageId) {
        PrioritizedDataStorage actual = prioritizedDataStorageRepository.findOne(prioritizedDataStorageId);
        PrioritizedDataStorage other = prioritizedDataStorageRepository
                .findOneByDataStorageTypeAndPriority(actual.getDataStorageType(), actual.getPriority() - 1);
        other.setPriority(actual.getPriority());
        actual.setPriority(actual.getPriority() - 1);
        prioritizedDataStorageRepository.save(Sets.newHashSet(other, actual));
    }

    @Override
    public void decreasePriority(Long prioritizedDataStorageId) {
        PrioritizedDataStorage actual = prioritizedDataStorageRepository.findOne(prioritizedDataStorageId);
        PrioritizedDataStorage other = prioritizedDataStorageRepository
                .findOneByDataStorageTypeAndPriority(actual.getDataStorageType(), actual.getPriority() + 1);
        other.setPriority(actual.getPriority());
        actual.setPriority(actual.getPriority() + 1);
        prioritizedDataStorageRepository.save(Sets.newHashSet(other, actual));
    }
}
