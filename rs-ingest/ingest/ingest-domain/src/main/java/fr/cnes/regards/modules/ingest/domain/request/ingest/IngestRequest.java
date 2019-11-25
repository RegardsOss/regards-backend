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
package fr.cnes.regards.modules.ingest.domain.request.ingest;

import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.springframework.lang.Nullable;

/**
 *
 * Each SIP received by message broker results in an {@link IngestRequest}
 *
 * @author Marc SORDI
 * @author Léo Mieulet
 *
 */
@Entity(name = RequestTypeConstant.INGEST_VALUE)
public class IngestRequest extends AbstractRequest {

    @Column(columnDefinition = "jsonb", name = "payload")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private IngestPayload config;

    /**
     * The {@link List} of AIPEntity created by this request
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "t_ingest_request_aip", joinColumns = @JoinColumn(name = "ingest_request_id"),
            inverseJoinColumns = @JoinColumn(name = "aip_id"),
            uniqueConstraints = {
                    @UniqueConstraint(name = "uk_ingest_request_aip_aip_id", columnNames = { "aip_id" }) },
            foreignKey = @ForeignKey(name = "fk_ingest_request_aip_request_id"),
            inverseForeignKey = @ForeignKey(name = "fk_ingest_request_aip_aip_id"))
    private List<AIPEntity> aips;

    public IngestPayload getConfig() {
        return config;
    }

    public void setConfig(IngestPayload config) {
        this.config = config;
    }


    public String getRequestId() {
        return config.getRequestId();
    }

    public void setRequestId(String requestId) {
        config.setRequestId(requestId);
    }

    public IngestMetadata getMetadata() {
        return config.getMetadata();
    }

    public void setMetadata(IngestMetadata metadata) {
        config.setMetadata(metadata);
    }

    public SIP getSip() {
        return config.getSip();
    }

    public void setSip(SIP sip) {
        config.setSip(sip);
    }

    public IngestRequestStep getStep() {
        return config.getStep();
    }


    /**
     * @param step local step
     */
    public void setStep(IngestRequestStep step) {
        this.config.setStep(step);
        this.setRemoteStepDeadline(null);
    }

    /**
     * @param step remote step
     * @param remoteStepTimeout timeout in minute
     */
    public void setStep(IngestRequestStep step, long remoteStepTimeout) {
        this.config.setStep(step, remoteStepTimeout);
        this.setRemoteStepDeadline(OffsetDateTime.now().plusMinutes(remoteStepTimeout));;
    }

    public List<AIPEntity> getAips() {
        return aips;
    }

    public void setAips(List<AIPEntity> aips) {
        this.aips = aips;
    }

    public static String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    public static IngestRequest build(IngestMetadata metadata, InternalRequestState state, IngestRequestStep step, SIP sip) {
        return build(generateRequestId(), metadata, state, step, sip, null);
    }

    public static IngestRequest build(IngestMetadata metadata, InternalRequestState state, IngestRequestStep step, SIP sip,
            @Nullable Set<String> errors) {
        return build(generateRequestId(), metadata, state, step, sip, errors);
    }

    public static IngestRequest build(String requestId, IngestMetadata metadata, InternalRequestState state,
            IngestRequestStep step, SIP sip) {
        return build(requestId, metadata, state, step, sip, null);
    }

    public static IngestRequest build(String requestId, IngestMetadata metadata, InternalRequestState state,
            IngestRequestStep step, SIP sip, @Nullable Set<String> errors) {
        IngestRequest request = new IngestRequest();
        request.setConfig(new IngestPayload());
        request.setRequestId(requestId);
        request.setMetadata(metadata);
        request.setState(state);
        request.setStep(step);
        request.setSip(sip);
        request.setProviderId(sip.getId());
        request.setSessionOwner(metadata.getSessionOwner());
        request.setSession(metadata.getSession());
        request.setErrors(errors);
        request.setCreationDate(OffsetDateTime.now());
        return request;
    }
}
