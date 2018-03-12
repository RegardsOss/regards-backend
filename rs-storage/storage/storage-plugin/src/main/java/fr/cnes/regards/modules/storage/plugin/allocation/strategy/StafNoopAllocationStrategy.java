/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.allocation.strategy;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.staf.domain.STAFArchive;
import fr.cnes.regards.framework.staf.protocol.STAFURLFactory;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.staf.STAFDataStorage;

/**
 * Allocation Strategy that analyse the {@link StorageDataFile} url to determine which {@link IDataStorage} should handle it:
 * <ul>
 *     <li>in case the url protocol is staf: extract the staf archive name and maps it to the corresponding {@link STAFDataStorage} configuration.</li>
 *     <li>otherwise, maps it to the provided "default" {@link IDataStorage}</li>
 * </ul>
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(author = "REGARDS Team",
        description = "Allocation Strategy plugin that analyse files url to determine which staf archive should be used",
        id = "StafNoopAllocationStrategy", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3",
        owner = "CNES", url = "https://regardsoss.github.io/")
public class StafNoopAllocationStrategy implements IAllocationStrategy {

    /**
     * Plugin parameter name for the default data storage plugin configuration id
     */
    public static final String DEFAULT_DATA_STORAGE_CONFIGURATION_ID = "Default_data_storage_configuration_id";

    public static final String QUICKLOOK_DATA_STORAGE_CONFIGURATION_ID = "Quicklook_data_storage_configuration_id";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(StafNoopAllocationStrategy.class);

    /**
     * {@link Gson} instance
     */
    @Autowired
    private Gson gson;

    /**
     * {@link IPluginService} instance
     */
    @Autowired
    private IPluginService pluginService;

    /**
     * Default data storage to use if the file to retrieve is not in the staf
     */
    @PluginParameter(name = DEFAULT_DATA_STORAGE_CONFIGURATION_ID,
            description = "Default data storage to use if the file to retrieve is not in the staf",
            label = "Default data storage configuration id")
    private Long dataStorageConfigurationId;

    @PluginParameter(name = QUICKLOOK_DATA_STORAGE_CONFIGURATION_ID,
            description = "Data storage to use if the file is a quicklook, must be an ONLINE data storage",
            label = "Quicklook data storage configuration id")
    private Long quicklookDataStorageConfigurationId;

    @PluginInit
    public void init() throws EntityNotFoundException, EntityInvalidException {
        //lets verify that quicklook data storage is an online data storage
        if (!pluginService.getPluginConfiguration(quicklookDataStorageConfigurationId).getInterfaceNames()
                .contains(IOnlineDataStorage.class.getName())) {
            throw new EntityInvalidException(
                    "Current active allocation strategy does specify a quicklook data storage which is not ONLINE");
        }
    }

    @Override
    public Multimap<Long, StorageDataFile> dispatch(Collection<StorageDataFile> dataFilesToHandle) {
        Multimap<Long, StorageDataFile> dispatch = HashMultimap.create();
        // Lets prepare the different staf configuration we have by staf archive name
        // First we get the Nearline type
        List<PluginConfiguration> nearlineDataStorages = pluginService
                .getPluginConfigurationsByType(INearlineDataStorage.class);
        // Now we extract the STAFDataStorage and map them to a map archiveName->id
        // @formatter:off
        Map<String, Long> stafArchiveConfMap = nearlineDataStorages.stream()
                .filter(conf -> conf.getPluginClassName().equals(STAFDataStorage.class.getName()))
                .collect(Collectors.toMap(stafConf -> gson.fromJson(stafConf.getParameter(STAFDataStorage.STAF_ARCHIVE_PARAMETER_NAME)
                                                                            .getValue(),STAFArchive.class).getArchiveName(),
                                          stafConf -> stafConf.getId()));
        // @formatter:on
        for (StorageDataFile dataFile : dataFilesToHandle) {
            for (URL dataFileUrl : dataFile.getUrls()) {
                String urlProtocol = dataFileUrl.getProtocol();
                switch (urlProtocol) {
                    case STAFURLFactory.STAF_URL_PROTOCOLE:
                        String archiveName = dataFileUrl.getHost();
                        Long chosenOne = stafArchiveConfMap.get(archiveName);
                        if (chosenOne == null) {
                            LOG.debug(String.format(
                                                    "Allocation strategy for data file %s failed, no corresponding staf data storage found.",
                                                    dataFile.getUrls()));
                        } else {
                            //This allocation strategy only allows files to be stored into 1 DataStorage
                            dataFile.increaseNotYetStoredBy();
                            dispatch.put(chosenOne, dataFile);
                        }
                        if (dataFile.isOnlineMandatory()) {
                            dataFile.increaseNotYetStoredBy();
                            dispatch.put(quicklookDataStorageConfigurationId, dataFile);
                        }
                        break;
                    default:
                        if (dataFile.isOnlineMandatory()) {
                            //This allocation strategy only allows files to be stored into 1 DataStorage
                            dataFile.setNotYetStoredBy(1L);
                            dispatch.put(quicklookDataStorageConfigurationId, dataFile);
                        } else {
                            //This allocation strategy only allows files to be stored into 1 DataStorage
                            dataFile.setNotYetStoredBy(1L);
                            dispatch.put(dataStorageConfigurationId, dataFile);
                        }
                        break;
                }
            }
        }
        return dispatch;
    }
}
