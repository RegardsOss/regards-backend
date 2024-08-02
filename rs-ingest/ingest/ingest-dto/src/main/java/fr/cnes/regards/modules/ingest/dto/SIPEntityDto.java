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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */


package fr.cnes.regards.modules.ingest.dto;

import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * DTO for the wrapper of SIP Dto
 *
 * @author mnguyen0
 */
public class SIPEntityDto {

    private String sipId;

    private SIPState state;

    private String checksum;

    private SIPDto sip;

    private boolean last = false;

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

    public SIPEntityDto() {
        // Do nothing
    }

    public String getSipId() {
        return sipId;
    }

    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    public SIPState getState() {
        return state;
    }

    public void setState(SIPState state) {
        this.state = state;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public SIPDto getSip() {
        return sip;
    }

    public void setSip(SIPDto sip) {
        this.sip = sip;
    }

    public OaisUniformResourceName getSipIdUrn() {
        return OaisUniformResourceName.fromString(sipId);
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
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
        return "SIPEntityDto{"
               + "sipId='"
               + sipId
               + '\''
               + ", state="
               + state
               + ", checksum='"
               + checksum
               + '\''
               + ", sip="
               + sip
               + ", last="
               + last
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
        SIPEntityDto that = (SIPEntityDto) o;
        return last == that.last
               && Objects.equals(sipId, that.sipId)
               && state == that.state
               && Objects.equals(checksum,
                                 that.checksum)
               && Objects.equals(sip, that.sip)
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
        return Objects.hash(sipId,
                            state,
                            checksum,
                            sip,
                            last,
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
