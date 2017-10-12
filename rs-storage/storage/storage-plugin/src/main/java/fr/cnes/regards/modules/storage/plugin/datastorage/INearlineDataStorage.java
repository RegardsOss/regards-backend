/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.datastorage;

import java.nio.file.Path;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface INearlineDataStorage<T extends IWorkingSubset> extends IDataStorage<T> {

    /**
     * Do the retreive action for the given {@link T} working subset.
     * @param workingSubset Subset of files to store.
     * @param destinationPath {@link Path} where to put retrieved files.
     * @param progressManager {@link IProgressManager} object to inform global store process after each transfer succeed or fail.
     */
    void retrieve(T workingSubset, Path destinationPath, IProgressManager progressManager);

}
