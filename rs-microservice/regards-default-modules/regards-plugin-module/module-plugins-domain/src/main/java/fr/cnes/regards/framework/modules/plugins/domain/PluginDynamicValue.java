/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.framework.modules.plugins.domain;

import javax.persistence.Embeddable;

/**
 * Class PluginDynamicValue
 * 
 * @author Christophe Mertz
 *
 */
@Embeddable
public class PluginDynamicValue {
    /**
     * the value
     */
    private String value;

    /**
     * Default constructor
     *
     */
    public PluginDynamicValue() {
        super();
    }

    /**
     * Constructor
     * 
     * @param pValue
     *            a value to set
     */
    public PluginDynamicValue(String pValue) {
        super();
        this.value = pValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String pValue) {
        this.value = pValue;
    }

}
