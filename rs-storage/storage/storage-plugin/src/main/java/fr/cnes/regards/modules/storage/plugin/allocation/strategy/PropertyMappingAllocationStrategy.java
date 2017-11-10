package fr.cnes.regards.modules.storage.plugin.allocation.strategy;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.jayway.jsonpath.JsonPath;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.plugin.datastorage.IDataStorage;

/**
 * Allocation strategy that map a given property value to a {@link IDataStorage}
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(author = "REGARDS Team", description = "Allocation Strategy plugin that map a property value to a data storage",
        id = "PropertyMappingAllocationStrategy", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3",
        owner = "CNES", url = "https://regardsoss.github.io/")
public class PropertyMappingAllocationStrategy implements IAllocationStrategy {

    private static final String PROPERTY_PATH = "Property_path";

    private static final String PROPERTY_VALUE_DATA_STORAGE_MAPPING = "Property_value_data_storage_mapping";

    @PluginParameter(name = PROPERTY_PATH,
            description = "Json path to the property from the AIP which value should discriminate data storages to use")
    private String propertyPath;

    @PluginParameter(name = PROPERTY_VALUE_DATA_STORAGE_MAPPING,
            description = "Collection representing the mapping between a value and the data storage to use")
    private Set<PropertyDataStorageMapping> propertyDataStorageMapping;

    @Override
    public Multimap<Long, DataFile> dispatch(Collection<DataFile> dataFilesToHandle) {
        Multimap<Long, DataFile> dispatch = HashMultimap.create();
        //First lets construct a map, which is way better to manipulate
        Map<String, Long> valueConfIdMap = propertyDataStorageMapping.stream().collect(Collectors.toMap(
                PropertyDataStorageMapping::getPropertyValue,
                PropertyDataStorageMapping::getDataStorageConfId));
        for (DataFile dataFile : dataFilesToHandle) {
            //now lets extract the property value from the AIP
            String propertyValue = JsonPath.read(dataFile.getAip(), propertyPath);
            Long chosenOne = valueConfIdMap.get(propertyValue);
            if (chosenOne != null) {
                dispatch.put(chosenOne, dataFile);
            }
        }

        return dispatch;
    }
}
