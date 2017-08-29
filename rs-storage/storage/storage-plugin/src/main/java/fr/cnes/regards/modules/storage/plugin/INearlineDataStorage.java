package fr.cnes.regards.modules.storage.plugin;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface INearlineDataStorage<T extends IWorkingSubset> extends IDataStorage<T> {

    /**
     * Do the retreive action for the given {@link T} working subset.
     * @param workingSubset Subset of files to store.
     * @param progressManager {@link ProgressManager} object to inform global store process after each transfer succeed or fail.
     */
    void retrieve(T workingSubset, ProgressManager progressManager);

}
