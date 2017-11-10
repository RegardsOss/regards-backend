package fr.cnes.regards.modules.storage.plugin.allocation.strategy;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.storage.plugin.datastorage.IDataStorage;

/**
 * POJO allowing us to map a property value to a {@link IDataStorage} configuration id
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class PropertyDataStorageMapping {

    private static final String PROPERTY_VALUE = "Property_value";

    private static final String DATA_STORAGE_CONF_ID_PARAMETER_NAME = "Data_storage_conf_id";

    @PluginParameter(name = PROPERTY_VALUE, description = "value of the property into the aip")
    private String propertyValue;

    @PluginParameter(name = DATA_STORAGE_CONF_ID_PARAMETER_NAME,
            description = "Configuration id of the data storage to use if the property value in the aip correspond to the one provided")
    private Long dataStorageConfId;

    public PropertyDataStorageMapping() {
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public Long getDataStorageConfId() {
        return dataStorageConfId;
    }

    public void setDataStorageConfId(Long dataStorageConfId) {
        this.dataStorageConfId = dataStorageConfId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PropertyDataStorageMapping that = (PropertyDataStorageMapping) o;

        return propertyValue != null ? propertyValue.equals(that.propertyValue) : that.propertyValue == null;
    }

    @Override
    public int hashCode() {
        return propertyValue != null ? propertyValue.hashCode() : 0;
    }
}
