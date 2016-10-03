/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.domain;

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
 * @since 1.0
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
    private String parameterName_;

    /**
     * Parameter value
     */
    @Column(length = 2048)
    private String parameterValue_;

    /**
     * Default constructor
     *
     * @since 1.0
     */
    public PluginParameter() {
        super();
    }

    /**
     * Constructor
     *
     * @param pParameterName
     *          the parameter name
     * @param pParameterValue
     *          the parameter value
     * @since 1.0
     */
    public PluginParameter(final String pParameterName, final String pParameterValue) {
        super();
        parameterName_ = pParameterName;
        parameterValue_ = pParameterValue;
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
     * @return the parameterName_
     */
    public final String getParameterName() {
        return parameterName_;
    }

    /**
     * Set method.
     *
     * @param pParameterName
     *            the parameterName_ to set
     */
    public final void setParameterName(final String pParameterName) {
        parameterName_ = pParameterName;
    }

    /**
     * Get method.
     *
     * @return the parameterValue_
     */
    public final String getParameterValue() {
        return parameterValue_;
    }

    /**
     * Set method.
     *
     * @param pParameterValue
     *            the parameterValue_ to set
     */
    public final void setParameterValue(String pParameterValue) {
        parameterValue_ = pParameterValue;
    }

}
