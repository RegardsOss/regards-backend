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
package fr.cnes.regards.modules.order.dto.output;

import org.hibernate.validator.constraints.Length;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Indicates the progress of a {@link fr.cnes.regards.modules.order.dto.input.OrderRequestDto}
 *
 * @author Iliana Ghazali
 **/
public class OrderRequestResponseDto {

    /**
     * Order identifier
     */
    @NotBlank(message = "correlationId must be present")
    @Length(message = "correlationId must not exceed 100 characters.", max = 100)
    private String correlationId;

    @NotNull(message = "status should be present")
    private OrderRequestStatus status;

    @Nullable
    private String message;

    @Nullable
    private String downloadLink;

    public OrderRequestResponseDto(String correlationId,
                                   OrderRequestStatus status,
                                   @Nullable String message,
                                   @Nullable String downloadLink) {
        this.correlationId = correlationId;
        this.status = status;
        this.message = message;
        this.downloadLink = downloadLink;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public OrderRequestStatus getStatus() {
        return status;
    }

    public void setStatus(OrderRequestStatus status) {
        this.status = status;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public void setMessage(@Nullable String message) {
        this.message = message;
    }

    @Nullable
    public String getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(@Nullable String downloadLink) {
        this.downloadLink = downloadLink;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderRequestResponseDto that = (OrderRequestResponseDto) o;
        return Objects.equals(correlationId, that.correlationId)
               && status == that.status
               && Objects.equals(message,
                                 that.message)
               && Objects.equals(downloadLink, that.downloadLink);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationId, status, message, downloadLink);
    }

    @Override
    public String toString() {
        return "OrderRequestResponseDto{"
               + "correlationId='"
               + correlationId
               + '\''
               + ", status="
               + status
               + ", message='"
               + message
               + '\''
               + ", downloadLink='"
               + downloadLink
               + '\''
               + '}';
    }
}
