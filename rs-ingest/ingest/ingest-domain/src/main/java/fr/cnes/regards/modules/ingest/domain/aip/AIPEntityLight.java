/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.aip;

import com.google.gson.JsonParser;
import fr.cnes.regards.modules.ingest.dto.AIPEntityLightRawDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Light version of {@link AIPEntity}. Does not contains SIP information.
 *
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_aip")
public class AIPEntityLight extends AbstractAIPEntity {

    @NotNull(message = "RAW JSON AIP is required")
    @Column(columnDefinition = "jsonb", name = "rawaip", nullable = false)
    private String aip;

    public String getAip() {
        return aip;
    }

    public void setAip(String aip) {
        this.aip = aip;
    }

    public AIPEntityLightRawDto toRawDto() {
        return new AIPEntityLightRawDto(JsonParser.parseString(this.getAip()).getAsJsonObject(),
                                        this.getAipId(),
                                        this.getCategories(),
                                        this.getCreationDate(),
                                        this.getDisseminationInfos(),
                                        this.getDisseminationStatus(),
                                        this.getId(),
                                        this.getIpType(),
                                        this.isLast(),
                                        this.getLastUpdate(),
                                        this.getOriginUrn(),
                                        this.getProviderId(),
                                        this.getSession(),
                                        this.getSessionOwner(),
                                        this.getState(),
                                        this.getStorages(),
                                        this.getTags(),
                                        this.getVersion());
    }
}
