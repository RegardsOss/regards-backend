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
package fr.cnes.regards.modules.ingest.domain.request;

import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.springframework.lang.Nullable;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;

/**
 *
 * Each SIP received by message broker results in an {@link IngestRequest}
 *
 * @author Marc SORDI
 *
 */
@Entity
@Table(name = "t_ingest_request",
        indexes = { @Index(name = "idx_ingest_request_id", columnList = "request_id"),
                @Index(name = "idx_ingest_request_step", columnList = "step"),
                @Index(name = "idx_ingest_remote_step_deadline", columnList = "remote_step_deadline"),
                @Index(name = "idx_ingest_request_remote_step_group_ids", columnList = "remote_step_group_ids"),
                @Index(name = "idx_ingest_request_state", columnList = "state") },
        uniqueConstraints = { @UniqueConstraint(name = "uk_ingest_request_id", columnNames = { "request_id" }) })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class IngestRequest extends AbstractRequest {

    @Id
    @SequenceGenerator(name = "ingestRequestSequence", initialValue = 1, sequenceName = "seq_ingest_request")
    @GeneratedValue(generator = "ingestRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Embedded
    private IngestMetadata metadata;

    /**
     * All internal request steps including local and remote ones
     */
    @NotNull(message = "Ingest request step is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "step", length = 50, nullable = false)
    private IngestRequestStep step;

    /**
     * Remote step dead line <br/>
     * A daemon controls this and passes this request in {@link RequestState#ERROR} if deadline is outdated!
     */
    @Column(name = "remote_step_deadline")
    private OffsetDateTime remoteStepDeadline;

    /**
     * Remote request group id
     */
    @Column(columnDefinition = "jsonb", name = "remote_step_group_ids")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private List<String> remoteStepGroupIds;

    @Column(columnDefinition = "jsonb", name = "rawsip")
    @Type(type = "jsonb")
    private SIP sip;

    /**
     * The {@link List} of files to build a product
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "t_ingest_request_aip", joinColumns = @JoinColumn(name = "ingest_request_id"),
            inverseJoinColumns = @JoinColumn(name = "aip_id"),
            uniqueConstraints = {
                    @UniqueConstraint(name = "uk_ingest_request_aip_aip_id", columnNames = { "aip_id" }) },
            foreignKey = @ForeignKey(name = "fk_ingest_request_aip_request_id"),
            inverseForeignKey = @ForeignKey(name = "fk_ingest_request_aip_aip_id"))
    private List<AIPEntity> aips;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IngestMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(IngestMetadata metadata) {
        this.metadata = metadata;
    }

    public SIP getSip() {
        return sip;
    }

    public void setSip(SIP sip) {
        this.sip = sip;
    }

    public IngestRequestStep getStep() {
        return step;
    }

    /**
     * @param step local step
     */
    public void setStep(IngestRequestStep step) {
        if (step.isRemote() && step.withTimeout()) {
            throw new IllegalArgumentException("Remote step needs a timeout, use dedicated setter!");
        }
        this.step = step;
        this.remoteStepDeadline = null;
    }

    /**
     * @param step remote step
     * @param remoteStepTimeout timeout in minute
     */
    public void setStep(IngestRequestStep step, long remoteStepTimeout) {
        if (!step.isRemote() && !step.withTimeout()) {
            throw new IllegalArgumentException("Local step don't need timeout, use dedicated setter!");
        }
        this.step = step;
        this.remoteStepDeadline = OffsetDateTime.now().plusMinutes(remoteStepTimeout);
    }

    public OffsetDateTime getRemoteStepDeadline() {
        return remoteStepDeadline;
    }

    public List<String> getRemoteStepGroupIds() {
        return remoteStepGroupIds;
    }

    public void setRemoteStepGroupIds(List<String> remoteStepGroupIds) {
        this.remoteStepGroupIds = remoteStepGroupIds;
    }

    public List<AIPEntity> getAips() {
        return aips;
    }

    public void setAips(List<AIPEntity> aips) {
        this.aips = aips;
    }

    public static IngestRequest build(IngestMetadata metadata, RequestState state, IngestRequestStep step, SIP sip) {
        return build(generateRequestId(), metadata, state, step, sip, null);
    }

    public static IngestRequest build(IngestMetadata metadata, RequestState state, IngestRequestStep step, SIP sip,
            @Nullable Set<String> errors) {
        return build(generateRequestId(), metadata, state, step, sip, errors);
    }

    public static IngestRequest build(String requestId, IngestMetadata metadata, RequestState state,
            IngestRequestStep step, SIP sip) {
        return build(requestId, metadata, state, step, sip, null);
    }

    public static IngestRequest build(String requestId, IngestMetadata metadata, RequestState state,
            IngestRequestStep step, SIP sip, @Nullable Set<String> errors) {
        IngestRequest request = new IngestRequest();
        request.setRequestId(requestId);
        request.setMetadata(metadata);
        request.setState(state);
        request.setStep(step);
        request.setSip(sip);
        request.setErrors(errors);
        return request;
    }
}
