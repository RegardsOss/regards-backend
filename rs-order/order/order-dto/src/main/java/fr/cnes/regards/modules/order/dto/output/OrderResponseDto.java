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

import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Indicates the progress of a {@link fr.cnes.regards.modules.order.dto.input.OrderRequestDto}
 *
 * @author Iliana Ghazali
 **/
public class OrderResponseDto {

    @NotNull(message = "status should be present")
    private OrderRequestStatus status;

    @Nullable
    private Long orderId;

    @Nullable
    private String correlationId;

    @Nullable
    private String message;

    @Nullable
    private String downloadLink;

    public OrderResponseDto(OrderRequestStatus status,
                            @Nullable Long orderId,
                            @Nullable String correlationId,
                            @Nullable String message,
                            @Nullable String downloadLink) {
        this.status = status;
        this.orderId = orderId;
        this.correlationId = correlationId;
        this.message = message;
        this.downloadLink = downloadLink;
    }

    public OrderRequestStatus getStatus() {
        return status;
    }

    public void setStatus(OrderRequestStatus status) {
        this.status = status;
    }

    @Nullable
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(@Nullable Long orderId) {
        this.orderId = orderId;
    }

    @Nullable
    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(@Nullable String correlationId) {
        this.correlationId = correlationId;
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
        OrderResponseDto that = (OrderResponseDto) o;
        return status == that.status
               && Objects.equals(orderId, that.orderId)
               && Objects.equals(correlationId,
                                 that.correlationId)
               && Objects.equals(message, that.message)
               && Objects.equals(downloadLink, that.downloadLink);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, orderId, correlationId, message, downloadLink);
    }

    @Override
    public String toString() {
        return "OrderRequestResponseDto{"
               + "status="
               + status
               + ", createdOrderId="
               + orderId
               + ", correlationId='"
               + correlationId
               + '\''
               + ", message='"
               + message
               + '\''
               + ", downloadLink='"
               + downloadLink
               + '\''
               + '}';
    }
}
