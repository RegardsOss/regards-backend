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
package fr.cnes.regards.modules.ltamanager.dto.submission.output;

import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Corresponding SubmissionRequest dto
 *
 * @author Iliana Ghazali
 **/
public final class SubmittedSearchResponseDto {

    @NotBlank(message = "id is required")
    @Schema(description = "Request identifier.")
    private final Long id;

    @NotBlank(message = "correlationId is required")
    @Schema(description = "Identifier of the submission request.")
    private final String correlationId;

    @NotBlank(message = "owner is required")
    @Schema(description = "Request sender.")
    private final String owner;

    @NotBlank(message = "session is required")
    @Schema(description = "Session to monitor the submission request.")
    private final String session;

    @NotNull(message = "status is required")
    @Schema(description = "Progress status of the submission request.")
    private final SubmissionRequestState status;

    @NotNull(message = "statusDate is required")
    @Schema(description = "Submission request last update date.")
    private final OffsetDateTime statusDate;

    @NotNull(message = "creationDate is required")
    @Schema(description = "Submission request creation date.")
    private final OffsetDateTime creationDate;

    @NotBlank(message = "model is required")
    @Schema(description = "Model used by the submitted product.")
    private final String model;

    @NotNull(message = "storePath is required")
    @Schema(description = "Product store path.")
    private final Path storePath;

    @Valid
    @Schema(description = "Product submitted.")
    private final SubmissionRequestDto product;

    @Nullable
    private final String message;

    public SubmittedSearchResponseDto(Long id,
                                      String correlationId,
                                      String owner,
                                      String session,
                                      SubmissionRequestState status,
                                      OffsetDateTime statusDate,
                                      OffsetDateTime creationDate,
                                      String model,
                                      Path storePath,
                                      SubmissionRequestDto product,
                                      @Nullable String message) {
        this.id = id;
        this.correlationId = correlationId;
        this.owner = owner;
        this.session = session;
        this.status = status;
        this.statusDate = statusDate;
        this.creationDate = creationDate;
        this.model = model;
        this.storePath = storePath;
        this.product = product;
        this.message = message;
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

    public SubmissionRequestState getStatus() {
        return status;
    }

    public OffsetDateTime getStatusDate() {
        return statusDate;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public String getModel() {
        return model;
    }

    public Path getStorePath() {
        return storePath;
    }

    public SubmissionRequestDto getProduct() {
        return product;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubmittedSearchResponseDto that = (SubmittedSearchResponseDto) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SubmittedSearchResponseDto{"
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
               + ", status="
               + status
               + ", statusDate="
               + statusDate
               + ", creationDate="
               + creationDate
               + ", model='"
               + model
               + '\''
               + ", storePath="
               + storePath
               + ", product="
               + product
               + ", message='"
               + message
               + '\''
               + '}';
    }
}
