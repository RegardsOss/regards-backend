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
package fr.cnes.regards.modules.ingest.domain.sip;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.UUID;

import javax.persistence.Column;
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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.AbstractOAISEntity;
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
public class SIPEntity extends AbstractOAISEntity {

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
        this.setIpType(sip.getIpType());
    }

    public Long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + (sipId == null ? 0 : sipId.hashCode());
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

    public static SIPEntity build(String tenant, IngestMetadata metadata, SIP sip, Integer version, SIPState state) {

        SIPEntity sipEntity = new SIPEntity();

        UniformResourceName urn = generationUrn(tenant, sip, version);
        sipEntity.setProviderId(sip.getId());
        sipEntity.setSipId(urn);
        sipEntity.setCreationDate(OffsetDateTime.now());
        sipEntity.setState(state);
        sipEntity.setSip(sip);
        // Extract from IngestMetadata
        sipEntity.setSession(metadata.getSession());
        sipEntity.setSessionOwner(metadata.getSessionOwner());
        sipEntity.setCategories(metadata.getCategories());

        sipEntity.setVersion(version);
        // Extracted from SIP for search purpose
        sipEntity.setTags(new HashSet<>(sip.getTags()));

        return sipEntity;
    }

    public static UniformResourceName generationUrn(String tenant, SIP sip, Integer version) {
        UUID uuid = UUID.nameUUIDFromBytes(sip.getId().getBytes());
        return new UniformResourceName(OAISIdentifier.SIP, sip.getIpType(), tenant, uuid, version);
    }

}
