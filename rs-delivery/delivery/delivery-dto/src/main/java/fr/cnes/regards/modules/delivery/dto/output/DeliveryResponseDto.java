/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.dto.output;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;

import jakarta.annotation.Nullable;

import jakarta.validation.constraints.NotBlank;

import java.util.Objects;
import java.util.Optional;

/**
 * Response to indicate the status of a delivery request.
 *
 * @author Iliana Ghazali
 **/
public class DeliveryResponseDto {

    /**
     * Identifier provided by the user. Used to monitor the request.
     */
    @NotBlank(message = "correlationId is mandatory")
    private final String correlationId;

    /**
     * Delivery status
     */
    @NotBlank
    private final DeliveryRequestStatus status;

    /**
     * Filled if an error occurred during the processing of the delivery request
     */
    @Nullable
    private final DeliveryErrorType errorType;

    /**
     * Optional message to explain status
     */
    @Nullable
    private final String message;

    /**
     * Contains S3 downloadable link if delivery has ended in success
     */
    @Nullable
    private final String url;

    /**
     * Contains file checksum indicated by url if the delivery has ended in success
     */
    @Nullable
    private final String md5;

    /**
     * Metadata to identify the technical requester. Field not serialized.
     */
    @GsonIgnore
    @NotBlank
    private final String originRequestAppId;

    /**
     * Order of priority to process request
     */
    @GsonIgnore
    private final Integer originRequestPriority;

    public DeliveryResponseDto(String correlationId,
                               DeliveryRequestStatus status,
                               @Nullable DeliveryErrorType errorType,
                               @Nullable String message,
                               @Nullable String url,
                               @Nullable String md5,
                               String originRequestAppId,
                               Integer originRequestPriority) {
        this.correlationId = correlationId;
        this.status = status;
        this.errorType = errorType;
        this.message = message;
        this.url = url;
        this.md5 = md5;
        this.originRequestAppId = originRequestAppId;
        this.originRequestPriority = originRequestPriority;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public DeliveryRequestStatus getStatus() {
        return status;
    }

    @Nullable
    public DeliveryErrorType getErrorType() {
        return errorType;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    @Nullable
    public String getMd5() {
        return md5;
    }

    public Optional<String> getOriginRequestAppId() {
        return Optional.ofNullable(originRequestAppId);
    }

    public Optional<Integer> getOriginRequestPriority() {
        return Optional.ofNullable(originRequestPriority);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (DeliveryResponseDto) obj;
        return Objects.equals(this.correlationId, that.correlationId)
               && Objects.equals(this.status, that.status)
               && Objects.equals(this.errorType, that.errorType)
               && Objects.equals(this.message, that.message)
               && Objects.equals(this.url, that.url)
               && Objects.equals(this.md5, that.md5)
               && Objects.equals(this.originRequestAppId, that.originRequestAppId)
               && Objects.equals(this.originRequestPriority, that.originRequestPriority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationId,
                            status,
                            errorType,
                            message,
                            url,
                            md5,
                            originRequestAppId,
                            originRequestPriority);
    }

    @Override
    public String toString() {
        return "DeliveryResponseDto["
               + "correlationId="
               + correlationId
               + ", "
               + "status="
               + status
               + ", "
               + "errorType="
               + errorType
               + ", "
               + "message="
               + message
               + ", "
               + "url="
               + url
               + ", "
               + "md5="
               + md5
               + ", "
               + "originRequestAppId="
               + originRequestAppId
               + ", "
               + "originRequestPriority="
               + originRequestPriority
               + ']';
    }

}