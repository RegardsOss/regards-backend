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
package fr.cnes.regards.modules.accessrights.domain.projects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * Models the different access settings.<br>
 * Instead of using a list of values, each field of this POJO defines a specific setting.
 * @author Xavier-Alexandre Brochard
 */
@Entity
@Table(name = "t_access_settings")
@SequenceGenerator(name = "accessSettingsSequence", initialValue = 1, sequenceName = "seq_access_settings")
public class AccessSettings implements IIdentifiable<Long> {

    /**
     * Access acceptation mode
     */
    public static final String MANUAL_MODE = "manual";

    /**
     * Access acceptation mode
     */
    public static final String AUTO_ACCEPT_MODE = "auto-accept";

    /**
     * The settings unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accessSettingsSequence")
    @Column(name = "id")
    private Long id;

    /**
     * The acceptance mode
     */
    @Pattern(regexp = MANUAL_MODE + "|" + AUTO_ACCEPT_MODE, flags = Pattern.Flag.CASE_INSENSITIVE)
    @Column(name = "mode")
    private String mode = AUTO_ACCEPT_MODE;

    @Override
    public Long getId() {
        return id;
    }

    /**
     * Get <code>mode</code>
     * @return The acceptance mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * Set <code>id</code>
     * @param pId The id
     */
    public void setId(Long pId) {
        id = pId;
    }

    /**
     * Set <code>mode</code>
     * @param mode The acceptance mode
     */
    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof AccessSettings) && (((AccessSettings) obj).getMode().equals(mode));
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;

        result = prime * result;
        if (mode != null) {
            result += mode.hashCode();
        }

        return result;
    }

}
