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
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.TypeDef;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.nio.file.Path;
import java.time.OffsetDateTime;
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

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

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
                             @Nullable String originUrn) {
        Assert.notNull(correlationId, "correlationId is mandatory ! Make sure other constraints are satisfied.");
        Assert.notNull(owner, "owner is mandatory ! Make sure other constraints are satisfied.");
        Assert.notNull(session, "session is mandatory ! Make sure other constraints are satisfied.");
        Assert.notNull(submissionStatus, "submissionStatus is mandatory ! Make sure other constraints are satisfied.");
        Assert.notNull(submittedProduct, "submittedProduct is mandatory ! Make sure other constraints are satisfied.");

        this.correlationId = correlationId;
        this.owner = owner;
        this.session = session;
        this.replaceMode = replaceMode;
        this.submissionStatus = submissionStatus;
        this.submittedProduct = submittedProduct;
        this.originUrn = originUrn;
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
               + "id='"
               + id
               + '\''
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
               + ", submissionStatus="
               + submissionStatus
               + ", submittedProduct="
               + submittedProduct
               + '}';
    }
}
