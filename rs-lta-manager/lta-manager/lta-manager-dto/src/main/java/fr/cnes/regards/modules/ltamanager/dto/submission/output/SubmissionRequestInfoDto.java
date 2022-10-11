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

import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Give information about the current state of a submission request.
 *
 * @author Iliana Ghazali
 **/
public final class SubmissionRequestInfoDto {

    @NotBlank(message = "requestId is required")
    @Schema(description = "Submission request id.")
    private final String requestId;

    @NotBlank(message = "productId is required")
    @Schema(description = "Id of the product sent.")
    private final String productId;

    @NotNull(message = "status is required")
    @Schema(description = "Submission request progress status.")
    private final SubmissionRequestState status;

    @NotNull(message = "statusDate is required")
    @Schema(description = "Submission request last update date.")
    private final OffsetDateTime statusDate;

    @NotBlank(message = "session is required")
    @Schema(description = "Session to monitor the submission request.")
    private final String session;

    @Nullable
    @Schema(description = "Possible error message.", nullable = true)
    private final String message;

    public SubmissionRequestInfoDto(String requestId,
                                    String productId,
                                    SubmissionRequestState status,
                                    OffsetDateTime statusDate,
                                    String session,
                                    @Nullable String message) {
        this.requestId = requestId;
        this.productId = productId;
        this.status = status;
        this.statusDate = statusDate;
        this.session = session;
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubmissionRequestInfoDto that = (SubmissionRequestInfoDto) o;
        return requestId.equals(that.requestId)
               && productId.equals(that.productId)
               && status == that.status
               && statusDate.equals(that.statusDate)
               && Objects.equals(session, that.session)
               && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, productId, status, statusDate, session, message);
    }

    @Override
    public String toString() {
        return "SubmissionRequestInfoDto{"
               + "requestId='"
               + requestId
               + '\''
               + ", productId='"
               + productId
               + '\''
               + ", status="
               + status
               + ", statusDate="
               + statusDate
               + ", session='"
               + session
               + '\''
               + ", message='"
               + message
               + '\''
               + '}';
    }
}
