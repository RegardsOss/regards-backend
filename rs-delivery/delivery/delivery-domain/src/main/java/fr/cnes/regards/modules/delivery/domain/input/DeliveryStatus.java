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
package fr.cnes.regards.modules.delivery.domain.input;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Status of a {@link DeliveryRequest}.
 *
 * @author Iliana Ghazali
 **/
@Embeddable
public class DeliveryStatus {

    @Column(name = "creation_date", nullable = false, updatable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    @NotNull(message = "creationDate is required")
    private OffsetDateTime creationDate;

    @Column(name = "expiry_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    @NotNull(message = "expiryDate is required")
    private OffsetDateTime expiryDate;

    @Column(name = "status_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    @NotNull(message = "statusDate is required")
    private OffsetDateTime statusDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "status is required")
    private DeliveryRequestStatus status;

    @Column(name = "error_cause")
    @Nullable
    private String errorCause;

    @Column(name = "error_type")
    @Enumerated(EnumType.STRING)
    @Nullable
    private DeliveryErrorType errorType;

    public DeliveryStatus() {
        // no-args constructor for jpa
    }

    public DeliveryStatus(OffsetDateTime creationDate,
                          OffsetDateTime statusDate,
                          int requestExpiresInHour,
                          DeliveryRequestStatus status,
                          @Nullable String errorCause,
                          @Nullable DeliveryErrorType errorType) {
        Assert.notNull(creationDate, "creationDate is mandatory !");
        Assert.notNull(statusDate, "statusDate is mandatory !");
        Assert.notNull(status, "status is mandatory !");

        this.creationDate = creationDate;
        this.statusDate = statusDate;
        this.expiryDate = creationDate.plusHours(requestExpiresInHour);
        this.status = status;
        this.errorCause = errorCause;
        this.errorType = errorType;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public OffsetDateTime getExpiryDate() {
        return expiryDate;
    }

    public OffsetDateTime getStatusDate() {
        return statusDate;
    }

    public DeliveryRequestStatus getStatus() {
        return status;
    }

    @Nullable
    public String getErrorCause() {
        return errorCause;
    }

    @Nullable
    public DeliveryErrorType getErrorType() {
        return errorType;
    }

    public void setExpiryDate(OffsetDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public void setStatusDate(OffsetDateTime statusDate) {
        this.statusDate = statusDate;
    }

    public void setStatus(DeliveryRequestStatus status) {
        this.status = status;
    }

    public void setError(DeliveryErrorType errorType, @Nullable String errorCause) {
        this.errorType = errorType;
        this.errorCause = errorCause;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeliveryStatus that = (DeliveryStatus) o;
        return creationDate.equals(that.creationDate)
               && expiryDate.equals(that.expiryDate)
               && statusDate.equals(that.statusDate)
               && status == that.status
               && Objects.equals(errorCause, that.errorCause)
               && Objects.equals(errorType, that.errorType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(creationDate, expiryDate, statusDate, status, errorCause, errorType);
    }

    @Override
    public String toString() {
        return "DeliveryStatus{"
               + "creationDate="
               + creationDate
               + ", expiryDate="
               + expiryDate
               + ", statusDate="
               + statusDate
               + ", status="
               + status
               + ", errorCause='"
               + errorCause
               + '\''
               + ", errorType="
               + errorType
               + '}';
    }
}
