package fr.cnes.regards.modules.storage.plugin.allocation.strategy;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;

/**
 * POJO allowing us to map a property value to a {@link IDataStorage} configuration id
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class PropertyDataStorageMapping {

    /**
     * Plugin parameter name of the property value
     */
    private static final String PROPERTY_VALUE = "Property_value";

    /**
     * Plugin parameter name of the data storage plugin configuration id
     */
    private static final String DATA_STORAGE_CONF_ID_PARAMETER_NAME = "Data_storage_conf_id";

    /**
     * Value of the property into the aip
     */
    @PluginParameter(name = PROPERTY_VALUE, description = "value of the property into the aip", label = "Property value")
    private String propertyValue;

    /**
     * Plugin configuration id of the data storage to use if the property value in the aip correspond to the one provided
     */
    @PluginParameter(name = DATA_STORAGE_CONF_ID_PARAMETER_NAME,
            description = "Configuration id of the data storage to use if the property value in the aip correspond to the one provided", label = "Data storage configuration id")
    private Long dataStorageConfId;

    /**
     * Default constructor
     */
    public PropertyDataStorageMapping() {
    }

    /**
     * constructor setting the parameters as attributes
     * @param propertyValue
     * @param dataStorageConfId
     */
    public PropertyDataStorageMapping(String propertyValue, Long dataStorageConfId) {
        this.propertyValue = propertyValue;
        this.dataStorageConfId = dataStorageConfId;
    }

    /**
     * @return the property value
     */
    public String getPropertyValue() {
        return propertyValue;
    }

    /**
     * Set the property value
     * @param propertyValue
     */
    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    /**
     * @return the data storage plugin configuration id
     */
    public Long getDataStorageConfId() {
        return dataStorageConfId;
    }

    /**
     * Set the data storage plugin configuration id
     * @param dataStorageConfId
     */
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
