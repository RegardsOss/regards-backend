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
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.ingest.domain.request.ingest;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.IngestErrorType;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Each SIP received by message broker results in an {@link IngestRequest}
 *
 * @author Marc SORDI
 * @author Léo Mieulet
 */
@Entity(name = RequestTypeConstant.INGEST_VALUE)
public class IngestRequest extends AbstractRequest {

    @Column(columnDefinition = "jsonb", name = "payload")
    @Type(JsonBinaryType.class)
    private IngestPayload config;

    /**
     * The {@link List} of AIPEntity created by this request
     */
    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "ta_ingest_request_aip",
               joinColumns = @JoinColumn(name = "ingest_request_id"),
               inverseJoinColumns = @JoinColumn(name = "aip_id"),
               uniqueConstraints = { @UniqueConstraint(name = "uk_ingest_request_aip_aip_id",
                                                       columnNames = { "aip_id" }) },
               foreignKey = @ForeignKey(name = "fk_ingest_request_aip_request_id"),
               inverseForeignKey = @ForeignKey(name = "fk_ingest_request_aip_aip_id"))
    private List<AIPEntity> aips;

    /**
     * This is a no-args constructor for jpa, don't use it
     */
    protected IngestRequest() {
    }

    public IngestRequest(String correlationId) {
        super(correlationId);
    }

    public static IngestRequest build(String requestId,
                                      IngestMetadata metadata,
                                      InternalRequestState state,
                                      IngestRequestStep step,
                                      SIPDto sip) {
        return build(requestId, metadata, state, step, sip, null, null);
    }

    public static IngestRequest build(@Nullable String requestId,
                                      IngestMetadata metadata,
                                      InternalRequestState state,
                                      IngestRequestStep step,
                                      SIPDto sip,
                                      @Nullable Set<String> errors,
                                      @Nullable IngestErrorType errorType) {
        IngestRequest request = new IngestRequest(requestId != null ? requestId : UUID.randomUUID().toString());
        request.setConfig(new IngestPayload());
        request.setDtype(RequestTypeConstant.INGEST_VALUE);
        request.setMetadata(metadata);
        request.setSubmissionDate(metadata.getSubmissionDate());
        request.setState(state);
        request.setStep(step);
        request.setSip(sip);
        request.setProviderId(sip.getId());
        request.setSessionOwner(metadata.getSessionOwner());
        request.setSession(metadata.getSession());
        request.setErrors(errorType, errors);
        request.setCreationDate(OffsetDateTime.now());

        return request;
    }

    public IngestPayload getConfig() {
        return config;
    }

    public void setConfig(IngestPayload config) {
        this.config = config;
    }

    public IngestMetadata getMetadata() {
        return config.getMetadata();
    }

    public void setMetadata(IngestMetadata metadata) {
        config.setMetadata(metadata);
    }

    public SIPDto getSip() {
        return config.getSip();
    }

    public void setSip(SIPDto sip) {
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
     * @param step              remote step
     * @param remoteStepTimeout timeout in minute
     */
    public void setStep(IngestRequestStep step, long remoteStepTimeout) {
        this.config.setStep(step, remoteStepTimeout);
        this.setRemoteStepDeadline(OffsetDateTime.now().plusMinutes(remoteStepTimeout));
    }

    public List<AIPEntity> getAips() {
        return aips;
    }

    public void setAips(List<AIPEntity> aips) {
        this.aips = aips;
    }

    /**
     * Remove given AIP from the list of AIPs of this request
     *
     * @param a {@link AIPEntity} to remove
     */
    public void removeAip(AIPEntity a) {
        if (aips.contains(a)) {
            aips.remove(a);
        }
    }

    public void addErrorInformation(IngestRequestError error) {
        config.getRequestErrors().add(error);
    }

    public boolean isErrorInformation() {
        return config.getRequestErrors() != null && !config.getRequestErrors().isEmpty();
    }

    public Set<IngestRequestError> getErrorInformation() {
        return config.getRequestErrors();
    }

    public void clearErrorInformation() {
        config.getRequestErrors().clear();
    }
}
