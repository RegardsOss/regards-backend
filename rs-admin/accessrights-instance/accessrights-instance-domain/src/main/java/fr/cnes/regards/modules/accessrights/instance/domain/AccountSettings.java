/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.instance.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
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
@Entity
@Table(name = "t_account_settings")
@SequenceGenerator(name = "accountSettingsSequence", initialValue = 1, sequenceName = "seq_account_settings")
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
    @Column(name = "mode", length = 16)
    private String mode = AUTO_ACCEPT_MODE;

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
