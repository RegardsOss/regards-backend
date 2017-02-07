/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * 
 * @author Christophe Mertz
 *
 */
@Entity(name = "T_NAVCTX_PARAMETER")
@SequenceGenerator(name = "navCtxParameterSequence", initialValue = 1, sequenceName = "SEQ_NAVCTX_PARAMETER")
public class ConfigParameter implements IIdentifiable<Long> {

    /**
     * The max size of a {@link String} value
     */
    public static final int MAX_STRING_VALUE = 2048;

    /**
     * Name min size
     */
    public static final int NAME_MIN_SIZE = 3;

    /**
     * Name max size
     */
    public static final int NAME_MAX_SIZE = 32;

    /**
     * Unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "navCtxParameterSequence")
    private Long id;

    /**
     * Parameter name
     */
    @NotNull
    @Size(min = NAME_MIN_SIZE, max = NAME_MAX_SIZE,
            message = "Parameter name must be between " + NAME_MIN_SIZE + " and " + NAME_MAX_SIZE + " length.")
    @Column(nullable = false, updatable = false, unique = true, length = NAME_MAX_SIZE)
    private String name;

    /**
     * Parameter value
     */
    @Column(length = MAX_STRING_VALUE)
    private String value;

    /**
     * Default constructor
     */
    public ConfigParameter() {
        super();
        name = "undefined";
    }

    /**
     * A constructor using fields
     * 
     * @param pName
     *            a parameter name
     * @param pValue
     *            a parameter value
     */
    public ConfigParameter(String pName, String pValue) {
        super();
        name = pName;
        value = pValue;
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String pValue) {
        this.value = pValue;
    }

}
