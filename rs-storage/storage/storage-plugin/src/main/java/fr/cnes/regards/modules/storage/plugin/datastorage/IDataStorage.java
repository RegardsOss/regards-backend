/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.datastorage;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
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
     * @param dataFiles {@link DataFile}s to transfer
     * @param {@link DataStorageAccessModeEnum} STORE or RESTORE
     * @return {@link Set} of Workingset containing plugin information needed for each file to transfert
     */
    Set<T> prepare(Collection<DataFile> dataFiles, DataStorageAccessModeEnum mode);

    /**
     * Do the delete action for the given {@link T} working subset. It is called "safe" because it checks if deletion is permitted by the configuration.
     * @throws IllegalStateException if this operation is forbidden due to the plugin configuration
     */
    default void safeDelete(Set<DataFile> dataFiles, IProgressManager progressManager) {
        if (canDelete()) {
            delete(dataFiles, progressManager);
        } else {
            throw new IllegalStateException("Deletion is currently forbidden for this plugin!");
        }
    }

    boolean canDelete();

    /**
     * Do the delete action for the given {@link T} working subset without checking if deletion is permitted by the configuration.
     * @param dataFiles Set of files to store.
     * @param progressManager {@link IProgressManager} object to inform global store process after each deletion succeed or fail.
     */
    void delete(Set<DataFile> dataFiles, IProgressManager progressManager);

    /**
     * Do the store action for the given {@link T} working subset.
     * @param workingSubset Subset of files to store.
     * @param replaceMode if file exists, to the store action should replace it ?
     * @param progressManager {@link IProgressManager} object to inform global store process after each transfer succeed or fail.
     */
    void store(T workingSubset, Boolean replaceMode, IProgressManager progressManager);

    Long getTotalSpace();
}
