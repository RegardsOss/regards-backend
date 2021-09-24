/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain;

import java.time.OffsetDateTime;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.framework.urn.EntityType;

/**
 * Define common attributes between SIPEntity and AIPEntity
 * @author Léo Mieulet
 */
@MappedSuperclass
public abstract class AbstractOAISEntity {

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION_OWNER)
    @Column(length = 128, name = "session_owner", nullable = false)
    private String sessionOwner;

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION)
    @Column(length = 128, name = "session_name", nullable = false)
    private String session;

    @Column(columnDefinition = "jsonb", nullable = false)
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> categories;

    /**
     * The provider identifier is provided by the user along the SIP, with no guaranty of uniqueness,
     * and propagated to children (i.e. AIPs)
     */
    @NotBlank(message = "Provider ID is required")
    @Column(name = "provider_id", length = 120, nullable = false)
    private String providerId;

    /**
     * Version : this value is also reported in {@link #sipId} and must be the same
     */
    @NotNull(message = "Version is required")
    @Min(1)
    @Max(999)
    private Integer version;

    @Column(columnDefinition = "jsonb")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> tags;

    @NotNull(message = "Creation date is required")
    @Column(name = "creation_date", nullable = false)
    private OffsetDateTime creationDate;

    @NotNull(message = "Last update date is required")
    @Column(name = "last_update", nullable = false)
    private OffsetDateTime lastUpdate;

    @Column(length = 20, name = "ip_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EntityType ipType;

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

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
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

    public EntityType getIpType() {
        return ipType;
    }

    public void setIpType(EntityType ipType) {
        this.ipType = ipType;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
