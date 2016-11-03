/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.domain;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Class PluginParameter
 *
 * Parameter associated to a plugin configuration <PluginConfiguration>
 *
 * @author CS
 */
@Entity
@Table(name = "T_PLUGIN_PARAMETER_VALUE")
public class PluginParameter {

    /**
     * Parameter unique id_
     */
    @Id
    @GeneratedValue
    private Long id_;

    /**
     * Parameter name
     */
    private String name_;

    /**
     * Parameter value
     */
    @Column(length = 2048)
    private String value_;

    /**
     * The parameter is dynamic
     */
    private Boolean isDynamic_;

    /**
     * The list of values for a dynamic parameters
     */
    private List<String> dynamicsValues_;

    /**
     * Default constructor
     *
     */
    public PluginParameter() {
        super();
    }

    /**
     * Constructor
     *
     * @param pName
     *            the parameter name
     * @param pValue
     *            the parameter value
     */
    public PluginParameter(final String pName, final String pValue) {
        super();
        name_ = pName;
        value_ = pValue;
    }

    /**
     * Get method.
     *
     * @return the id_
     */
    public final Long getId() {
        return id_;
    }

    /**
     * Set method.
     *
     * @param pId
     *            the id_ to set
     */
    public final void setId(Long pId) {
        id_ = pId;
    }

    /**
     * Get method.
     *
     * @return the name_
     */
    public final String getName() {
        return name_;
    }

    /**
     * Set method.
     *
     * @param pParameterName
     *            the name_ to set
     */
    public final void setName(final String pName) {
        name_ = pName;
    }

    /**
     * Get method.
     *
     * @return the value_
     */
    public final String getValue() {
        return value_;
    }

    /**
     * Set method.
     *
     * @param pParameterValue
     *            the value_ to set
     */
    public final void setValue(String pValue) {
        value_ = pValue;
    }

    /**
     * Get the
     * 
     * @return is the parameter dynamic
     */
    public final Boolean getIsDynamic() {
        return isDynamic_;
    }

    /**
     * Set or unset the parameter to dynamic
     * 
     * @param pIsDynamic
     *            boolean
     */
    public final void setIsDynamic(Boolean pIsDynamic) {
        this.isDynamic_ = pIsDynamic;
    }

    /**
     * Get the dynamic values of the parameter
     * 
     * @return the dynamic values of the parameter
     */
    public final List<String> getDynamicsValues() {
        return dynamicsValues_;
    }

    /**
     * Set the dynamic values to the parameter
     * 
     * @param pDynamicsValues
     *            the dynamic values
     */
    public final void setDynamicsValues(List<String> pDynamicsValues) {
        this.dynamicsValues_ = pDynamicsValues;
    }

}
