package fr.cnes.regards.modules.storage.service;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginConfEvent;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.domain.database.DataStorageType;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@RegardsTransactional
public class PrioritizedDataStorageService
        implements IProritizedDataStorageService, ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IPrioritizedDataStorageRepository prioritizedDataStorageRepository;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(PluginConfEvent.class, new PluginConfEventHandler());
    }

    private PrioritizedDataStorage create(Long pluginConfId) {
        PluginConfiguration dataStorageConf;
        try {
            dataStorageConf = pluginService.getPluginConfiguration(pluginConfId);
        } catch (EntityNotFoundException e) {
            throw new RsRuntimeException(e);
        }
        DataStorageType dataStorageType;
        if (dataStorageConf.getInterfaceNames().contains(IOnlineDataStorage.class.getName())) {
            dataStorageType = DataStorageType.ONLINE;
        } else if (dataStorageConf.getInterfaceNames().contains(INearlineDataStorage.class.getName())) {
            dataStorageType = DataStorageType.NEARLINE;
        } else {
            throw new IllegalArgumentException(String.format(
                    "Given plugin configuration(id: %s, label: %s) is not a configuration for an online or nearline data storage (respectfully %s or %s)!",
                    dataStorageConf.getId().toString(),
                    dataStorageConf.getLabel(),
                    IOnlineDataStorage.class.getName(),
                    INearlineDataStorage.class.getName()));
        }
        Long lowestPriority = getLowestPriority(dataStorageType);
        return prioritizedDataStorageRepository
                .save(new PrioritizedDataStorage(dataStorageConf, lowestPriority, dataStorageType));
    }

    public Multimap<DataStorageType, PrioritizedDataStorage> findAllByTypes() {
        Multimap<DataStorageType, PrioritizedDataStorage> result = ArrayListMultimap.create();
        for (DataStorageType dataStorageType : DataStorageType.values()) {
            result.putAll(dataStorageType,
                          prioritizedDataStorageRepository.findAllByDataStorageTypeOrderByPriorityAsc(dataStorageType));
        }
        return result;
    }

    public Long getLowestPriority(DataStorageType dataStorageType) {
        PrioritizedDataStorage lowestPrioritizedDataStorage = prioritizedDataStorageRepository
                .findFirstByDataStorageTypeOrderByPriorityDesc(dataStorageType);
        if (lowestPrioritizedDataStorage == null) {
            //in case there is no one yet, lets give it the highest priority
            return PrioritizedDataStorage.HIGHEST_PRIORITY;
        }
        return lowestPrioritizedDataStorage.getPriority();
    }

    @Nullable
    public PrioritizedDataStorage getFirstActiveByType(DataStorageType dataStorageType) {
        PrioritizedDataStorage prioritizedDataStorage = prioritizedDataStorageRepository
                .findFirstByDataStorageTypeByDataStorageConfigurationActiveOrderByPriorityAsc(dataStorageType, true);
        return prioritizedDataStorage;
    }

    public void delete(Long pluginConfId) {
        Optional<PrioritizedDataStorage> toDeleteOpt = prioritizedDataStorageRepository
                .findOneByDataStorageConfigurationId(pluginConfId);
        if (toDeleteOpt.isPresent()) {
            //first we need to increase all the priorities of those which are less prioritized than the one to delete
            PrioritizedDataStorage toDelete = toDeleteOpt.get();
            Set<PrioritizedDataStorage> lessPrioritizeds = prioritizedDataStorageRepository
                    .findAllByDataStorageTypeAndGreaterPriorityThan(toDelete.getDataStorageType(),
                                                                    toDelete.getPriority());
            prioritizedDataStorageRepository.delete(toDelete);
            for (PrioritizedDataStorage lessPrioritized : lessPrioritizeds) {
                lessPrioritized.setPriority(lessPrioritized.getPriority() - 1);
            }
            prioritizedDataStorageRepository.save(lessPrioritizeds);
        }
    }

    private class PluginConfEventHandler implements IHandler<PluginConfEvent> {

        @Override
        public void handle(TenantWrapper<PluginConfEvent> wrapper) {
            runtimeTenantResolver.forceTenant(wrapper.getTenant());
            PluginConfEvent event = wrapper.getContent();
            //here we only care for IDataStorage configurations
            if (event.getPluginTypes().contains(IDataStorage.class.getName())) {
                switch (event.getAction()) {
                    case CREATE:
                        create(event.getPluginConfId());
                        break;
                    case DELETE:
                        delete(event.getPluginConfId());
                        break;
                    default:
                        break;
                }
            }
        }

    }
}
