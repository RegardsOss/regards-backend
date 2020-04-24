/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.domain;

import javax.persistence.Column;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;

/**
 *
 * @author Sébastien Binda
 *
 */
public class FeatureEntityHistory {

    @NotBlank(message = "Feature creator is required")
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Transient
    private String deletedBy;

    public static FeatureEntityHistory build(String createdBy, String updatedBy) {
        FeatureEntityHistory h = new FeatureEntityHistory();
        h.createdBy = createdBy;
        h.updatedBy = updatedBy;
        return h;
    }

    public static FeatureEntityHistory build(String createdBy) {
        FeatureEntityHistory h = new FeatureEntityHistory();
        h.createdBy = createdBy;
        return h;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

}
