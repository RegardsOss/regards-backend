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
package fr.cnes.regards.modules.storage.domain.plugin;

import java.util.Collection;
import java.util.Map;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * Plugin interface for all storage systems.
 * <b>DO NOT IMPLEMENT DIRECTLY BY CLASSES</b><br/>
 * <b>Use one of its children for implementations so priority can be handled properly when restoring files.</b>
 *
 * @author Sylvain Vissiere-Guerinet
 * @authot Sébastien Binda
 */
@PluginInterface(description = "Contract to respect by any data storage plugin")
public interface IDataStorage<T extends IWorkingSubset> {

    /**
     * Generate all working subsets divided by archiving mode {@link DataStorageAccessModeEnum}
     * @param dataFiles {@link Collection} of {@link StorageDataFile} to dispatch
     * @param mode {@link DataStorageAccessModeEnum}
     * @return {@link WorkingSubsetWrapper} containing all working subsets
     */
    WorkingSubsetWrapper<T> prepare(Collection<StorageDataFile> dataFiles, DataStorageAccessModeEnum mode);

    /**
     * Do the delete action for the given working subset. It is called "safe" because it checks
     * if deletion is permitted by the configuration.
     * @param workingSubset Subset of files to delete.
     * @param progressManager {@link IProgressManager} object to inform global store process after each deletion succeed or fail.
     * @throws IllegalStateException if this operation is forbidden due to the plugin configuration
     */
    default void safeDelete(T workingSubset, IProgressManager progressManager) {
        if (canDelete()) {
            delete(workingSubset, progressManager);
        } else {
            throw new IllegalStateException("Deletion is currently forbidden for this plugin!");
        }
    }

    /**
     * @return whether the deletion action is allowed on this data storage
     */
    boolean canDelete();

    /**
     * Do the delete action for the given working subset without checking if deletion is permitted by the configuration.
     * @param workingSubset Subset of files to delete.
     * @param progressManager {@link IProgressManager} object to inform global store process after each deletion succeed or fail.
     */
    void delete(T workingSubset, IProgressManager progressManager);

    /**
     * Do the store action for the given working subset.
     * @param workingSubset Subset of files to store.
     * @param replaceMode if file exists, to the store action should replace it ?
     * @param progressManager {@link IProgressManager} object to inform global store process after each transfer succeed or fail.
     */
    void store(T workingSubset, Boolean replaceMode, IProgressManager progressManager);

    /**
     * @return the total space allocated to this data storage
     */
    Long getTotalSpace();

    /**
     * Method called before each configuration update of this plugin to know if the modification is allowed or not.
     * The plugin implementation of this method should ensure that already stored files will always be accessible after
     * the modification.
     * @param newConfiguration {@link PluginConfiguration} with the new parameters for update
     * @param currentConfiguration {@link PluginConfiguration} with the current parameters before update.
     * @param filesAlreadyStored {@link boolean} Does files has been already stored with the current configuration ?
     * @return {@link PluginConfUpdatable} true if the plugin allows the modification. If not updatable contains the rejection cause
     */
    PluginConfUpdatable allowConfigurationUpdate(PluginConfiguration newConfiguration,
            PluginConfiguration currentConfiguration, boolean filesAlreadyStored);

    /**
     * Return type being a {@link Map}, you can add whatever information. Plugin configuration information are added on service level
     *
     * @return debug information specific to plugin implementation
     */
    Map<String, Object> getDiagnosticInfo();
}
