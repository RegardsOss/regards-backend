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

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import fr.cnes.regards.framework.jpa.IIdentifiable;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import java.util.List;
import java.util.Objects;

/**
 * Models the different access settings.<br>
 * Instead of using a list of values, each field of this POJO defines a specific setting.
 * @author Xavier-Alexandre Brochard
 */
@Entity
@Table(name = "t_access_settings")
@SequenceGenerator(name = "accessSettingsSequence", initialValue = 1, sequenceName = "seq_access_settings")
@TypeDef(
    name = "string-array",
    typeClass = ListArrayType.class
)
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

    @Valid
    @ManyToOne
    @JoinColumn(name = "default_role_id", foreignKey = @ForeignKey(name = "fk_access_settings_default_role"))
    private Role defaultRole;

    @Type(type = "string-array")
    @Column(
        name = "default_groups",
        columnDefinition = "text[]"
    )
    private List<String> defaultGroups;

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

    public Role getDefaultRole() {
        return defaultRole;
    }

    public List<String> getDefaultGroups() {
        return defaultGroups;
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

    public void setDefaultRole(Role defaultRole) {
        this.defaultRole = defaultRole;
    }

    public void setDefaultGroups(List<String> defaultGroups) {
        this.defaultGroups = defaultGroups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AccessSettings that = (AccessSettings) o;
        return Objects.equals(id, that.id)
            && Objects.equals(mode, that.mode)
            && Objects.equals(defaultRole, that.defaultRole)
            && Objects.equals(defaultGroups, that.defaultGroups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, mode, defaultRole, defaultGroups);
    }
}
