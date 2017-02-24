/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.instance;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;

/**
 * Models the different account settings.<br>
 * Instead of using a list of values, each field of this POJO defines a specific setting.
 *
 * @author Xavier-Alexandre Brochard
 */
@InstanceEntity
@Entity(name = "T_ACCOUNT_SETTINGS")
@SequenceGenerator(name = "accountSettingsSequence", initialValue = 1, sequenceName = "SEQ_ACCOUNT_SETTINGS")
public class AccountSettings implements IIdentifiable<Long> {

    /**
     * Account acceptation mode
     */
    public static final String MANUAL_MODE = "manual";

    /**
     * Account acceptation mode
     */
    public static final String AUTO_ACCEPT_MODE = "auto-accept";

    /**
     * The settings unique id
     */
    @NotNull
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accountSettingsSequence")
    @Column(name = "id")
    private Long id;

    /**
     * The acceptance mode
     */
    @Pattern(regexp = MANUAL_MODE + "|" + AUTO_ACCEPT_MODE, flags = Pattern.Flag.CASE_INSENSITIVE)
    @Column(name = "mode")
    private String mode = MANUAL_MODE;

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

}
