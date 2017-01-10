/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.framework.modules.plugins.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Class PluginDynamicValue
 * 
 * @author Christophe Mertz
 *
 */
@Embeddable
@Entity
@Table(name = "T_PLUGIN_DYN_VALUE")
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

    public final void setId(Long pId) {
        id = pId;
    }

    public Long getId() {
        return id;
    }

}
