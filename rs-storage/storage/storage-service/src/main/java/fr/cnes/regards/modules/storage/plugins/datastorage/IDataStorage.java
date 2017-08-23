/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugins.datastorage;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.plugins.datastorage.domain.DataStorageInfo;
import fr.cnes.regards.modules.storage.plugins.datastorage.domain.DataStorageType;
import fr.cnes.regards.modules.storage.plugins.datastorage.exception.StorageCorruptedException;

/**
 * TODO: add something to get the storage progress
 *
 * @author Sylvain Vissiere-Guerinet
 */
@PluginInterface(description = "Contract to respect by any data storage plugin")
public interface IDataStorage {

    Integer DEFAULT_PRIORITY = 0;

    String BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME = "destination";

    /**
     * This method should be considered as if it was a static one. That means its return must be some sort of constant.
     * @return {@link DataStorageType} of the plugin
     */
    DataStorageType getType();

    /**
     *
     * This method implementation handle the storage of an AIP on a storage unit(local file system, ftp, ...).
     * It is of the implementation responsibility to ensure that the storage has not been corrupted by any kind of transfer.
     *
     * @param pAip
     *            {@link AIP} to save
     * @throws IOException
     * @throws StorageCorruptedException when the transfer to the final storage location corrupted the file.
     */
    AIP storeMetadata(AIP pAip) throws IOException, StorageCorruptedException;

    /**
     * TODO: change signature to storeDataFile(DataFile)
     * @param pAip
     *            {@link AIP} whose file are to be saved
     * @return JobId of the job handling the storage of all files from an AIP
     */
    UUID storeAIPDataFiles(AIP pAip);

    /**
     * TODO: add retrieveDataFile
     * @param pAip
     *            {@link AIP} to retrieve
     * @return JobId of the job handling the retrieve
     */
    UUID retrieveMetadata(AIP pAip);

    /**
     * TODO: change to deleteMetadata and create deleteDataFile
     * @param pAip
     *            {@link AIP} to delete
     * @return JobId of the job handling the delete
     */
    UUID deleteAIP(AIP pAip);

    /**
     * @param pProject
     *            project on which informations are wanted
     * @return monitoring information of the storage used
     */
    DataStorageInfo getInfo(String pProject);

    void checkIntegrity(AIP aip) throws NoSuchAlgorithmException, IOException;
}
