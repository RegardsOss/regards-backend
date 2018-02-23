package fr.cnes.regards.modules.storage.service;

import javax.annotation.Nullable;

import com.google.common.collect.Multimap;
import fr.cnes.regards.modules.storage.domain.database.DataStorageType;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IProritizedDataStorageService {

    /**
     * Retrieve all prioritized data storages split per type ordered by priority
     * @return all prioritized data storages split per type ordered by priority
     */
    Multimap<DataStorageType, PrioritizedDataStorage> findAllByTypes();

    /**
     * retrieve the lowest priority for a given type
     * @param dataStorageType {@link DataStorageType}
     * @return the lowest priority for the given type
     */
    Long getLowestPriority(DataStorageType dataStorageType);

    /**
     * Retrieve the data storage with the highest priority and which is active
     * @param dataStorageType
     * @return the data storage with the highest priority and which is active , null if none is found
     */
    @Nullable
    PrioritizedDataStorage getFirstActiveByType(DataStorageType dataStorageType);

    /**
     * Deletes the given
     * @param pluginConfId
     */
    void delete(Long pluginConfId);

    void increasePriority(Long prioritizedDataStorageId);

    void decreasePriority(Long prioritizedDataStorageId);
}
