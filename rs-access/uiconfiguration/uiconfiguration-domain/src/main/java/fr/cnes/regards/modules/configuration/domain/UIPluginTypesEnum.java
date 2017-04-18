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
public enum UIPluginTypesEnum {

    /**
     * UI Plugins for search-forms. Create a new UI Criteria.
     */
    CRITERIA("criteria"),

    /**
     * UI Plugins for search results. To apply a new service to a selected list of entities.
     */
    SERVICE("service");

    private final String value;

    private UIPluginTypesEnum(final String pValue) {
        value = pValue;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static UIPluginTypesEnum parse(final String pType) {
        for (final UIPluginTypesEnum type : UIPluginTypesEnum.values()) {
            if (type.toString().equals(pType) || type.name().equals(pType)) {
                return type;
            }
        }
        return null;
    }

}
