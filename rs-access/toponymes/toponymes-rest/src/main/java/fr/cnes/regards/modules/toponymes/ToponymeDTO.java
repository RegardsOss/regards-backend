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
package fr.cnes.regards.modules.toponymes;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;

/**
 *
 * @author Sébastien Binda
 *
 */
public class ToponymeDTO {

    private String businessId;

    private String label;

    private String labelFr;

    private IGeometry geometry;

    private String copyright;

    public static ToponymeDTO build(String businessId, String label, String labelFr, IGeometry geometry,
            String copyright) {
        ToponymeDTO dto = new ToponymeDTO();
        dto.setBusinessId(businessId);
        dto.setLabel(labelFr);
        dto.setLabelFr(labelFr);
        dto.setGeometry(geometry);
        dto.setCopyright(copyright);
        return dto;
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

}
