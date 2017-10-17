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
package fr.cnes.regards.modules.ingest.domain.entity;

import java.time.OffsetDateTime;
import java.util.Optional;

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
import fr.cnes.regards.modules.ingest.domain.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.SIP;

/**
 * System POJO for storing SIP.
 *
 * @author Marc Sordi
 *
 */
@Entity
@Table(name = "t_sip", indexes = { @Index(name = "idx_sip_id", columnList = "sipId,ipId,checksum") }, // PostgreSQL
                                                                                                      // manage both
                                                                                                      // single indexes
                                                                                                      // and multiple
                                                                                                      // ones
        uniqueConstraints = { @UniqueConstraint(name = "uk_sip_ipId", columnNames = "ipId"),
                @UniqueConstraint(name = "uk_sip_checksum", columnNames = "checksum") })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class SIPEntity {

    /**
     * Length used as the checksum column definition.
     * Why 128? it allows to use sha-512. That should limit issues with checksum length for a few years
     */
    public static final int CHECKSUM_MAX_LENGTH = 128;

    private static final int MAX_URN_SIZE = 128;

    @Id
    @SequenceGenerator(name = "SipSequence", initialValue = 1, sequenceName = "seq_sip")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SipSequence")
    private Long id;

    /**
     * The SIP identifier = the feature ID
     */
    @NotBlank
    @Column(length = 100)
    private String sipId;

    /**
     * The internal SIP identifier. If two SIP are ingested with same id, this idIp will distinguish them as 2 different
     * versions
     */
    @NotBlank
    @Column(name = "ipId", length = MAX_URN_SIZE)
    private String ipId;

    /**
     * SIP version : this value is also reported in {@link #ipId} and must be the same
     */
    @NotNull
    @Min(1)
    @Max(999)
    private Integer version;

    @NotNull
    @Enumerated(EnumType.STRING)
    private SIPState state;

    /**
     * Optional message when SIP is rejected / not persisted in database
     */
    @Transient
    private String reasonForRejection;

    /**
     * Real SIP content checksum
     */
    @NotBlank
    @Column(length = CHECKSUM_MAX_LENGTH)
    private String checksum;

    @NotNull
    @Column(columnDefinition = "jsonb", name = "rawsip")
    @Type(type = "jsonb")
    private SIP sip;

    @NotNull
    private OffsetDateTime ingestDate;

    /**
     * Processing chain name from {@link IngestMetadata}
     */
    @NotBlank
    @Column(length = 100)
    private String processing;

    /**
     * Session identifier from {@link IngestMetadata}
     */
    @Column(length = 100)
    private String sessionId;

    public String getIpId() {
        return ipId;
    }

    public void setIpId(String ipId) {
        this.ipId = ipId;
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

    public Optional<String> getSessionId() {
        return Optional.ofNullable(sessionId);
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSipId() {
        return sipId;
    }

    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getReasonForRejection() {
        return reasonForRejection;
    }

    public void setReasonForRejection(String reasonForRejection) {
        this.reasonForRejection = reasonForRejection;
    }
}
