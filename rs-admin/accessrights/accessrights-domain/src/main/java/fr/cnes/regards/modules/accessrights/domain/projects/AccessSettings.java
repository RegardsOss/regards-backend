/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * Models the different access settings.<br>
 * Instead of using a list of values, each field of this POJO defines a specific setting.
 *
 * @author Xavier-Alexandre
 */
@Entity(name = "T_ACCESS_SETTINGS")
@SequenceGenerator(name = "accessSettingsSequence", initialValue = 1, sequenceName = "SEQ_ACCESS_SETTINGS")
public class AccessSettings implements IIdentifiable<Long> {

    /**
     * The settings unique id
     */
    @NotNull
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accessSettingsSequence")
    @Column(name = "id")
    private Long id;

    /**
     * The acceptance mode
     */
    @Pattern(regexp = "manual|auto-accept", flags = Pattern.Flag.CASE_INSENSITIVE)
    @Column(name = "mode")
    private String mode;

    /**
     * Create an access setting with empty fields
     */
    public AccessSettings() {
        super();
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
     * Get <code>mode</code>
     *
     * @return The acceptance mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * Set <code>id</code>
     *
     * @param pId
     *            The id
     */
    public void setId(final Long pId) {
        id = pId;
    }

    /**
     * Set <code>mode</code>
     *
     * @param pMode
     *            The acceptance mode
     */
    public void setMode(final String pMode) {
        mode = pMode;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object pObj) {
        return (pObj instanceof AccessSettings) && (((AccessSettings) pObj).getMode() == mode);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        result = (prime * result);
        if (mode != null) {
            result += mode.hashCode();
        }

        return result;
    }

}
