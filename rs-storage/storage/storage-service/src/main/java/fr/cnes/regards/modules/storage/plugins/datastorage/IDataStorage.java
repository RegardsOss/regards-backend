/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugins.datastorage;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.plugins.datastorage.domain.DataStorageInfo;
import fr.cnes.regards.modules.storage.plugins.datastorage.domain.DataStorageType;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@PluginInterface(description = "Contract to respect by any data storage plugin")
public interface IDataStorage {

    /**
     * @return {@link DataStorageType} of the plugin
     */
    DataStorageType getType();

    /**
     *
     * @param pAip
     *            {@link AIP} to save
     * @return JobId of the job handling the storage of the descriptor
     */
    Long storeAIPDescriptor(AIP pAip);

    /**
     *
     * @param pAip
     *            {@link AIP} whose file are to be saved
     * @return JobId of the job handling the storage of all files from an AIP
     */
    Long storeAIPFiles(AIP pAip);

    /**
     *
     * @param pAip
     *            {@link AIP} to retrieve
     * @return JobId of the job handling the retrieve
     */
    Long retrieveAIP(AIP pAip);

    /**
     *
     * @param pAip
     *            {@link AIP} to delete
     * @return JobId of the job handling the delete
     */
    Long deleteAIP(AIP pAip);

    /**
     * @param pProject
     *            project on which informations are wanted
     * @return monitoring information of the storage used
     */
    DataStorageInfo getInfo(String pProject);
}
