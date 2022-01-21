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
package fr.cnes.regards.modules.configuration.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;

import fr.cnes.regards.framework.module.manager.ConfigIgnore;

/**
 *
 * Class Theme
 *
 * Entity to describe an IHM Theme configuration.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Entity
@Table(name = "t_ui_theme",
                uniqueConstraints = @UniqueConstraint(name = "uk_ui_theme_name", columnNames = {"name"}))
public class Theme {

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "ihmThemesSequence", initialValue = 1, sequenceName = "seq_ui_theme")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ihmThemesSequence")
    @ConfigIgnore
    private Long id;

    /**
     * Theme name. Use to instantiate the right module
     */
    @NotNull
    @Column(name="name",nullable = false, length = 16)
    private String name;

    /**
     * Active theme is the default theme used for IHM
     */
    @NotNull
    @Column(nullable = false)
    private boolean active = false;

    /**
     * Theme configuration
     */
    @NotNull
    @Column(nullable = false)
    @Type(type = "text")
    private String configuration;

    public Long getId() {
        return id;
    }

    public void setId(final Long pId) {
        id = pId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String pName) {
        name = pName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean pActive) {
        active = pActive;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final String pConfiguration) {
        configuration = pConfiguration;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Theme other = (Theme) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else
            if (!id.equals(other.id)) {
                return false;
            }
        return true;
    }

    @Override
    public String toString() {
        return "Theme [id=" + id + ", name=" + name + ", active=" + active + ", configuration=" + configuration + "]";
    }

}
