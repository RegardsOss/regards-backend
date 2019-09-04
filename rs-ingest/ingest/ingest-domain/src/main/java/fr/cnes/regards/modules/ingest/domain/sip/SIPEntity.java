/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.sip;

import fr.cnes.regards.modules.ingest.domain.OAISEntity;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;

/**
 * System POJO for storing SIP.
 *
 * @author Marc Sordi
 *
 */
@Entity
@Table(name = "t_sip",
        indexes = { @Index(name = "idx_sip_id", columnList = "provider_id,sipId,checksum"),
                @Index(name = "idx_sip_ingest_chain", columnList = "ingest_chain"),
                @Index(name = "idx_sip_state", columnList = "state"),
                @Index(name = "idx_sip_providerId", columnList = "provider_id"),
                @Index(name = "idx_sip_creation_date", columnList = "creation_date"),
                @Index(name = "idx_sip_version", columnList = "version"),
                @Index(name = "idx_sip_session_owner", columnList = "session_owner"),
                @Index(name = "idx_sip_session", columnList = "session_name") },
        // PostgreSQL manage both single indexes and multiple ones
        uniqueConstraints = { @UniqueConstraint(name = "uk_sip_sipId", columnNames = "sipId"),
                @UniqueConstraint(name = "uk_sip_checksum", columnNames = "checksum") })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class SIPEntity extends OAISEntity {

    /**
     * Length used as the checksum column definition.
     * Why 128? it allows to use sha-512. That should limit issues with checksum length for a few years
     */
    public static final int CHECKSUM_MAX_LENGTH = 128;

    public static final int MAX_URN_SIZE = 128;

    @Id
    @SequenceGenerator(name = "SipSequence", initialValue = 1, sequenceName = "seq_sip")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SipSequence")
    private Long id;

    /**
     * The SIP internal identifier (generated URN). If two SIP are ingested with same id, this idIp will distinguish
     * them as 2 different versions
     */
    @NotBlank(message = "SIP ID is required")
    @Column(name = "sipId", length = MAX_URN_SIZE)
    private String sipId;

    /**
     * SIP version : this value is also reported in {@link #sipId} and must be the same
     */
    @NotNull(message = "Version is required")
    @Min(1)
    @Max(999)
    private Integer version;

    @NotNull(message = "SIP state is required")
    @Enumerated(EnumType.STRING)
    private SIPState state;

    /**
     * Real SIP content checksum
     */
    @NotBlank(message = "Checksum is required")
    @Column(length = CHECKSUM_MAX_LENGTH)
    private String checksum;

    @NotNull(message = IngestValidationMessages.MISSING_SIP)
    @Column(columnDefinition = "jsonb", name = "rawsip")
    @Type(type = "jsonb")
    private SIP sip;

    public String getSipId() {
        return sipId;
    }

    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    public UniformResourceName getSipIdUrn() {
        return UniformResourceName.fromString(sipId);
    }

    public void setSipId(UniformResourceName sipId) {
        this.sipId = sipId.toString();
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

    public SIP getSip() {
        return sip;
    }

    public void setSip(SIP sip) {
        this.sip = sip;
    }

    public Long getId() {
        return id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (sipId == null ? 0 : sipId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SIPEntity other = (SIPEntity) obj;
        if (sipId == null) {
            if (other.sipId != null) {
                return false;
            }
        } else if (!sipId.equals(other.sipId)) {
            return false;
        }
        return true;
    }

    public static SIPEntity build(String tenant, IngestMetadata metadata, SIP sip, Integer version, SIPState state,
            EntityType entityType) {

        SIPEntity sipEntity = new SIPEntity();

        UUID uuid = UUID.nameUUIDFromBytes(sip.getId().getBytes());
        UniformResourceName urn = new UniformResourceName(OAISIdentifier.SIP, entityType, tenant, uuid, version);

        sipEntity.setProviderId(sip.getId());
        sipEntity.setSipId(urn);
        sipEntity.setCreationDate(OffsetDateTime.now());
        sipEntity.setState(state);
        sipEntity.setSip(sip);
        sipEntity.setIngestMetadata(metadata);
        sipEntity.setVersion(version);
        // Extracted from SIP for search purpose
        sipEntity.setTags(new HashSet<>(sip.getTags()));

        return sipEntity;
    }

}
