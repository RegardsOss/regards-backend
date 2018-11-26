package fr.cnes.regards.modules.storage.domain.database;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public enum DataFileState {
    /**
     * has been scheduled for storage
     */
    PENDING,
    /**
     * storage process ended, successfully or not
     */
    STORED,
    /**
     * Error during storage process
     */
    ERROR,

    /**
     * Data file is waiting to be handle for deletion
     */
    TO_BE_DELETED,

    /**
     * Data file deletion is scheduled on all data storages
     */
    DELETION_PENDING,
    /**
     * Data file deletion is scheduled on some data storages
     */
    PARTIAL_DELETION_PENDING;

    @Override
    public String toString() {
        return this.name();
    }
}
