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
    ERROR;

    @Override
    public String toString() {
        return this.name();
    }
}
