/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.dto.SIPDto;

/**
 * System POJO for storing SIP.
 *
 * @author Marc Sordi
 *
 */
@Entity
@Table(name = "t_sip",
        indexes = { @Index(name = "idx_sip_id", columnList = "providerId,sipId,checksum"),
                @Index(name = "idx_sip_processing", columnList = "processing") },
        // PostgreSQL manage both single indexes and multiple ones
        uniqueConstraints = { @UniqueConstraint(name = "uk_sip_sipId", columnNames = "sipId"),
                @UniqueConstraint(name = "uk_sip_checksum", columnNames = "checksum") })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@NamedEntityGraphs({ @NamedEntityGraph(name = "graph.sip.entity.complete",
        attributeNodes = { @NamedAttributeNode(value = "processingErrors") }) })
public class SIPEntity {

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
     * The provider identifier = the feature ID
     */
    @NotBlank(message = "Provider ID is required")
    @Column(length = 100)
    private String providerId;

    /**
     * The SIP internal identifier (generated URN). If two SIP are ingested with same id, this idIp will distinguish
     * them as 2 different
     * versions
     */
    @NotBlank(message = "SIP ID is required")
    @Column(name = "sipId", length = MAX_URN_SIZE)
    private String sipId;

    @NotBlank(message = "Owner is required")
    @Column(name = "owner", length = 128)
    private String owner;

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
     * Optional message(s) when SIP is rejected
     */
    @Transient
    private List<String> rejectionCauses;

    @ElementCollection
    @JoinTable(name = "ta_sip_errors", joinColumns = @JoinColumn(name = "sip_entity_id",
            foreignKey = @ForeignKey(name = "fk_errors_sip_entity_id")))
    @Column(name = "error")
    private List<String> processingErrors;

    /**
     * Real SIP content checksum
     */
    @NotBlank(message = "Checksum is required")
    @Column(length = CHECKSUM_MAX_LENGTH)
    private String checksum;

    @NotNull(message = "SIP as JSON is required")
    @Column(columnDefinition = "jsonb", name = "rawsip")
    @Type(type = "jsonb")
    private SIP sip;

    @NotNull(message = "Ingestion date is required")
    private OffsetDateTime ingestDate;

    private OffsetDateTime lastUpdateDate;

    /**
     * Processing chain name from {@link IngestMetadata}
     */
    @NotBlank(message = "Processing chain name is required")
    @Column(length = 100)
    private String processing;

    /**
     * Session identifier from {@link IngestMetadata}
     */
    @NotNull(message = "Session is required")
    @ManyToOne
    @JoinColumn(name = "session", foreignKey = @ForeignKey(name = "fk_sip_session"))
    private SIPSession session;

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

    public OffsetDateTime getIngestDate() {
        return ingestDate;
    }

    public void setIngestDate(OffsetDateTime ingestDate) {
        this.ingestDate = ingestDate;
    }

    public String getProcessing() {
        return processing;
    }

    public void setProcessing(String processing) {
        this.processing = processing;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
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

    public List<String> getRejectionCauses() {
        if (rejectionCauses == null) {
            rejectionCauses = new ArrayList<>();
        }
        return rejectionCauses;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public OffsetDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(OffsetDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public SIPSession getSession() {
        return session;
    }

    public void setSession(SIPSession session) {
        this.session = session;
    }

    /**
     * Programmatic mapper between {@link SIPEntity} to {@link SIPDto}
     * @return {@link SIPDto}
     */
    public SIPDto toDto() {
        SIPDto dto = new SIPDto();
        dto.setId(providerId);
        dto.setSipId(sipId.toString());
        dto.setRejectionCauses(rejectionCauses);
        dto.setState(state);
        dto.setVersion(version);
        return dto;
    }

    public List<String> getProcessingErrors() {
        return processingErrors;
    }

    public void setProcessingErrors(List<String> errors) {
        this.processingErrors = errors;
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
}
