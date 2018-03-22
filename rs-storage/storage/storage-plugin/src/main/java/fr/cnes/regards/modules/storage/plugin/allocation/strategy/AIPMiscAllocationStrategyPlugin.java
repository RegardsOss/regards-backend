/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.allocation.strategy;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.adapter.InformationPackageMap;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;

/**
 * This plugin dispatch files for storage by reading informations from AIP.misc.storage property. <br/>
 * Expected AIP information format : <br/>
 * <pre>
 * {
 *  ...
 *  "miscInformation":{
 *      "storage": [
 *          {
 *              "pluginId": "<{@link PluginMetaData#getPluginId()}>",
 *              "directory": "/directory/wher/to/store/files/from/this/aip"
 *          },
 *          ...
 *      ]
 *  }
 *  </pre>
 * @author Sébastien Binda
 *
 */
@Plugin(author = "REGARDS Team",
        description = "Define where to store files by reading informations from the misc information of supplied AIPs",
        id = "AIPMiscStrategy", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class AIPMiscAllocationStrategyPlugin implements IAllocationStrategy {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AIPMiscAllocationStrategyPlugin.class);

    /**
     * Plugin parameter name of the storage base location as a string
     */
    public static final String MAP_PLUGINID_PLUGINCONFID_PARAMETER = "mapPluginConfIdForPluginId";

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
     * Plugin parameter to define plugin configuration(s) to use for each plugin id. <br/>
     * Exemple : <br/>
     * LocalDataStorage -> [1,52] means that if a file need to be stored with a LocalDataStorage plugin
     * then it will be stored into two storage using the two configurations with identifiers 1 and 52.
     */
    @PluginParameter(label = "List configured storage identifiers for each available data storage plugin",
            keylabel = "Available data storage plugin identifier", optional = true,
            description = "Association between a data storage plugin identifier and a list of storage configuration."
                    + " This parameter is optional, nevertheless you have to define it if there is more than one"
                    + " configuration for a given data storage plugin type. Exemple : "
                    + " LocalDataStorage -> [1,52] means that if a file need to be stored with a LocalDataStorage plugin"
                    + " then it will be stored into two storage using the two configurations with identifiers 1 and 52.")
    private final Map<String, PluginConfigurationIdentifiersWrapper> mapPluginConfIdForPluginId = Maps.newHashMap();

    /**
     * Plugin configuration identifier to use to store online files like Quicklook, thumbnails or documents
     * if no online plugin is defined in the misc informations.
     */
    @PluginParameter(label = "Online plugin configuration identifier" + "", optional = true,
            description = "This plugin will be used to store files in online archive if no one is defined "
                    + "in the AIP misc information. Some files should be stored in an online storage for user interface "
                    + "needs like Quicklooks, Thumbnails and Documents. Indeed, those files needs to be directly downloadable.")
    private Long onlinePluginConfigrationId;

    /**
     * Plugin configuration loaded at plugin initializiation if the plugin parameter
     * {@link AIPMiscAllocationStrategyPlugin#onlinePluginConfigrationId} is defined.
     */
    private PluginConfiguration onlinePluginConfigration = null;

    /**
     * If this parameter is set to true, then the directory section must be define for each {@link AIPMiscStorageInformation} read
     * from the AIP misc information.
     */
    @PluginParameter(label = "Do not archive files if directory is not defined in AIP", optional = true,
            defaultValue = "false")
    private boolean noEmptyDirectory;

    @PluginInit
    public void init() {
        // If an online plugin configuration identifier is configured retrieve the plugin configuration associated.
        if (onlinePluginConfigrationId != null) {
            try {
                PluginConfiguration pluginConfigration = pluginService
                        .getPluginConfiguration(onlinePluginConfigrationId);
                if (pluginConfigration.getInterfaceNames().contains(IOnlineDataStorage.class.getName())) {
                    onlinePluginConfigration = pluginConfigration;
                } else {
                    LOG.error("Invalid Online plugin configuration id {}. The plugin is not an IOnlineDataStorage plugin.");
                }
            } catch (EntityNotFoundException e) {
                LOG.error("Invalid plugin configuration id {}. This id does not match any existing plugin configuration.",
                          onlinePluginConfigrationId, e);
            }
        }
    }

    @Override
    public Multimap<Long, StorageDataFile> dispatch(Collection<StorageDataFile> dataFilesToHandle) {
        Multimap<Long, StorageDataFile> dispatched = HashMultimap.create();
        for (StorageDataFile file : dataFilesToHandle) {
            InformationPackageMap misc = file.getAip().getProperties().getMiscInformation();
            if ((misc != null) && !misc.isEmpty()) {
                List<PluginConfiguration> pluginConfigurations = Lists.newArrayList();
                // 1. Read storage information from misc section
                Set<AIPMiscStorageInformation> infos = readMiscStorageInformations(misc);
                // 2. Select the unique plugin configuration for each storage defined
                infos.forEach(info -> pluginConfigurations.addAll(getPluginConfsForFile(info, file)));
                // 3. Handle special online mandatory files
                handleMandatoryOnlineFile(file, pluginConfigurations);
                // 4. Add each plugin conf to the global result map
                pluginConfigurations.forEach(pluginConf -> dispatched.put(pluginConf.getId(), file));
            }
        }
        return dispatched;
    }

    /**
     * Determine the {@link PluginConfiguration}s to use to store the given {@link StorageDataFile} according
     * to the given {@link AIPMiscStorageInformation}.
     * @param info {@link AIPMiscStorageInformation} storage information
     * @param file {link StorageDataFile} file to dispatc
     */
    private List<PluginConfiguration> getPluginConfsForFile(AIPMiscStorageInformation info, StorageDataFile file) {
        List<PluginConfiguration> pluginConfs = Lists.newArrayList();
        if (info.getPluginId() != null) {
            // 1. Check that pluginId match an existing plugin
            PluginMetaData plugin = pluginService.getPluginMetaDataById(info.getPluginId());
            if (plugin == null) {
                LOG.error("StorageDataFile \"{}\" not dispatched. Cause : pluginId {} does  not match an existing plugin",
                          file.getName(), info.getPluginId());
                return pluginConfs;
            }
        } else {
            LOG.error("StorageDataFile \"{}\" not dispatched. Cause : pluginId to use is not defined", file.getName());
            return pluginConfs;
        }

        // 1. Retrieve confs for the given pluginId
        PluginConfigurationIdentifiersWrapper configuredConfs = mapPluginConfIdForPluginId.get(info.getPluginId());
        List<PluginConfiguration> confs = pluginService.getActivePluginConfigurations(info.getPluginId());
        if ((configuredConfs == null) || configuredConfs.getPluginConfIdentifiers().isEmpty()) {
            // If mapPluginConfIdForPluginId parameter is not defined in this plugin :
            // Do not dispatch file if there is more than once available and active configuration
            // Else use the only one active.
            if (confs.size() == 1) {
                addPluginConfiguration(info, file, confs.get(0), pluginConfs);
            } else {
                LOG.error("StorageDataFile \"{}\" not dispatched. Cause : Unable to determine the plugin configuration to use for plugin id \"{}\"",
                          file.getName(), info.getPluginId());
            }
        } else {
            // For each configured conf, check if the conf exists and is active, then dispatch StorageDataFile with each ones.
            for (Long pluginConfId : configuredConfs.getPluginConfIdentifiers()) {
                // @formatter:off
                    confs
                    .stream()
                    .filter(c -> c.getId().equals(pluginConfId)).findFirst()
                    .ifPresent(conf -> addPluginConfiguration(info, file, conf, pluginConfs));
                    // @formatter:on
            }
        }
        return pluginConfs;
    }

    /**
     * Dispatch the given {@link StorageDataFile} into the dispatched files map with the given {@link PluginConfiguration}
     * @param info {@link AIPMiscStorageInformation} storage info from AIP misc informations
     * @param file {@link StorageDataFile} file to dispatch
     * @param PluginConfiguration {@link PluginConfiguration}
     */
    private void addPluginConfiguration(AIPMiscStorageInformation info, StorageDataFile file,
            PluginConfiguration pluginConf, List<PluginConfiguration> pluginConfigurations) {
        if (!noEmptyDirectory || (info.getDirectory() != null)) {
            file.increaseNotYetStoredBy();
            file.setStorageDirectory(info.getDirectory());
            pluginConfigurations.add(pluginConf);
        } else {
            LOG.error("StorageDataFile \"{}\" not dispatched. Cause : No directory define in AIP for pluginId \"{}\"",
                      file.getName(), info.getPluginId());
        }
    }

    /**
     * Read storage informations from miscInformation properties of given AIP.
     * @param miscInformation
     * @return {@link AIPMiscStorageInformation}s read from miscInformation
     */
    private Set<AIPMiscStorageInformation> readMiscStorageInformations(InformationPackageMap miscInformation) {
        Set<AIPMiscStorageInformation> infos = Sets.newHashSet();
        JsonObject miscJson = gson.toJsonTree(miscInformation).getAsJsonObject();
        JsonElement storageSection = miscJson.get("storage");
        if ((storageSection != null) && storageSection.isJsonArray()) {
            for (JsonElement storage : storageSection.getAsJsonArray()) {
                try {
                    AIPMiscStorageInformation info = gson.fromJson(storage, AIPMiscStorageInformation.class);
                    infos.add(info);
                } catch (JsonSyntaxException e) {
                    LOG.error("Invalid storage misc information {}", storage.toString(), e);
                }
            }
        } else {
            LOG.error("Invalid storage misc information {}", miscJson.toString());
        }
        return infos;
    }

    /**
     * Check that the given {@link StorageDataFile} is dispatched with an {@link IOnlineDataStorage} plugin if the file is online mandatory.
     * If it is not, dispatch the file with the configured {@link AIPMiscAllocationStrategyPlugin#onlinePluginConfigration}
     * @param file {@link StorageDataFile} to check for online dispatch
     * @param pluginConfigurations actual dispatched {@link PluginConfiguration}s for the given file.
     */
    private void handleMandatoryOnlineFile(StorageDataFile file, List<PluginConfiguration> pluginConfigurations) {
        if (file.isOnlineMandatory() && (onlinePluginConfigration != null)) {
            for (PluginConfiguration conf : pluginConfigurations) {
                if (!conf.getInterfaceNames().contains(IOnlineDataStorage.class.getName())) {
                    pluginConfigurations.add(onlinePluginConfigration);
                }
            }
        }
    }

}
