package fr.cnes.regards.modules.storagelight.domain.database;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public enum CachedFileState {

    /**
     * File is waiting for available free space in cache to be restored
     */
    QUEUED,

    /**
     * File is being restored
     */
    RESTORING,

    /**
     * File is available in cache
     */
    AVAILABLE

}
