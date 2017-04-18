/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.domain;

/**
 *
 * Class PluginTypesEnum
 *
 * Enum for all plugin types.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public enum PluginTypesEnum {

    /**
     * UI Plugins for search-forms. Create a new UI Criteria.
     */
    CRITERIA("criteria"),

    /**
     * UI Plugins for search results. To apply a new service to a selected list of entities.
     */
    SERVICE("service");

    private final String value;

    private PluginTypesEnum(final String pValue) {
        value = pValue;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static PluginTypesEnum parse(final String pType) {
        for (final PluginTypesEnum type : PluginTypesEnum.values()) {
            if (type.toString().equals(pType) || type.name().equals(pType)) {
                return type;
            }
        }
        return null;
    }

}
