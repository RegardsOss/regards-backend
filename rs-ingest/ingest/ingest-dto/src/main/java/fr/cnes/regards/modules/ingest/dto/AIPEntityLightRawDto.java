/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.dto;

import com.google.gson.JsonObject;
import fr.cnes.regards.framework.urn.EntityType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * DTO representation of an AIP entity without the SIP field.
 * This is the same Dto as {@link AIPEntityLightDto} with the aip attribute serialized as a
 * JsonObject.
 *
 * @author Thibaud Michaudel
 **/
public class AIPEntityLightRawDto {

    @Schema(description = "The raw product AIP")
    private final JsonObject aip;

    @Schema(description = "The AIP Internal identifier (generated URN) identifying the product during its complete lifecycle (technical identifier).",
            example = "URN:AIP:DATA:IAS:fe9714af-5aca-316b-80c7-874847e918d2:V1")
    private final String aipId;

    @Schema(description = "The category associated to the AIP.")
    private final String category;

    @Schema(description = "The creation date of the AIP.")
    private final OffsetDateTime creationDate;

    @Schema(description = "The list of dissemination information of the AIP.")
    private final List<DisseminationInfo> disseminationInfos;

    @Schema(description = "The dissemination status of the AIP.")
    private final DisseminationStatus disseminationStatus;

    @Schema(description = "Unique identifier of the product in database")
    private final Long id;

    @Schema(description = "The type of the AIP.")
    private final EntityType ipType;

    @Schema(description = "Indicates if this AIP is the last version of the product.")
    private final boolean last;

    @Schema(description = "The last update date of the AIP.")
    private final OffsetDateTime lastUpdate;

    @Schema(description = "Optional parameter showing that the entity come from another entity in an external catalog.")
    private final String originUrn;

    @Schema(description = "Identifier given by the provider to identify the product (business identifier)")
    private final String providerId;

    @Schema(description = "Session name in which the product was created")
    private final String session;

    @Schema(description = "Session Owner that created the product")
    private final String sessionOwner;

    @Schema(description = "The state of the AIP.")
    private final AIPState state;

    @Schema(description = "Storage lists used by this AIP to store its files. Files are stored in one or more of storage listed here.")
    private final Set<String> storages;

    @Schema(description = "The list of tags associated to the AIP.")
    private final Set<String> tags;

    @Schema(description = "The version number of the AIP. This number is incremented at each new version of the AIP for a given product.")
    private final Integer version;

    public AIPEntityLightRawDto(JsonObject aip,
                                String aipId,
                                String category,
                                OffsetDateTime creationDate,
                                List<DisseminationInfo> disseminationInfos,
                                DisseminationStatus disseminationStatus,
                                Long id,
                                EntityType ipType,
                                boolean last,
                                OffsetDateTime lastUpdate,
                                String originUrn,
                                String providerId,
                                String session,
                                String sessionOwner,
                                AIPState state,
                                Set<String> storages,
                                Set<String> tags,
                                Integer version) {
        this.aip = aip;
        this.aipId = aipId;
        this.category = category;
        this.creationDate = creationDate;
        this.disseminationInfos = disseminationInfos;
        this.disseminationStatus = disseminationStatus;
        this.id = id;
        this.ipType = ipType;
        this.last = last;
        this.lastUpdate = lastUpdate;
        this.originUrn = originUrn;
        this.providerId = providerId;
        this.session = session;
        this.sessionOwner = sessionOwner;
        this.state = state;
        this.storages = storages;
        this.tags = tags;
        this.version = version;
    }

    public JsonObject getAip() {
        return aip;
    }

    public String getAipId() {
        return aipId;
    }

    public String getCategory() {
        return category;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public List<DisseminationInfo> getDisseminationInfos() {
        return disseminationInfos;
    }

    public DisseminationStatus getDisseminationStatus() {
        return disseminationStatus;
    }

    public Long getId() {
        return id;
    }

    public EntityType getIpType() {
        return ipType;
    }

    public boolean isLast() {
        return last;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public String getOriginUrn() {
        return originUrn;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getSession() {
        return session;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public AIPState getState() {
        return state;
    }

    public Set<String> getStorages() {
        return storages;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Integer getVersion() {
        return version;
    }
}
