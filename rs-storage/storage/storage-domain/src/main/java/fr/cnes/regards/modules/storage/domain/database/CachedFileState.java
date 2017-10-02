package fr.cnes.regards.modules.storage.domain.database;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public enum CachedFileState {

    /**
     * File is waiting for available free space in cache to be restored
     */
    QUEUED,

    /**
     * File is restoring
     */
    RESTORING,

    /**
     * File is available in cache
     */
    AVAILABLE

}
