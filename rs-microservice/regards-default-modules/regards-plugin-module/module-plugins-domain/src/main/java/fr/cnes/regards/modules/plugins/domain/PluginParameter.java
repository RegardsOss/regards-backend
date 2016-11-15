/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.NotNull;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * Parameter associated to a plugin configuration <PluginConfiguration>
 *
 * @author Christophe Mertz
 */
@Entity(name = "T_PLUGIN_PARAMETER_VALUE")
@SequenceGenerator(name = "pluginParameterSequence", initialValue = 1, sequenceName = "SEQ_PLUGIN_PARAMETER")
public class PluginParameter implements IIdentifiable<Long> {

    /**
     * The max size of a {@link String} value
     */
    private static final int MAX_STRING_VALUE = 2048;

    /**
     * Unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pluginParameterSequence")
    private Long id;

    /**
     * Parameter name
     */
    @NotNull
    private String name;

    /**
     * Parameter value
     */
    @Column(length = MAX_STRING_VALUE)
    private String value;

    /**
     * {@link PluginConfiguration} parameter is optional
     */
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "PLUGIN_CONF_ID", referencedColumnName = "id", unique = true, nullable = true, insertable = true,
            updatable = true, foreignKey = @javax.persistence.ForeignKey(name = "FK_PLUGIN_CONF"))
    private PluginConfiguration pluginConfiguration;

    /**
     * The parameter is dynamic
     */
    private Boolean dynamic = false;

    /**
     * The list of values for a dynamic parameters
     */
    @ElementCollection
    @CollectionTable(joinColumns = @JoinColumn(name = "ID",
            foreignKey = @javax.persistence.ForeignKey(name = "FK_PARAM_DYN_ID")))
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PluginDynamicValue> dynamicsValues;

    /**
     * Default constructor
     *
     */
    public PluginParameter() {
        super();
        name="undefined";
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
        name = pName;
        value = pValue;
    }

    /**
     * Constructor
     *
     * @param pName
     *            the parameter name
     * @param pPluginConfiguration
     *            the plugin configuration
     */
    public PluginParameter(final String pName, final PluginConfiguration pPluginConfiguration) {
        super();
        name = pName;
        pluginConfiguration = pPluginConfiguration;
    }

    public final String getName() {
        return name;
    }

    public final void setName(final String pName) {
        name = pName;
    }

    public final String getValue() {
        return value;
    }

    public final void setValue(String pValue) {
        value = pValue;
    }

    public final Boolean isDynamic() {
        return dynamic;
    }

    public final void setIsDynamic(Boolean pIsDynamic) {
        this.dynamic = pIsDynamic;
    }

    public final PluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }

    public List<PluginDynamicValue> getDynamicsValues() {
        return dynamicsValues;
    }

    public List<String> getDynamicsValuesAsString() {
        final List<String> result = new ArrayList<>();
        if ((dynamicsValues != null) && !dynamicsValues.isEmpty()) {
            dynamicsValues.forEach(d -> result.add(d.getValue()));
        }
        return result;
    }

    public void setDynamicsValues(List<PluginDynamicValue> pDynamicValues) {
        this.dynamicsValues = pDynamicValues;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }

}
