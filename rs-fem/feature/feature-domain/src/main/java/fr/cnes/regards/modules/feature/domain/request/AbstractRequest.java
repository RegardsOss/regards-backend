/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.domain.request;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.feature.dto.FeatureRequestDTO;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;

/**
 * Common request properties
 *
 * @author Marc SORDI
 *
 */
@MappedSuperclass
public abstract class AbstractRequest {

    public static final String COLUMN_REQUEST_ID = "request_id";

    public static final String COLUMN_REQUEST_OWNER = "request_owner";

    public static final String COLUMN_REQUEST_TIME = "request_date";

    public static final String COLUMN_REGISTRATION_DATE = "registration_date";

    public static final String COLUMN_STEP = "step";

    public static final String COLUMN_LAST_EXEC_ERROR_STEP = "last_exec_error_step";

    public static final String COLUMN_PRIORITY = "priority";

    public static final String COLUMN_URN = "urn";

    public static final String COLUMN_STATE = "state";

    @Column(name = COLUMN_REQUEST_ID, length = 36, nullable = false, updatable = false)
    protected String requestId;

    @Column(name = COLUMN_REQUEST_OWNER, length = 128, nullable = false, updatable = false)
    protected String requestOwner;

    @NotNull(message = "Feature request state is required")
    @Enumerated(EnumType.STRING)
    @Column(name = COLUMN_STATE, length = 50, nullable = false)
    protected RequestState state;

    /**
     * External request registration date
     */
    @Column(name = COLUMN_REQUEST_TIME, nullable = false, updatable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    protected OffsetDateTime requestDate;

    /**
     * Internal request registration date
     */
    @Column(name = COLUMN_REGISTRATION_DATE, nullable = false, updatable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    protected OffsetDateTime registrationDate;

    /**
     * All internal request steps including local and remote ones
     */
    @NotNull(message = "Feature request step is required")
    @Enumerated(EnumType.STRING)
    @Column(name = COLUMN_STEP, length = 50, nullable = false)
    protected FeatureRequestStep step;

    /**
     * Last execution error step. Used to retry a request from last step
     */
    @Enumerated(EnumType.STRING)
    @Column(name = COLUMN_LAST_EXEC_ERROR_STEP, length = 50, nullable = true)
    protected FeatureRequestStep lastExecErrorStep;

    @NotNull(message = "Priority of the request")
    @Enumerated(EnumType.ORDINAL)
    @Column(name = COLUMN_PRIORITY, length = 50, nullable = false)
    protected PriorityLevel priority;

    @Column(columnDefinition = "jsonb", name = "errors")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    protected Set<String> errors;

    @SuppressWarnings("unchecked")
    protected <T extends AbstractRequest> T with(String requestId, String requestOwner, OffsetDateTime requestDate,
            PriorityLevel priority, RequestState state, FeatureRequestStep step) {
        Assert.notNull(requestId, "Request id is required");
        Assert.notNull(requestDate, "Request date is required");
        Assert.notNull(priority, "Request priority is required");
        Assert.notNull(state, "Request state is required");
        Assert.notNull(step, "Request step is required");
        Assert.notNull(requestOwner, "Request owner is required");

        this.requestId = requestId;
        this.requestOwner = requestOwner;
        this.requestDate = requestDate;
        this.registrationDate = OffsetDateTime.now();
        this.priority = priority;
        this.step = step;
        this.state = state;
        return (T) this;
    }

    public String getRequestId() {
        return requestId;
    }

    public OffsetDateTime getRequestDate() {
        return requestDate;
    }

    public OffsetDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setRequestDate(OffsetDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public void setRegistrationDate(OffsetDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public FeatureRequestStep getStep() {
        return step;
    }

    public void setStep(FeatureRequestStep step) {
        this.step = step;
    }

    public Set<String> getErrors() {
        return errors;
    }

    public void setErrors(Set<String> errors) {
        this.errors = errors;
    }

    public void addError(String error) {
        if (errors == null) {
            errors = new HashSet<>();
        }
        errors.add(error);
    }

    public FeatureRequestStep getLastExecErrorStep() {
        return lastExecErrorStep;
    }

    public void setLastExecErrorStep(FeatureRequestStep lastExecErrorStep) {
        this.lastExecErrorStep = lastExecErrorStep;
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
    }

    public RequestState getState() {
        return state;
    }

    public void setState(RequestState state) {
        this.state = state;
    }

    public String getRequestOwner() {
        return requestOwner;
    }

    public void setRequestOwner(String requestOwner) {
        this.requestOwner = requestOwner;
    }

    public boolean isDeletable() {
        return (this.state == RequestState.ERROR);
    }

    public boolean isRetryable() {
        return (this.state == RequestState.ERROR);
    }

    public static FeatureRequestDTO toDTO(AbstractRequest request) {
        FeatureRequestDTO dto = new FeatureRequestDTO();
        dto.setId(request.getId());
        dto.setRegistrationDate(request.getRegistrationDate());
        dto.setState(request.getState());
        dto.setStep(request.getStep());
        dto.setProcessing(request.getStep().isProcessing());
        dto.setErrors(request.getErrors());
        if (request instanceof FeatureCreationRequest) {
            FeatureCreationRequest fcr = (FeatureCreationRequest) request;
            dto.setProviderId(fcr.getProviderId());
            dto.setType(FeatureRequestTypeEnum.CREATION.toString());
            dto.setSession(fcr.getMetadata().getSession());
            dto.setSource(fcr.getMetadata().getSessionOwner());
        }
        if (request instanceof FeatureUpdateRequest) {
            dto.setProviderId(((FeatureUpdateRequest) request).getProviderId());
            dto.setType(FeatureRequestTypeEnum.UPDATE.toString());
        }
        if (request instanceof FeatureSaveMetadataRequest) {
            dto.setType(FeatureRequestTypeEnum.SAVE_METADATA.toString());
        }
        if (request instanceof FeatureDeletionRequest) {
            dto.setType(FeatureRequestTypeEnum.DELETION.toString());
        }
        if (request instanceof FeatureNotificationRequest) {
            dto.setType(FeatureRequestTypeEnum.NOTIFICATION.toString());
        }
        if (request instanceof FeatureCopyRequest) {
            dto.setType(FeatureRequestTypeEnum.COPY.toString());
        }
        return dto;
    }

    public abstract Long getId();

}
