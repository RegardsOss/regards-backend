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
package fr.cnes.regards.modules.ltamanager.domain.submission;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.modules.ltamanager.domain.settings.DatatypeParameter;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import org.hibernate.annotations.TypeDef;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * A submission request is a wrapper of {@link SubmissionRequestDto} with additional metadata.
 * It contains all necessary information to store a product in a long-term storage space.
 *
 * @author Iliana Ghazali
 **/
@Entity
@Table(name = "t_submission_requests")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class SubmissionRequest {

    public static final String SUBMISSION_STATUS_FIELD_NAME = "submissionStatus";

    public static final String DATATYPE_FILED_NAME = "submittedProduct.datatype";

    @Id
    @SequenceGenerator(name = "submissionRequestSequence", initialValue = 1, sequenceName = "seq_submission_request")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "submissionRequestSequence")
    private Long id;

    @Column(name = "correlation_id", nullable = false, updatable = false, unique = true)
    @NotBlank(message = "correlationId is required to track this request.")
    private String correlationId;

    @Column(length = 128, nullable = false, updatable = false)
    @NotBlank(message = "owner is required")
    private String owner;

    @Column(length = 128, nullable = false, updatable = false)
    @NotBlank(message = "session is required")
    private String session;

    @Column(name = "replace_mode", updatable = false)
    private boolean replaceMode;

    @Column(name = "origin_urn", updatable = false)
    @Nullable
    private String originUrn;

    @Column(name = "origin_request_appid", length = 128)
    @Nullable
    private String originRequestAppId;

    @Column(name = "origin_request_priority")
    @Nullable
    private Integer originRequestPriority;

    @Embedded
    @Valid
    private SubmissionStatus submissionStatus;

    @Embedded
    @Valid
    private SubmittedProduct submittedProduct;

    public SubmissionRequest() {
        // no-args constructor for jpa
    }

    public SubmissionRequest(String correlationId,
                             String owner,
                             String session,
                             boolean replaceMode,
                             SubmissionStatus submissionStatus,
                             SubmittedProduct submittedProduct,
                             @Nullable String originUrn,
                             @Nullable String originalRequestAppId,
                             @Nullable Integer originRequestPriority) {
        Assert.notNull(correlationId, "correlationId is mandatory !");
        Assert.notNull(owner, "owner is mandatory !");
        Assert.notNull(session, "session is mandatory !");
        Assert.notNull(submissionStatus, "submissionStatus is mandatory !");
        Assert.notNull(submittedProduct, "submittedProduct is mandatory !");

        this.correlationId = correlationId;
        this.owner = owner;
        this.session = session;
        this.replaceMode = replaceMode;
        this.submissionStatus = submissionStatus;
        this.submittedProduct = submittedProduct;
        this.originUrn = originUrn;
        this.originRequestAppId = originalRequestAppId;
        this.originRequestPriority = originRequestPriority;
    }

    public static SubmissionRequest buildSubmissionRequest(SubmissionRequestDto requestDto,
                                                           DatatypeParameter datatypeConfig,
                                                           OffsetDateTime currentDateTime,
                                                           Integer requestExpiresInHour,
                                                           @Nullable String originRequestAppId,
                                                           @Nullable Integer originRequestPriority) {
        String session = requestDto.getSession();
        String owner = requestDto.getOwner();

        if (session == null) {
            // if session is not provided, replace it with <owner.name>-<YYYYMMdd>
            session = String.format("%s-%s",
                                    owner.split("@")[0],
                                    currentDateTime.format(DateTimeFormatter.BASIC_ISO_DATE));
        }

        return new SubmissionRequest(requestDto.getCorrelationId(),
                                     owner,
                                     session,
                                     requestDto.isReplaceMode(),
                                     new SubmissionStatus(currentDateTime,
                                                          currentDateTime,
                                                          requestExpiresInHour,
                                                          SubmissionRequestState.VALIDATED,
                                                          null),
                                     new SubmittedProduct(requestDto.getDatatype(),
                                                          datatypeConfig.getModel(),
                                                          Paths.get(datatypeConfig.getStorePath()),
                                                          requestDto),
                                     requestDto.getOriginUrn(),
                                     originRequestAppId,
                                     originRequestPriority);
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getOwner() {
        return owner;
    }

    public String getSession() {
        return session;
    }

    public boolean isReplaceMode() {
        return replaceMode;
    }

    public SubmissionStatus getSubmissionStatus() {
        return submissionStatus;
    }

    public SubmittedProduct getSubmittedProduct() {
        return submittedProduct;
    }

    public String getDatatype() {
        return getSubmittedProduct().getDatatype();
    }

    public String getModel() {
        return getSubmittedProduct().getModel();
    }

    public Path getStorePath() {
        return getSubmittedProduct().getStorePath();
    }

    public SubmissionRequestDto getProduct() {
        return getSubmittedProduct().getProduct();
    }

    public OffsetDateTime getCreationDate() {
        return getSubmissionStatus().getCreationDate();
    }

    public OffsetDateTime getExpiryDate() {
        return getSubmissionStatus().getExpiryDate();
    }

    public OffsetDateTime getStatusDate() {
        return getSubmissionStatus().getStatusDate();
    }

    public SubmissionRequestState getStatus() {
        return getSubmissionStatus().getStatus();
    }

    @Nullable
    public String getMessage() {
        return getSubmissionStatus().getMessage();
    }

    @Nullable
    public String getOriginUrn() {
        return originUrn;
    }

    public void setOriginUrn(@Nullable String originUrn) {
        this.originUrn = originUrn;
    }

    public Long getId() {
        return id;
    }

    public String getOriginRequestAppId() {
        return originRequestAppId;
    }

    public void setOriginRequestAppId(String originRequestAppId) {
        this.originRequestAppId = originRequestAppId;
    }

    public Integer getOriginRequestPriority() {
        return originRequestPriority;
    }

    public void setOriginRequestPriority(Integer originRequestPriority) {
        this.originRequestPriority = originRequestPriority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubmissionRequest that = (SubmissionRequest) o;
        return correlationId.equals(that.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationId);
    }

    @Override
    public String toString() {
        return "SubmissionRequest{"
               + "id="
               + id
               + ", correlationId='"
               + correlationId
               + '\''
               + ", owner='"
               + owner
               + '\''
               + ", session='"
               + session
               + '\''
               + ", replaceMode="
               + replaceMode
               + ", originUrn='"
               + originUrn
               + '\''
               + ", originRequestAppId='"
               + originRequestAppId
               + '\''
               + ", originRequestPriority="
               + originRequestPriority
               + ", submissionStatus="
               + submissionStatus
               + ", submittedProduct="
               + submittedProduct
               + '}';
    }
}
