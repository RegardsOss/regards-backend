/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entity representing an group of user having rights on some data
 *
 * @author Sylvain Vissiere-Guerinet
 * <p>
 *         FIXME: for V2 or whenever users will be granted rights: add into THIS class a way to distinguish a group
 *         which name is an email and so is a "fake" group only linked to a user. isUserGroup maybe, to be established
 *         with the front.
 */
@Entity
@Table(name = "t_access_group",
       uniqueConstraints = @UniqueConstraint(name = "uk_access_group_name", columnNames = { "name" }))
public class AccessGroup implements IIdentifiable<Long> {

    public static final String NAME_REGEXP = "[a-zA-Z_][0-9a-zA-Z_]*";

    public static final int NAME_MIN_SIZE = 3;

    public static final int NAME_MAX_SIZE = 32;

    @Id
    @SequenceGenerator(name = "AccessGroupSequence", initialValue = 1, sequenceName = "seq_access_group")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AccessGroupSequence")
    private Long id;

    @NotNull
    @Pattern(regexp = NAME_REGEXP, message = "Group name must conform to regular expression \"" + NAME_REGEXP + "\".")
    @Size(min = NAME_MIN_SIZE,
          max = NAME_MAX_SIZE,
          message = "Group name must be between " + NAME_MIN_SIZE + " and " + NAME_MAX_SIZE + " length.")
    @Column(length = NAME_MAX_SIZE, updatable = false)
    private String name;

    @Column(name = "public")
    private boolean isPublic = Boolean.FALSE;

    @Column(name = "internal")
    private boolean isInternal = Boolean.FALSE;

    public AccessGroup() {
    }

    public AccessGroup(final String name) {
        this.name = name;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Schema(name = "isPublic") // Force name isPublic otherwise swagger believes that attribute name is "public"
    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(final boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void setInternal(boolean isInternal) {
        this.isInternal = isInternal;
    }

    @Schema(name = "isInternal") // Force name isInternal otherwise swagger believes that attribute name is "internal"
    public boolean isInternal() {
        return isInternal;
    }

    @Override
    public boolean equals(final Object other) {
        return (other instanceof AccessGroup) && ((AccessGroup) other).name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
