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

import io.swagger.v3.oas.annotations.media.Schema;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Represent a submission request response.
 *
 * @author Iliana Ghazali
 **/
public class SubmissionResponseDto  {

    @NotBlank(message = "correlationId is required")
    @Schema(description = "Identifier to track the request through the workflow.")
    private String correlationId;

    @NotNull(message = "responseStatus is required")
    @Schema(description = "Acceptance status of the submitted product.")
    private SubmissionResponseStatus responseStatus;

    @Nullable
    @Schema(description = "Identifier of the submitted product.")
    private final String productId;

    @Nullable
    @Schema(description = "Expiration date of the created request.", nullable = true)
    private OffsetDateTime expires;

    @Nullable
    @Schema(description = "Session to monitor the created submission request.", nullable = true)
    private String session;

    @Nullable
    @Schema(description = "Possible error message.", nullable = true)
    private String message;

    public SubmissionResponseDto(String correlationId,
                                 SubmissionResponseStatus responseStatus,
                                 @Nullable String productId,
                                 @Nullable String message) {
        this.correlationId = correlationId;
        this.productId = productId;
        this.responseStatus = responseStatus;
        this.message = message;
    }

    public SubmissionResponseDto(String correlationId,
                                 SubmissionResponseStatus responseStatus,
                                 @Nullable String productId,
                                 @Nullable OffsetDateTime expires,
                                 @Nullable String session,
                                 @Nullable String message) {
        this(correlationId, responseStatus, productId, message);
        this.correlationId = correlationId;
        this.expires = expires;
        this.session = session;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    @Nullable
    public String getProductId() {
        return productId;
    }

    public SubmissionResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(SubmissionResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    @Nullable
    public OffsetDateTime getExpires() {
        return expires;
    }

    public void setExpires(@Nullable OffsetDateTime expires) {
        this.expires = expires;
    }

    @Nullable
    public String getSession() {
        return session;
    }

    public void setSession(@Nullable String session) {
        this.session = session;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public void setMessage(@Nullable String message) {
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
        SubmissionResponseDto that = (SubmissionResponseDto) o;
        return correlationId.equals(that.correlationId)
               && Objects.equals(productId, that.productId)
               && responseStatus == that.responseStatus
               && Objects.equals(expires, that.expires)
               && Objects.equals(session, that.session)
               && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationId, productId, responseStatus, expires, session, message);
    }

    @Override
    public String toString() {
        return "SubmissionResponseDto{"
               + "correlationId='"
               + correlationId
               + '\''
               + ", productId='"
               + productId
               + '\''
               + ", responseStatus="
               + responseStatus
               + ", expires="
               + expires
               + ", session='"
               + session
               + '\''
               + ", message='"
               + message
               + '\''
               + '}';
    }
}
