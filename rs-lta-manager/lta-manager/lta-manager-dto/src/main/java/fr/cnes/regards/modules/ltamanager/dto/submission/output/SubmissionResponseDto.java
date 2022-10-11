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
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Represent a submission request response.
 *
 * @author Iliana Ghazali
 **/
public class SubmissionResponseDto  {

    @Nullable
    @Schema(description = "Submission request created to process the product. Can be null if the request "
                          + "could not be saved in database.", nullable = true)
    private String requestId;

    @NotNull(message = "productId is required")
    @Schema(description = "Id of the product submitted.")
    private final String productId;

    @NotNull(message = "responseStatus is required")
    @Schema(description = "Acceptance status of the product sent.")
    private SubmissionResponseStatus responseStatus;

    @Nullable
    @Schema(description = "Expiration date of the created request.", nullable = true)
    private OffsetDateTime expires;

    @Nullable
    @Schema(description = "Session to monitor the created submission request.", nullable = true)
    private String session;

    @Nullable
    @Schema(description = "Possible error message.", nullable = true)
    private String message;

    public SubmissionResponseDto(String productId, SubmissionResponseStatus responseStatus, @Nullable String message) {
        this.productId = productId;
        this.responseStatus = responseStatus;
        this.message = message;
    }

    public SubmissionResponseDto(String productId,
                                 SubmissionResponseStatus responseStatus,
                                 @Nullable String requestId,
                                 @Nullable OffsetDateTime expires,
                                 @Nullable String session,
                                 @Nullable String message) {
        this(productId, responseStatus, message);
        this.requestId = requestId;
        this.expires = expires;
        this.session = session;
    }

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
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(@Nullable String requestId) {
        this.requestId = requestId;
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
        return productId.equals(that.productId)
               && responseStatus == that.responseStatus
               && Objects.equals(requestId,
                                 that.requestId)
               && Objects.equals(expires, that.expires)
               && Objects.equals(session, that.session)
               && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, responseStatus, requestId, expires, session, message);
    }

    @Override
    public String toString() {
        return "SubmissionResponseDto{"
               + "productId='"
               + productId
               + '\''
               + ", responseStatus="
               + responseStatus
               + ", requestId='"
               + requestId
               + '\''
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
