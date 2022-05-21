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
package fr.cnes.regards.modules.toponyms.domain;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import org.geolatte.geom.Geometry;

import javax.persistence.Embedded;

/**
 * DTO to transfer {@link Toponym} objects with {@link IGeometry} in place of {@link Geometry}
 *
 * @author Sébastien Binda
 */
public class ToponymDTO {

    /**
     * Business unique identifier
     */
    private String businessId;

    /**
     * English label
     */
    private String labelEn;

    /**
     * French label
     */
    private String labelFr;

    /**
     * Description
     */
    private String Description;

    /**
     * Geojson geomtry
     */
    private IGeometry geometry;

    /**
     * Owner
     */
    private String copyright;

    /**
     * Origin
     */
    private boolean visible;

    /**
     * Toponym metadata
     */
    @Embedded
    private ToponymMetadata toponymMetadata;

    /**
     * Creates a new {@link ToponymDTO}
     *
     * @param businessId  unique identifier
     * @param labelEn     English label
     * @param labelFr     French label
     * @param geometry
     * @param copyright   owner
     * @param description
     * @param visible
     * @return
     */
    public static ToponymDTO build(String businessId,
                                   String labelEn,
                                   String labelFr,
                                   IGeometry geometry,
                                   String copyright,
                                   String description,
                                   boolean visible,
                                   ToponymMetadata toponymMetadata) {
        ToponymDTO dto = new ToponymDTO();
        dto.setBusinessId(businessId);
        dto.setLabelEn(labelEn);
        dto.setLabelFr(labelFr);
        dto.setGeometry(geometry);
        dto.setCopyright(copyright);
        dto.setDescription(description);
        dto.setVisible(visible);
        dto.setToponymMetadata(toponymMetadata);
        return dto;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getLabelEn() {
        return labelEn;
    }

    public void setLabelEn(String label) {
        this.labelEn = label;
    }

    public String getLabelFr() {
        return labelFr;
    }

    public void setLabelFr(String labelFr) {
        this.labelFr = labelFr;
    }

    public IGeometry getGeometry() {
        return geometry;
    }

    public void setGeometry(IGeometry geometry) {
        this.geometry = geometry;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public ToponymMetadata getToponymMetadata() {
        return toponymMetadata;
    }

    public void setToponymMetadata(ToponymMetadata toponymMetadata) {
        this.toponymMetadata = toponymMetadata;
    }
}
