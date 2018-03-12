/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain.plugin;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * Represents a subset of {@link StorageDataFile} prepared by {@link INearlineDataStorage} plugins. <br>
 * Only the implementation of the plugin can dispatch storage action by bucket of {@link StorageDataFile} to handle.<br>
 * AIP service uses those subsets to run an asynchronous storage job for each one.
 * @author Sébastien Binda
 */
public interface IWorkingSubset {

    /**
     * Return the subset of {@link StorageDataFile}s to handle.
     * @return {@link Set}<{@link StorageDataFile}>
     */
    Set<StorageDataFile> getDataFiles();

}
