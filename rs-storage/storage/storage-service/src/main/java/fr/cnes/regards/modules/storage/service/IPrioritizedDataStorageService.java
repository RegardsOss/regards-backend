package fr.cnes.regards.modules.storage.service;

import java.util.List;

import javax.annotation.Nullable;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.storage.domain.database.DataStorageType;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IPrioritizedDataStorageService {

    /**
     * Create a prioritized data storage thanks to a IDataStorage plugin configuration
     * @param toBeCreated
     * @return created prioritized data storage
     * @throws ModuleException
     */
    PrioritizedDataStorage create(PluginConfiguration toBeCreated) throws ModuleException;

    /**
     * Retrieve all prioritized data storages of a type ordered by priority
     * @param type type of data storages to retrieve
     * @return all prioritized data storages of a type ordered by priority
     */
    List<PrioritizedDataStorage> findAllByType(DataStorageType type);

    /**
     * retrieve the lowest priority for a given type
     * @param dataStorageType {@link DataStorageType}
     * @return the lowest priority for the given type, null if no data storage of this type has been created
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
     * @throws ModuleException
     */
    void delete(Long pluginConfId) throws ModuleException;

    /**
     * Increase the given data storage priority
     * @param prioritizedDataStorageId data storage id
     * @throws EntityNotFoundException
     */
    void increasePriority(Long prioritizedDataStorageId) throws EntityNotFoundException;

    /**
     * Decrease the given data storage priority
     * @param prioritizedDataStorageId data storage id
     * @throws EntityNotFoundException
     */
    void decreasePriority(Long prioritizedDataStorageId) throws EntityNotFoundException;

    /**
     * Retrieve a prioritized data storage
     * @param id
     * @return retrieved prioritized data storage
     * @throws EntityNotFoundException
     */
    PrioritizedDataStorage retrieve(Long id) throws EntityNotFoundException;

    /**
     * Only update the subsequent plugin configuration of the prioritized data storage
     * @param id data storage id
     * @param updated updated data storage from the client
     * @return updated data storage
     * @throws ModuleException
     */
    PrioritizedDataStorage update(Long id, PrioritizedDataStorage updated) throws ModuleException;

    /**
     * Determine whether a prioritized data storage can be deleted or not
     * @param prioritizedDataStorage prioritized data storage to be checked
     * @return if it can be deleted
     */
    boolean canDelete(PrioritizedDataStorage prioritizedDataStorage);
}
