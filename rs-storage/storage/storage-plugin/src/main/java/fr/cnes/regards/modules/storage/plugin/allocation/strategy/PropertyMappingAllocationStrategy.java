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
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;

/**
 * Allocation strategy that map a given property value to a {@link IDataStorage}
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(author = "REGARDS Team", description = "Allocation Strategy plugin that map a property value to a data storage",
        id = "PropertyMappingAllocationStrategy", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3",
        owner = "CNES", url = "https://regardsoss.github.io/")
public class PropertyMappingAllocationStrategy implements IAllocationStrategy {

    /**
     * Plugin parameter name of the property path
     */
    public static final String PROPERTY_PATH = "Property_path";

    /**
     * Plugin parameter name of the property data storage mappings
     */
    public static final String PROPERTY_VALUE_DATA_STORAGE_MAPPING = "Property_value_data_storage_mapping";

    public static final String QUICKLOOK_DATA_STORAGE_CONFIGURATION_ID = "Quicklook_data_storage_configuration_id";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PropertyMappingAllocationStrategy.class);

    /**
     * {@link Gson} instance
     */
    @Autowired
    private Gson gson;

    @Autowired
    private IPluginService pluginService;

    /**
     * Json path to the property from the AIP which value should discriminate data storages to use
     */
    @PluginParameter(name = PROPERTY_PATH,
            description = "Json path to the property from the AIP which value should discriminate data storages to use", label = "Property path")
    private String propertyPath;

    /**
     * Collection representing the mapping between a value and the data storage to use
     */
    @PluginParameter(name = PROPERTY_VALUE_DATA_STORAGE_MAPPING,
            description = "Collection representing the mapping between a value and the data storage to use", label = "Property value - Data storage mappings")
    private List<PropertyDataStorageMapping> propertyDataStorageMappings;

    @PluginParameter(name = QUICKLOOK_DATA_STORAGE_CONFIGURATION_ID,
            description = "Data storage to use if the file is a quicklook, must be an ONLINE data storage",
            label = "Quicklook data storage configuration id")
    private Long quicklookDataStorageConfigurationId;

    /**
     * Plugin init method
     */
    @PluginInit
    public void init() throws EntityNotFoundException, EntityInvalidException {
        if (!propertyPath.startsWith("$.")) {
            // our json path lib only understand path that starts with "$.", so lets add it in case the user didn't
            propertyPath = "$." + propertyPath;
        }
        //lets verify that quicklook data storage is an online data storage
        if(!pluginService.getPluginConfiguration(quicklookDataStorageConfigurationId).getInterfaceNames().contains(
                IOnlineDataStorage.class.getName())) {
            throw new EntityInvalidException("Current active allocation strategy does specify a quicklook data storage which is not ONLINE");
        }
    }

    @Override
    public Multimap<Long, StorageDataFile> dispatch(Collection<StorageDataFile> dataFilesToHandle) {
        Multimap<Long, StorageDataFile> dispatch = HashMultimap.create();
        // First lets construct a map, which is way better to manipulate
        Map<String, Long> valueConfIdMap = propertyDataStorageMappings.stream().collect(Collectors
                .toMap(PropertyDataStorageMapping::getPropertyValue, PropertyDataStorageMapping::getDataStorageConfId));
        for (StorageDataFile dataFile : dataFilesToHandle) {
            // now lets extract the property value from the AIP
            if(dataFile.isQuicklook()) {
                //This allocation strategy only allows files to be stored into 1 DataStorage
                dataFile.increaseNotYetStoredBy();
                dispatch.put(quicklookDataStorageConfigurationId, dataFile);
            } else {
                try {
                    String propertyValue = JsonPath.read(gson.toJson(dataFile.getAip()), propertyPath);
                    Long chosenOne = valueConfIdMap.get(propertyValue);
                    if (chosenOne == null) {
                        LOG.error(String.format(
                                "File(urls: %s) could not be associated to any data storage the allocation strategy do not have any mapping for the value of the property.",
                                dataFile.getUrls()));
                    } else {
                        //This allocation strategy only allows files to be stored into 1 DataStorage
                        dataFile.increaseNotYetStoredBy();
                        dispatch.put(chosenOne, dataFile);
                    }
                } catch (PathNotFoundException e) {
                    LOG.error(String.format(
                            "File(url: %s) could not be associated to any data storage because the aip associated(ipId: %s) do not have the following property: %s",
                            dataFile.getUrls(),
                            dataFile.getAip().getId(),
                            propertyPath), e);
                }
            }
        }

        return dispatch;
    }
}
