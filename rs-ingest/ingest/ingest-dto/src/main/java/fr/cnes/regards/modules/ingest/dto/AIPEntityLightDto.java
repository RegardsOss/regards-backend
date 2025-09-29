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

import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.urn.EntityType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * DTO representation of an AIP entity without the SIP field.
 *
 * @author Thibaud Michaudel
 **/
public class AIPEntityLightDto {

    private final AIPDto aip;

    private final String aipId;

    private final String category;

    private final OffsetDateTime creationDate;

    private final List<DisseminationInfo> disseminationInfos;

    private final DisseminationStatus disseminationStatus;

    private final Long id;

    private final EntityType ipType;

    private final boolean last;

    private final OffsetDateTime lastUpdate;

    private final String originUrn;

    private final String providerId;

    private final String session;

    private final String sessionOwner;

    private final AIPState state;

    private final Set<String> storages;

    private final Set<String> tags;

    private final Integer version;

    public AIPEntityLightDto(AIPDto aip,
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

    public AIPDto getAip() {
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AIPEntityLightDto that = (AIPEntityLightDto) o;
        return last == that.last
               && Objects.equals(aip, that.aip)
               && Objects.equals(aipId, that.aipId)
               && Objects.equals(category, that.category)
               && Objects.equals(creationDate, that.creationDate)
               && Objects.equals(disseminationInfos, that.disseminationInfos)
               && disseminationStatus == that.disseminationStatus
               && Objects.equals(id, that.id)
               && ipType == that.ipType
               && Objects.equals(lastUpdate, that.lastUpdate)
               && Objects.equals(originUrn, that.originUrn)
               && Objects.equals(providerId, that.providerId)
               && Objects.equals(session, that.session)
               && Objects.equals(sessionOwner, that.sessionOwner)
               && state == that.state
               && Objects.equals(storages, that.storages)
               && Objects.equals(tags, that.tags)
               && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aip,
                            aipId,
                            category,
                            creationDate,
                            disseminationInfos,
                            disseminationStatus,
                            id,
                            ipType,
                            last,
                            lastUpdate,
                            originUrn,
                            providerId,
                            session,
                            sessionOwner,
                            state,
                            storages,
                            tags,
                            version);
    }

    @Override
    public String toString() {
        return "AIPEntityLightDto{"
               + "aip="
               + aip
               + ", aipId='"
               + aipId
               + '\''
               + ", category="
               + category
               + ", creationDate="
               + creationDate
               + ", disseminationInfos="
               + disseminationInfos
               + ", disseminationStatus="
               + disseminationStatus
               + ", id="
               + id
               + ", ipType="
               + ipType
               + ", last="
               + last
               + ", lastUpdate="
               + lastUpdate
               + ", originUrn='"
               + originUrn
               + '\''
               + ", providerId='"
               + providerId
               + '\''
               + ", session='"
               + session
               + '\''
               + ", sessionOwner='"
               + sessionOwner
               + '\''
               + ", state="
               + state
               + ", storages="
               + storages
               + ", tags="
               + tags
               + ", version="
               + version
               + '}';
    }
}
