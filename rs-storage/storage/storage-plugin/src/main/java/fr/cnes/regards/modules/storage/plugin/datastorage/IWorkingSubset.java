/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.datastorage;

import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.DataFile;

/**
 * Represents a subset of {@link DataFile} prepared by {@link INearlineDataStorage} plugins. <br>
 * Only the implementation of the plugin can dispatch storage action by bucket of {@link DataFile} to handle.<br>
 * AIP service uses those subsets to run an asynchronous storage job for each one.
 * @author Sébastien Binda
 */
@FunctionalInterface
public interface IWorkingSubset {

    /**
     * Return the subset of {@link DataFile}s to handle.
     * @return {@link Set}<{@link DataFile}>
     */
    Set<DataFile> getDataFiles();

}
