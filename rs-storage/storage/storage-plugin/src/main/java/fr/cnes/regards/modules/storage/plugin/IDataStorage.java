/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin;

import java.util.Set;

import com.google.common.collect.Multimap;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.DataFile;

/**
 * Plugin interface for all storage systems.
 *
 * @author Sylvain Vissiere-Guerinet
 * @authot Sébastien Binda
 */
@PluginInterface(description = "Contract to respect by any data storage plugin")
public interface IDataStorage<T extends IWorkingSubset> {

    /**
     * Allow plugins to prepare data before actually doing the storage action
     * @param aips {@link Multimap} keys are {@link AIP}s to handle, values are the{@link DataFile} to transfer for the {@link AIP}
     * @return {@link Set} of Workingset containing plugin information needed for each file to transfert
     */
    Set<T> prepare(Multimap<AIP, Set<DataFile>> aips);

    /**
     * Do the store action for the given {@link T} working subset.
     * @param workingSubset Subset of files to store.
     * @param replaceMode if file exists, to the store action should replace it ?
     * @param progressManager {@link ProgressManager} object to inform global store process after each transfer succeed or fail.
     */
    void store(T workingSubset, Boolean replaceMode, ProgressManager progressManager);

    /**
     * Do the retreive action for the given {@link T} working subset.
     * @param workingSubset Subset of files to store.
     * @param progressManager {@link ProgressManager} object to inform global store process after each transfer succeed or fail.
     */
    void retrieve(T workingSubset, ProgressManager progressManager);

    /**
     * Do the delete action for the given {@link T} working subset.
     * @param workingSubset Subset of files to store.
     * @param progressManager {@link ProgressManager} object to inform global store process after each deletion succeed or fail.
     */
    void delete(T workingSubset, ProgressManager progressManager);

    /**
     * Retreive informations about the storage system.
     * @return {@link Set} of {@link DataStorageInfo} containing storage informations
     */
    Set<DataStorageInfo> getMonitoringInfos();

    /**
     * Return the {@link DataStorageType} of the current plugin.
     * NEARLINE : Low latency files access. Asynchronous access to files
     * INLINE : High latency files access. Synchronous access to files
     * @return {@link DataStorageType}
     */
    DataStorageType getType();
}
