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

import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * Class Layout
 * <p>
 * Layout configuration for projects IHMs
 *
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_ui_layout",
    uniqueConstraints = { @UniqueConstraint(name = "uk_ui_layout_application_id", columnNames = { "application_id" }) })
public class UILayout {

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "ihmLayoutsSequence", initialValue = 1, sequenceName = "seq_ui_layout")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ihmLayoutsSequence")
    private Long id;

    @NotNull
    @Column(name = "application_id", nullable = false, length = 16)
    private String applicationId;

    /**
     * JSON representation of layout configuration
     */
    @NotNull
    @Column(nullable = false)
    @Type(type = "text")
    protected String layout;

    public Long getId() {
        return id;
    }

    public void setId(final Long pId) {
        id = pId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(final String pApplicationId) {
        applicationId = pApplicationId;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(final String pLayout) {
        layout = pLayout;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((applicationId == null) ? 0 : applicationId.hashCode());
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
        final UILayout other = (UILayout) obj;
        if (applicationId == null) {
            if (other.applicationId != null) {
                return false;
            }
        } else if (!applicationId.equals(other.applicationId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Layout [id=" + id + ", applicationId=" + applicationId + ", layout=" + layout + "]";
    }

}
