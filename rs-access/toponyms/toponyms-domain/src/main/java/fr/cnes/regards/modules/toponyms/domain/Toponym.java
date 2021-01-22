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
package fr.cnes.regards.modules.toponyms.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.Position;

import com.sun.istack.NotNull;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;

/**
 * POJO for Toponym object
 * Geometry field is handled by hibernate-spatial and postgres POSTGIS extension.
 * *
 * @author Sébastien Binda
 *
 */
@InstanceEntity
@Entity
@Table(name = "t_toponyms", uniqueConstraints = @UniqueConstraint(name = "uk_toponym_bid", columnNames = { "bid" }))
public class Toponym {

    /**
     * Unique business id
     */
    @Id
    @NotNull
    @Column(name = "bid", nullable = false)
    String businessId;

    /**
     * English label
     */
    @NotNull
    @Column(name = "label", nullable = false)
    String label;

    @NotNull
    @Column(name = "label_fr", nullable = false)
    String labelFr;

    @Column(name = "geom")
    private Geometry<Position> geometry;

    @NotNull
    @Column(name = "copyright")
    String copyright;

    @Column(name = "description")
    String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Geometry<Position> getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry<Position> geometry) {
        this.geometry = geometry;
    }

    public String getLabelFr() {
        return labelFr;
    }

    public void setLabelFr(String labelFr) {
        this.labelFr = labelFr;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

}
