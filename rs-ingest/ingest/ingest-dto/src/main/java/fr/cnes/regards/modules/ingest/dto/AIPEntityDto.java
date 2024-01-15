/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */


package fr.cnes.regards.modules.ingest.dto;

import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * DTO for the wrapper of AIP Dto
 *
 * @author mnguyen0
 */
public class AIPEntityDto {

    private SIPEntityDto sip;

    private Long id;

    /**
     * The AIP Internal identifier (generated URN)
     * versions
     */
    private String aipId;

    private AIPState state;

    private AIPDto aip;

    /**
     * Storage lists used by this AIP to store its files
     */
    private Set<String> storages = new HashSet<>();

    /**
     * Optional parameter showing that the entity come from another entity in an external catalog.
     * The origin urn is the urn of the external entity.
     */
    private String originUrn;

    private boolean last = false;

    private List<DisseminationInfoDto> disseminationInfos;

    private DisseminationStatus disseminationStatus = DisseminationStatus.NONE;

    private String sessionOwner;

    private String session;

    private Set<String> categories;

    /**
     * The provider identifier is provided by the user along the AIP, with no guaranty of uniqueness,
     * and propagated to children
     */
    private String providerId;

    private Integer version;

    private Set<String> tags;

    private OffsetDateTime creationDate;

    private OffsetDateTime lastUpdate;

    private EntityType ipType;

    public AIPEntityDto() {
    }

    public SIPEntityDto getSip() {
        return sip;
    }

    public void setSip(SIPEntityDto sip) {
        this.sip = sip;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAipId() {
        return aipId;
    }

    public void setAipId(String aipId) {
        this.aipId = aipId;
    }

    public OaisUniformResourceName getAipIdUrn() {
        return OaisUniformResourceName.fromString(aipId);
    }

    public AIPState getState() {
        return state;
    }

    public void setState(AIPState state) {
        this.state = state;
    }

    public AIPDto getAip() {
        return aip;
    }

    public void setAip(AIPDto aip) {
        this.aip = aip;
    }

    public Set<String> getStorages() {
        return storages;
    }

    public void setStorages(Set<String> storages) {
        this.storages = storages;
    }

    public String getOriginUrn() {
        return originUrn;
    }

    public void setOriginUrn(String originUrn) {
        this.originUrn = originUrn;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public List<DisseminationInfoDto> getDisseminationInfos() {
        return disseminationInfos;
    }

    public void setDisseminationInfos(List<DisseminationInfoDto> disseminationInfos) {
        this.disseminationInfos = disseminationInfos;
    }

    public DisseminationStatus getDisseminationStatus() {
        return disseminationStatus;
    }

    public void setDisseminationStatus(DisseminationStatus disseminationStatus) {
        this.disseminationStatus = disseminationStatus;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public void setSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public EntityType getIpType() {
        return ipType;
    }

    public void setIpType(EntityType ipType) {
        this.ipType = ipType;
    }

    @Override
    public String toString() {
        return "AIPEntityDto{"
               + "sip="
               + sip
               + ", id="
               + id
               + ", aipId='"
               + aipId
               + '\''
               + ", state="
               + state
               + ", aip="
               + aip
               + ", storages="
               + storages
               + ", originUrn='"
               + originUrn
               + '\''
               + ", last="
               + last
               + ", disseminationInfos="
               + disseminationInfos
               + ", disseminationStatus="
               + disseminationStatus
               + ", sessionOwner='"
               + sessionOwner
               + '\''
               + ", session='"
               + session
               + '\''
               + ", categories="
               + categories
               + ", providerId='"
               + providerId
               + '\''
               + ", version="
               + version
               + ", tags="
               + tags
               + ", creationDate="
               + creationDate
               + ", lastUpdate="
               + lastUpdate
               + ", ipType="
               + ipType
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AIPEntityDto that = (AIPEntityDto) o;
        return last == that.last
               && Objects.equals(sip, that.sip)
               && Objects.equals(id, that.id)
               && Objects.equals(aipId, that.aipId)
               && state == that.state
               && Objects.equals(aip, that.aip)
               && Objects.equals(storages, that.storages)
               && Objects.equals(originUrn, that.originUrn)
               && Objects.equals(disseminationInfos, that.disseminationInfos)
               && disseminationStatus == that.disseminationStatus
               && Objects.equals(sessionOwner, that.sessionOwner)
               && Objects.equals(session, that.session)
               && Objects.equals(categories, that.categories)
               && Objects.equals(providerId, that.providerId)
               && Objects.equals(version, that.version)
               && Objects.equals(tags, that.tags)
               && Objects.equals(creationDate, that.creationDate)
               && Objects.equals(lastUpdate, that.lastUpdate)
               && ipType == that.ipType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sip,
                            id,
                            aipId,
                            state,
                            aip,
                            storages,
                            originUrn,
                            last,
                            disseminationInfos,
                            disseminationStatus,
                            sessionOwner,
                            session,
                            categories,
                            providerId,
                            version,
                            tags,
                            creationDate,
                            lastUpdate,
                            ipType);
    }
}
