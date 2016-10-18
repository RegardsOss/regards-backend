/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Class PluginDynamicValue
 * 
 * @author cmertz
 *
 */
@Entity(name = "T_PLUGIN_DYN_VALUE")
public class PluginDynamicValue {

    /**
     * Parameter unique id
     */
    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;

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
