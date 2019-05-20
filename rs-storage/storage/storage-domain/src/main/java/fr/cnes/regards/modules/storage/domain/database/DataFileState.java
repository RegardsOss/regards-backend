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
    PARTIAL_DELETION_PENDING,
    DELETION_ERROR,
    /**
     * After an update, we have to delete metadata. This deletion is particular because the old metadata file
     * stayed on the archives and now it is to be deleted without checking REGARDS permissions. This kind of deletion
     * is managed by a different job from classical one.
     */
    TO_BE_DELETED_AFTER_UPDATE;

    @Override
    public String toString() {
        return this.name();
    }
}
