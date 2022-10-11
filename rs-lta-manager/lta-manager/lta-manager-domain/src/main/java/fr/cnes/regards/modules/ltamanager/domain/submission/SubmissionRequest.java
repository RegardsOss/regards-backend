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
    @Column(name = "request_id", length = 36, nullable = false, updatable = false)
    private String requestId;

    @Column(length = 128, nullable = false, updatable = false)
    @NotBlank(message = "owner is required")
    private String owner;

    @Column(length = 128, nullable = false, updatable = false)
    @NotBlank(message = "session is required")
    private String session;

    @Column(name = "replace_mode", updatable = false)
    private boolean replaceMode;

    @Column(name = "need_ack", updatable = false)
    private boolean needAck;

    @Embedded
    @Valid
    private SubmissionStatus submissionStatus;

    @Embedded
    @Valid
    private SubmissionProduct submissionProduct;

    public SubmissionRequest() {
        // no-args constructor for jpa
    }
    public SubmissionRequest(String owner,
                             String session,
                             boolean replaceMode,
                             SubmissionStatus submissionStatus,
                             SubmissionProduct submissionProduct) {
        this.owner = owner;
        this.session = session;
        this.replaceMode = replaceMode;
        this.submissionStatus = submissionStatus;
        this.submissionProduct = submissionProduct;
    }

    public String getRequestId() {
        return requestId;
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

    public SubmissionProduct getSubmissionProduct() {
        return submissionProduct;
    }

    public String getDatatype() {
        return getSubmissionProduct().getDatatype();
    }

    public String getModel() {
        return getSubmissionProduct().getModel();
    }

    public Path getStorePath() {
        return getSubmissionProduct().getStorePath();
    }

    public SubmissionRequestDto getProduct() {
        return getSubmissionProduct().getProduct();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubmissionRequest that = (SubmissionRequest) o;
        return requestId.equals(that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    @Override
    public String toString() {
        return "SubmissionRequest{"
               + "requestId='"
               + requestId
               + '\''
               + ", owner='"
               + owner
               + '\''
               + ", session='"
               + session
               + '\''
               + ", replaceMode="
               + replaceMode
               + ", needAck="
               + needAck
               + ", submissionStatus="
               + submissionStatus
               + ", submissionProduct="
               + submissionProduct
               + '}';
    }
}
