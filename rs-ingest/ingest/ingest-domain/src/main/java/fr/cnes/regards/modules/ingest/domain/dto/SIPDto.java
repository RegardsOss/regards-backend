/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.dto;

import java.util.List;

import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * Data Transfer Object for synchronous bulk submission request.
 * @author Marc Sordi
 *
 */
public class SIPDto {

    /**
     * The feature id
     */
    private String id;

    /**
     * The unique ingest IP identifier
     */
    private String ipId;

    private Integer version;

    private SIPState state;

    private List<String> rejectionCauses;

    public List<String> getRejectionCauses() {
        return rejectionCauses;
    }

    public void setRejectionCauses(List<String> rejectionCauses) {
        this.rejectionCauses = rejectionCauses;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIpId() {
        return ipId;
    }

    public void setIpId(String ipId) {
        this.ipId = ipId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public SIPState getState() {
        return state;
    }

    public void setState(SIPState state) {
        this.state = state;
    }
}
