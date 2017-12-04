package fr.cnes.regards.modules.storage.plugin.allocation.strategy;

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
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.staf.domain.STAFArchive;
import fr.cnes.regards.framework.staf.protocol.STAFURLFactory;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.datastorage.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.INearlineDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.staf.STAFDataStorage;

/**
 * Allocation Strategy that analyse the {@link DataFile} url to determine which {@link IDataStorage} should handle it:
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
            description = "Default data storage to use if the file to retrieve is not in the staf", label = "Default data storage configuration id")
    private Long dataStorageConfigurationId;

    @Override
    public Multimap<Long, DataFile> dispatch(Collection<DataFile> dataFilesToHandle) {
        Multimap<Long, DataFile> dispatch = HashMultimap.create();
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
        for (DataFile dataFile : dataFilesToHandle) {
            String urlProtocol = dataFile.getUrl().getProtocol();
            switch (urlProtocol) {
                case STAFURLFactory.STAF_URL_PROTOCOLE:
                    String archiveName = dataFile.getUrl().getHost();
                    Long chosenOne = stafArchiveConfMap.get(archiveName);
                    if (chosenOne == null) {
                        LOG.debug(String.format(
                                "Allocation strategy for data file %s failed, no corresponding staf data storage found.",
                                dataFile.getUrl()));
                    } else {
                        dispatch.put(chosenOne, dataFile);
                    }
                    break;
                default:
                    dispatch.put(dataStorageConfigurationId, dataFile);
                    break;
            }
        }
        return dispatch;
    }
}
