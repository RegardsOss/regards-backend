/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain.plugin;

/**
 * Access mode to a IDataStorage plugin.
 * @author Sébastien Binda
 */
public enum DataStorageAccessModeEnum {

    /**
     * Data storage access to store new files.
     */
    STORE_MODE,

    /**
     * Data storage access to retrieve files.
     */
    RETRIEVE_MODE;

}
