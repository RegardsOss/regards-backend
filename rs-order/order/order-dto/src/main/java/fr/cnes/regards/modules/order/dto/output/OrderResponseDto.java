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

import fr.cnes.regards.modules.order.dto.OrderErrorCode;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Indicates the response/progress of a {@link fr.cnes.regards.modules.order.dto.input.OrderRequestDto}
 *
 * @author Iliana Ghazali
 **/
public class OrderResponseDto {

    @NotNull(message = "status should be present")
    private OrderRequestStatus status;

    @Nullable
    private Long orderId;

    @NotNull(message = "correlation identifier should be present")
    private String correlationId;

    @Nullable
    private String message;

    @Nullable
    private String downloadLink;

    @Nullable
    private OrderErrorCode errorCode;

    /**
     * Number of errors of order or sub-order
     */
    @Nullable
    private Integer errors;

    @Nullable
    private Integer totalSubOrders;

    @Nullable
    private Long subOrderId;

    public OrderResponseDto(OrderRequestStatus status,
                            @Nullable Long orderId,
                            String correlationId,
                            @Nullable String message,
                            @Nullable String downloadLink,
                            @Nullable OrderErrorCode errorCode,
                            @Nullable Integer errors,
                            @Nullable Integer totalSubOrders,
                            @Nullable Long subOrderId) {
        this.status = status;
        this.orderId = orderId;
        this.correlationId = correlationId;
        this.message = message;
        this.downloadLink = downloadLink;
        this.errorCode = errorCode;
        this.errors = errors;
        this.totalSubOrders = totalSubOrders;
        this.subOrderId = subOrderId;
    }

    public static OrderResponseDto buildErrorResponse(OrderRequestDto orderRequest,
                                                      String message,
                                                      OrderRequestStatus responseStatus,
                                                      OrderErrorCode errorCode) {
        return new OrderResponseDto(responseStatus,
                                    null,
                                    orderRequest.getCorrelationId(),
                                    message,
                                    null,
                                    errorCode,
                                    null,
                                    null,
                                    null);
    }

    public static OrderResponseDto buildSuccessResponse(OrderRequestDto orderRequest,
                                                        Long createdOrderId,
                                                        OrderRequestStatus responseStatus) {
        return new OrderResponseDto(responseStatus,
                                    createdOrderId,
                                    orderRequest.getCorrelationId(),
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null);
    }

    public OrderRequestStatus getStatus() {
        return status;
    }

    @Nullable
    public Long getOrderId() {
        return orderId;
    }

    @Nullable
    public String getCorrelationId() {
        return correlationId;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public String getDownloadLink() {
        return downloadLink;
    }

    @Nullable
    public OrderErrorCode getErrorCode() {
        return errorCode;
    }

    @Nullable
    public Integer getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        if (this.errors == null) {
            return false;
        }
        return this.errors > 0;
    }

    @Nullable
    public Integer getTotalSubOrders() {
        return totalSubOrders;
    }

    @Nullable
    public Long getSubOrderId() {
        return subOrderId;
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
        return "OrderResponseDto{"
               + "status="
               + status
               + ", orderId="
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
               + ", errorCode="
               + errorCode
               + ", errors="
               + errors
               + ", totalSubOrders="
               + totalSubOrders
               + ", subOrderId="
               + subOrderId
               + '}';
    }
}
